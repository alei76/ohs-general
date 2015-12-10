package ohs.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import ohs.math.ArrayUtils;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.utils.ByteSize;

/**
 * @author Heung-Seon Oh
 * @version 1.2
 * @date 2009. 5. 10
 * 
 */
public class IOUtils {

	public static final String UTF_8 = "UTF-8";

	public static final String EUC_KR = "euc-kr";

	private static void addFilesUnder(File root, List<File> files, boolean recursive) {
		if (root != null) {
			File[] children = root.listFiles();
			if (children != null) {
				for (File child : root.listFiles()) {
					if (child.isFile()) {
						files.add(child);
					} else {
						if (recursive) {
							addFilesUnder(child, files, recursive);
						}
					}
				}
			}
		}
	}

	public static File appendFileNameSuffix(File file, String suffix) {
		String filePath = getCanonicalPath(file);
		if (!filePath.endsWith(suffix)) {
			filePath += suffix;
			file = new File(filePath);
		}
		return file;
	}

	public static void copy(String inFileName, String outDirName) throws Exception {
		createFolders(outDirName);

		InputStream is = null;
		OutputStream os = null;
		is = new FileInputStream(inFileName);
		os = new FileOutputStream(outDirName);
		byte[] buffer = new byte[1024];
		int length;
		while ((length = is.read(buffer)) > 0) {
			os.write(buffer, 0, length);
		}
		is.close();
		os.close();
	}

	public static void copyFolder(String srcDir, String desDir) throws Exception {
		for (File inFile : getFilesUnder(srcDir)) {
			String path = inFile.getPath();
			path = path.replace(srcDir, desDir);
			copy(inFile.getPath(), path);
		}
	}

