package ohs.ir.medical.clef.ehealth_2014;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;

import ohs.types.Pair;

/**
 * The AbbreviationExtractor class implements a simple algorithm for extraction of abbreviations and their definitions from biomedical text.
 * Abbreviations (short forms) are extracted from the input file, and those abbreviations for which a definition (long form) is found are
 * printed out, along with that definition, one per line.
 * 
 * A file consisting of short-form/long-form pairs (tab separated) can be specified in tandem with the -testlist option for the purposes of
 * evaluating the algorithm.
 * 
 * @see <a href="http://biotext.berkeley.edu/papers/psb03.pdf">A Simple Algorithm for Identifying Abbreviation Definitions in Biomedical
 *      Text</a> A.S. Schwartz, M.A. Hearst; Pacific Symposium on Biocomputing 8:451-462(2003) for a detailed description of the algorithm.
 * 
 * @author Ariel Schwartz
 * @version 03/12/03
 */
public class AbbreviationExtractor {

	private Pattern p1 = Pattern.compile("^[a-zA-Z]+?$");

	private Pattern p2 = Pattern.compile("^[a-zA-Z\\- &']+$");

	private Pattern p3 = Pattern.compile("[\\w]+( or [\\w]+){2,}+", Pattern.CASE_INSENSITIVE);

	private Pattern p4 = Pattern.compile("[\\w]+( and [\\w]+){2,}+", Pattern.CASE_INSENSITIVE);

	private boolean accept(String shortForm, String longForm) {
		if (shortForm.length() == 1 || longForm.length() <= 1) {
			return false;
		}

		String bestLongForm = findBestLongForm(shortForm, longForm);

		if (bestLongForm == null) {
			return false;
		}

		StringTokenizer tokenizer = new StringTokenizer(bestLongForm, " \t\n\r\f-");

		int longFormSize = tokenizer.countTokens();
		int shortFormSize = shortForm.length();

		for (int i = shortFormSize - 1; i >= 0; i--) {
			if (!Character.isLetterOrDigit(shortForm.charAt(i))) {
				shortFormSize--;
			}
		}
		if (bestLongForm.length() < shortForm.length() || bestLongForm.indexOf(shortForm + " ") > -1

				|| bestLongForm.endsWith(shortForm) || longFormSize > shortFormSize * 2

				|| longFormSize > shortFormSize + 5 || shortFormSize > 10) {
			return false;
		}

		return true;
	}

	private boolean accept2(String shortForm, String longForm) {
		if (shortForm.equals("OR") || shortForm.equals("AND")) {
			return false;
		}

		/*
		 * Accept a short form composed of English characters
		 */
		if (!p1.matcher(shortForm).find()) {
			// System.out.printf("[%text : %text]\n", shortForm, longForm);
			return false;
		}

		/*
		 * Accept a long form composed of English characters, -, & and '.
		 */
		if (!p2.matcher(longForm).find()) {
			// System.out.printf("[%text : %text]\n", shortForm, longForm);
			return false;
		}

		/*
		 * Reject a long form which is a sequential words with OR.
		 */

		if (p3.matcher(longForm).find()) {
			return false;
		}

		/*
		 * Reject a long form which is a sequential words with AND.
		 */

		if (p4.matcher(longForm).find()) {
			return false;
		}

		/*
		 * Accept a short form which has some lower characters but has a high ratio.
		 */

		{
			int numCapitals = 0;
			for (int i = 0; i < shortForm.length(); i++) {
				if (Character.isUpperCase(shortForm.charAt(i))) {
					numCapitals++;
				}
			}
			double ratio = numCapitals / shortForm.length();

			if (ratio < 0.5) {
				return false;
			}
		}

		{

		}

		return true;
	}

