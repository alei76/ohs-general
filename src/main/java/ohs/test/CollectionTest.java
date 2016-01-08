package ohs.test;

public class CollectionTest {

	public static void main(String[] args) {
		System.out.println("process begins.");
		String s = "A\nB";
		s = s.replace("\n", "\\n");
		s = s.replace("\\n", "\n");

		System.out.println(s);
		System.out.println("process ends.");
	}

}
