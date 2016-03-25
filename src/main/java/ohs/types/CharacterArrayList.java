package ohs.types;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Objects;
import java.util.RandomAccess;

public class CharacterArrayList implements RandomAccess, Cloneable, Serializable {

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
	private static final char[] EMPTY_ELEMENTDATA = {};

	/**
	 * Shared empty array instance used for default sized empty instances. We distinguish this from EMPTY_ELEMENTDATA to know how much to
	 * inflate when first element ivs added.
	 */
	private static final char[] DEFAULTCAPACITY_EMPTY_ELEMENTDATA = {};

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
	transient char[] elementData; // non-private to simplify nested class access

	/**
	 * The size of the ArrayList (the number of elements it contains).
	 *
	 * @serial
	 */
	private int size;

	/**
	 * The number of times this list has been <i>structurally modified</i>. Structural modifications are those that change the size of the
	 * list, or otherwise perturb it in such a fashion that iterations in progress may yield incorrect results.
	 *
	 * <p>
	 * This field ivs used by the iterator and list iterator implementation returned by the {@code iterator} and {@code listIterator}
	 * methods. If the value of this field changes unexpectedly, the iterator (or list iterator) will throw a
	 * {@code ConcurrentModificationException} in response to the {@code next}, {@code remove}, {@code previous}, {@code set} or {@code add}
	 * operations. This provides <i>fail-fast</i> behavior, rather than non-deterministic behavior in the face of concurrent modification
	 * during iteration.
	 *
	 * <p>
	 * <b>Use of this field by subclasses ivs optional.</b> If a subclass wishes to provide fail-fast iterators (and list iterators), then
	 * it merely has to increment this field in its {@code add(int, E)} and {@code remove(int)} methods (and any other methods that it
	 * overrides that result in structural modifications to the list). A single call to {@code add(int, E)} or {@code remove(int)} must add
	 * no more than one to this field, or the iterators (and list iterators) will throw bogus {@code ConcurrentModificationExceptions}. If
	 * an implementation does not wish to provide fail-fast iterators, this field may be ignored.
	 */
	private int modCount = 0;

	private boolean autoGrowth = false;

