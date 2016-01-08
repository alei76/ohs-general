package ohs.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * from Stanford Core NLP
 * 
 * Class to gather unsafe operations into one place.
 * 
 * @author dlwh
 *
 */
public class ErasureUtils {
	@SuppressWarnings("unchecked")
	public static <T> T[][] mkT2DArray(Class<?> klass, int[] dim) {
		if (dim.length != 2)
			throw new RuntimeException("dim should be an array of size 2.");
		return (T[][]) (Array.newInstance(klass, dim));
	}

	/**
	 * Makes an array based on klass, but casts it to be of type T[]. This is a
	 * very unsafe operation and should be used carefully. Namely, you should
	 * ensure that klass is a subtype of T, or that klass is a supertype of T
	 * *and* that the array will not escape the generic constant *and* that
	 * klass is the same as the erasure of T.
	 * 
	 * @param <T>
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] mkTArray(Class<?> klass, int size) {
		return (T[]) (Array.newInstance(klass, size));

	}

	/**
	 * Does nothing, occasionally used to make Java happy that a value is used
	 */
	public static void noop(Object o) {
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> sortedIfPossible(Collection<T> collection) {
		List<T> result = new ArrayList<T>(collection);
		try {
			Collections.sort((List) result);
		} catch (ClassCastException e) {
			// unable to sort, just return the copy
		} catch (NullPointerException npe) {
			// this happens if there are null elements in the collection; just
			// return the copy
		}
		return result;
	}

	/**
	 * Casts an Object to a T
	 * 
	 * @param <T>
	 */
	@SuppressWarnings("unchecked")
	public static <T> T uncheckedCast(Object o) {
		return (T) o;
	}

	private ErasureUtils() {
	}
}
