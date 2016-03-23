package ohs.tree.trie.array;

/*
 *Copyright (C) Jeshua Bratman 2012
 *              (jeshuabratman@gmail.com) 
 *
 *Permission is hereby granted, free of charge, to any person obtaining a copy
 *of this software and associated documentation files (the "Software"), to deal
 *in the Software without restriction, including without limitation the rights
 *to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *copies of the Software, and to permit persons to whom the Software is
 *furnished to do so, subject to the following conditions:
 *
 *The above copyright notice and this permission notice shall be included in
 *all copies or substantial portions of the Software.
 *
 *THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *THE SOFTWARE.
 */

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.TreeMap;

/**
 * On disk version of the Double-Array trie. This implementation is pretty rudimentary. It essentially stores the two arrays directly as
 * bytes on the disk with additionally some simple caching capability.
 * 
 * Also includes functions to convert from and to an in-memory DA Trie
 * 
 * @author Jeshua Bratman
 */
public class DATrieDisk extends DATrie {

	// OPTIONS

	// should we cache some of the trie in memory?
	public static boolean DA_DISK_CACHE = false;
	// how much should we readahead on the disk
	public static int DA_DISK_CACHE_READAHEAD = 10;
	// max number of entries to cache
	public static int DA_DISK_CACHE_MAX = 100000;
	// cache misses and hits (just for logging purposes)
	public int misses;
	public int hits;

