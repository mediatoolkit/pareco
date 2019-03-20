package com.mediatoolkit.pareco;

import com.mediatoolkit.pareco.transfer.model.TransferTask;
import java.io.IOException;
import java.nio.file.attribute.PosixFilePermission;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 29/10/2018
 */
@Slf4j
public class IntegrationTest extends BaseIntegration {

	@Test
	public void uploadEmpty_then_nothingHappens() {
		DirContents empty = DirContents.newDir();
		TransferTestCase transferTestCase = TransferTestCase.builder()
			.transferTask(defaultTask)
			.sourceContents(empty)
			.destinationContents(empty)
			.expectedDestinationContents(empty)
			.build();
		evalTransferTestCase(transferTestCase);
	}

	@Test
	public void uploadOnlyDirectories_then_directoriesAreCreated() {
		DirContents empty = DirContents.newDir();
		DirContents srcDir = DirContents.newDir()
			.withDir("foo")
			.withDir("foo/bar")
			.withDir("baz");
		TransferTestCase transferTestCase = TransferTestCase.builder()
			.transferTask(defaultTask)
			.sourceContents(srcDir)
			.destinationContents(empty)
			.expectedDestinationContents(srcDir)
			.build();
		evalTransferTestCase(transferTestCase);
	}

	@Test
	public void uploadSingleNonExistingFile_then_fileIsFullyUploaded() throws IOException {
		String content = "I'm dummy content spreading across 2 chunks";
		DirContents empty = DirContents.newDir();
		DirContents srcDir = DirContents.newDir()
			.withFile("foo.txt", content);
		TransferTestCase transferTestCase = TransferTestCase.builder()
			.transferTask(defaultTask)
			.sourceContents(srcDir)
			.destinationContents(empty)
			.expectedDestinationContents(srcDir)
			.transferredFiles(1)
			.transferredBytes(content.getBytes("utf-8").length)
			.build();
		evalTransferTestCase(transferTestCase);
	}

	@Test
	public void singleExistingTotallyDifferentFile_then_fileIsFullyUploaded() {
		String content1 = "updated content of file";
		String content2 = "totally different text than src";
		DirContents srcDir = DirContents.newDir()
			.withFile("dummy.txt", content1);
		DirContents destDir = DirContents.newDir()
			.withFile("dummy.txt", content2);
		TransferTestCase transferTestCase = TransferTestCase.builder()
			.transferTask(defaultTask)
			.sourceContents(srcDir)
			.destinationContents(destDir)
			.expectedDestinationContents(srcDir)
			.transferredFiles(1)
			.transferredBytes(byteCountOf(content1))
			.build();
		evalTransferTestCase(transferTestCase);
	}

	@Test
	public void singleExistingFileWithOnlyOneChunkDifferent_then_fileIsPartiallyUploaded() {
		TransferTask task = defaultTask.withOptions(defaultTask.getOptions().withChunkSizeBytes(10));
		String content1 = "0123456789abcdefghij--ver1";
		String content2 = "0123456789abcdefghij--ver2";
		DirContents srcDir = DirContents.newDir()
			.withFile("dummy.txt", content1);
		DirContents destDir = DirContents.newDir()
			.withFile("dummy.txt", content2);
		TransferTestCase transferTestCase = TransferTestCase.builder()
			.transferTask(task)
			.sourceContents(srcDir)
			.destinationContents(destDir)
			.expectedDestinationContents(srcDir)
			.transferredFiles(1)
			.transferredBytes(byteCountOf("--ver1"))
			.skippedBytes(2 * 10)
			.build();
		evalTransferTestCase(transferTestCase);
	}

	@Test
	public void singleExistingEqualFile_then_fileIsSkipped() {
		String content = "dummy content which will be skipped";
		DirContents dir = DirContents.newDir()
			.withFile("forSkip.txt", content);
		TransferTestCase transferTestCase = TransferTestCase.builder()
			.transferTask(defaultTask)
			.sourceContents(dir)
			.destinationContents(dir)
			.expectedDestinationContents(dir)
			.skippedFiles(1)
			.skippedBytes(byteCountOf(content))
			.build();
		evalTransferTestCase(transferTestCase);
	}

	@Test
	public void whenHavingUnexpectedFiles_then_unexpectedAreDeleted() {
		TransferTask task = defaultTask.withOptions(defaultTask.getOptions().withDeleteUnexpected(true));
		String content = "x";
		DirContents srcDir = DirContents.newDir()
			.withFile("expected.txt", content);
		DirContents destDir = srcDir
			.withFile("unexpected.txt", content);
		TransferTestCase transferTestCase = TransferTestCase.builder()
			.transferTask(task)
			.sourceContents(srcDir)
			.destinationContents(destDir)
			.expectedDestinationContents(srcDir)
			.skippedFiles(1)
			.skippedBytes(1)
			.deletedFiles(1)
			.build();
		evalTransferTestCase(transferTestCase);
	}

	@Test
	public void whenHavingNotHavingUnexpectedFiles_then_nothingHappens() {
		TransferTask task = defaultTask.withOptions(defaultTask.getOptions().withDeleteUnexpected(true))
			.withInclude("expected*");
		String content = "x";
		DirContents srcDir = DirContents.newDir()
			.withFile("expected.txt", content);
		DirContents destDir = srcDir
			.withFile("unexpected.txt", content);
		TransferTestCase transferTestCase = TransferTestCase.builder()
			.transferTask(task)
			.sourceContents(srcDir)
			.destinationContents(destDir)
			.expectedDestinationContents(destDir)
			.skippedFiles(1)
			.skippedBytes(1)
			.build();
		evalTransferTestCase(transferTestCase);
	}

	@Test
	public void singleFileWithDifferentMetadata_then_metadataIsUpdated() {
		//round to whole second because some filesystems do not support millisecond precision
		long lastModified = 1000 * (System.currentTimeMillis() / 1000 - TimeUnit.DAYS.toSeconds(30));
		String content = "I'm executable file";
		DirContents srcDir = DirContents.newDir()
			.withFile("a.out", content, lastModified,
				PosixFilePermission.OWNER_READ,
				PosixFilePermission.GROUP_EXECUTE,
				PosixFilePermission.OTHERS_EXECUTE,
				PosixFilePermission.OWNER_EXECUTE
			);
		DirContents destDir = srcDir
			.withFile("a.out", content);
		TransferTestCase transferTestCase = TransferTestCase.builder()
			.transferTask(defaultTask)
			.sourceContents(srcDir)
			.destinationContents(destDir)
			.expectedDestinationContents(srcDir)
			.skippedFiles(1)
			.skippedBytes(byteCountOf(content))
			.build();
		evalTransferTestCase(transferTestCase);
	}

	@Test
	public void weirdCharsInFileName() {
		String content = "this is file content";
		DirContents srcDir = DirContents.newDir()
			.withFile("weird '\\name' [](){}=.?!#& kraj", content)
			.withDir("sub\\ ?&dir")
			.withFile("sub\\ ?&dir/file", "123");
		DirContents destDir = DirContents.newDir();
		TransferTestCase transferTestCase = TransferTestCase.builder()
			.transferTask(defaultTask)
			.sourceContents(srcDir)
			.destinationContents(destDir)
			.expectedDestinationContents(srcDir)
			.transferredFiles(2)
			.transferredBytes(byteCountOf(content) + "123".length())
			.build();
		evalTransferTestCase(transferTestCase);
	}
}
