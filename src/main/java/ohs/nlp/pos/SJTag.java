package ohs.nlp.pos;

import java.util.List;

import ohs.utils.Generics;

public enum SJTag {

	/**
	 * 일반명사
	 */
	NNG,

	/**
	 * 고유명사
	 */
	NNP,

	/**
	 * 의존명사
	 */
	NNB,

	/**
	 * 수사
	 */
	NR,

	/**
	 * 대명사
	 */
	NP,

	/**
	 * 동사
	 */
	VV,

	/**
	 * 형용사
	 */
	VA,

	/**
	 * 보조 용언
	 */
	VX,

	/**
	 * 긍정 지정사
	 */
	VCP,

	/**
	 * 부정 지정사
	 */
	VCN,

	/**
	 * 관형사
	 */
	MM,

	/**
	 * 일반 부사
	 */
	MAG,

	/**
	 * 접속 부사
	 */
	MAJ,

	/**
	 * 감탄사
	 */
	IC,

	/**
	 * 주격 조사
	 */
	JKS,

	/**
	 * 보격 조사
	 */
	JKC,

	/**
	 * 관형격 조사
	 */
	JKG,

	/**
	 * 목적격 조사
	 */
	JKO,

	/**
	 * 부사격 조사
	 */
	JKB,

	/**
	 * 호격 조사
	 */
	JKV,

	/**
	 * 인용격 조사
	 */
	JKQ,

	/**
	 * 보조사
	 */
	JX,

	/**
	 * 접속 조사
	 */
	JC,

	/**
	 * 선어말 어미
	 */
	EP,

	/**
	 * 종결 어미
	 */
	EF,

	/**
	 * 연결 어미
	 */
	EC,

	/**
	 * 명사형 전성 어미
	 */
	ETN,

	/**
	 * 관형형 전성 어미
	 */
	ETM,

	/**
	 * 체언 접두사
	 */
	XPN,

	/**
	 * 명사 파생 접미사
	 */
	XSN,

	/**
	 * 동사 파생 접미사
	 */
	XSV,

	/**
	 * 형용사 파생 접미사
	 */
	XSA,

	/**
	 * 어근
	 */
	XR,

	/**
	 * 마침표, 물음표, 느낌표
	 */
	SF,

	/**
	 * 쉼표, 가운뎃점, 콜론, 빗금
	 */
	SP,

	/**
	 * 따옴표, 괄호표, 줄표
	 */
	SS,

	/**
	 * 줄임표
	 */
	SE,

	/**
	 * 붙임표 (물결, 숨김, 빠짐)
	 */
	SO,

	/**
	 * 기타기호 (논리수학기호, 화폐기호)
	 */
	SW,

	/**
	 * 명사추정범주
	 */
	NF,

	/**
	 * 용언추정범주
	 */
	NV,

	/**
	 * 명사추정범주
	 */
	NA,

	/**
	 * 외국어
	 */
	SL,

	/**
	 * 한자
	 */
	SH,

	/**
	 * 숫자
	 */
	SN;

	/**
	 * 체언
	 */
	public static final SJTag[] CHE_EON = { NNG, NNP, NNB, NR, NP };

	/**
	 * 용언
	 */
	public static final SJTag[] YONG_EON = { VV, VA, VX, VCP, VCN };

	/**
	 * 관형사
	 */
	public static final SJTag[] GWAN_HYEONG_SA = { MM };

	/**
	 * 부사
	 */
	public static final SJTag[] BU_SA = { MAG, MAJ };

	/**
	 * 감탄사
	 */
	public static final SJTag[] GAM_TAN_SA = { IC };

	/**
	 * 조사
	 */
	public static final SJTag[] JO_SA = { JKS, JKC, JKG, JKO, JKB, JKV, JKQ, JX, JC };

	/**
	 * 선어말어미
	 */
	public static final SJTag[] SEON_EO_MAL_EO_MI = { EP };

	/**
	 * 어말어미
	 */
	public static final SJTag[] EO_MAL_EO_MI = { EF, EC, ETN, ETM };

	/**
	 * 접두사
	 */
	public static final SJTag[] JEOM_DU_SA = { XPN };

	/**
	 * 접미사
	 */
	public static final SJTag[] JEOM_MI_SA = { XSN, XSV, XSA };

	/**
	 * 어근
	 */
	public static final SJTag[] EO_GEUN = { XR };

	/**
	 * 어근
	 */
	public static final SJTag[] BU_HO = { SF, SP, SS, SE, SO, SW };

	/**
	 * 분석불능
	 */
	public static final SJTag[] NOT_ANALYZED = { NF, NV, NA };

	/**
	 * 분석불능
	 */
	public static final SJTag[] NOT_KOREAN = { SL, SH, SN };

	/**
	 * 어휘형태소
	 * 
	 * 체언 - 일반 명사(NNG), 고유 명사(NNP), 의존 명사(NNB), 수사(NR), 대명사(NP)
	 * 
	 * 용언 - 동사(VV), 형용사(VA), 보조 용언(VX), 긍정 지정사(VCP), 부정 지정사(VCN)
	 * 
	 * 부사 - 일반 부사(MAG), 접속부사(MAJ)
	 * 
	 * 관형사 - 관형사(MM)
	 * 
	 * 감탄사 - 감탄사(IC)
	 * 
	 */
	public static SJTag[] MORPHEME_VOCAB = null;

	/**
	 * 문법형태소
	 * 
	 * 조사 - 주격 조사(JKS), 보격 조사(JKC), 관형격 조사(JKG), 목적격 조사(JKO), 부사격 조사(JKB), 호격 조사(JKV), 인용격 조사(JKQ), 보조사(JX), 접속 조사(JC)
	 * 
	 * 선어말 어미 - 선어말 어미(EP)
	 * 
	 * 어말 어미 - 종결 어미(EF), 연결 어미(EC), 명사형 전성 어미(ETN), 관형형 전성 어미(ETM)
	 * 
	 * 접두사 - 체언 접두사(XPN)
	 * 
	 * 접미사 - 명사 파생 접미사(XSN), 동사 파생 접미사(XSV), 형용사 파생 접미사(XSA)
	 * 
	 * 
	 */
	public static SJTag[] MORPHEME_GRAMMAR = null;

	static {
		List<SJTag> list = Generics.newArrayList();
		for (SJTag t : CHE_EON) {
			list.add(t);
		}

		for (SJTag t : YONG_EON) {
			list.add(t);
		}

		for (SJTag t : BU_SA) {
			list.add(t);
		}

		for (SJTag t : GWAN_HYEONG_SA) {
			list.add(t);
		}

		for (SJTag t : GAM_TAN_SA) {
			list.add(t);
		}

		MORPHEME_VOCAB = list.toArray(new SJTag[list.size()]);

		list = Generics.newArrayList();

		for (SJTag t : JEOM_DU_SA) {
			list.add(t);
		}

		for (SJTag t : JEOM_MI_SA) {
			list.add(t);
		}

		for (SJTag t : JO_SA) {
			list.add(t);
		}

		for (SJTag t : EO_MAL_EO_MI) {
			list.add(t);
		}

		for (SJTag t : SEON_EO_MAL_EO_MI) {
			list.add(t);
		}

		MORPHEME_GRAMMAR = list.toArray(new SJTag[list.size()]);
	}

}
