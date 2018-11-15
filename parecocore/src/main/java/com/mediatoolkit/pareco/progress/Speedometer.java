package com.mediatoolkit.pareco.progress;

/**
 * Speedometer is a thread-safe class for calculating "current" speed approximation.
 *
 * It relies on {@link TimeSource} to keep track of "time" passed by. Default time source is
 * {@link System#currentTimeMillis()}.
 *
 * Speedometer tracks speed of arbitrary value change which can be passed using method {@link #increment(double)}.
 *
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 30/10/2018
 */
public class Speedometer {

	private final TimeSource timeSource;
	private final double[] window;
	private final int granulationMs;
	private final int numSlots;
	private final long start;
	private final int startSlot;

	private int lastSlot = 0;

	/**
	 * Default constructor which uses {@link System#currentTimeMillis()} as {@link TimeSource}, granulation of
	 * {@literal 100ms} and number of {@literal 100} slots. These settings will smooth/average-out spikes and drops
	 * in speed in sliding window of 10 seconds.
	 */
	public Speedometer() {
		this(100, 100, System::currentTimeMillis);
	}

	/**
	 * Main constructor
	 *
	 * @param granulationMs how many milliseconds is between slots, lower the value faster the response in speed change
	 * @param numSlots how many slots in past to keep in memory, higher the value smoother the speed approximation,
	 *                    but higher memory consumption and more computation to calculate current speed
	 * @param timeSource to be used to get current time
	 */
	public Speedometer(int granulationMs, int numSlots, TimeSource timeSource) {
		if (granulationMs <= 0) {
			throw new IllegalArgumentException("Granulation must be >= 1, got: " + granulationMs);
		}
		if (numSlots <= 0) {
			throw new IllegalArgumentException("NumSlots must be >= 1, got: " + numSlots);
		}
		this.timeSource = timeSource;
		this.granulationMs = granulationMs;
		this.numSlots = numSlots;
		this.window = new double[numSlots];
		start = timeSource.getTimeMillis();
		startSlot = slotOf(start);
	}

	/**
	 * Method to which is main input of value changes for which speed will be measured.
	 *
	 * @param amount of increase in measured value change
	 */
	public void increment(double amount) {
		long time = timeSource.getTimeMillis();
		int slot = slotOf(time);
		doIncrement(amount, slot);
	}

	private int slotOf(long time) {
		return (int) ((time / granulationMs) % numSlots);
	}

	private synchronized void doIncrement(double amount, int slot) {
		for (int slotI = slot; slotI != lastSlot; slotI = (slotI - 1 + numSlots) % numSlots) {
			window[slotI] = 0;
		}
		window[slot] += amount;
		lastSlot = slot;
	}

	/**
	 * Method to calculate current speed based on previous invocations of {@link #increment(double)}.
	 * @return current speed value in #/sec units
	 */
	public double getSpeed() {
		increment(0);
		long now = timeSource.getTimeMillis();
		int maxValidSlot = (int) ((now - start) / granulationMs);
		double sum = 0;
		int n = 0;
		for (int i = 0; i < window.length && i <= maxValidSlot; i++) {
			n++;
			sum += window[(i + startSlot) % numSlots];
		}
		if (n == 0) {
			return 0;
		}
		return (1000. / granulationMs) * sum / n;
	}

	public interface TimeSource {

		long getTimeMillis();
	}

}