	/**
	 * Create a on-disk trie. Notice it uses 2 files. This really isn't necessary or the most efficient. I just wrote it this way to begin
	 * with and haven't gotten around to incorporating them into one file.
	 * 
	 * @param filename_base
	 *            file to store base array
	 * @param filename_check
	 *            file to store check array
	 * @param delete_if_exists
	 *            delete the files if they already exist
	 * @throws IOException
	 */
	public DATrieDisk(String filename_base, String filename_check, boolean delete_if_exists) throws IOException {
		super();
		this.hits = 0;
		this.misses = 0;
		file_base = new File(filename_base);
		file_check = new File(filename_check);
		this.base_buffer = new TreeMap<Integer, Integer>();
		this.check_buffer = new TreeMap<Integer, Integer>();
		this.address_buffer = new TreeMap<Integer, Integer>();
		this.history_check = new LinkedList<Integer>();
		this.history_base = new LinkedList<Integer>();
		this.history_address = new LinkedList<Integer>();
		buffering_enabled = DA_DISK_CACHE;
		read_ahead = DA_DISK_CACHE_READAHEAD;
		if (read_ahead <= 0)
			buffering_enabled = false;
		boolean files_exist = false;

		if (file_base.exists() && file_check.exists()) {

			if (delete_if_exists) {
				file_base.delete();
				file_check.delete();
			} else
				files_exist = true;
		} else if ((file_base.exists() && !file_check.exists()) || (!file_base.exists() && file_check.exists())) {
			System.err.println("Error: only one of the two files exists");
			System.exit(1);
		}

		base = new RandomAccessFile(file_base, "rw");
		check = new RandomAccessFile(file_check, "rw");

		// get size of files
		if (files_exist) {
			try {
				array_size = (int) (base.length() / base_cell_size);
				if (array_size <= INITIAL_ARRAY_SIZE) {
					initializeFiles(INITIAL_ARRAY_SIZE);
				} else {

					check_max_fp = (int) check.length();
					base_max_fp = (int) base.length() - 4;
					// initialize buffers
					readCheck(0);
					readBase(0);
				}

			} catch (IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		} else {

			initializeFiles(INITIAL_ARRAY_SIZE);
		}
	}

	/**
	 * Constructor - doesn't delete files if they exist
	 */
	public DATrieDisk(String filename_base, String filename_check) throws IOException {
		this(filename_base, filename_check, false);
	}

	/**
	 * Copy the data from an in-memory trie to store on disk.
	 * 
	 * @param trie
	 * @throws IOException
	 */
	public void copyFromMemTrie(DATrieMem trie) throws IOException {
		int size = trie.getArraySize();
		for (int i = HEAD; i < size; i++) {
			this.writeCheck(i, trie.readCheck(i));
			this.writeBase(i, trie.readBase(i));
			this.writeAddress(i, trie.readAddress(i));
		}
	}

	/**
	 * Copy from data on disk and create an in-memory trie.
	 * 
	 * @return
	 * @throws IOException
	 */
	public DATrieMem convertToMemTrie() throws IOException {
		DATrieMem trie = new DATrieMem(minChar, maxChar);
		boolean buffer_enabled = this.buffering_enabled;
		this.buffering_enabled = false;
		int size = this.getArraySize();
		trie.setLength(size);

		base.seek(0);
		check.seek(0);
		byte[] b = new byte[(int) base.length()];
		base.readFully(b);
		byte[] c = new byte[(int) check.length()];
		check.readFully(c);
		trie.setLength(c.length / check_cell_size);
		int t;
		for (int i = HEAD; i < size; i++) {
			int temp = 0;
			for (int j = 0; j < base_cell_size / 2; j++) {
				t = b[i * base_cell_size + j];
				if (j > 0)
					t &= 0xFF;
				temp |= (t << (3 - j) * 8);
			}
			trie.writeBase(i, temp);
			temp = 0;
			for (int j = (base_cell_size / 2); j < base_cell_size; j++) {
				t = b[i * base_cell_size + j];
				if (j > 4)
					t &= 0xFF;
				temp |= (t << (7 - j) * 8);
			}

			trie.writeAddress(i, temp);
			temp = 0;
			for (int j = 0; j < check_cell_size; j++) {
				t = c[i * check_cell_size + j];
				if (j > 0)
					t &= 0xFF;
				temp |= (t << (3 - j) * 8);
			}
			trie.writeCheck(i, temp);
		}
		this.buffering_enabled = buffer_enabled;
		return trie;
	}

	/**
	 * Close the files.
	 */
	public void close() {
		try {
			base.close();
			check.close();
		} catch (Exception e) {
		}
	}

	// ==================================================
	// IMPLEMENTATION:

	private RandomAccessFile base;
	private int base_max_fp;
	private RandomAccessFile check;
	private TreeMap<Integer, Integer> check_buffer, base_buffer, address_buffer;
	private LinkedList<Integer> history_check, history_base, history_address;
	private File file_base, file_check;
	private int check_max_fp;
	private int check_cell_size = 4; // bytes per cell
	private int base_cell_size = 8; // bytes per cell

	private boolean buffering_enabled;
	private int read_ahead;

	private void initializeFiles(int size) throws IOException {
		array_size = size;
		base.seek(0);
		check.seek(0);
		System.out.flush();
		for (int i = 0; i <= array_size; i++) {

			base.writeInt(0);
			base.writeInt(0);
			check.writeInt(0);
		}
		check_max_fp = (int) check.length();
		base_max_fp = (int) base.length() - 4;
	}

	protected void setLength(int length) {
		try {
			base.seek((length + 1) * base_cell_size);
			check.seek((length + 1) * check_cell_size);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	protected void ensureLength(int length) {
		if (length <= array_size)
			return;
		else
			System.out.println("ensuring length " + length);
		try {
			// seek end of file
			base.seek((array_size + 1) * base_cell_size);
			check.seek((array_size + 1) * check_cell_size);
			int size = array_size;
			int new_size = (int) (array_size * ARRAY_LENGTHEN_FACTOR);
			for (int i = size; i < new_size; i++) {
				base.writeInt(0);
				base.writeInt(0);
				check.writeInt(0);
			}
			array_size = new_size;
			check_max_fp = (int) check.length();
			base_max_fp = (int) base.length() - 4;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// --------------------------------------------------------------------------------

	protected int read(int state, RandomAccessFile f, TreeMap<Integer, Integer> buffer, LinkedList<Integer> history, boolean address)
			throws IOException {

		if (buffering_enabled)
			history.addLast(state);
		if (buffering_enabled && buffer.containsKey(state)) {
			this.hits++;
			return buffer.get(state);
		} else {

			int file_loc = 0;
			if (f == check)
				file_loc += state * check_cell_size;
			else if (f == base && !address)
				file_loc += state * base_cell_size;
			else if (f == base && address)
				file_loc += state * base_cell_size + 4;
			// seek in the file
			f.seek(file_loc);

			if (!buffering_enabled) {
				return f.readInt();
			} else {
				this.misses++;
				int st;
				for (int i = 0; i < read_ahead; i++) {
					st = state + i;
					if (f == base && address) {
						address_buffer.put(st, f.readInt());
						if ((st + 1) * base_cell_size >= base_max_fp) {
							base_buffer.put(st + 1, f.readInt());
						} else {
							break;
						}
					} else if (f == base) {
						base_buffer.put(st, f.readInt());
						address_buffer.put(st, f.readInt());
						if ((st + 1) * base_cell_size >= base_max_fp - base_cell_size) {
							break;
						}
					} else {
						check_buffer.put(st, f.readInt());
						if ((st + 1) * check_cell_size >= check_max_fp - check_cell_size) {
							break;
						}
					}
				}
				// System.out.println("I have "+buffer.size()+" items buffered ");
				if (buffer.size() > DA_DISK_CACHE_MAX) {
					buffer.remove(history.getFirst());
					history.removeFirst();

				}
				return buffer.get(state);
			}

		}
	}

	// reads a byte from the check file
	protected int readCheck(int state) throws IOException {
		return (read(state, check, check_buffer, history_check, false));
	}

	// write v to base in check file
	protected void writeCheck(int state, int v) throws IOException {
		check.seek(state * check_cell_size);
		check.writeInt(v);
	}

	// reads a byte from the base file
	protected int readBase(int state) throws IOException {
		return (read(state, base, base_buffer, history_base, false));
	}

	protected void writeBase(int state, int v) throws IOException {
		base.seek(state * base_cell_size);
		base.writeInt(v);
	}

	protected int readAddress(int state) throws IOException {
		return (read(state, base, address_buffer, history_address, true));
	}

	protected void writeAddress(int s, int addr) throws IOException {
		base.seek(s * base_cell_size + 4);
		base.writeInt(addr);
	}

	protected void clear() throws IOException {
		check.seek(0);
		for (int i = 0; i < array_size; i++) {
			writeCheck(i, EMPTY);
			writeAddress(i, EMPTY);
		}
	}
}