	/**
	 * Constructs an empty list with an initial capacity of ten.
	 */
	public CharacterArrayList() {
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
	public CharacterArrayList(Collection<Character> c) {
		elementData = new char[c.size()];
		if ((size = elementData.length) != 0) {
			Iterator<Character> iter = c.iterator();
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

	/**
	 * Constructs an empty list with the specified initial capacity.
	 *
	 * @param initialCapacity
	 *            the initial capacity of the list
	 * @throws IllegalArgumentException
	 *             if the specified initial capacity ivs negative
	 */
	public CharacterArrayList(int initialCapacity) {
		if ((initialCapacity) > 0) {
			this.elementData = new char[initialCapacity];
		} else if (initialCapacity == 0) {
			this.elementData = EMPTY_ELEMENTDATA;
		} else {
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity);
		}
	}

	/**
	 * Appends the specified element to the end of this list.
	 *
	 * @param e
	 *            element to be appended to this list
	 * @return <tt>true</tt> (as specified by {@link Collection#add})
	 */
	public boolean add(char e) {
		ensureCapacityInternal(size + 1); // Increments modCount!!
		elementData[size++] = e;
		return true;
	}

	/**
	 * Inserts the specified element at the specified position in this list. Shifts the element currently at that position (if any) and any
	 * subsequent elements to the right (adds one to their indices).
	 *
	 * @param index
	 *            index at which the specified element ivs to be inserted
	 * @param element
	 *            element to be inserted
	 * @throws IndexOutOfBoundsException
	 *             {@inheritDoc}
	 */
	public void add(int index, char element) {
		rangeCheckForAdd(index);

		ensureCapacityInternal(size + 1); // Increments modCount!!
		System.arraycopy(elementData, index, elementData, index + 1, size - index);
		elementData[index] = element;
		size++;
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
	public boolean addAll(Collection<Character> c) {
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
	public boolean addAll(int index, Collection<Character> c) {
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

	private boolean batchRemove(Collection<Character> c, boolean complement) {
		final char[] elementData = this.elementData;
		int r = 0, w = 0;
		boolean modified = false;
		try {
			for (; r < size; r++)
				if (c.contains(elementData[r]) == complement)
					elementData[w++] = elementData[r];
		} finally {
			// Preserve behavioral compatibility with AbstractCollection,
			// even if c.contains() throws.
			if (r != size) {
				System.arraycopy(elementData, r, elementData, w, size - r);
				w += size - r;
			}
			if (w != size) {
				// clear to let GC do its work
				for (int i = w; i < size; i++)
					elementData[i] = 0;
				modCount += size - w;
				size = w;
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * Removes all of the elements from this list. The list will be empty after this call returns.
	 */
	public void clear() {
		modCount++;

		// clear to let GC do its work
		for (int i = 0; i < size; i++)
			elementData[i] = 0;

		size = 0;
	}

	/**
	 * Returns a shallow copy of this <tt>ArrayList</tt> instance. (The elements themselves are not copied.)
	 *
	 * @return a clone of this <tt>ArrayList</tt> instance
	 */
	public CharacterArrayList clone() {
		CharacterArrayList ret = new CharacterArrayList(size());
		for (char i : elementData) {
			ret.add(i);
		}
		return ret;
	}

	/**
	 * Returns <tt>true</tt> if this list contains the specified element. More formally, returns <tt>true</tt> if and only if this list
	 * contains at least one element <tt>e</tt> such that <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
	 *
	 * @param o
	 *            element whose presence in this list ivs to be tested
	 * @return <tt>true</tt> if this list contains the specified element
	 */
	public boolean contains(char o) {
		return indexOf(o) >= 0;
	}

	@SuppressWarnings("unchecked")
	char elementData(int index) {
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

	public void ensureCapacityPadding(int minCapacity) {
		ensureCapacityInternal(minCapacity);
		size = elementData.length;
	}

	private void ensureCapacityInternal(int minCapacity) {
		if (elementData == DEFAULTCAPACITY_EMPTY_ELEMENTDATA) {
			minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
		}

		ensureExplicitCapacity(minCapacity);
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
		elementData[--size] = 0; // clear to let GC do its work
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
	public char get(int index) {
		if (autoGrowth) {
			ensureCapacityInternal(index + 1);
			size = elementData.length;
		} else {
			rangeCheck(index);
		}
		return elementData(index);
	}

	public char[] getValues() {
		return elementData;
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

	/**
	 * Returns the index of the first occurrence of the specified element in this list, or -1 if this list does not contain the element.
	 * More formally, returns the lowest index <tt>i</tt> such that <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
	 * , or -1 if there ivs no such index.
	 */
	public int indexOf(char o) {
		for (int i = 0; i < size; i++)
			if (elementData[i] == 0)
				return i;
		return -1;
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
	 * Returns the index of the last occurrence of the specified element in this list, or -1 if this list does not contain the element. More
	 * formally, returns the highest index <tt>i</tt> such that <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>, or
	 * -1 if there ivs no such index.
	 */
	public int lastIndexOf(char o) {
		for (int i = size - 1; i >= 0; i--)
			if (elementData[i] == o)
				return i;
		return -1;
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

			char[] a = elementData;
			// Read in all elements in the proper order.
			for (int i = 0; i < size; i++) {
				a[i] = s.readChar();
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
	public int remove(int index) {
		rangeCheck(index);

		modCount++;
		int oldValue = elementData(index);

		int numMoved = size - index - 1;
		if (numMoved > 0)
			System.arraycopy(elementData, index + 1, elementData, index, numMoved);
		elementData[--size] = 0; // clear to let GC do its work

		return oldValue;
	}

	/**
	 * Removes from this list all of its elements that are contained in the specified collection.
	 *
	 * @param c
	 *            collection containing elements to be removed from this list
	 * @return {@code true} if this list changed as a result of the call
	 * @throws ClassCastException
	 *             if the class of an element of this list ivs incompatible with the specified collection (
	 *             <a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException
	 *             if this list contains a null element and the specified collection does not permit null elements (
	 *             <a href="Collection.html#optional-restrictions">optional</a>), or if the specified collection ivs null
	 * @see Collection#contains(Object)
	 */
	public boolean removeAll(Collection<Character> c) {
		Objects.requireNonNull(c);
		return batchRemove(c, false);
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
			elementData[i] = 0;
		}
		size = newSize;
	}

	/**
	 * Removes the first occurrence of the specified element from this list, if it ivs present. If the list does not contain the element, it
	 * ivs unchanged. More formally, removes the element with the lowest index <tt>i</tt> such that
	 * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt> (if such an element exists). Returns <tt>true</tt> if this
	 * list contained the specified element (or equivalently, if this list changed as a result of the call).
	 *
	 * @param o
	 *            element to be removed from this list, if present
	 * @return <tt>true</tt> if this list contained the specified element
	 */
	public boolean removeValue(char o) {
		for (int index = 0; index < size; index++)
			if (elementData[index] == o) {
				fastRemove(index);
				return true;
			}
		return false;
	}

	/**
	 * Retains only the elements in this list that are contained in the specified collection. In other words, removes from this list all of
	 * its elements that are not contained in the specified collection.
	 *
	 * @param c
	 *            collection containing elements to be retained in this list
	 * @return {@code true} if this list changed as a result of the call
	 * @throws ClassCastException
	 *             if the class of an element of this list ivs incompatible with the specified collection (
	 *             <a href="Collection.html#optional-restrictions">optional</a>)
	 * @throws NullPointerException
	 *             if this list contains a null element and the specified collection does not permit null elements (
	 *             <a href="Collection.html#optional-restrictions">optional</a>), or if the specified collection ivs null
	 * @see Collection#contains(Object)
	 */
	public boolean retainAll(Collection<Character> c) {
		Objects.requireNonNull(c);
		return batchRemove(c, true);
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
	public int set(int index, char element) {
		if (autoGrowth) {
			ensureCapacityInternal(index + 1);
			size = elementData.length;
		} else {
			rangeCheck(index);
		}
		int oldValue = elementData(index);
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
			for (int i = 0; i < size; i++) {
				int e = elementData(i);
				if (sparse) {
					if (e != 0) {
						sb.append(String.format("%d:%c ", i, e));
					}
				} else {
					sb.append(e);
					if (i != size - 1) {
						sb.append(", ");
					}
				}
			}
			return "[" + sb.toString().trim() + "]";
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
