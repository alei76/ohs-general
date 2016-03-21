package com.medallia.word2vec;

import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

/** Provides search functionality */
public interface Searcher {
	/** Represents a match to a search word */
	public interface Match {
		/** {@link Ordering} which compares {@link Match#distance()} */
		Ordering<Match> ORDERING = Ordering.natural().onResultOf(new Function<Match, Double>() {
			@Override
			public Double apply(Match match) {
				return match.distance();
			}
		});
		/** {@link Function} which forwards to {@link #match()} */
		Function<Match, String> TO_WORD = new Function<Match, String>() {
			@Override
			public String apply(Match result) {
				return result.match();
			}
		};

		/** @return Cosine distance of the match */
		double distance();

		/** @return Matching word */
		String match();
	}

	/** Represents the similarity between two words */
	public interface SemanticDifference {
		/** @return Top matches to the given word which share this semantic relationship */
		List<Match> getMatches(String word, int maxMatches) throws UnknownWordException;
	}

	/** Exception when a word is unknown to the {@link Word2VecModel}'text vocabulary */
	public static class UnknownWordException extends Exception {
		UnknownWordException(String word) {
			super(String.format("Unknown search word '%text'", word));
		}
	}

	/** @return true if a word is inside the model'text vocabulary. */
	public boolean contains(String word);

	/** @return cosine similarity between two words. */
	public double cosineDistance(String s1, String s2) throws UnknownWordException;

	/** @return Top matches to the given vector */
	public List<Match> getMatches(final double[] vec, int maxNumMatches);

	/** @return Top matches to the given word */
	public List<Match> getMatches(String word, int maxMatches) throws UnknownWordException;

	public Word2VecModel getModel();

	/** @return Raw word vector */
	public ImmutableList<Double> getRawVector(String word) throws UnknownWordException;

	public double[] getVector(String word);

	/** @return {@link SemanticDifference} between the word vectors for the given */
	public SemanticDifference similarity(String s1, String s2) throws UnknownWordException;
}
