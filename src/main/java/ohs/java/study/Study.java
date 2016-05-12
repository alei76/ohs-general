package ohs.java.study;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import ohs.utils.Generics;

public class Study {

	interface TestFunctionalInterface<T> {
		public T doSomething(T t1, T t2);
	}

	public static void main(String[] args) {
		System.out.println("process begins.");

		test01();

		System.out.println("process ends.");
	}

	public static void test01() {
		List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);

		List<Integer> numbers2 = Generics.newArrayList();

		numbers.forEach(new Consumer<Integer>() {
			@Override
			public void accept(Integer value) {
				System.out.println(value);

				numbers2.add(value);
			}
		});

		numbers.forEach((Integer value) -> System.out.println(value));

		System.out.println("-------------");

		numbers.forEach(System.out::println);
	}

	public static void test02() {

		List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6);

		TestFunctionalInterface<String> stringAdder = (String s1, String s2) -> s1 + s2;

	}
}
