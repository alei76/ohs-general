package ohs.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import ohs.math.ArrayUtils;
import ohs.types.Counter;
import ohs.types.CounterMap;
import ohs.types.Indexer;
import ohs.utils.ByteSize;
import ohs.utils.ByteSize.Type;

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

	private static void compress(File root, File input, TarArchiveOutputStream taos) throws IOException {

		if (input.isFile()) {
			System.out.println("Adding File: " + root.toURI().relativize(input.toURI()).getPath());

			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(input));

			/** Step: 3 ---> Create a tar entry for each file that is read. **/

			/**
			 * relativize is used to to add a file to a tar, without including the entire path from root.
			 **/

			TarArchiveEntry tae = new TarArchiveEntry(input, root.getParentFile().toURI().relativize(input.toURI()).getPath());

			/** Step: 4 ---> Put the tar entry using putArchiveEntry. **/

			taos.putArchiveEntry(tae);

			/**
			 * Step: 5 ---> Write the data to the tar file and close the input stream.
			 **/

			int count;
			byte data[] = new byte[2048];
			while ((count = bis.read(data, 0, 2048)) != -1) {
				taos.write(data, 0, count);
			}
			bis.close();

			/** Step: 6 --->close the archive entry. **/

			taos.closeArchiveEntry();

		} else {
			if (input.listFiles() != null) {
				/** Add an empty folder to the tar **/
				if (input.listFiles().length == 0) {

					System.out.println("Adding Empty Folder: " + root.toURI().relativize(input.toURI()).getPath());
					TarArchiveEntry entry = new TarArchiveEntry(input, root.getParentFile().toURI().relativize(input.toURI()).getPath());
					taos.putArchiveEntry(entry);
					taos.closeArchiveEntry();
				}

				for (File file : input.listFiles())
					compress(root, file, taos);
			}
		}
	}

	public static void compress(String inputPath, String outputFileName) throws Exception {

		if (!outputFileName.endsWith(".tar.gz")) {
			outputFileName += ".tar.gz";
		}

		/** Step: 1 ---> create a TarArchiveOutputStream object. **/
		TarArchiveOutputStream taos = new TarArchiveOutputStream(
				new GzipCompressorOutputStream(new BufferedOutputStream(new FileOutputStream(outputFileName))));

		// TAR has an 8 gig file limit by default, this gets around that
		taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR); // to get past the 8 gig limit
		// TAR originally didn't support long file names, so enable the support for it
		taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

		/**
		 * Step: 2 --->Open the source data and get a list of files from given directory recursively.
		 **/

		File input = new File(inputPath);

		compress(input.getParentFile(), input, taos);

		/** Step: 7 --->close the output stream. **/

		taos.close();

		System.out.println("tar.gz file created successfully!!");

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

	public static boolean exists(String fileName) {
		return new File(fileName).exists();
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

	private static List<File> getFilesUnder(File dir, boolean recursive) {
		List<File> files = new ArrayList<File>();
		addFilesUnder(dir, files, recursive);
		// Collections.sort(files);

		long total_bytes = 0;

		for (int i = 0; i < files.size(); i++) {
			total_bytes += files.get(i).length();
		}

		ByteSize fs = new ByteSize(total_bytes);

		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(2);

		System.out.println(String.format("read [%d, %s MBs] files from [%s]", files.size(), nf.format(fs.size(Type.MEGA)), dir.getName()));
		return files;
	}

	public static List<File> getFilesUnder(String dirName) {
		return getFilesUnder(new File(dirName));
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		// compress("../../data/news_ir/content_nlp", "../../data/news_ir/test.tar.gz");

		String s = "ABCDE";

		{
			ObjectOutputStream oos = openObjectOutputStream("../../data/entity_iden/wiki/test1.ser");
			oos.write(s.getBytes());
			oos.close();
		}

		{
			ObjectOutputStream oos = openObjectOutputStream("../../data/entity_iden/wiki/test2.ser");

			for (int i = 0; i < 1; i++) {
				oos.writeUTF(s);
			}

			oos.close();
		}

		{
			ObjectOutputStream oos = openObjectOutputStream("../../data/entity_iden/wiki/test3.ser");
			write(oos, s);

			oos.close();
		}

		{
			ObjectInputStream ois = openObjectInputStream("../../data/entity_iden/wiki/test2.ser");

			for (int i = 0; i < 1; i++) {
				String ss = ois.readUTF();
				System.out.println(ss);
			}
		}

		{
			double[] ar = ArrayUtils.range(10000000, 0.0, 1);
			ObjectOutputStream oos = openObjectOutputStream("../../data/entity_iden/wiki/test-a1.ser.gz");

			IOUtils.write(oos, ar);
			oos.close();

		}

		{
			int[] ar = ArrayUtils.range(10, 0, 1);
			ObjectOutputStream oos = openObjectOutputStream("../../data/entity_iden/wiki/test-a2.ser.gz");

			IOUtils.write(oos, ar);
			oos.close();
		}

		System.out.println("process ends.");
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
		if (fileName.endsWith(".ser.gz")) {
			ret = new ObjectInputStream(new GZIPInputStream(new FileInputStream(fileName)));
		} else if (fileName.endsWith(".ser")) {
			ret = new ObjectInputStream(new FileInputStream(fileName));
		}
		return ret;
	}

	public static ObjectOutputStream openObjectOutputStream(String fileName) throws Exception {
		return openObjectOutputStream(fileName, false);
	}

	public static ObjectOutputStream openObjectOutputStream(String fileName, boolean append) throws Exception {
		System.out.printf("write at [%s].\n", fileName);

		File file = new File(fileName);
		File parent = file.getParentFile();
		if (!parent.exists()) {
			parent.mkdirs();
		}

		ObjectOutputStream ret = null;
		if (file.getName().endsWith(".ser.gz")) {
			ret = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file, append)));
		} else if (file.getName().endsWith(".ser")) {
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

	public static Counter<String> readCounter(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		Counter<String> ret = new Counter<String>(size);
		for (int i = 0; i < size; i++) {
			ret.setCount(ois.readUTF(), ois.readDouble());
		}
		return ret;
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

	public static CounterMap<String, String> readCounterMap(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		CounterMap<String, String> ret = new CounterMap<String, String>(size);
		for (int i = 0; i < size; i++) {
			ret.setCounter(ois.readUTF(), readCounter(ois));
		}
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
		double[] ret = new double[size];
		for (int i = 0; i < size; i++) {
			ret[i] = ois.readDouble();
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
		int size = ois.readInt();
		Indexer<String> ret = new Indexer<String>(size);
		for (int i = 0; i < size; i++) {
			ret.add(ois.readUTF());
		}
		return ret;
	}

	public static Indexer<String> readIndexer(String fileName) throws Exception {
		System.out.printf("read [%s].\n", fileName);
		Indexer<String> ret = new Indexer<String>();
		BufferedReader br = openBufferedReader(fileName);
		String line = null;
		while ((line = br.readLine()) != null) {
			ret.add(line);
		}
		br.close();
		return ret;
	}

	public static int[] readIntegerArray(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		int[] ret = new int[size];
		for (int i = 0; i < size; i++) {
			ret[i] = ois.readInt();
		}
		return ret;
	}

	public static int[] readIntegerArray(String fileName) throws Exception {
		ObjectInputStream ois = openObjectInputStream(fileName);
		int[] ret = readIntegerArray(ois);
		ois.close();
		return ret;
	}

	public static Map<Integer, Integer> readIntegerMap(ObjectInputStream ois) throws Exception {
		int size = ois.readInt();
		Map<Integer, Integer> ret = new HashMap<Integer, Integer>(size);
		for (int i = 0; i < size; i++) {
			int key = ois.readInt();
			int value = ois.readInt();
			ret.put(key, value);
		}
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
		System.out.printf("read [%d, %d] matrix at [%s].\n", ret.length, ArrayUtils.maxColumnSize(ret), fileName);
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
			ret.add(ois.readUTF());
		}
		return ret;
	}

	public static String readText(File file) throws Exception {
		return readText(file.getCanonicalPath(), UTF_8);
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
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, Collection<String> c) throws Exception {
		oos.writeInt(c.size());

		Iterator<String> iter = c.iterator();
		while (iter.hasNext()) {
			oos.writeUTF(iter.next());
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, Counter<String> x) throws Exception {
		oos.writeInt(x.size());
		for (String key : x.keySet()) {
			oos.writeUTF(key);
			oos.writeDouble(x.getCount(key));
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, CounterMap<String, String> cm) throws Exception {
		oos.writeInt(cm.keySet().size());
		Iterator<String> iter = cm.keySet().iterator();
		while (iter.hasNext()) {
			oos.writeUTF(iter.next());
			write(oos, cm.getCounter(iter.next()));
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, double[] x) throws Exception {
		int size = x.length;
		oos.writeInt(size);
		for (int i = 0; i < x.length; i++) {
			oos.writeDouble(x[i]);
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
			oos.writeUTF(indexer.getObject(i));
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, int[] x) throws Exception {
		int size = x.length;
		oos.writeInt(size);
		for (int i = 0; i < x.length; i++) {
			oos.writeInt(x[i]);
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

	public static void write(ObjectOutputStream oos, Map<Integer, Integer> m) throws Exception {
		oos.writeInt(m.size());
		for (Integer key : m.keySet()) {
			oos.writeInt(key);
			oos.writeInt(m.get(key));
		}
		oos.flush();
	}

	public static void write(ObjectOutputStream oos, String s) throws Exception {
		oos.writeUTF(s);
	}

	public static void write(String fileName, boolean append, String text) throws Exception {
		write(fileName, UTF_8, append, text);
	}

	public static void write(String fileName, boolean[] x) throws Exception {
		ObjectOutputStream oos = openObjectOutputStream(fileName);
		write(oos, x);
		oos.close();
	}

	public static void write(String fileName, Counter<String> c) throws Exception {
		write(fileName, c, false);
	}

	public static void write(String fileName, Counter<String> c, boolean orderAlphabetically) throws Exception {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setGroupingUsed(false);
		write(fileName, c, nf, orderAlphabetically);
	}

	public static void write(String fileName, Counter<String> c, NumberFormat nf, boolean orderAlphabetically) throws Exception {
		BufferedWriter bw = openBufferedWriter(fileName, UTF_8, false);
		List<String> keys = new ArrayList<String>();
		if (orderAlphabetically) {
			keys = new ArrayList<String>(c.keySet());
			Collections.sort(keys);
		} else {
			keys = c.getSortedKeys();
		}
		for (int i = 0; i < keys.size(); i++) {
			String key = keys.get(i);
			double cnt = c.getCount(key);
			String output = String.format("%s\t%s", key, nf.format(cnt));
			bw.write(output);
			if (i != keys.size() - 1) {
				bw.write("\n");
			}
		}
		bw.close();
	}

	public static void write(String fileName, CounterMap<String, String> cm) throws Exception {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMinimumFractionDigits(0);
		nf.setGroupingUsed(false);
		write(fileName, cm, nf);
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
		TextFileWriter writer = new TextFileWriter(fileName);
		for (int i = 0; i < indexer.getObjects().size(); i++) {
			String label = indexer.getObject(i);
			writer.write(label + "\n");
		}
		writer.close();
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

}
