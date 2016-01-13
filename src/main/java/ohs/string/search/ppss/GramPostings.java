package ohs.string.search.ppss;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ohs.string.search.ppss.Gram.Type;
import ohs.types.DeepMap;

/**
 * 
 * 
 * @author Heung-Seon Oh
 */
public class GramPostings implements Serializable {
	private List<GramPostingEntry> entries;

	private DeepMap<Type, Integer, Integer> typeLenLoc;

	public GramPostings() {
		entries = new ArrayList<GramPostingEntry>();

		typeLenLoc = new DeepMap<Type, Integer, Integer>();
	}

	public List<GramPostingEntry> getEntries() {
		return entries;
	}

	public GramPostingEntry getEntry(int i) {
		return entries.get(i);
	}

	public DeepMap<Type, Integer, Integer> getTypeLengthLocs() {
		return typeLenLoc;
	}

	public int size() {
		return entries.size();
	}

	@Override
	public String toString() {
		return toString(false);
	}

	public String toString(boolean showDetails) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < entries.size(); i++) {
			sb.append(entries.get(i).toString());
			if (i != entries.size() - 1) {
				sb.append(" ");
			}
		}

		if (showDetails) {
			sb.append("\n");
			Type[] types = new Type[] { Type.PREFIX, Type.PIVOT };
			for (int i = 0; i < types.length; i++) {
				Type type = types[i];
				sb.append(type.getSymbol() + " -> ");

				Map<Integer, Integer> lenLocs = typeLenLoc.get(type, false);

				if (lenLocs != null) {
					List<Integer> lens = new ArrayList<Integer>(lenLocs.keySet());
					Collections.sort(lens);

					for (int j = 0; j < lens.size(); j++) {
						int len = lens.get(j);
						int loc = lenLocs.get(len);
						sb.append(String.format("(%d, %d)", len, loc));

						if (j != lens.size() - 1) {
							sb.append(" ");
						}
					}
				}

				if (i != types.length - 1) {
					sb.append("\n");
				}
			}
		}

		return sb.toString();
	}
}
