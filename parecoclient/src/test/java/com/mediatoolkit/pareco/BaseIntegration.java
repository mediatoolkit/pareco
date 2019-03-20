package com.mediatoolkit.pareco;

import com.google.common.collect.Sets;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.model.DirectoryMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import com.mediatoolkit.pareco.progress.CompositeTransferProgressListener;
import com.mediatoolkit.pareco.progress.LoggingTransferProgressListener;
import com.mediatoolkit.pareco.progress.LoggingTransferProgressListener.LoggingFilter;
import com.mediatoolkit.pareco.progress.Speedometer;
import com.mediatoolkit.pareco.progress.StatsRecordingTransferProgressListener;
import com.mediatoolkit.pareco.progress.TransferProgressListener;
import com.mediatoolkit.pareco.transfer.download.DownloadTransferExecutor;
import com.mediatoolkit.pareco.transfer.model.ServerInfo;
import com.mediatoolkit.pareco.transfer.model.TransferOptions;
import com.mediatoolkit.pareco.transfer.model.TransferOptions.FileIntegrityOptions;
import com.mediatoolkit.pareco.transfer.model.TransferTask;
import com.mediatoolkit.pareco.transfer.upload.UploadTransferExecutor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Wither;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 29/10/2018
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
	classes = {ParecoServer.class, ParecoClient.class},
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = {
		"session.expire.max_inactive=10000"
	}
)
@ActiveProfiles("test")
public abstract class BaseIntegration {

	@LocalServerPort
	private int port;

	private ServerInfo serverInfo;
	private File baseTestWorkspace;
	TransferTask defaultTask;

	@Autowired
	private UploadTransferExecutor uploader;
	@Autowired
	private DownloadTransferExecutor downloader;

	BaseIntegration() {
		baseTestWorkspace = Files.newTemporaryFolder();
	}

	@PostConstruct
	public void init() {
		serverInfo = new ServerInfo("http", "localhost", port);
		defaultTask = TransferTask.builder()
			.localRootDirectory(null)
			.remoteRootDirectory(null)
			.include(null)
			.exclude(null)
			.serverInfo(serverInfo)
			.options(TransferOptions.builder()
				.chunkSizeBytes(20)
				.deleteUnexpected(false)
				.numTransferConnections(4)
				.fileIntegrityOptions(FileIntegrityOptions.metadataAndDigest(DigestType.CRC_32))
				.build()
			)
			.build();
	}

	@Before
	public void before() {
		delete(baseTestWorkspace);
		baseTestWorkspace.mkdir();
	}

	@After
	public void after() {
		delete(baseTestWorkspace);
	}

	void delete(File file) {
		Files.delete(file);
	}

	private String createRootDir(String dir) {
		String rootDir = baseTestWorkspace.toPath().toAbsolutePath().toString() + File.separator + dir;
		new File(rootDir).mkdirs();
		return rootDir;
	}

	@SneakyThrows
	void writeToFile(
		String rootDir, FilePath filePath, String content
	) {
		File dir = new File(rootDir + File.separator + filePath.getRelativeDirectory());
		dir.mkdirs();
		try (FileOutputStream out = new FileOutputStream(filePath.toAbsolutePath(rootDir))) {
			out.write(content.getBytes(Charset.forName("utf-8")));
		}
	}

	ProgressListener newProgressListener() {
		StatsRecordingTransferProgressListener statsProgressListener = new StatsRecordingTransferProgressListener();
		LoggingTransferProgressListener loggingProgressListener = LoggingTransferProgressListener.builder()
			.forceColors(true)
			.statsListener(statsProgressListener)
			.speedometer(new Speedometer())
			.loggingFilter(LoggingFilter.CHUNKS)
			.build();
		return ProgressListener.builder()
			.loggingProgressListener(loggingProgressListener)
			.statsProgressListener(statsProgressListener)
			.build();
	}

	@Value
	@Builder
	protected static class ProgressListener {

		private LoggingTransferProgressListener loggingProgressListener;
		private StatsRecordingTransferProgressListener statsProgressListener;

		public TransferProgressListener toCompositeListener() {
			return CompositeTransferProgressListener.of(StreamEx
				.of(statsProgressListener, loggingProgressListener)
				.nonNull()
				.toList()
			);
		}

	}

	protected void evalTransferTestCase(TransferTestCase transferTestCase) {
		testUpload(transferTestCase);
		testDownload(transferTestCase);
	}

