package com.mediatoolkit.pareco.progress;

/**
 * @author Antonio Tomac, <antonio.tomac@mediatoolkit.com>
 * @since 30/10/2018
 */
public class Speedometer {

	private final double[] window;
	private final int granulationMs;
	private final int numSlots;
	private final long start;
	private final int startSlot;

	private int lastSlot = 0;

	public Speedometer() {
		this(100, 100);
	}

	public Speedometer(int granulationMs, int numSlots) {
		if (granulationMs <= 0) {
			throw new IllegalArgumentException("Granulation must be >= 1, got: " + granulationMs);
		}
		if (numSlots <= 0) {
			throw new IllegalArgumentException("NumSlots must be >= 1, got: " + numSlots);
		}
		this.granulationMs = granulationMs;
		this.numSlots = numSlots;
		this.window = new double[numSlots];
		start = System.currentTimeMillis();
		startSlot = slotOf(start);
	}

	public void increment(double amount) {
		long time = System.currentTimeMillis();
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

	public double getSpeed() {
		increment(0);
		long now = System.currentTimeMillis();
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

}
