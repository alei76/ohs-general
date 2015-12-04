package ohs.string.search.ppss;

import java.io.BufferedReader;
import java.io.IOException;

import ohs.string.search.ppss.Gram.Type;
import ohs.types.ListMap;

/**
 * A utility class for handing grams.
 * 
 * @author Heung-Seon
 */
public class GramUtils {

	public static int getNumLinesToRead(BufferedReader reader) throws IOException {
		String line = null;

		while ((line = reader.readLine()) != null) {
			if (line.startsWith("## ")) {
				break;
			}
		}

		String[] parts = line.split("\t");
		int num_read = Integer.parseInt(parts[1]);
		return num_read;
	}

	public static int getStringLength(Gram[] grams) {
		int q = grams[0].getString().length();
		return grams.length + q - 1;
	}

	public static ListMap<Type, Integer> groupGramsByTypes(Gram[] grams, boolean allowPivotAsPrefix) {
		ListMap<Type, Integer> ret = new ListMap<Type, Integer>();
		for (int i = 0; i < grams.length; i++) {
			Gram gram = grams[i];
			Type type = gram.getType();

			if (!allowPivotAsPrefix) {
				ret.put(type, i);
			} else {
				if (type == Type.PREFIX || type == Type.PIVOT) {
					if (type == Type.PREFIX) {
						ret.put(type, i);
					} else if (type == Type.PIVOT) {
						ret.put(type, i);
						ret.put(Type.PREFIX, i);
					}
				} else {
					ret.put(type, i);
				}
			}
		}
		return ret;
	}

	public static String toString(Gram[] grams) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < grams.length; i++) {
			sb.append(grams[i].toString());
			if (i != grams.length - 1) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}
}