	public List<Pair<String, String>> extract(String text) {
		String tmpStr, longForm = "", shortForm = "";
		String currSentence = "";
		int openParenIndex, closeParenIndex = -1, sentenceEnd, newCloseParenIndex, tmpIndex = -1;
		boolean newParagraph = true;
		StringTokenizer shortTokenizer;
		List<Pair<String, String>> ret = new ArrayList<Pair<String, String>>();
		String[] lines = text.split(" ");

		for (int u = 0; u < lines.length; u++) {
			String line = lines[u];

			if (line.length() == 0 || newParagraph && !Character.isUpperCase(line.charAt(0))) {
				currSentence = "";
				newParagraph = true;
				continue;
			}

			newParagraph = false;
			line += " ";
			currSentence += line;
			openParenIndex = currSentence.indexOf(" (");

			do {
				if (openParenIndex > -1) {
					openParenIndex++;
				}

				sentenceEnd = Math.max(currSentence.lastIndexOf(". "), currSentence.lastIndexOf(", "));

				if ((openParenIndex == -1) && (sentenceEnd == -1)) {
					// Do nothing
				} else if (openParenIndex == -1) {
					currSentence = currSentence.substring(sentenceEnd + 2);
				} else if ((closeParenIndex = currSentence.indexOf(')', openParenIndex)) > -1) {
					sentenceEnd = Math.max(currSentence.lastIndexOf(". ", openParenIndex), currSentence.lastIndexOf(", ", openParenIndex));
					if (sentenceEnd == -1)
						sentenceEnd = -2;
					longForm = currSentence.substring(sentenceEnd + 2, openParenIndex);
					shortForm = currSentence.substring(openParenIndex + 1, closeParenIndex);
				}

				if (shortForm.length() > 0 || longForm.length() > 0) {
					if (shortForm.length() > 1 && longForm.length() > 1) {
						if ((shortForm.indexOf('(') > -1) && ((newCloseParenIndex = currSentence.indexOf(')', closeParenIndex + 1)) > -1)) {
							shortForm = currSentence.substring(openParenIndex + 1, newCloseParenIndex);
							closeParenIndex = newCloseParenIndex;
						}
						if ((tmpIndex = shortForm.indexOf(", ")) > -1)
							shortForm = shortForm.substring(0, tmpIndex);
						if ((tmpIndex = shortForm.indexOf("; ")) > -1)
							shortForm = shortForm.substring(0, tmpIndex);
						shortTokenizer = new StringTokenizer(shortForm);
						if (shortTokenizer.countTokens() > 2 || shortForm.length() > longForm.length()) {
							// Long form in ( )
							tmpIndex = currSentence.lastIndexOf(" ", openParenIndex - 2);
							tmpStr = currSentence.substring(tmpIndex + 1, openParenIndex - 1);
							longForm = shortForm;
							shortForm = tmpStr;
							if (!hasCapital(shortForm))
								shortForm = "";
						}

						if (isValidShortForm(shortForm)) {
							String sf = shortForm.trim();
							String lf = longForm.trim();

							String test1 = text.substring(sentenceEnd + 2, openParenIndex);
							String test2 = currSentence.substring(openParenIndex + 1, closeParenIndex);

							if (accept(sf, lf) && accept2(sf, lf)) {
								ret.add(new Pair<String, String>(sf, lf));
							}
						}
					}
					currSentence = currSentence.substring(closeParenIndex + 1);
				} else if (openParenIndex > -1) {
					if ((currSentence.length() - openParenIndex) > 200)
						// Matching close paren was not found
						currSentence = currSentence.substring(openParenIndex + 1);
					break; // Read next line
				}
				shortForm = "";
				longForm = "";
			} while ((openParenIndex = currSentence.indexOf(" (")) > -1);
		}

		return ret;
	}

