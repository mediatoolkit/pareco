package com.mediatoolkit.pareco.components;

import java.io.File;
import org.springframework.stereotype.Component;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 05/11/2018
 */
@Component
public class PathOverlapAnalyzer {

	/**
	 * Paths are overlapping if any file can be child of {@code path1} and {@code path2}
	 * at the same time. This will happen if and only if one path is parent to the other
	 * or if they are equal.
	 *
	 * @param path1
	 * @param path2
	 * @return true if paths are overlapping, false otherwise
	 */
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
