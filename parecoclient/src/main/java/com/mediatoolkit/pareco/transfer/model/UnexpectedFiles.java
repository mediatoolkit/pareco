package com.mediatoolkit.pareco.transfer.model;

import com.mediatoolkit.pareco.model.FilePath;
import java.util.List;
import lombok.Value;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 01/11/2018
 */
@Value
public class UnexpectedFiles {

	private List<FilePath> files;
	private List<FilePath> directories;
	private List<FilePath> all;
}
