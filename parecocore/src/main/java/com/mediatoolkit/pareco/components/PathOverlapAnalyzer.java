package com.mediatoolkit.pareco.components;

import java.io.File;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 05/11/2018
 */
@Component
public class PathOverlapAnalyzer {

	public boolean pathsDoOverlap(
		String path1, String path2
	) {
		String p1 = new File(path1).toPath().toAbsolutePath().toString();
		String p2 = new File(path2).toPath().toAbsolutePath().toString();
		boolean p2ParentOfP1 = p1.startsWith(p2);
		boolean p1ParentOfP2 = p2.startsWith(p1);
		return p2ParentOfP1 || p1ParentOfP2;
	}
}