	public static int countLines(String fileName) throws Exception {
		int numLines = 0;

		BufferedReader reader = openBufferedReader(fileName);
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			} else {
				numLines++;
			}
		}
		reader.close();
		return numLines;
	}

	public static boolean create(File file) {
		if (file.exists()) {
			deleteFilesUnder(file);
		}
		return file.mkdirs();
	}

	public static void createFolders(String fileName) {
		String fileSeparator = System.getProperty("file.separator");

		if (fileSeparator.equals("\\")) {
			fileSeparator = "\\\\";
		}

		File file = new File(fileName);

		if (file.getPath().split(fileSeparator).length > 1) {
			File parentFile = new File(file.getParent());
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
		}
	}

	private static int deleteFiles(File root) {
		int numFiles = 0;
		if (root.exists()) {
			if (root.isDirectory()) {
				for (File child : root.listFiles()) {
					numFiles += deleteFiles(child);
				}
				root.delete();
			} else if (root.isFile()) {
				root.delete();
				numFiles++;
			}
		}
		return numFiles;
	}

	public static void deleteFilesUnder(File dir) {
		int numFiles = deleteFiles(dir);
		System.out.println(String.format("delete [%d] files under [%s]", numFiles, dir.getPath()));
	}

	public static void deleteFilesUnder(String dirName) {
		deleteFilesUnder(new File(dirName));
	}

	public static String getCanonicalPath(File file) {
		String ret = null;
		try {
			ret = file.getCanonicalPath();
			ret = ret.replace("\\", "/");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static String getExtension(String fileName) {
		int idx = fileName.lastIndexOf(".");
		if (idx > 0) {
			fileName = fileName.substring(idx + 1);
		}
		return fileName;
	}

	public static List<File> getFilesUnder(File dir) {
		return getFilesUnder(dir, true);
	}

	public static List<File> getFilesUnder(File dir, boolean recursive) {
		List<File> files = new ArrayList<File>();
		addFilesUnder(dir, files, recursive);
		// Collections.sort(files);

		long totalBytes = 0;

		for (int i = 0; i < files.size(); i++) {
			totalBytes += files.get(i).length();
		}

		ByteSize fs = new ByteSize(totalBytes);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(2);

		System.out.println(String.format("read [%d, %s GBs] files from [%s]", files.size(), nf.format(fs.getGigaBytes()), dir.getName()));
		return files;
	}

	public static List<File> getFilesUnder(String dirName) {
		return getFilesUnder(new File(dirName));
	}

	public static void main(String[] args) throws Exception {
		String text = readText("text8");
		System.out.println(text + "\n");

		BufferedWriter bw = openBufferedWriter("text8.gz");
		bw.write(text);
		bw.close();

		String text2 = readText("text8.gz");

		System.out.println(text2);
	}

	public static void move(String inFileName, String outDirName) throws Exception {
		copy(inFileName, outDirName);
		new File(inFileName).delete();
	}

	public static void moveFolder(String srcDir, String desDir) throws Exception {
		List<File> files = getFilesUnder(srcDir);
		for (File inputFile : files) {
			String path = inputFile.getPath();
			path = path.replace(srcDir, desDir);
			copy(inputFile.getPath(), path);
		}

		for (File file : files) {
			file.delete();
		}
	}

	public static BufferedReader openBufferedReader(String fileName) throws Exception {
		return openBufferedReader(fileName, UTF_8);
	}

	public static BufferedReader openBufferedReader(String fileName, String encoding) throws Exception {
		File file = new File(fileName);
		FileInputStream fis = new FileInputStream(file);
		InputStreamReader isr = null;

		if (file.getName().endsWith(".gz")) {
			CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP, fis);
			isr = new InputStreamReader(cis, encoding);
		} else if (file.getName().endsWith(".bz2")) {
			// byte[] ignoreBytes = new byte[2];
			// fis.read(ignoreBytes); // "B", "Z" bytes from commandline tools
			// ret = new BufferedReader(new InputStreamReader(new
			// CBZip2InputStream(fis)));
			CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.BZIP2, fis);
			isr = new InputStreamReader(cis, encoding);
		} else {
			isr = new InputStreamReader(fis, encoding);
		}
		return new BufferedReader(isr);
	}

	public static BufferedWriter openBufferedWriter(String fileName) throws Exception {
		return openBufferedWriter(fileName, UTF_8, false);
	}

	public static BufferedWriter openBufferedWriter(String fileName, String encoding, boolean append) throws Exception {
		String fileSeparator = System.getProperty("file.separator");

		if (fileSeparator.equals("\\")) {
			fileSeparator = "\\\\";
		}

		File file = new File(fileName);

		if (file.getPath().split(fileSeparator).length > 1) {
			File parentFile = new File(file.getParent());
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
		}

		if (!file.exists()) {
			append = false;
		}

		FileOutputStream fos = new FileOutputStream(file, append);
		OutputStreamWriter osw = null;

		if (file.getName().endsWith(".gz")) {
			// osw = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(file, append)), encoding);
			CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.GZIP, fos);
			osw = new OutputStreamWriter(cos, encoding);
		} else if (file.getName().endsWith(".bz2")) {
			// osw = new OutputStreamWriter(new CBZip2OutputStream(new
			// FileOutputStream(file, append)), encoding);
			CompressorOutputStream cos = new CompressorStreamFactory().createCompressorOutputStream(CompressorStreamFactory.BZIP2, fos);
			osw = new OutputStreamWriter(cos, encoding);
		} else {
			osw = new OutputStreamWriter(fos, encoding);
		}

		return new BufferedWriter(osw);
	}

	public static ObjectInputStream openObjectInputStream(String fileName) throws Exception {
		ObjectInputStream ret = null;
		if (fileName.endsWith(".gz")) {
			ret = new ObjectInputStream(new GZIPInputStream(new FileInputStream(fileName)));
		} else {
			ret = new ObjectInputStream(new FileInputStream(fileName));
		}
		return ret;
	}

	public static ObjectOutputStream openObjectOutputStream(String fileName) throws Exception {
		System.out.printf("write at [%s].\n", fileName);

		File file = new File(fileName);

		File parent = file.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}

		ObjectOutputStream ret = null;
		if (file.getName().endsWith(".gz")) {
			ret = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
		} else {
			ret = new ObjectOutputStream(new FileOutputStream(file));
		}

		return ret;
	}

	public static InputStreamReader openUrl(URL url) throws IOException {
		URLConnection urlConn = url.openConnection();
		urlConn.setRequestProperty("User-agent", "Mozilla/4.0");

		HttpURLConnection httpUrlConn = (HttpURLConnection) urlConn;
		httpUrlConn.setConnectTimeout(2000);
		int responseCode = httpUrlConn.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			return new InputStreamReader(urlConn.getInputStream(), "UTF-8");
		} else {
			throw new IOException();
		}
	}

	public static Counter<String> readCounter(String fileName) throws Exception {
		Counter<String> ret = new Counter<String>();
		String line = null;
		BufferedReader br = openBufferedReader(fileName);
		while ((line = br.readLine()) != null) {
			String[] parts = line.split("\t");
			ret.setCount(parts[0], Double.parseDouble(parts[1]));
		}
		br.close();
		System.out.printf("read a counter with [%d] entries from [%s]\n", ret.size(), fileName);
		return ret;
	}

	public static CounterMap<String, String> readCounterMap(String fileName) throws Exception {
		CounterMap<String, String> ret = new CounterMap<String, String>();
		String line = null;
		BufferedReader br = openBufferedReader(fileName);
		while ((line = br.readLine()) != null) {
			String[] parts = line.split("\t");
			String outerKey = parts[0].split(":")[0];

			for (int i = 1; i < parts.length; i++) {
				String[] two = parts[i].split(":");
				String innerKey = two[0];
				ret.setCount(two[0], innerKey, Double.parseDouble(two[1]));
			}
		}
		br.close();
		return ret;
	}

	public static double[] readDoubleArray(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		int num_nonzeros = ois.readInt();
		double[] ret = new double[size];

		if (size == num_nonzeros) {
			for (int i = 0; i < size; i++) {
				ret[i] = ois.readDouble();
			}
		} else {
			for (int i = 0; i < num_nonzeros; i++) {
				int index = ois.readInt();
				double value = ois.readDouble();
				ret[index] = value;
			}
		}
		return ret;
	}

	public static double[][] readDoubleMatrix(ObjectInputStream ois) throws Exception {
		int rowSize = ois.readInt();
		double[][] ret = new double[rowSize][];
		for (int i = 0; i < rowSize; i++) {
			ret[i] = readDoubleArray(ois);
		}
		return ret;
	}

	public static Indexer<String> readIndexer(ObjectInputStream ois) throws Exception {
		Indexer<String> ret = new Indexer<String>();
		int size = ois.readInt();
		for (int i = 0; i < size; i++) {
			String item = readString(ois);
			ret.add(item);
		}
		return ret;
	}

	public static Indexer<String> readIndexer(String fileName) throws Exception {
		System.out.printf("read [%s].\n", fileName);
		Indexer<String> ret = new Indexer<String>();

		if (fileName.endsWith(".txt")) {
			BufferedReader br = openBufferedReader(fileName);
			String line = null;
			while ((line = br.readLine()) != null) {
				ret.add(line);
			}
			br.close();
		} else {
			ObjectInputStream ois = openObjectInputStream(fileName);
			ret = readIndexer(ois);
			ois.close();
		}
		return ret;
	}

	public static Object[] readIndexValuePairs(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		int[] indexes = new int[size];
		double[] values = new double[size];
		for (int i = 0; i < size; i++) {
			indexes[i] = ois.readInt();
			values[i] = ois.readDouble();
		}
		return new Object[] { indexes, values };
	}

	public static int[] readIntegerArray(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		int num_nonzeros = ois.readInt();
		int[] ret = new int[size];

		if (size == num_nonzeros) {
			for (int i = 0; i < size; i++) {
				ret[i] = ois.readInt();
			}
		} else {
			for (int i = 0; i < num_nonzeros; i++) {
				int index = ois.readInt();
				int value = ois.readInt();
				ret[index] = value;
			}
		}
		return ret;
	}

	public static int[] readIntegerArray(String fileName) throws Exception {
		ObjectInputStream ois = openObjectInputStream(fileName);
		int[] ret = readIntegerArray(ois);
		ois.close();
		return ret;
	}

	public static int[][] readIntegerMatrix(ObjectInputStream ois) throws Exception {
		int rowSize = ois.readInt();
		int[][] ret = new int[rowSize][];
		for (int i = 0; i < rowSize; i++) {
			ret[i] = readIntegerArray(ois);
		}
		return ret;
	}

	public static int[][] readIntegerMatrix(String fileName) throws Exception {
		ObjectInputStream ois = openObjectInputStream(fileName);
		int[][] ret = readIntegerMatrix(ois);
		ois.close();
		System.out.printf("read [%d, %d] matrix from [%s].\n", ret.length, ArrayUtils.maxColumnSize(ret), fileName);
		return ret;
	}

	public static List<String> readLines(BufferedReader reader, int num_lines_to_read) throws Exception {
		List<String> ret = new ArrayList<String>();
		while (true) {
			String line = reader.readLine();
			if (line == null || ret.size() == num_lines_to_read) {
				break;
			} else {
				ret.add(line);
			}
		}
		return ret;
	}

	public static List<String> readLines(String fileName) throws Exception {
		return readLines(fileName, UTF_8, Integer.MAX_VALUE);
	}

	public static List<String> readLines(String fileName, int num_read) throws Exception {
		return readLines(fileName, UTF_8, num_read);
	}

	public static List<String> readLines(String fileName, String encoding) throws Exception {
		return readLines(fileName, encoding, Integer.MAX_VALUE);
	}

	public static List<String> readLines(String fileName, String encoding, int num_read) throws Exception {
		BufferedReader reader = openBufferedReader(fileName, encoding);
		List<String> ret = readLines(reader, num_read);
		reader.close();
		return ret;
	}

	public static Map<String, String> readMap(String fileName) throws Exception {
		Map<String, String> ret = new HashMap<String, String>();
		for (String line : readLines(fileName)) {
			String[] parts = line.split("\t");
			if (parts.length != 2) {
				throw new Exception("# parts is not 2.");
			}
			ret.put(parts[0], parts[1]);
		}

		return ret;
	}

	public static HashSet<String> readSet(String fileName) throws Exception {
		return new HashSet<String>(readLines(fileName));
	}

	public static List<String> readStrings(ObjectInputStream ois) throws Exception {
		List<String> ret = new ArrayList<String>();
		int size = ois.readInt();
		for (int i = 0; i < size; i++) {
			String s = readString(ois);
			ret.add(s);
		}
		return ret;
	}

	public static String readString(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		StringBuffer sb = new StringBuffer(size);
		for (int j = 0; j < size; j++) {
			sb.append((char) ois.readByte());
		}
		return sb.toString();
	}

	public static String readText(Reader reader) throws Exception {
		StringBuffer sb = new StringBuffer();
		while (true) {
			int i = reader.read();
			if (i == -1) {
				break;
			} else {
				sb.append((char) i);
			}
		}
		return sb.toString();
	}

	public static String readText(String fileName) throws Exception {
		return readText(fileName, UTF_8);
	}

	public static String readText(String fileName, String encoding) throws Exception {
		StringBuffer ret = new StringBuffer();
		BufferedReader reader = openBufferedReader(fileName, encoding);
		ret.append(readText(reader));
		reader.close();
		return ret.toString();
	}

	public static String removeExtension(String fileName) {
		int end = fileName.lastIndexOf(".");
		if (end > 0) {
			fileName = fileName.substring(0, end);
		}
		return fileName;
	}

	public static void write(ObjectOutputStream oos, boolean[] x) throws IOException {
		oos.writeInt(x.length);
		for (int i = 0; i < x.length; i++) {
			oos.writeBoolean(x[i]);
		}
	}

	public static void write(ObjectOutputStream oos, Counter<String> x) throws Exception {
		oos.writeInt(x.size());
		for (String key : x.keySet()) {
			double value = x.getCount(key);
			write(oos, key);
			oos.writeDouble(value);
		}
	}

	public static void write(ObjectOutputStream ois, CounterMap<String, String> x) throws Exception {
		List<String> keys = new ArrayList<String>(x.keySet());
		ois.writeInt(keys.size());

		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			Counter<String> counter = x.getCounter(key);
			write(ois, key);
			write(ois, counter);
		}
	}

	public static void write(ObjectOutputStream oos, double[] x) throws Exception {
		int size = x.length;
		oos.writeInt(size);

		List<Integer> indexes = new ArrayList<Integer>();
		List<Double> values = new ArrayList<Double>();

		for (int i = 0; i < x.length; i++) {
			if (x[i] != 0) {
				indexes.add(i);
				values.add(x[i]);
			}
		}

		int num_nonzeros = indexes.size();

		oos.writeInt(num_nonzeros);

		if (num_nonzeros >= (x.length / 2f)) {
			for (int i = 0; i < x.length; i++) {
				oos.writeDouble(x[i]);
			}
		} else {
			for (int i = 0; i < indexes.size(); i++) {
				oos.writeInt(indexes.get(i));
				oos.writeDouble(values.get(i));
			}
		}

		oos.flush();
	}

	public static void write(ObjectOutputStream oos, Double[] x) throws Exception {
		oos.writeInt(x.length);
		for (int i = 0; i < x.length; i++) {
			oos.writeDouble(x[i].doubleValue());
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, double[][] x) throws Exception {
		oos.writeInt(x.length);
		for (int i = 0; i < x.length; i++) {
			write(oos, x[i]);
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, Indexer<String> indexer) throws Exception {
		oos.writeInt(indexer.size());
		for (int i = 0; i < indexer.size(); i++) {
			write(oos, indexer.getObject(i));
		}
	}

	public static void write(ObjectOutputStream oos, int[] x) throws Exception {
		int size = x.length;
		oos.writeInt(size);

		List<Integer> indexes = new ArrayList<Integer>();
		List<Integer> values = new ArrayList<Integer>();

		for (int i = 0; i < x.length; i++) {
			if (x[i] != 0) {
				indexes.add(i);
				values.add(x[i]);
			}
		}

		int num_nonzeros = indexes.size();

		oos.writeInt(num_nonzeros);

		if (num_nonzeros > (size / 2f)) {
			for (int i = 0; i < x.length; i++) {
				oos.writeInt(x[i]);
			}
		} else {
			for (int i = 0; i < indexes.size(); i++) {
				oos.writeInt(indexes.get(i));
				oos.writeInt(values.get(i));
			}
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, int[] indexes, double[] values) throws Exception {
		int size = indexes.length;
		oos.writeInt(size);
		for (int i = 0; i < indexes.length; i++) {
			oos.writeInt(indexes[i]);
			oos.writeDouble(values[i]);
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, int[][] x) throws Exception {
		oos.writeInt(x.length);
		for (int i = 0; i < x.length; i++) {
			write(oos, x[i]);
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, Integer[] x) throws Exception {
		oos.writeInt(x.length);
		for (int i = 0; i < x.length; i++) {
			oos.writeInt(x[i].intValue());
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, List<String> list) throws Exception {
		oos.writeInt(list.size());
		for (int i = 0; i < list.size(); i++) {
			write(oos, list.get(i));
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, String s) throws Exception {
		oos.writeInt(s.length());
		for (int i = 0; i < s.length(); i++) {
			oos.writeByte(s.charAt(i));
		}
		oos.flush();
	}

	public static void write(String fileName, boolean append, String text) throws Exception {
		write(fileName, UTF_8, append, text);
	}

	public static void write(String fileName, boolean[] x) throws Exception {
		ObjectOutputStream oos = openObjectOutputStream(fileName);
		write(oos, x);
		oos.close();
	}

	public static void write(String fileName, Counter<String> counter) throws Exception {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setGroupingUsed(false);
		write(fileName, counter, nf);
	}

	public static void write(String fileName, Counter<String> counter, NumberFormat nf) throws Exception {
		BufferedWriter bw = openBufferedWriter(fileName, UTF_8, false);
		List<String> keys = counter.getSortedKeys();
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			double count = counter.getCount(key);
			String output = String.format("%s\t%s", key, nf.format(count));
			bw.write(output);
			if (i != keys.size() - 1) {
				bw.write("\n");
			}
		}
		bw.close();
	}

	public static void write(String fileName, CounterMap<String, String> counterMap) throws Exception {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setGroupingUsed(false);
		write(fileName, counterMap, nf);
	}

	public static void write(String fileName, CounterMap<String, String> cm, NumberFormat nf) throws Exception {
		BufferedWriter bw = openBufferedWriter(fileName, UTF_8, false);
		List<String> keys = cm.getInnerCountSums().getSortedKeys();

		for (int i = 0; i < keys.size(); i++) {
			String outerKey = keys.get(i);
			Counter<String> innerCounter = cm.getCounter(outerKey);
			StringBuffer sb = new StringBuffer();

			double sum = innerCounter.totalCount();

			sb.append(String.format("%s:%s", outerKey, nf == null ? Double.toString(sum) : nf.format(sum)));

			for (String innerKey : innerCounter.getSortedKeys()) {
				double value = innerCounter.getCount(innerKey);
				sb.append(String.format("\t%s:%s", innerKey, nf == null ? Double.toString(value) : nf.format(value)));
			}

			bw.write(sb.toString());
			if (i != keys.size() - 1) {
				bw.write("\n");
			}
		}
		bw.close();
	}

	public static void write(String fileName, Indexer<String> indexer) throws Exception {

		if (fileName.endsWith(".txt")) {
			TextFileWriter writer = new TextFileWriter(fileName);
			for (int i = 0; i < indexer.getObjects().size(); i++) {
				String label = indexer.getObject(i);
				writer.write(label + "\n");
			}
			writer.close();
		} else {
			ObjectOutputStream oos = openObjectOutputStream(fileName);
			write(oos, indexer);
			oos.close();
		}
	}

	public static void write(String fileName, int[] x) throws Exception {
		ObjectOutputStream oos = openObjectOutputStream(fileName);
		write(oos, x);
		oos.close();
	}

	public static void write(String fileName, int[][] x) throws Exception {
		ObjectOutputStream oos = openObjectOutputStream(fileName);
		write(oos, x);
		oos.close();
	}

	public static void write(String fileName, String text) throws Exception {
		write(fileName, UTF_8, false, text);
	}

	public static void write(String fileName, String encoding, boolean append, String text) throws Exception {
		Writer writer = openBufferedWriter(fileName, encoding, append);
		writer.write(text);
		writer.flush();
		writer.close();
	}

	public static void writeText(String fileName, Indexer<String> indexer) throws Exception {
		System.out.printf("write to %s.\n", fileName);
		BufferedWriter writer = openBufferedWriter(fileName, UTF_8, false);
		for (String str : indexer.getObjects()) {
			writer.write(str + "\n");
			writer.flush();
		}
		writer.close();
	}

}
