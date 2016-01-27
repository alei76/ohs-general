package ohs.ir.lucene.common;

import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.EnglishPossessiveFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.SetKeywordMarkerFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.std40.StandardTokenizer40;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;
import org.apache.lucene.util.Version;

import ohs.io.FileUtils;
import ohs.ir.medical.general.MIRPath;

/**
 * {@link Analyzer} for English.
 */
public final class MedicalEnglishAnalyzer extends StopwordAnalyzerBase {
	/**
	 * Atomically loads the DEFAULT_STOP_SET in a lazy fashion once the outer
	 * class accesses the static final set the first time.;
	 */
	private static class DefaultSetHolder {
		static final CharArraySet DEFAULT_STOP_SET = StandardAnalyzer.STOP_WORDS_SET;
	}

	public static MedicalEnglishAnalyzer newAnalyzer() throws Exception {
		return newAnalyzer(MIRPath.STOPWORD_INQUERY_FILE);
	}

	public static MedicalEnglishAnalyzer newAnalyzer(String stopwordFileName) throws Exception {
		Set<String> stopwords = FileUtils.readSet(stopwordFileName);
		return new MedicalEnglishAnalyzer(new CharArraySet(stopwords, true));
	}

	/**
	 * Returns an unmodifiable instance of the default stop words set.
	 * 
	 * @return default stop words set.
	 */
	public static CharArraySet getDefaultStopSet() {
		return DefaultSetHolder.DEFAULT_STOP_SET;
	}

	private final CharArraySet stemExclusionSet;

	public MedicalEnglishAnalyzer() {
		this(getDefaultStopSet(), CharArraySet.EMPTY_SET);
	}

	/**
	 * Builds an analyzer with the given stop words.
	 * 
	 * @param matchVersion
	 *            lucene compatibility version
	 * @param stopwords
	 *            a stopword set
	 */
	public MedicalEnglishAnalyzer(CharArraySet stopwords) {
		this(stopwords, CharArraySet.EMPTY_SET);
	}

	/**
	 * Builds an analyzer with the given stop words. If a non-empty stem
	 * exclusion set is provided this analyzer will add a
	 * {@link SetKeywordMarkerFilter} before stemming.
	 * 
	 * @param matchVersion
	 *            lucene compatibility version
	 * @param stopwords
	 *            a stopword set
	 * @param stemExclusionSet
	 *            a set of terms not to be stemmed
	 */
	public MedicalEnglishAnalyzer(CharArraySet stopwords, CharArraySet stemExclusionSet) {
		super(stopwords);
		this.stemExclusionSet = CharArraySet.unmodifiableSet(CharArraySet.copy(stemExclusionSet));
	}

	/**
	 * Creates a
	 * {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents} which
	 * tokenizes all the text in the provided {@link Reader}.
	 * 
	 * @return A
	 *         {@link org.apache.lucene.analysis.Analyzer.TokenStreamComponents}
	 *         built from an {@link StandardTokenizer} filtered with
	 *         {@link StandardFilter}, {@link EnglishPossessiveFilter},
	 *         {@link NumberFilter}, {@link NoiseFilter} ,
	 *         {@link SetKeywordMarkerFilter} if a stem exclusion set is
	 *         provided and {@link PorterStemFilter}.
	 */
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		final Tokenizer source;
		if (getVersion().onOrAfter(Version.LUCENE_4_7_0)) {
			source = new StandardTokenizer();
		} else {
			source = new StandardTokenizer40();
		}
		TokenStream result = new StandardFilter(source);
		result = new EnglishPossessiveFilter(result);
		result = new LengthFilter(result);
		result = new PunctuationFilter(result);
		result = new LowerCaseFilter(result);
		// result = new StopFilter(result, stopwords);
		result = new NumberStopFilter(result, stopwords);
		if (!stemExclusionSet.isEmpty())
			result = new SetKeywordMarkerFilter(result, stemExclusionSet);
		result = new PorterStemFilter(result);
		// result = new NumberFilter(result);
		return new TokenStreamComponents(source, result);
	}

}