	@SneakyThrows
	protected void testUpload(TransferTestCase testCase) {
		before();
		String localSrcDir = createRootDir("localSrc");
		String remoteDestDir = createRootDir("remoteDest");

		createDirContents(localSrcDir, testCase.getSourceContents());
		createDirContents(remoteDestDir, testCase.getDestinationContents());
		ProgressListener progressListener = newProgressListener();

		TransferTask transferTask = testCase.getTransferTask()
			.withLocalRootDirectory(localSrcDir)
			.withRemoteRootDirectory(remoteDestDir);
		uploader.executeUpload(transferTask, progressListener.toCompositeListener());

		StatsRecordingTransferProgressListener stats = progressListener.getStatsProgressListener();
		assertThat(stats.getTransferredFiles()).as("transferred files").isEqualTo(testCase.getTransferredFiles());
		assertThat(stats.getSkippedFiles()).as("skipped files").isEqualTo(testCase.getSkippedFiles());
		assertThat(stats.getTransferredBytes()).as("transferred bytes").isEqualTo(testCase.getTransferredBytes());
		assertThat(stats.getSkippedBytes()).as("skipped bytes").isEqualTo(testCase.getSkippedBytes());
		assertThat(stats.getDeletedFiles()).as("deleted files").isEqualTo(testCase.getDeletedFiles());
		assertThat(stats.getDeletedDirectories()).as("deleted directories").isEqualTo(testCase.getDeletedDirs());

		assertDirContents(localSrcDir, testCase.getSourceContents());
		assertDirContents(remoteDestDir, testCase.getExpectedDestinationContents());
	}

	@SneakyThrows
	protected void testDownload(TransferTestCase testCase) {
		before();
		String localDestDir = createRootDir("localDest");
		String remoteSrcDir = createRootDir("remoteSrcDest");

		createDirContents(localDestDir, testCase.getDestinationContents());
		createDirContents(remoteSrcDir, testCase.getSourceContents());
		ProgressListener progressListener = newProgressListener();

		TransferTask transferTask = testCase.getTransferTask()
			.withLocalRootDirectory(localDestDir)
			.withRemoteRootDirectory(remoteSrcDir);
		downloader.executeDownload(transferTask, progressListener.toCompositeListener());

		StatsRecordingTransferProgressListener stats = progressListener.getStatsProgressListener();
		assertThat(stats.getTransferredFiles()).as("transferred files").isEqualTo(testCase.getTransferredFiles());
		assertThat(stats.getSkippedFiles()).as("skipped files").isEqualTo(testCase.getSkippedFiles());
		assertThat(stats.getTransferredBytes()).as("transferred bytes").isEqualTo(testCase.getTransferredBytes());
		assertThat(stats.getSkippedBytes()).as("skipped bytes").isEqualTo(testCase.getSkippedBytes());
		assertThat(stats.getDeletedFiles()).as("deleted files").isEqualTo(testCase.getDeletedFiles());
		assertThat(stats.getDeletedDirectories()).as("deleted directories").isEqualTo(testCase.getDeletedDirs());

		assertDirContents(remoteSrcDir, testCase.getSourceContents());
		assertDirContents(localDestDir, testCase.getExpectedDestinationContents());
	}

