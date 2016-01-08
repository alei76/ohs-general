package ohs.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;

import ohs.utils.ByteSize.Type;

public class MemoryChecker {

	public static void main(String[] args) {
		System.out.println("process begins.");
		MemoryChecker c = new MemoryChecker();
		System.out.println(c.getUsedSize().size());

		Set<Integer> set = new HashSet<Integer>();

		for (int i = 0; i < 10000; i++) {
			set.add(i);
		}

		c.check();
		System.out.println(c.getUsedSize().size());

		System.out.println("process ends.");
	}

	private ByteSize total;

	private ByteSize max;

	private ByteSize free;

	private ByteSize used;

	private ByteSize used_heap;

	public MemoryChecker() {
		check();
	}

	public void check() {
		Runtime rt = Runtime.getRuntime();
		total = new ByteSize(rt.totalMemory());
		free = new ByteSize(rt.freeMemory());
		max = new ByteSize(rt.maxMemory());
		used = new ByteSize(total.size() - free.size());

		MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
		MemoryUsage usage = bean.getHeapMemoryUsage();
		used_heap = new ByteSize(usage.getUsed());
	}

	public ByteSize getFreeSize() {
		return free;
	}

	public ByteSize getMaxSize() {
		return max;
	}

	public ByteSize getTotalSize() {
		return total;
	}

	public ByteSize getUsedHeapSize() {
		return used_heap;
	}

	public ByteSize getUsedSize() {
		return used;
	}

	public String toString() {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(1);

		StringBuffer sb = new StringBuffer();
		sb.append(String.format("Max:\t%s MB", nf.format(max.size(Type.MEGA))));
		sb.append(String.format("\nTotal:\t%s MB", nf.format(total.size(Type.MEGA))));
		sb.append(String.format("\nFree:\t%s MB", nf.format(free.size(Type.MEGA))));
		sb.append(String.format("\nUsed:\t%s MB", nf.format(used.size(Type.MEGA))));
		sb.append(String.format("\nUsed Heap:\t%s MB", nf.format(used_heap.size(Type.MEGA))));
		return sb.toString();
	}
}
