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

	private int maxLines;

	private int maxNexts;

	private int numLines;

	private int numNexts;

	boolean printNexts;

	private BufferedReader reader;

	private StopWatch stopWatch;

	public TextFileReader(File file) {
		this(file.getPath(), IOUtils.UTF_8);
	}

	public TextFileReader(String fileName) {
		this(fileName, IOUtils.UTF_8);
	}

	public TextFileReader(String fileName, String encoding) {
		try {
			reader = IOUtils.openBufferedReader(fileName, encoding);
		} catch (Exception e) {
			e.printStackTrace();
		}

		currentLine = null;
		numLines = 0;
		numNexts = 0;
		stopWatch = new StopWatch();
		printNexts = true;

		maxNexts = Integer.MAX_VALUE;
		maxLines = Integer.MAX_VALUE;
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

		numNexts++;

		return ret;
	}

	public int getNumLines() {
		return numLines;
	}

	public int getNumNexts() {
		return numNexts;
	}

	public boolean hasNext() {
		boolean ret = true;

		if (numNexts > maxNexts || numLines > maxLines) {
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
				numLines++;
			}
		}
		return ret;
	}

	public String next() {
		return currentLine;
	}

	public void print(int amount) {
		if (stopWatch.startTime == 0) {
			stopWatch.start();
		}

		int remain = 0;

		if (printNexts) {
			remain = numNexts % amount;
		} else {
			remain = numLines % amount;
		}

		if (remain == 0) {
			System.out.print(String.format("\r[%d nexts, %s lines, %s]", numNexts, numLines, stopWatch.stop()));
		}
	}

	public void printLast() {
		stopWatch.stop();
		System.out.println(String.format("\r[%d nexts, %s lines, %s]", numNexts, numLines, stopWatch.toString()));
	}

	public void setMaxLines(int maxLines) {
		this.maxLines = maxLines;
	}

	public void setMaxNexts(int maxNumNexts) {
		this.maxNexts = maxNumNexts;
	}

	public void setPrintNexts(boolean printNexts) {
		this.printNexts = printNexts;
	}

	public StopWatch stopWatch() {
		return stopWatch;
	}

}