	@SneakyThrows
	private void assertDirContents(String rootDir, DirContents dirContents) {
		Path rootPath = new File(rootDir).toPath();
		String absoluteRootPath = rootPath.toAbsolutePath().toString();
		java.nio.file.Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.toString().equals(absoluteRootPath)) {
					return super.preVisitDirectory(dir, attrs);
				}
				FilePath dirFilePath = filePathOfPath(dir);
				DirectoryMetadata dirMetadata = dirContents.getDirs().get(dirFilePath);
				if (dirMetadata == null) {
					fail("Did not expect to have directory: " + dir + " present in root dir: " + rootDir);
				}
				if (dirMetadata.getPermissions() != null) {
					try {
						Set<PosixFilePermission> currentPermissions = java.nio.file.Files.getPosixFilePermissions(dir);
						assertThat(currentPermissions).as("Permissions of dir: " + dir).containsOnlyElementsOf(dirMetadata.getPermissions());
					} catch (UnsupportedOperationException ignore) {
					}
				}
				return super.preVisitDirectory(dir, attrs);
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				FilePath filePath = filePathOfPath(file);
				FileContent fileContent = dirContents.getFiles().get(filePath);
				if (fileContent == null) {
					fail("Did not expect to have file: " + file + " present in root dir: " + rootDir);
				}
				String content = Files.contentOf(file.toFile(), Charset.forName("utf-8"));
				assertThat(content).as("Content of: " + file).isEqualTo(fileContent.getContent());
				if (fileContent.getLastModified() != null) {
					assertThat(attrs.lastModifiedTime()).as("Last modified time of: " + file).isEqualTo(FileTime.fromMillis(fileContent.getLastModified()));
				}
				if (fileContent.getPermissions() != null) {
					try {
						Set<PosixFilePermission> currentPermissions = java.nio.file.Files.getPosixFilePermissions(file);
						assertThat(currentPermissions).as("Permissions of: " + file).containsOnlyElementsOf(fileContent.getPermissions());
					} catch (UnsupportedOperationException ignore) {
					}
				}
				return super.visitFile(file, attrs);
			}

			private FilePath filePathOfPath(Path path) {
				String pathStr = path.toString();
				String filePathStr = pathStr.substring(absoluteRootPath.length() + 1);
				return filePathOf(filePathStr);
			}

		});
	}

	@SneakyThrows
	private void createDirContents(String rootDir, DirContents dirContents) {
		//create directories
		for (DirectoryMetadata dir : dirContents.getDirs().values()) {
			File file = new File(dir.getFilePath().toAbsolutePath(rootDir));
			Path path = file.toPath();
			if (dir.getPermissions() != null) {
				try {
					java.nio.file.Files.setPosixFilePermissions(path, dir.getPermissions());
				} catch (UnsupportedOperationException ignore) {
				}
			}
		}
		//create files
		for (FileContent fileContent : dirContents.getFiles().values()) {
			writeToFile(rootDir, fileContent.getPath(), fileContent.getContent());
			File file = new File(fileContent.getPath().toAbsolutePath(rootDir));
			Path path = file.toPath();
			if (fileContent.getPermissions() != null) {
				try {
					java.nio.file.Files.setPosixFilePermissions(path, fileContent.getPermissions());
				} catch (UnsupportedOperationException ignore) {
				}
			}
			if (fileContent.getLastModified() != null) {
				file.setLastModified(fileContent.getLastModified());
			}
		}
	}

	@SneakyThrows
	protected int byteCountOf(String of) {
		return of.getBytes(Charset.forName("utf-8")).length;
	}

	@Value
	@Builder
	@Wither
	protected static class TransferTestCase {

		//given
		@NonNull
		private final DirContents sourceContents;
		@NonNull
		private final DirContents destinationContents;

		@NonNull
		private TransferTask transferTask;

		//expected
		private int transferredFiles;
		private int skippedFiles;
		private long transferredBytes;
		private long skippedBytes;
		private int deletedFiles;
		private int deletedDirs;
		@NonNull
		private final DirContents expectedDestinationContents;

	}

	@Value
	protected static class DirContents {

		protected static DirContents newDir() {
			return new DirContents(new HashMap<>(), new HashMap<>());
		}

		private Map<FilePath, DirectoryMetadata> dirs;
		private Map<FilePath, FileContent> files;

		protected DirContents withDir(String path) {
			PosixFilePermission[] permissions = null;
			return withDir(path, permissions);
		}

		protected DirContents withDir(String path, PosixFilePermission... permissions) {
			FilePath dirFilePath = filePathOf(path);
			checkHasDir(dirFilePath.getRelativeDirectory());
			DirectoryMetadata dir = DirectoryMetadata.builder()
				.filePath(dirFilePath)
				.permissions(permissions != null ? Sets.newHashSet(permissions) : null)
				.build();
			Map<FilePath, DirectoryMetadata> dirs = new HashMap<>(this.dirs);
			dirs.put(dirFilePath, dir);
			return new DirContents(dirs, files);
		}

		protected DirContents withoutDir(String path) {
			FilePath dirFilePath = filePathOf(path);
			if (!dirs.containsKey(dirFilePath)) {
				return this;
			}
			Map<FilePath, DirectoryMetadata> dirs = new HashMap<>(this.dirs);
			dirs.remove(dirFilePath);
			Map<FilePath, FileContent> files = EntryStream.of(this.files)
				.removeKeys(filePath -> filePath.getRelativeDirectory().startsWith(path))
				.toMap();
			return new DirContents(dirs, files);
		}

		protected DirContents withFile(
			String path, String content
		) {
			PosixFilePermission[] permissions = null;
			return withFile(path, content, null, permissions);
		}

		protected DirContents withFile(
			String path, String content, Long lastModified, PosixFilePermission... permissions
		) {
			FilePath filePath = filePathOf(path);
			checkHasDir(filePath.getRelativeDirectory());
			FileContent fileContent = FileContent.builder()
				.path(filePath)
				.content(content)
				.lastModified(lastModified)
				.permissions(permissions != null ? Sets.newHashSet(permissions) : null)
				.build();
			Map<FilePath, FileContent> files = new HashMap<>(this.files);
			files.put(filePath, fileContent);
			return new DirContents(dirs, files);
		}

		protected DirContents withoutFile(String path) {
			FilePath filePath = filePathOf(path);
			if (!files.containsKey(filePath)) {
				return this;
			}
			Map<FilePath, FileContent> files = new HashMap<>(this.files);
			files.remove(filePath);
			return new DirContents(dirs, files);
		}

		private void checkHasDir(String path) {
			if (path.isEmpty()) {
				return;
			}
			FilePath dirFilePath = filePathOf(path);
			if (!dirs.containsKey(dirFilePath)) {
				throw new IllegalStateException("No parent dir: " + path);
			}
		}

	}

	static FilePath filePathOf(String path) {
		int lastSeparatorIndex = path.lastIndexOf('/');
		String relativeDir, name;
		if (lastSeparatorIndex == -1) {
			relativeDir = "";
			name = path;
		} else {
			relativeDir = path.substring(0, lastSeparatorIndex);
			name = path.substring(lastSeparatorIndex + 1);
		}
		return FilePath.of(relativeDir, name);
	}

	@Value
	@Builder
	protected static class FileContent {

		private FilePath path;
		private String content;
		private Long lastModified;
		private Set<PosixFilePermission> permissions;

	}

}
