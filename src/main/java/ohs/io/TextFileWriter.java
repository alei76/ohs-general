package ohs.io;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import ohs.utils.StopWatch;

/**
 * 
 * @author Heung-Seon Oh
 * @version 1.0
 * @date 2009. 4. 14
 * 
 */

public class TextFileWriter {

	private int num_writes;

	private StopWatch stopWatch;

	private Writer writer;

	public TextFileWriter(File file) {
		this(file.getPath(), IOUtils.UTF_8, false);
	}

	public TextFileWriter(String fileName) {
		this(fileName, IOUtils.UTF_8, false);
	}

	public TextFileWriter(String fileName, String encoding, boolean append) {
		try {
			writer = IOUtils.openBufferedWriter(fileName, encoding, append);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public TextFileWriter(Writer writer) {
		this.writer = writer;
	}

	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	public void print(int amount) {
		if (stopWatch.startTime == 0) {
			stopWatch.start();
		}

		if (num_writes % amount == 0) {
			System.out.print(String.format("\r[%d writes, %s]", num_writes, stopWatch.stop()));
		}
	}

	public void printLast() {
		System.out.println(String.format("\r[%d writes, %s]", num_writes, stopWatch.stop()));
	}

	public void write(String text) {
		try {
			writer.write(text);
			writer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
