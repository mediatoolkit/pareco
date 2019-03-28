package com.mediatoolkit.pareco.components;

import com.mediatoolkit.pareco.exceptions.NotDirectoryException;
import com.mediatoolkit.pareco.model.DirectoryMetadata;
import com.mediatoolkit.pareco.model.DirectoryStructure;
import com.mediatoolkit.pareco.model.FileMetadata;
import com.mediatoolkit.pareco.model.FilePath;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import one.util.streamex.StreamEx;
import static org.assertj.core.api.Assertions.assertThat;
import org.assertj.core.util.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DirectoryStructureReaderTest {

	private final File root = new File("testTmpDir");
	private final DirectoryStructureReader reader = new DirectoryStructureReader(
		new DirectoryLister(), new MetadataReader()
	);

	@Before
	public void setUp() {
		Files.delete(root);
		root.mkdirs();
	}

	@After
	public void teadDown() {
		Files.delete(root);
	}

	@Test(expected = NotDirectoryException.class)
	public void testOnMissingDir() throws IOException {
		reader.readDirectoryStructure("missing", null, null);
	}

	@Test
	public void testOnEmptyDir() throws IOException {
		DirectoryStructure structure = reader.readDirectoryStructure(root.getName(), null, null);
		assertStructure(structure).isEmpty();
	}

	@Test
	public void testSingleFile() throws IOException {
		createFile(root, "foo.txt", "content");
		DirectoryStructure structure = reader.readDirectoryStructure(root.getName(), null, null);
		assertStructure(structure).hasNoDirectories().hasFiles("foo.txt");
	}

	@Test
	public void testSingleDir() throws IOException {
		createDir(root, "dir");
		DirectoryStructure structure = reader.readDirectoryStructure(root.getName(), null, null);
		assertStructure(structure).hasDirectories("dir").hasNoFiles();
	}

	@Test
	public void testMultipleFilesAndDirs() throws IOException {
		createDirsAndFiles();
		DirectoryStructure structure = reader.readDirectoryStructure(root.getName(), null, null);
		assertStructure(structure)
			.hasDirectories("dir1", "dir2", "dir3", "dir3/dir3X", "dir3/dir3Y")
			.hasFiles("fileA", "fileB", "dir1/file1A", "dir1/file1B", "dir2/file2A", "dir3/dir3X/file3XA");
	}

	@Test
	public void testIncludeFiles() throws IOException {
		createFile(root, "fileA.txt", "");
		createFile(root, "fileB.txt", "");
		createFile(root, "fileA.zip", "");
		createFile(root, "fileA.tar", "");
		DirectoryStructure structure1 = reader.readDirectoryStructure(root.getName(), "*.txt", null);
		assertStructure(structure1).hasNoDirectories().hasFiles("fileA.txt", "fileB.txt");
		DirectoryStructure structure2 = reader.readDirectoryStructure(root.getName(), "fileA.*", null);
		assertStructure(structure2).hasNoDirectories().hasFiles("fileA.txt", "fileA.zip", "fileA.tar");
		DirectoryStructure structure3 = reader.readDirectoryStructure(root.getName(), "fileC.*", null);
		assertStructure(structure3).isEmpty();
	}

	@Test
	public void testExcludeFiles() throws IOException {
		createFile(root, "fileA.txt", "");
		createFile(root, "fileB.txt", "");
		createFile(root, "fileA.zip", "");
		createFile(root, "fileA.tar", "");
		DirectoryStructure structure1 = reader.readDirectoryStructure(root.getName(), null, "*.txt");
		assertStructure(structure1).hasNoDirectories().hasFiles("fileA.zip", "fileA.tar");
		DirectoryStructure structure2 = reader.readDirectoryStructure(root.getName(), null, "fileA.*");
		assertStructure(structure2).hasNoDirectories().hasFiles("fileB.txt");
		DirectoryStructure structure3 = reader.readDirectoryStructure(root.getName(), null, "fileC.*");
		assertStructure(structure3).hasNoDirectories().hasFiles("fileA.txt", "fileB.txt", "fileA.zip", "fileA.tar");
	}

	@Test
	public void testIncludeExcludeFiles() throws IOException {
		createFile(root, "fileA.txt", "");
		createFile(root, "fileB.txt", "");
		createFile(root, "fileA.zip", "");
		createFile(root, "fileA.tar", "");
		DirectoryStructure structure = reader.readDirectoryStructure(root.getName(), "*.txt", "fileA.*");
		assertStructure(structure).hasNoDirectories().hasFiles("fileB.txt");
	}

	@Test
	public void testIncludeDir() throws IOException {
		createDirsAndFiles();
		DirectoryStructure structure = reader.readDirectoryStructure(root.getName(), "dir1", null);
		assertStructure(structure)
			.hasDirectories("dir1")
			.hasFiles("dir1/file1A", "dir1/file1B");
	}

	@Test
	public void testIncludeNestedFile() throws IOException {
		createDirsAndFiles();
		DirectoryStructure structure = reader.readDirectoryStructure(root.getName(), "**file*A", null);
		assertStructure(structure)
			.hasDirectories("dir1", "dir2", "dir3", "dir3/dir3X")
			.hasFiles("fileA", "dir1/file1A", "dir2/file2A", "dir3/dir3X/file3XA");
	}

	@Test
	public void testExcludeDir() throws IOException {
		createDirsAndFiles();
		DirectoryStructure structure = reader.readDirectoryStructure(root.getName(), null, "dir3");
		assertStructure(structure)
			.hasDirectories("dir1", "dir2")
			.hasFiles("fileA", "fileB", "dir1/file1A", "dir1/file1B", "dir2/file2A");
	}

	@Test
	public void testExcludeNestedFile() throws IOException {
		createDirsAndFiles();
		DirectoryStructure structure = reader.readDirectoryStructure(root.getName(), null, "**file*A");
		assertStructure(structure)
			.hasDirectories("dir1", "dir2", "dir3", "dir3/dir3X", "dir3/dir3Y")
			.hasFiles("fileB", "dir1/file1B");
	}

	private void createDirsAndFiles() throws IOException {
		createDir(root, "dir1");
		createDir(root, "dir2");
		createDir(root, "dir3");
		createDir(root, "dir3/dir3X");
		createDir(root, "dir3/dir3Y");
		createFile(root, "fileA", "");
		createFile(root, "fileB", "");
		createFile(root, "dir1/file1A", "");
		createFile(root, "dir1/file1B", "");
		createFile(root, "dir2/file2A", "");
		createFile(root, "dir3/dir3X/file3XA", "");
	}


	void createDir(File root, String path) {
		File dir = new File(root.getPath()+File.separator+path);
		dir.mkdirs();
	}

	void createFile(File root, String path, String content) throws IOException {
		File file = new File(root.getPath() + File.separator + path);
		try (FileOutputStream out = new FileOutputStream(file)) {
			out.write(content.getBytes());
		}
	}

	AssertStructure assertStructure(DirectoryStructure structure) {
		return new AssertStructure(structure);
	}

	@AllArgsConstructor
	public static class AssertStructure {

		private final DirectoryStructure structure;

		public AssertStructure hasNoDirectories() {
			assertThat(structure.getDirectories()).isEmpty();
			return this;
		}

		public AssertStructure hasNoFiles() {
			assertThat(structure.getFiles()).isEmpty();
			return this;
		}

		public AssertStructure isEmpty() {
			hasNoDirectories();
			hasNoFiles();
			return this;
		}

		public AssertStructure hasFiles(String... files) {
			List<String> existingFiles = StreamEx.of(structure.getFiles())
				.map(FileMetadata::getFilePath)
				.map(FilePath::toRelativePath)
				.toList();
			assertThat(existingFiles).containsOnly(files);
			return this;
		}

		public AssertStructure hasDirectories(String... dirs) {
			List<String> existingFiles = StreamEx.of(structure.getDirectories())
				.map(DirectoryMetadata::getFilePath)
				.map(FilePath::toRelativePath)
				.toList();
			assertThat(existingFiles).containsOnly(dirs);
			return this;
		}

	}
}