	private Vector extractAbbrPairs(String inFile) {
		String str, tmpStr, longForm = "", shortForm = "";
		String currSentence = "";
		int openParenIndex, closeParenIndex = -1, sentenceEnd, newCloseParenIndex, tmpIndex = -1;
		boolean newParagraph = true;
		StringTokenizer shortTokenizer;
		Vector candidates = new Vector();

		try {
			BufferedReader fin = new BufferedReader(new FileReader(inFile));
			while ((str = fin.readLine()) != null) {
				if (str.length() == 0 || newParagraph && !Character.isUpperCase(str.charAt(0))) {
					currSentence = "";
					newParagraph = true;
					continue;
				}
				newParagraph = false;
				str += " ";
				currSentence += str;
				openParenIndex = currSentence.indexOf(" (");
				do {
					if (openParenIndex > -1)
						openParenIndex++;
					sentenceEnd = Math.max(currSentence.lastIndexOf(". "), currSentence.lastIndexOf(", "));
					if ((openParenIndex == -1) && (sentenceEnd == -1)) {
						// Do nothing
					} else if (openParenIndex == -1) {
						currSentence = currSentence.substring(sentenceEnd + 2);
					} else if ((closeParenIndex = currSentence.indexOf(')', openParenIndex)) > -1) {
						sentenceEnd = Math.max(currSentence.lastIndexOf(". ", openParenIndex),
								currSentence.lastIndexOf(", ", openParenIndex));
						if (sentenceEnd == -1)
							sentenceEnd = -2;
						longForm = currSentence.substring(sentenceEnd + 2, openParenIndex);
						shortForm = currSentence.substring(openParenIndex + 1, closeParenIndex);
					}
					if (shortForm.length() > 0 || longForm.length() > 0) {
						if (shortForm.length() > 1 && longForm.length() > 1) {
							if ((shortForm.indexOf('(') > -1)
									&& ((newCloseParenIndex = currSentence.indexOf(')', closeParenIndex + 1)) > -1)) {
								shortForm = currSentence.substring(openParenIndex + 1, newCloseParenIndex);
								closeParenIndex = newCloseParenIndex;
							}
							if ((tmpIndex = shortForm.indexOf(", ")) > -1)
								shortForm = shortForm.substring(0, tmpIndex);
							if ((tmpIndex = shortForm.indexOf("; ")) > -1)
								shortForm = shortForm.substring(0, tmpIndex);
							shortTokenizer = new StringTokenizer(shortForm);
							if (shortTokenizer.countTokens() > 2 || shortForm.length() > longForm.length()) {
								// Long form in ( )
								tmpIndex = currSentence.lastIndexOf(" ", openParenIndex - 2);
								tmpStr = currSentence.substring(tmpIndex + 1, openParenIndex - 1);
								longForm = shortForm;
								shortForm = tmpStr;
								if (!hasCapital(shortForm))
									shortForm = "";
							}
							if (isValidShortForm(shortForm)) {
								accept(shortForm.trim(), longForm.trim());
							}
						}
						currSentence = currSentence.substring(closeParenIndex + 1);
					} else if (openParenIndex > -1) {
						if ((currSentence.length() - openParenIndex) > 200)
							// Matching close paren was not found
							currSentence = currSentence.substring(openParenIndex + 1);
						break; // Read next line
					}
					shortForm = "";
					longForm = "";
				} while ((openParenIndex = currSentence.indexOf(" (")) > -1);
			}
			fin.close();
		} catch (Exception ioe) {
			ioe.printStackTrace();
			System.out.println(currSentence);
			System.out.println(tmpIndex);
		}
		return candidates;
	}

	private String findBestLongForm(String shortForm, String longForm) {
		int sIndex;
		int lIndex;
		char currChar;

		sIndex = shortForm.length() - 1;
		lIndex = longForm.length() - 1;
		for (; sIndex >= 0; sIndex--) {
			currChar = Character.toLowerCase(shortForm.charAt(sIndex));
			if (!Character.isLetterOrDigit(currChar))
				continue;
			while (((lIndex >= 0) && (Character.toLowerCase(longForm.charAt(lIndex)) != currChar))
					|| ((sIndex == 0) && (lIndex > 0) && (Character.isLetterOrDigit(longForm.charAt(lIndex - 1)))))
				lIndex--;
			if (lIndex < 0)
				return null;
			lIndex--;
		}
		lIndex = longForm.lastIndexOf(" ", lIndex) + 1;
		return longForm.substring(lIndex);
	}

	private boolean hasCapital(String str) {
		for (int i = 0; i < str.length(); i++)
			if (Character.isUpperCase(str.charAt(i)))
				return true;
		return false;
	}

	private boolean hasLetter(String str) {
		for (int i = 0; i < str.length(); i++)
			if (Character.isLetter(str.charAt(i)))
				return true;
		return false;
	}

	private boolean isValidShortForm(String str) {
		return (hasLetter(str) && (Character.isLetterOrDigit(str.charAt(0)) || (str.charAt(0) == '(')));
	}

}
