package ohs.utils;

import java.util.Collection;

import ohs.matrix.Vector;
import ohs.types.Counter;

public class Conditions {

	public static <E> Collection<E> bigger(Collection<E> a, Collection<E> b) {
		return a.size() > b.size() ? a : b;
	}

	public static <E> Counter<E> bigger(Counter<E> a, Counter<E> b) {
		return a.size() > b.size() ? a : b;
	}

	public static Vector bigger(Vector a, Vector b) {
		return a.size() > b.size() ? a : b;
	}

	public static String longer(String a, String b) {
		return a.length() > b.length() ? a : b;
	}

	public static String shorter(String a, String b) {
		return a.length() < b.length() ? a : b;
	}

	public static <E> Collection<E> smaller(Collection<E> a, Collection<E> b) {
		return a.size() < b.size() ? a : b;
	}

	public static <E> Counter<E> smaller(Counter<E> a, Counter<E> b) {
		return a.size() < b.size() ? a : b;
	}

	public static Vector smaller(Vector a, Vector b) {
		return a.size() < b.size() ? a : b;
	}

	public static boolean isApproxEqual(double a, double b, double approx) {
		return Math.abs(a - b) <= approx ? true : false;
	}

	public static boolean isApproxEqual(int a, int b, int approx) {
		return Math.abs(a - b) <= approx ? true : false;
	}

	public static boolean isEqual(double a, double b) {
		return a == b ? true : false;
	}

	public static boolean isEqual(int a, int b) {
		return a == b ? true : false;
	}

	/**
	 * a > b
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isGreaterThan(int a, int b) {
		return a > b ? true : false;
	}

	/**
	 * start <= a < end
	 * 
	 * @param start
	 * @param end
	 * @param a
	 * @return
	 */
	public static boolean isInArrayRange(int start, int end, int a) {
		return a >= start && a < end ? true : false;
	}

	/**
	 * 
	 * start <= a <= end
	 * 
	 * @param start
	 * @param end
	 * @param a
	 * @return
	 */
	public static boolean isInClosedInterval(double start, double end, double a) {
		return a >= start && a <= end ? true : false;
	}

	/**
	 * start < a <= end
	 * 
	 * @param start
	 * @param end
	 * @param a
	 * @return
	 */
	public static boolean isInLeftOpenInterval(double start, double end, double a) {
		return a > start && a <= end ? true : false;
	}

	/**
	 * start < a < end
	 * 
	 * @param start
	 * @param end
	 * @param a
	 * @return
	 */
	public static boolean isInOpenInterval(double start, double end, double a) {
		return a > start && a < end ? true : false;
	}

	/**
	 * start <= a < end
	 * 
	 * @param start
	 * @param end
	 * @param a
	 * @return
	 */
	public static boolean isInRightOpenInterval(double start, double end, double a) {
		return a >= start && a < end ? true : false;
	}

	/**
	 * a < b
	 * 
	 * @param a
	 * @param b
	 * @return
	 */
	public static boolean isLessThan(int a, int b) {
		return a < b ? true : false;
	}

	public static char value(boolean a, char b, char c) {
		return a ? b : c;
	}

	public static double value(boolean a, double b, double c) {
		return a ? b : c;
	}

	public static <E> E value(boolean a, E b, E c) {
		return a ? b : c;
	}

	public static float value(boolean a, float b, float c) {
		return a ? b : c;
	}

	public static int value(boolean a, int b, int c) {
		return a ? b : c;
	}

	public static String value(boolean a, String b, String c) {
		return a ? b : c;
	}

}
