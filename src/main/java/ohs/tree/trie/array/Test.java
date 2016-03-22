/*
 * Copyright 2010 Christos Gioran
 *
 * This file is part of DoubleArrayTrie.
 *
 * DoubleArrayTrie is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DoubleArrayTrie is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with DoubleArrayTrie.  If not, see <http://www.gnu.org/licenses/>.
 */
package ohs.tree.trie.array;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Test {

	public static void main(String[] args) {
		System.out.println("process begins.");
		testCountingTrie();

		System.out.println("process ends.");
	}

	public static void testRobustnessUnderStress() {

		final int ALPHABET_SIZE = 120;
		final int NUMBER_OF_STRINGS = 100000;
		final int MAXIMUM_STRING_SIZE = 100;

		List<IntegerList> data = new ArrayList<IntegerList>(NUMBER_OF_STRINGS);
		AbstractDoubleArrayTrie trie = new DoubleArrayTrieImpl(ALPHABET_SIZE);
		Random rng = new Random();

		for (int i = 0; i < NUMBER_OF_STRINGS; i++) {
			IntegerList toAdd = new IntegerArrayList(MAXIMUM_STRING_SIZE);
			for (int j = 0; j < MAXIMUM_STRING_SIZE; j++) {
				toAdd.add(rng.nextInt(ALPHABET_SIZE));
			}
			data.add(toAdd);
		}

		for (IntegerList list : data) {
			int removeSize = rng.nextInt(list.size()) + 1;
			for (; removeSize > 0; removeSize--) {
				list.remove(list.size() - 1);
			}
		}

		// TODO
		// Insert a NOT_FOUND test here that is on the one hand random and
		// on the other succeeds always regardless of the generated strings.
	}

	public static void testMarginCases() {
		AbstractDoubleArrayTrie trie = new DoubleArrayTrieImpl(3);

		IntegerList empty = new IntegerArrayList();

		IntegerList notIn = new IntegerArrayList();
		notIn.add(1);
		notIn.add(2);

		IntegerList one = new IntegerArrayList();
		one.add(2);
		one.add(1);
	}

	public static void testCountingTrie() {
		CountingTrie trie = new CountingTrie(4);
		IntegerList string1 = new IntegerArrayList();
		string1.add(0);
		string1.add(1);
		string1.add(2);
		string1.add(3);
		IntegerList string2 = new IntegerArrayList();
		string2.add(1);
		string2.add(2);
		string2.add(3);
		trie.addToTrie(string1);
		trie.addToTrie(string2);

		IntegerList string3 = new IntegerArrayList();
		string3.add(1);
		string3.add(2);

		KDATrie kdatrie = new KDATrie(4);
		kdatrie.addToTrie(string1);
		kdatrie.addToTrie(string2);
		kdatrie.addToTrie(string3);

	}

}
