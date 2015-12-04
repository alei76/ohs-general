package ohs.java.study;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class Study {

	public static void main(String[] args) {
		System.out.println("process begins.");

		test01();

		System.out.println("process ends.");
	}

	public static void test01() {
		List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);

		numbers.forEach(new Consumer<Integer>() {
			public void accept(Integer value) {
				System.out.println(value);
			}
		});

		numbers.forEach((Integer value) -> System.out.println(value));

		numbers.forEach(System.out::println);
	}

	public static void test02() {

		List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);

		TestFunctionalInterface<String> stringAdder = (String s1, String s2) -> s1 + s2;

	}

	interface TestFunctionalInterface<T> {
		public T doSomething(T t1, T t2);
	}
}
