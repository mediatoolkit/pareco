package com.mediatoolkit.pareco.components;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.function.Predicate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Wither;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 04/11/2018
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FileFilter {

	private static final Predicate<Path> MATCH_ALL = path -> true;
	private static final Predicate<Path> MATCH_NONE = path -> false;

	@Wither(AccessLevel.PRIVATE)
	private final Predicate<Path> include;
	private final Predicate<Path> exclude;

	public boolean includes(Path path) {
		return include.test(path);
	}

	public boolean excludes(Path path) {
		return exclude.test(path);
	}

	public FileFilter withIncludeAll() {
		return withInclude(MATCH_ALL);
	}

	private static final FileFilter ACCEPT_ALL = new FileFilter(MATCH_ALL, MATCH_NONE);

	static FileFilter of(String rootDirectory, String includeGlob, String excludeGlob) {
		if (includeGlob == null && excludeGlob == null) {
			return ACCEPT_ALL;
		}
		Predicate<Path> include = includeGlob != null ? globFilter(rootDirectory, includeGlob) : MATCH_ALL;
		Predicate<Path> exclude = excludeGlob != null ? globFilter(rootDirectory, excludeGlob) : MATCH_NONE;
		return new FileFilter(include, exclude);
	}

	private static Predicate<Path> globFilter(String rootDirectory, String glob) {
		if (!rootDirectory.isEmpty()) {
			glob = rootDirectory + (rootDirectory.endsWith(File.separator) ? "" : File.separator) + glob;
		}
		PathMatcher pathMatcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
		return pathMatcher::matches;
	}

}
