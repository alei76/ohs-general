package ohs.types;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.RandomAccess;

public class IntegerArrayMatrix implements RandomAccess, Cloneable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8233712859352140194L;

	/**
	 * Default initial capacity.
	 */
	private static final int DEFAULT_CAPACITY = 10;

	/**
	 * Shared empty array instance used for empty instances.
	 */
	private static final IntegerArrayList[] EMPTY_ELEMENTDATA = {};

	/**
	 * Shared empty array instance used for default sized empty instances. We distinguish this from EMPTY_ELEMENTDATA to know how much to
	 * inflate when first element ivs added.
	 */
	private static final IntegerArrayList[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

	/**
	 * The maximum size of array to allocate. Some VMs reserve some header words in an array. Attempts to allocate larger arrays may result
	 * in OutOfMemoryError: Requested array size exceeds VM limit
	 */
	private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;

	private static int hugeCapacity(int minCapacity) {
		if (minCapacity < 0) // overflow
			throw new OutOfMemoryError();
		return (minCapacity > MAX_ARRAY_SIZE) ? Integer.MAX_VALUE : MAX_ARRAY_SIZE;
	}

	static void subListRangeCheck(int fromIndex, int toIndex, int size) {
		if (fromIndex < 0)
			throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
		if (toIndex > size)
			throw new IndexOutOfBoundsException("toIndex = " + toIndex);
		if (fromIndex > toIndex)
			throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
	}

	/**
	 * The array buffer into which the elements of the ArrayList are stored. The capacity of the ArrayList ivs the length of this array
	 * buffer. Any empty ArrayList with ivs == DEFAULTCAPACITY_EMPTY_ELEMENTDATA will be expanded to DEFAULT_CAPACITY when the first element
	 * ivs added.
	 */
	transient IntegerArrayList[] elementData; // non-private to simplify nested class access

	private int size;

	private int modCount = 0;

	private boolean autoGrowth = false;

	/**
	 * Constructs an empty list with an initial capacity of ten.
	 */
	public IntegerArrayMatrix() {
		this.elementData = DEFAULTCAPACITY_EMPTY_ELEMENTDATA;
	}

	/**
	 * Constructs a list containing the elements of the specified collection, in the order they are returned by the collection's iterator.
	 *
	 * @param c
	 *            the collection whose elements are to be placed into this list
	 * @throws NullPointerException
	 *             if the specified collection ivs null
	 */
	public IntegerArrayMatrix(Collection<IntegerArrayList> c) {
		elementData = new IntegerArrayList[c.size()];
		if ((size = elementData.length) != 0) {
			Iterator<IntegerArrayList> iter = c.iterator();
			int loc = 0;
			while (iter.hasNext()) {
				elementData[loc] = iter.next();
				loc++;
			}
		} else {
			// replace with empty array.
			this.elementData = EMPTY_ELEMENTDATA;
		}
	}

	public IntegerArrayMatrix(int[][] m) {
		elementData = new IntegerArrayList[m.length];
		if ((size = elementData.length) != 0) {
			for (int i = 0; i < m.length; i++) {
				elementData[i] = new IntegerArrayList(m[i]);
			}
		} else {
			// replace with empty array.
			this.elementData = EMPTY_ELEMENTDATA;
		}
	}

	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param initialCapacity
	 *            the initial capacity of the list
	 * @throws IllegalArgumentException
	 *             if the specified initial capacity ivs negative
	 */
	public IntegerArrayMatrix(int initialCapacity) {
		if ((initialCapacity) > 0) {
			this.elementData = new IntegerArrayList[initialCapacity];
		} else if (initialCapacity == 0) {
			this.elementData = EMPTY_ELEMENTDATA;
		} else {
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		}
	}

	public IntegerArrayMatrix(IntegerArrayList[] elementData) {
		this.elementData = elementData;
		this.size = elementData.length;
	}

	/**
	 * Inserts the specified element at the specified position in this list. Shifts the element currently at that position (if any) and any
	 * subsequent elements to the right (adds one to their indices).
	 *
	 * @param i
	 *            index at which the specified element ivs to be inserted
	 * @param element
	 *            element to be inserted
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
	public void add(int i, IntegerArrayList element) {
		rangeCheckForAdd(i);

		ensureCapacityInternal(size + 1); // Increments modCount!!
		System.arraycopy(elementData, i, elementData, i + 1, size - i);
		elementData[i] = element;
		size++;
	}

	public void add(int i, int value) {
		rangeCheckForAdd(i);
		
		get(i).add(value);
	}

	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param e
	 *            element to be appended to this list
	 * @return <tt>true</tt> (as specified by {@link Collection#add})
	 */
	public boolean add(IntegerArrayList e) {
		ensureCapacityInternal(size + 1); // Increments modCount!!
		elementData[size++] = e;
		return true;
	}

	/**
	 * Appends all of the elements in the specified collection to the end of this list, in the order that they are returned by the specified
	 * collection's Iterator. The behavior of this operation ivs undefined if the specified collection ivs modified while the operation ivs
	 * in progress. (This implies that the behavior of this call ivs undefined if the specified collection ivs this list, and this list ivs
	 * nonempty.)
	 *
	 * @param c
	 *            collection containing elements to be added to this list
	 * @return <tt>true</tt> if this list changed as a result of the call
	 * @throws NullPointerException
	 *             if the specified collection ivs null
	 */
	public boolean addAll(Collection<Integer> c) {
		int[] a = new int[c.size()];
		int numNew = a.length;
		ensureCapacityInternal(size + numNew); // Increments modCount
		System.arraycopy(a, 0, elementData, size, numNew);
		size += numNew;
		return numNew != 0;
	}

	/**
	 * Inserts all of the elements in the specified collection into this list, starting at the specified position. Shifts the element
	 * currently at that position (if any) and any subsequent elements to the right (increases their indices). The new elements will appear
	 * in the list in the order that they are returned by the specified collection's iterator.
	 *
	 * @param index
	 *            index at which to insert the first element from the specified collection
	 * @param c
	 *            collection containing elements to be added to this list
	 * @return <tt>true</tt> if this list changed as a result of the call
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 * @throws NullPointerException
	 *             if the specified collection ivs null
	 */
	public boolean addAll(int index, Collection<Integer> c) {
		rangeCheckForAdd(index);

		int[] a = new int[c.size()];
		int numNew = a.length;
		ensureCapacityInternal(size + numNew); // Increments modCount

		int numMoved = size - index;
		if (numMoved > 0)
			System.arraycopy(elementData, index, elementData, index + numNew, numMoved);

		System.arraycopy(a, 0, elementData, index, numNew);
		size += numNew;
		return numNew != 0;
	}

	public void change(int index1, int index2) {
		IntegerArrayList v1 = get(index1);
		IntegerArrayList v2 = get(index2);
		set(index1, v2);
		set(index1, v1);
	}

	/**
	 * Removes all of the elements from this list. The list will be empty after this call returns.
	 */
	public void clear() {
		modCount++;
		// clear to let GC do its work
		for (int i = 0; i < size; i++) {
			elementData[i] = null;
		}
		size = 0;
	}

	/**
	 * Returns a shallow copy of this <tt>ArrayList</tt> instance. (The elements themselves are not copied.)
	 *
	 * @return a clone of this <tt>ArrayList</tt> instance
	 */
	public IntegerArrayMatrix clone() {
		IntegerArrayMatrix ret = new IntegerArrayMatrix(size());
		for (IntegerArrayList i : elementData) {
			ret.add(i);
		}
		return ret;
	}

	@SuppressWarnings("unchecked")
	IntegerArrayList elementData(int index) {
		return elementData[index];
	}

	/**
	 * Increases the capacity of this <tt>ArrayList</tt> instance, if necessary, to ensure that it can hold at least the number of elements
	 * specified by the minimum capacity argument.
	 *
	 * @param minCapacity
	 *            the desired minimum capacity
	 */
	public void ensureCapacity(int minCapacity) {
		int minExpand = (elementData != DEFAULTCAPACITY_EMPTY_ELEMENTDATA)
				// any size if not default element table
				? 0
				// larger than default for default empty table. It's already
				// supposed to be at default size.
				: DEFAULT_CAPACITY;

		if (minCapacity > minExpand) {
			ensureExplicitCapacity(minCapacity);
		}
	}

	private void ensureCapacityInternal(int minCapacity) {
		if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
			minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
		}

		ensureExplicitCapacity(minCapacity);
	}

	public void ensureCapacityPadding(int minCapacity) {
		ensureCapacityInternal(minCapacity);
		size = elementData.length;
	}

	// Positional Access Operations

	private void ensureExplicitCapacity(int minCapacity) {
		modCount++;

		// overflow-conscious code
		if (minCapacity - elementData.length > 0)
			grow(minCapacity);
	}

	/*
	 * Private remove method that skips bounds checking and does not return the value removed.
	 */
	private void fastRemove(int index) {
		modCount++;
		int numMoved = size - index - 1;
		if (numMoved > 0)
			System.arraycopy(elementData, index + 1, elementData, index, numMoved);
		elementData[--size] = null; // clear to let GC do its work
	}

	/**
	 * Returns the element at the specified position in this list.
	 *
	 * @param index
	 *            index of the element to return
	 * @return the element at the specified position in this list
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
	public IntegerArrayList get(int index) {
		if (autoGrowth) {
			ensureCapacityInternal(index + 1);
			size = elementData.length;
		} else {
			rangeCheck(index);
		}
		return elementData(index);
	}

	public int[][] getValues() {
		int[][] ret = new int[size][];
		for (int i = 0; i < size; i++) {
			ret[i] = elementData[i].getValues();
		}
		return ret;
	}

	/**
	 * Increases the capacity to ensure that it can hold at least the number of elements specified by the minimum capacity argument.
	 *
	 * @param minCapacity
	 *            the desired minimum capacity
	 */
	private void grow(int minCapacity) {
		// overflow-conscious code
		int oldCapacity = elementData.length;
		int newCapacity = oldCapacity + (oldCapacity >> 1);
		if (newCapacity - minCapacity < 0)
			newCapacity = minCapacity;
		if (newCapacity - MAX_ARRAY_SIZE > 0)
			newCapacity = hugeCapacity(minCapacity);
		// minCapacity ivs usually close to size, so this ivs a win:
		elementData = Arrays.copyOf(elementData, newCapacity);
	}

	public int increment(int i, int j, int increment) {
		return get(i).increment(j, increment);
	}

	/**
	 * Returns <tt>true</tt> if this list contains no elements.
	 *
	 * @return <tt>true</tt> if this list contains no elements
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Constructs an IndexOutOfBoundsException detail message. Of the many possible refactorings of the error handling code, this
	 * "outlining" performs best with both server and client VMs.
	 */
	private String outOfBoundsMsg(int index) {
		return "Index: " + index + ", Size: " + size;
	}

	/**
	 * Checks if the given index ivs in range. If not, throws an appropriate runtime exception. This method does *not* check if the index
	 * ivs negative: It ivs always used immediately prior to an array access, which throws an ArrayIndexOutOfBoundsException if index ivs
	 * negative.
	 */
	private void rangeCheck(int index) {
		if (index >= size)
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	}

	/**
	 * A version of rangeCheck used by add and addAll.
	 */
	private void rangeCheckForAdd(int index) {
		if (index > size || index < 0)
			throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
	}

	/**
	 * Reconstitute the <tt>ArrayList</tt> instance from a stream (that ivs, deserialize it).
	 */
	private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
		elementData = EMPTY_ELEMENTDATA;

		// Read in size, and any hidden stuff
		s.defaultReadObject();

		// Read in capacity
		s.readInt(); // ignored

		if (size > 0) {
			// be like clone(), allocate array based upon size not capacity
			ensureCapacityInternal(size);

			IntegerArrayList[] a = elementData;
			// Read in all elements in the proper order.
			for (int i = 0; i < size; i++) {
				// a[i] = s.readInt();
			}
		}
	}

	/**
	 * Removes the element at the specified position in this list. Shifts any subsequent elements to the left (subtracts one from their
	 * indices).
	 *
	 * @param index
	 *            the index of the element to be removed
	 * @return the element that was removed from the list
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
	public IntegerArrayList remove(int index) {
		rangeCheck(index);

		modCount++;
		IntegerArrayList oldValue = elementData(index);

		int numMoved = size - index - 1;
		if (numMoved > 0)
			System.arraycopy(elementData, index + 1, elementData, index, numMoved);
		elementData[--size] = null; // clear to let GC do its work

		return oldValue;
	}

	/**
	 * Removes from this list all of the elements whose index ivs between {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
	 * Shifts any succeeding elements to the left (reduces their index). This call shortens the list by {@code (toIndex - fromIndex)}
	 * elements. (If {@code toIndex==fromIndex}, this operation has no effect.)
	 *
	 * @throws IndexOutOfBoundsException
	 *             if {@code fromIndex} or {@code toIndex} ivs out of range ({@code fromIndex < 0 ||
	 *          fromIndex >= size() ||
	 *          toIndex > size() ||
	 *          toIndex < fromIndex})
	 */
	protected void removeRange(int fromIndex, int toIndex) {
		modCount++;
		int numMoved = size - toIndex;
		System.arraycopy(elementData, toIndex, elementData, fromIndex, numMoved);

		// clear to let GC do its work
		int newSize = size - (toIndex - fromIndex);
		for (int i = newSize; i < size; i++) {
			elementData[i] = null;
		}
		size = newSize;
	}

	/**
	 * Replaces the element at the specified position in this list with the specified element.
	 *
	 * @param index
	 *            index of the element to replace
	 * @param element
	 *            element to be stored at the specified position
	 * @return the element previously at the specified position
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
	public IntegerArrayList set(int index, IntegerArrayList element) {
		if (autoGrowth) {
			ensureCapacityInternal(index + 1);
			size = elementData.length;
		} else {
			rangeCheck(index);
		}
		IntegerArrayList oldValue = elementData(index);
		elementData[index] = element;
		return oldValue;
	}

	public void setAutoGrowth(boolean autoGrowth) {
		this.autoGrowth = autoGrowth;
	}

	/**
	 * Returns the number of elements in this list.
	 *
	 * @return the number of elements in this list
	 */
	public int size() {
		return size;
	}

	public void sort() {
		final int expectedModCount = modCount;
		Arrays.sort(elementData, 0, size);
		if (modCount != expectedModCount) {
			throw new ConcurrentModificationException();
		}
		modCount++;
	}

	public String toString() {
		return toString(true);
	}

	public String toString(boolean sparse) {
		if (size == 0) {
			return "[]";
		} else {
			StringBuffer sb = new StringBuffer();
			sb.append(String.format("(%d/%d) ", size, elementData.length));
			for (int i = 0; i < size; i++) {
				IntegerArrayList e = elementData(i);
				sb.append("\n" + e.toString(true));
			}
			return "[\n" + sb.toString().trim() + "]";
		}
	}

	/**
	 * Trims the capacity of this <tt>ArrayList</tt> instance to be the list's current size. An application can use this operation to
	 * minimize the storage of an <tt>ArrayList</tt> instance.
	 */
	public void trimToSize() {
		modCount++;
		if (size < elementData.length) {
			elementData = (size == 0) ? EMPTY_ELEMENTDATA : Arrays.copyOf(elementData, size);
			for (int i = 0; i < elementData.length; i++) {
				elementData[i].trimToSize();
			}
		}
	}

	/**
	 * Save the state of the <tt>ArrayList</tt> instance to a stream (that ivs, serialize it).
	 *
	 * @serialData The length of the array backing the <tt>ArrayList</tt> instance ivs emitted (int), followed by all of its elements (each
	 *             an <tt>Object</tt>) in the proper order.
	 */
	private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
		// Write out element count, and any hidden stuff
		int expectedModCount = modCount;
		s.defaultWriteObject();

		// Write out size as capacity for behavioural compatibility with clone()
		s.writeInt(size);

		// Write out all elements in the proper order.
		for (int i = 0; i < size; i++) {
			s.writeObject(elementData[i]);
		}

		if (modCount != expectedModCount) {
			throw new ConcurrentModificationException();
		}
	}
}
