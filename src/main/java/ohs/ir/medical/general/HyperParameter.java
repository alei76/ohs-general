package ohs.ir.medical.general;

public class HyperParameter {

	public static HyperParameter parse(String[] parts) {
		int top_k = Integer.parseInt(parts[0]);
		int top_k_in_wiki = Integer.parseInt(parts[1]);
		int num_fb_docs = Integer.parseInt(parts[2]);
		int num_fb_words = Integer.parseInt(parts[3]);
		double dirichlet_prior = Double.parseDouble(parts[4]);
		double mixture_for_all_colls = Double.parseDouble(parts[5]);
		boolean useDocPrior = Boolean.parseBoolean(parts[6]);
		boolean useDoubleScoring = Boolean.parseBoolean(parts[7]);
		boolean useWiki = Boolean.parseBoolean(parts[8]);
		double mixture_for_fb_model = Double.parseDouble(parts[9]);
		boolean smoothCollectionMixtures = Boolean.parseBoolean(parts[10]);
		boolean adjustNumbers = Boolean.parseBoolean(parts[11]);

		HyperParameter ret = new HyperParameter(top_k, top_k_in_wiki, num_fb_docs, num_fb_words, dirichlet_prior, mixture_for_all_colls,
				useDocPrior, useDoubleScoring, useWiki, mixture_for_fb_model, smoothCollectionMixtures, adjustNumbers);
		return ret;
	}

	private double mixture_for_all_colls;

	private double dirichlet_prior;

	private int num_fb_words;

	private int num_fb_docs;

	private int top_k;

	private int top_k_in_wiki;

	private boolean useDocPrior;

	private boolean useDoubleScoring;

	private boolean useWiki;

	private double mixture_for_fb_model;

	boolean smoothCollectionMixtures;

	boolean adjustNumbers;

	public HyperParameter() {
		this(1000, 10, 5, 25, 2000, 0.5, false, false, false, 0.5, false, false);
	}

	public HyperParameter(int top_k, int top_k_in_wiki, int num_fb_docs, int num_fb_words, double dirichlet_prior,
			double mixture_for_all_colls, boolean useDocPrior, boolean useDoubleScoring, boolean useWiki, double mixture_for_fb_model,
			boolean smoothCollectionMixtures, boolean adjustNumbers) {
		super();
		this.mixture_for_all_colls = mixture_for_all_colls;
		this.dirichlet_prior = dirichlet_prior;
		this.num_fb_words = num_fb_words;
		this.num_fb_docs = num_fb_docs;
		this.top_k = top_k;
		this.top_k_in_wiki = top_k_in_wiki;
		this.useDocPrior = useDocPrior;
		this.useDoubleScoring = useDoubleScoring;
		this.useWiki = useWiki;
		this.mixture_for_fb_model = mixture_for_fb_model;
		this.smoothCollectionMixtures = smoothCollectionMixtures;
		this.adjustNumbers = adjustNumbers;
	}

	public double getDirichletPrior() {
		return dirichlet_prior;
	}

	public double getMixtureForAllCollections() {
		return mixture_for_all_colls;
	}

	public double getMixtureForFeedbackModel() {
		return mixture_for_fb_model;
	}

	public int getNumFBDocs() {
		return num_fb_docs;
	}

	public int getNumFBWords() {
		return num_fb_words;
	}

	public int getTopK() {
		return top_k;
	}

	public int getTopKInWiki() {
		return top_k_in_wiki;
	}

	public boolean isAdjustNumbers() {
		return adjustNumbers;
	}

	public boolean isSmoothCollectionMixtures() {
		return smoothCollectionMixtures;
	}

	public boolean isUseDocPrior() {
		return useDocPrior;
	}

	public boolean isUseDoubleScoring() {
		return useDoubleScoring;
	}

	public boolean isUseWiki() {
		return useWiki;
	}

	public void setAdjustNumbers(boolean adjustNumbers) {
		this.adjustNumbers = adjustNumbers;
	}

	public void setDirichletPrior(double dirichlet_prior) {
		this.dirichlet_prior = dirichlet_prior;
	}

	public void setMixtureForAllCollections(double mixture_for_all_colls) {
		this.mixture_for_all_colls = mixture_for_all_colls;
	}

	public void setMixtureForFeedbackModel(double mixture_for_fb_model) {
		this.mixture_for_fb_model = mixture_for_fb_model;
	}

	public void setNumFBDocs(int num_fb_docs) {
		this.num_fb_docs = num_fb_docs;
	}

	public void setNumFBWords(int num_fb_words) {
		this.num_fb_words = num_fb_words;
	}

	public void setSmoothCollMixtures(boolean smoothCollectionMixtures) {
		this.smoothCollectionMixtures = smoothCollectionMixtures;
	}

	public void setTopK(int top_k) {
		this.top_k = top_k;
	}

	public void setTopKInWiki(int top_k_in_wiki) {
		this.top_k_in_wiki = top_k_in_wiki;
	}

	public void setUseDocPrior(boolean useDocPrior) {
		this.useDocPrior = useDocPrior;
	}

	public void setUseDoubleScoring(boolean useDoubleScoring) {
		this.useDoubleScoring = useDoubleScoring;
	}

	public void setUseWiki(boolean useWiki) {
		this.useWiki = useWiki;
	}

	public String toString() {
		return toString(false);
	}

	public String toString(boolean onlyValues) {
		StringBuffer sb = new StringBuffer();
		if (onlyValues) {
			sb.append(String.format("%s", top_k));
			sb.append(String.format("_%s", top_k_in_wiki));
			sb.append(String.format("_%s", num_fb_docs));
			sb.append(String.format("_%s", num_fb_words));
			sb.append(String.format("_%s", dirichlet_prior));
			sb.append(String.format("_%s", mixture_for_all_colls));
			sb.append(String.format("_%s", useDocPrior));
			sb.append(String.format("_%s", useDoubleScoring));
			sb.append(String.format("_%s", useWiki));
			sb.append(String.format("_%s", mixture_for_fb_model));
			sb.append(String.format("_%s", smoothCollectionMixtures));
			sb.append(String.format("_%s", adjustNumbers));
		} else {
			sb.append(String.format("top_k\t%s\n", top_k));
			sb.append(String.format("top_k_in_wiki\t%s\n", top_k_in_wiki));
			sb.append(String.format("num_fb_docs\t%s\n", num_fb_docs));
			sb.append(String.format("num_fb_words\t%s\n", num_fb_words));
			sb.append(String.format("dirichlet_prior\t%s\n", dirichlet_prior));
			sb.append(String.format("mixture_for_all_colls\t%s\n", mixture_for_all_colls));
			sb.append(String.format("useDocPrior\t%s\n", useDocPrior));
			sb.append(String.format("useDoubleScoring\t%s\n", useDoubleScoring));
			sb.append(String.format("useWiki\t%s\n", useWiki));
			sb.append(String.format("mixture_for_fb_model\t%s\n", mixture_for_fb_model));
			sb.append(String.format("smoothCollectionMixtures\t%s\n", smoothCollectionMixtures));
			sb.append(String.format("adjustNumbers\t%s\n", adjustNumbers));
		}
		return sb.toString();
	}
}
