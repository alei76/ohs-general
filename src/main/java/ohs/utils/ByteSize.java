package ohs.utils;

public class ByteSize {

	public static enum Type {
		BYTE, KILO, MEGA, GIGA, TERA
	}

	public static void main(String[] args) {
		long bytes = Long.MAX_VALUE;
		ByteSize bs = new ByteSize(bytes);

		System.out.println(bs.toString());
	}

	private long bytes = 0;

	private double[] sizes = new double[4];

	public ByteSize(long bytes) {
		this.bytes = bytes;

		final double denominator = 1024;
		sizes[0] = 1f * bytes / denominator;
		for (int i = 1; i < sizes.length; i++) {
			sizes[i] = sizes[i - 1] / denominator;
		}
	}

	public long size() {
		return bytes;
	}

	public double size(Type type) {
		return sizes[type.ordinal()];
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[%d] BTs", bytes));
		sb.append(String.format("\n[%.2f] KBs", sizes[0]));
		sb.append(String.format("\n[%.2f] MBs", sizes[1]));
		sb.append(String.format("\n[%.2f] GBs", sizes[2]));
		sb.append(String.format("\n[%.2f] TBs", sizes[3]));
		return sb.toString();
	}
}
