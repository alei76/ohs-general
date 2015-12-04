package ohs.utils;

public class ByteSize {

	private long bytes = 0;

	private double kilo_bytes = 0;

	private double mega_bytes = 0;

	private double giga_bytes = 0;

	private double tera_bytes = 0;

	private double peta_bytes = 0;

	private double exa_bytes = 0;

	private double zetta_bytes = 0;

	private double yotta_bytes = 0;

	public ByteSize(long bytes) {
		this.bytes = bytes;
		compute();
	}

	private void compute() {
		final double denominator = 1024;

		kilo_bytes = 1f * bytes / denominator;
		mega_bytes = kilo_bytes / denominator;
		giga_bytes = mega_bytes / denominator;
		tera_bytes = giga_bytes / denominator;
		peta_bytes = tera_bytes / denominator;
		exa_bytes = peta_bytes / denominator;
		zetta_bytes = exa_bytes / denominator;
		yotta_bytes = zetta_bytes / denominator;
	}

	public long getBytes() {
		return bytes;
	}

	public double getExaBytes() {
		return exa_bytes;
	}

	public double getGigaBytes() {
		return giga_bytes;
	}

	public double getKiloBytes() {
		return kilo_bytes;
	}

	public double getMegaBytes() {
		return mega_bytes;
	}

	public double getPetaBytes() {
		return peta_bytes;
	}

	public double getTeraBytes() {
		return tera_bytes;
	}

	public double getYottaBytes() {
		return yotta_bytes;
	}

	public double getZettaBytes() {
		return zetta_bytes;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[%d] BTs", bytes));
		sb.append(String.format("\n[%f] KBs", kilo_bytes));
		sb.append(String.format("\n[%f] MBs", mega_bytes));
		sb.append(String.format("\n[%f] GBs", giga_bytes));
		sb.append(String.format("\n[%f] TBs", tera_bytes));
		sb.append(String.format("\n[%f] PBs", peta_bytes));
		sb.append(String.format("\n[%f] EBs", exa_bytes));
		sb.append(String.format("\n[%f] ZBs", zetta_bytes));
		sb.append(String.format("\n[%f] YBs", yotta_bytes));
		return sb.toString();
	}
}
