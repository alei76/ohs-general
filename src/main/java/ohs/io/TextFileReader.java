package ohs.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ohs.utils.StopWatch;

/**
 * 
 * @author Heung-Seon Oh
 * @version 1.0
 * @date 2009. 4. 14
 * 
 */
public class TextFileReader {

	private String currentLine;

	private int max_lines;

	private int max_nexts;

	private int num_lines;

	private int num_nexts;

	boolean print_nexts;

	private BufferedReader reader;

	private StopWatch stopWatch;

	private int print_size = 10000;

	public TextFileReader(File file) {
		this(file.getPath(), FileUtils.UTF_8);
	}

	public TextFileReader(String fileName) {
		this(fileName, FileUtils.UTF_8);
	}

	public TextFileReader(String fileName, String encoding) {
		try {
			reader = FileUtils.openBufferedReader(fileName, encoding);
		} catch (Exception e) {
			e.printStackTrace();
		}

		currentLine = null;
		num_lines = 0;
		num_nexts = 0;
		stopWatch = new StopWatch();
		print_nexts = true;

		max_nexts = Integer.MAX_VALUE;
		max_lines = Integer.MAX_VALUE;
	}

	public void close() {
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	public BufferedReader getBufferedReader() {
		return reader;
	}

	public List<String> getNextLines() {
		List<String> ret = new ArrayList<String>();
		do {
			if (next() == null || next().equals("")) {
				break;
			} else {
				ret.add(next());
			}
		} while (hasNext());

		num_nexts++;

		return ret;
	}

	public int getNumLines() {
		return num_lines;
	}

	public int getNumNexts() {
		return num_nexts;
	}

	public StopWatch getStopWatch() {
		return stopWatch;
	}

	public boolean hasNext() {
		boolean ret = true;

		if (num_nexts > max_nexts || num_lines > max_lines) {
			ret = false;
		} else {
			currentLine = null;
			try {
				currentLine = reader.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (currentLine == null) {
				ret = false;
			} else {
				num_lines++;
			}
		}
		return ret;
	}

	public String next() {
		return currentLine;
	}

	public void printProgress() {
		if (stopWatch.startTime == 0) {
			stopWatch.start();
		}

		int remain = 0;

		if (print_nexts) {
			remain = num_nexts % print_size;
		} else {
			remain = num_lines % print_size;
		}

		if (remain == 0) {
			if (print_nexts) {
				System.out.print(String.format("\r[%d nexts, %d lines, %s]", num_nexts, num_lines, stopWatch.stop()));
			} else {
				System.out.print(String.format("\r[%s lines, %s]", num_lines, stopWatch.stop()));
			}
		} else if (currentLine == null) {
			if (print_nexts) {
				System.out.print(String.format("\r[%d nexts, %d lines, %s]\n", num_nexts, num_lines, stopWatch.stop()));
			} else {
				System.out.print(String.format("\r[%s lines, %s]\n", num_lines, stopWatch.stop()));
			}
		}
	}

	public void setMaxLines(int max_lines) {
		this.max_lines = max_lines;
	}

	public void setMaxNexts(int max_nexts) {
		this.max_nexts = max_nexts;
	}

	public void setPrintNexts(boolean print_nexts) {
		this.print_nexts = print_nexts;
	}

	public void setPrintSize(int print_size) {
		this.print_size = print_size;
	}

}
