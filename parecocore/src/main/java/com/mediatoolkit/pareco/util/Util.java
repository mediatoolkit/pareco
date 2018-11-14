package com.mediatoolkit.pareco.util;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import one.util.streamex.EntryStream;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 29/10/2018
 */
@UtilityClass
public class Util {

	public static <T, E extends Throwable> Consumer<T> uncheckedConsumerSneaky(
		CheckedConsumer<T, E> consumer
	) {
		return consumer;
	}

	public interface CheckedConsumer<T, E extends Throwable> extends Consumer<T> {

		void handle(T t) throws E;

		@SneakyThrows
		default void accept(T t) {
			handle(t);
		}
	}

	public static <T, E extends Throwable> Supplier<T> uncheckedSupplierSneaky(
		CheckedSupplier<T, E> supplier
	) {
		return supplier;
	}

	public interface CheckedSupplier<T, E extends Throwable> extends Supplier<T> {

		T supply() throws E;

		@SneakyThrows
		default T get() {
			return supply();
		}
	}

	public static <E extends Throwable> Runnable uncheckedRunnableSneaky(
		CheckedRunnable<E> runnable
	) {
		return runnable;
	}

	public interface CheckedRunnable<E extends Throwable> extends Runnable {

		void exec() throws E;

		@SneakyThrows
		default void run() {
			exec();
		}
	}

	public static void runIgnoreException(CheckedRunnable<?> runnable) {
		try {
			runnable.exec();
		} catch (Throwable ignore) {

		}
	}

	public static double divRound1d(long numerator, long denominator) {
		return round1d((double) numerator / denominator);
	}

	public static double round1d(double d) {
		return Math.round(d * 10) / 10.;
	}

	public static String fileSizePretty(double sizeBytes) {
		if (sizeBytes < 1024) {
			return sizeBytes + "B";
		}
		double sizeKb = sizeBytes / 1024.;
		if (sizeKb < 1024) {
			return round1d(sizeKb) + "kB";
		}
		double sizeMb = sizeKb / 1024.;
		if (sizeMb < 1024) {
			return round1d(sizeMb) + "MB";
		}
		double sizeGb = sizeMb / 1024.;
		if (sizeGb < 1024) {
			return round1d(sizeGb) + "GB";
		}
		double sizeTb = sizeGb / 1024.;
		return round1d(sizeTb) + "TB";
	}

	public static String durationPretty(Duration duration) {
		return durationPretty(
			duration, true, 2
		);
	}

	public static String durationPretty(Duration duration, boolean shortUnits, int numSignificant) {
		long secondsTotal = duration.getSeconds();
		long days = secondsTotal / (3600 * 24);
		long hours = (secondsTotal / 3600) % 24;
		long minutes = (secondsTotal / 60) % 60;
		long seconds = secondsTotal % 60;
		long millis = duration.toMillis() % 1000;
		return EntryStream
			.of(
				days, shortUnits ? "d" : (days > 1 ? "days" : "day"),
				hours, shortUnits ? "h" : (hours > 1 ? "hours" : "hour"),
				minutes, shortUnits ? "m" : (minutes > 1 ? "mins" : "min"),
				seconds, shortUnits ? "s" : (seconds > 1 ? "secs" : "sec"),
				millis, shortUnits ? "ms" : (millis > 1 ? "msecs" : "msec")
			)
			.dropWhile(entry -> entry.getKey() == 0 && !entry.getValue().startsWith("ms"))
			.limit(numSignificant)
			.join("")
			.joining(" ");
	}

	public static <T> T thisOrDefault(T val, T defaultIfNull) {
		return val == null ? defaultIfNull : val;
	}

}
