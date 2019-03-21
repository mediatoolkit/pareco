package com.mediatoolkit.pareco;

import com.google.common.collect.Sets;
import com.mediatoolkit.pareco.model.ChunkInfo;
import com.mediatoolkit.pareco.model.DigestType;
import com.mediatoolkit.pareco.model.DirectoryMetadata;
import com.mediatoolkit.pareco.model.DirectoryStructure;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Wither;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import org.assertj.core.api.SoftAssertions;
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
		"session.expire.max_inactive=10000",
		"session.auto-expire.enabled=false"
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

	static void delete(File file) {
		Files.delete(file);
	}

	private String createRootDir(String dir) {
		String rootDir = baseTestWorkspace.toPath().toAbsolutePath().toString() + File.separator + dir;
		new File(rootDir).mkdirs();
		return rootDir;
	}

	@SneakyThrows
	static void writeToFile(
		String rootDir, FilePath filePath, String content
	) {
		File dir = new File(rootDir + File.separator + filePath.getRelativeDirectory());
		dir.mkdirs();
		try (FileOutputStream out = new FileOutputStream(filePath.toAbsolutePath(rootDir))) {
			out.write(content.getBytes(Charset.forName("utf-8")));
		}
	}

	ProgressListener newProgressListener(
		TransferContext transferContext,
		InjectingActions injectingActions
	) {
		StatsRecordingTransferProgressListener statsProgressListener = new StatsRecordingTransferProgressListener();
		LoggingTransferProgressListener loggingProgressListener = LoggingTransferProgressListener.builder()
			.forceColors(true)
			.statsListener(statsProgressListener)
			.speedometer(new Speedometer())
			.loggingFilter(LoggingFilter.CHUNKS)
			.build();
		TransferListenerAction transferListenerAction = injectingActions != null
			? new TransferListenerAction(transferContext, injectingActions)
			: null;
		return ProgressListener.builder()
			.loggingProgressListener(loggingProgressListener)
			.statsProgressListener(statsProgressListener)
			.transferListenerAction(transferListenerAction)
			.build();
	}

	@Value
	@Builder
	protected static class ProgressListener {

		private LoggingTransferProgressListener loggingProgressListener;
		private StatsRecordingTransferProgressListener statsProgressListener;
		private TransferListenerAction transferListenerAction;

		public TransferProgressListener toCompositeListener() {
			return CompositeTransferProgressListener.of(StreamEx
				.of(statsProgressListener, loggingProgressListener, transferListenerAction)
				.nonNull()
				.toList()
			);
		}

	}

	protected void evalTransferTestCase(TransferTestCase transferTestCase) {
		testDownload(transferTestCase);
		testUpload(transferTestCase);
	}

	@SneakyThrows
	protected void testUpload(TransferTestCase testCase) {
		before();
		String localSrcDir = createRootDir("localSrc");
		String remoteDestDir = createRootDir("remoteDest");
		TransferContext transferContext = new TransferContext(localSrcDir, remoteDestDir);

		createDirContents(localSrcDir, testCase.getSourceContents());
		createDirContents(remoteDestDir, testCase.getDestinationContents());
		ProgressListener progressListener = newProgressListener(
			transferContext, testCase.injectingActions
		);

		TransferTask transferTask = testCase.getTransferTask()
			.withLocalRootDirectory(localSrcDir)
			.withRemoteRootDirectory(remoteDestDir);
		uploader.executeUpload(transferTask, progressListener.toCompositeListener());

		StatsRecordingTransferProgressListener stats = progressListener.getStatsProgressListener();
		SoftAssertions soft = new SoftAssertions();
		soft.assertThat(stats.getTransferredFiles()).as("transferred files").isEqualTo(testCase.getTransferredFiles());
		soft.assertThat(stats.getSkippedFiles()).as("skipped files").isEqualTo(testCase.getSkippedFiles());
		soft.assertThat(stats.getTransferredBytes()).as("transferred bytes").isEqualTo(testCase.getTransferredBytes());
		soft.assertThat(stats.getSkippedBytes()).as("skipped bytes").isEqualTo(testCase.getSkippedBytes());
		soft.assertThat(stats.getDeletedFiles()).as("deleted files").isEqualTo(testCase.getDeletedFiles());
		soft.assertThat(stats.getDeletedDirectories()).as("deleted directories").isEqualTo(testCase.getDeletedDirs());
		soft.assertThat(stats.getConcurrentDeletions()).as("concurrent deletions").isEqualTo(testCase.getDeletedConcurrently());
		assertDirContents(soft, localSrcDir, testCase.getSourceContents());
		assertDirContents(soft, remoteDestDir, testCase.getExpectedDestinationContents());
		soft.assertAll();
	}

	@SneakyThrows
	protected void testDownload(TransferTestCase testCase) {
		before();
		String localDestDir = createRootDir("localDest");
		String remoteSrcDir = createRootDir("remoteSrcDest");
		TransferContext transferContext = new TransferContext(remoteSrcDir, localDestDir);

		createDirContents(localDestDir, testCase.getDestinationContents());
		createDirContents(remoteSrcDir, testCase.getSourceContents());
		ProgressListener progressListener = newProgressListener(
			transferContext, testCase.injectingActions
		);

		TransferTask transferTask = testCase.getTransferTask()
			.withLocalRootDirectory(localDestDir)
			.withRemoteRootDirectory(remoteSrcDir);
		downloader.executeDownload(transferTask, progressListener.toCompositeListener());

		StatsRecordingTransferProgressListener stats = progressListener.getStatsProgressListener();
		SoftAssertions soft = new SoftAssertions();
		soft.assertThat(stats.getTransferredFiles()).as("transferred files").isEqualTo(testCase.getTransferredFiles());
		soft.assertThat(stats.getSkippedFiles()).as("skipped files").isEqualTo(testCase.getSkippedFiles());
		soft.assertThat(stats.getTransferredBytes()).as("transferred bytes").isEqualTo(testCase.getTransferredBytes());
		soft.assertThat(stats.getSkippedBytes()).as("skipped bytes").isEqualTo(testCase.getSkippedBytes());
		soft.assertThat(stats.getDeletedFiles()).as("deleted files").isEqualTo(testCase.getDeletedFiles());
		soft.assertThat(stats.getDeletedDirectories()).as("deleted directories").isEqualTo(testCase.getDeletedDirs());
		soft.assertThat(stats.getConcurrentDeletions()).as("concurrent deletions").isEqualTo(testCase.getDeletedConcurrently());
		assertDirContents(soft, remoteSrcDir, testCase.getSourceContents());
		assertDirContents(soft, localDestDir, testCase.getExpectedDestinationContents());
		soft.assertAll();
	}

	@SneakyThrows
	private void assertDirContents(SoftAssertions soft, String rootDir, DirContents dirContents) {
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
					soft.fail("Did not expect to have directory: " + dir + " present in root dir: " + rootDir);
				}
				if (dirMetadata.getPermissions() != null) {
					try {
						Set<PosixFilePermission> currentPermissions = java.nio.file.Files.getPosixFilePermissions(dir);
						soft.assertThat(currentPermissions).as("Permissions of dir: " + dir).containsOnlyElementsOf(dirMetadata.getPermissions());
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
					soft.fail("Did not expect to have file: " + file + " present in root dir: " + rootDir);
				}
				String content = Files.contentOf(file.toFile(), Charset.forName("utf-8"));
				soft.assertThat(content).as("Content of: " + file).isEqualTo(fileContent.getContent());
				if (fileContent.getLastModified() != null) {
					soft.assertThat(attrs.lastModifiedTime()).as("Last modified time of: " + file).isEqualTo(FileTime.fromMillis(fileContent.getLastModified()));
				}
				if (fileContent.getPermissions() != null) {
					try {
						Set<PosixFilePermission> currentPermissions = java.nio.file.Files.getPosixFilePermissions(file);
						soft.assertThat(currentPermissions).as("Permissions of: " + file).containsOnlyElementsOf(fileContent.getPermissions());
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
		private int deletedConcurrently;
		@NonNull
		private final DirContents expectedDestinationContents;
		private InjectingActions injectingActions;

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

	@Value
	protected class TransferContext {

		@NonNull
		private String srcRootDir;
		@NonNull
		private String dstRotDir;

		void deleteSrcFiles(String... filePaths) {
			for (String filePath : filePaths) {
				File file = new File(filePathOf(filePath).toAbsolutePath(srcRootDir));
				delete(file);
			}
		}

	}

	protected interface InjectingAction<ARGS> {

		void doAction(TransferContext ctx, ARGS args);
	}

	@Value
	@Builder
	protected static class InjectingActions {

		private final InjectingAction<InitializingArg> onInitializing;
		private final InjectingAction<StartedArg> onStarted;
		private final InjectingAction<List<FilePath>> onDeletedFiles;
		private final InjectingAction<List<FilePath>> onDeletedDirectories;
		private final InjectingAction<FilePath> onFileAnalyze;
		private final InjectingAction<FilePath> onFileSkipped;
		private final InjectingAction<FilePath> onFileStarted;
		private final InjectingAction<FilePath> onFileDeleted;
		private final InjectingAction<FilePath> onFileCompleted;
		private final InjectingAction<FileChunkArg> onChunkSkipped;
		private final InjectingAction<FileChunkProgressArg> onChunkTransferProgress;
		private final InjectingAction<FileChunkArg> onChunkTransferred;
		private final InjectingAction<Void> onCompleted;
		private final InjectingAction<Void> onAborted;
	}

	@Value
	protected class InitializingArg {
		String transferMode;
		String sourceRootDir;
		String destinationRootDir;
	}

	@Value
	protected class StartedArg {
		DirectoryStructure directoryStructure;
		long chunkSizeBytes;
	}

	@Value
	protected class FileChunkArg {
		FilePath filePath;
		ChunkInfo chunkInfo;
	}

	@Value
	protected class FileChunkProgressArg {
		FilePath filePath;
		ChunkInfo chunkInfo;
		long bytesTransferred;
	}

	@AllArgsConstructor
	protected class TransferListenerAction implements TransferProgressListener {

		@NonNull
		private final TransferContext transferContext;
		@NonNull
		private final InjectingActions injectingActions;

		private <ARG> void doAction(InjectingAction<ARG> injectingAction, ARG arg) {
			if (injectingAction == null) {
				return;
			}
			injectingAction.doAction(transferContext, arg);
		}

		@Override
		public void initializing(String transferMode, String sourceRootDir, String destinationRootDir) {
			doAction(injectingActions.onInitializing, new InitializingArg(transferMode, sourceRootDir, destinationRootDir));
		}

		@Override
		public void started(DirectoryStructure directoryStructure, long chunkSizeBytes) {
			doAction(injectingActions.onStarted, new StartedArg(directoryStructure, chunkSizeBytes));
		}

		@Override
		public void deletedFiles(List<FilePath> filePaths) {
			doAction(injectingActions.onDeletedFiles, filePaths);
		}

		@Override
		public void deletedDirectories(List<FilePath> filePaths) {
			doAction(injectingActions.onDeletedDirectories, filePaths);
		}

		@Override
		public void fileAnalyze(FilePath filePath) {
			doAction(injectingActions.onFileAnalyze, filePath);
		}

		@Override
		public void fileSkipped(FilePath filePath) {
			doAction(injectingActions.onFileSkipped, filePath);
		}

		@Override
		public void fileStarted(FilePath filePath) {
			doAction(injectingActions.onFileStarted, filePath);
		}

		@Override
		public void fileDeleted(FilePath filePath) {
			doAction(injectingActions.onFileDeleted, filePath);
		}

		@Override
		public void fileCompleted(FilePath filePath) {
			doAction(injectingActions.onFileCompleted, filePath);
		}

		@Override
		public void fileChunkSkipped(FilePath filePath, ChunkInfo chunkInfo) {
			doAction(injectingActions.onChunkSkipped, new FileChunkArg(filePath, chunkInfo));
		}

		@Override
		public void fileChunkTransferProgress(FilePath filePath, ChunkInfo chunkInfo, long bytesTransferred) {
			doAction(injectingActions.onChunkTransferProgress, new FileChunkProgressArg(filePath, chunkInfo, bytesTransferred));
		}

		@Override
		public void fileChunkTransferred(FilePath filePath, ChunkInfo chunkInfo) {
			doAction(injectingActions.onChunkTransferred, new FileChunkArg(filePath, chunkInfo));
		}

		@Override
		public void completed() {
			doAction(injectingActions.onCompleted, null);
		}

		@Override
		public void aborted() {
			doAction(injectingActions.onAborted, null);
		}
	}

}
