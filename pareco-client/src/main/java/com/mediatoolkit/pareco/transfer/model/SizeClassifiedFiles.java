package com.mediatoolkit.pareco.transfer.model;

import com.mediatoolkit.pareco.model.FileMetadata;
import java.util.List;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 01/11/2018
 */
@Value
@Builder
public class SizeClassifiedFiles {

	@Singular
	private List<FileMetadata> smallFiles;
	@Singular
	private List<FileMetadata> bigFiles;
}
