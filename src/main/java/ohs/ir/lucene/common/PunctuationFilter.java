package ohs.ir.lucene.common;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.FilteringTokenFilter;
import org.apache.lucene.util.Version;

/**
 * Removes stop words from a token stream.
 * 
 * <a name="version"/>
 * <p>
 * You must specify the required {@link Version} compatibility when creating NoiseFilter:
 * <ul>
 * <li>As of 3.1, NoiseFilter correctly handles Unicode 4.0 supplementary characters in stopwords and position increments are preserved
 * </ul>
 */

public final class PunctuationFilter extends FilteringTokenFilter {

	private static final Pattern p = Pattern.compile("\\p{Punct}+");
	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	public PunctuationFilter(TokenStream in) {
		super(in);
	}

	/**
	 * Returns the next input Token whose term() is not a stop word.
	 */
	@Override
	protected boolean accept() {
		String term = new String(termAtt.buffer(), 0, termAtt.length());
		Matcher m = p.matcher(term);
		int num_puncs = 0;
		if (m.find()) {
			num_puncs = m.groupCount();
		}

		return num_puncs < 2 ? true : false;
	}

}
