package ohs.ir.medical.general;

import java.util.ArrayList;
import java.util.List;

import ohs.matrix.SparseVector;
import ohs.types.Pair;

public class PlmUtils {
	public static final double pi = Math.PI;

	// Cumulative distribution function of Arc Kernel
	public static double ArcCDF(double x, double mean, double sigma) {
		double res;
		x = (x - mean) / sigma;

		if (x >= 1) {
			res = (pi - 1.0) * sigma / 2.0;
		} else if (x < -1) {
			res = 0;
		} else if (x < 0) {
			res = sigma * (Math.asin(x) + pi / 2.0 - Math.sqrt(1 - x * x) + (1 - Math.abs(x)) * (1 - Math.abs(x)) / 2.0) / 2.0;
		} else {
			res = (pi - 1.0) * sigma / 2.0
					- sigma * (Math.asin(-x) + pi / 2.0 - Math.sqrt(1 - x * x) + (1 - Math.abs(x)) * (1 - Math.abs(x)) / 2.0) / 2.0;
		}
		return res;
	}

	// Cumulative distribution function of Circle Kernel
	public static double CircleCDF(double x, double mean, double sigma) {
		double res;
		x = (x - mean) / sigma;

		if (x >= 1) {
			res = (pi - 2.0) * sigma;
		} else if (x < -1) {
			res = 0;
		} else if (x < 0) {
			res = sigma * (Math.asin(x) + pi / 2.0 - Math.sqrt(1 - x * x));
		} else {
			res = (pi - 2.0) * sigma - sigma * (Math.asin(-x) + pi / 2.0 - Math.sqrt(1 - x * x));
		}
		return res;
	}

	// Cumulative distribution function of Cosine Kernel
	public static double CosineCDF(double x, double mean, double sigma) {
		double res;
		x = (x - mean) / sigma;
		if (x >= 1) {
			res = sigma;
		} else if (x < -1) {
			res = 0;
		} else if (x < 0) {
			res = sigma * (1 + x - Math.sin(pi * x) / pi) / 2.0;
		} else {
			res = sigma - sigma * (1 - x + Math.sin(pi * x) / pi) / 2.0;
		}
		return res;
	}

	public static double GaussianCDF(double x, double mean, double sigma) {
		double res;
		x = (x - mean) / sigma;
		if (x == 0) {
			res = 0.5;
		} else {
			double oor2pi = 1 / (Math.sqrt(2 * pi));
			double t = 1 / (1 + 0.2316419 * Math.abs(x));
			t *= oor2pi * Math.exp(-0.5 * x * x)
					* (0.31938153 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429))));
			if (x >= 0) {
				res = 1 - t;
			} else {
				res = t;
			}
		}
		return res;
	}

	public static List<Pair<Integer, Integer>> getQueryLocsInDocument(SparseVector queryModel, List<Integer> words) {
		List<Pair<Integer, Integer>> ret = new ArrayList<Pair<Integer, Integer>>();
		for (int j = 0; j < words.size(); j++) {
			int w = words.get(j);
			if (queryModel.location(w) < 0) {
				continue;
			}
			ret.add(new Pair<Integer, Integer>(j, w));
		}
		return ret;

	}

	public static double PropagationCount(double dis, int propFunction) {
		dis = Math.abs(dis);
		switch (propFunction) {
		case -1:
			if (dis <= 1.0)
				return 1.0;
			else
				return 0;
		case 1:
			if (dis <= 1.0)
				return (1 + Math.cos(pi * dis)) / 2.0;
			else
				return 0;
		case 2:
			if (dis <= 1.0)
				return 1 - dis;
			else
				return 0;
		case 3:
			if (dis <= 1.0)
				return (1 - dis + Math.sqrt(1 - dis * dis)) / 2.0;
			else
				return 0;
		case 4:
			if (dis <= 1.0)
				return Math.sqrt(1 - dis * dis);
			else
				return 0;
		case 0:
		default:
			return Math.exp(-dis * dis / 2);
		}
	}

	public static double PropagationCountSum(double pos, double doc_len, int propFunction, double sigma) {
		double ret = 0;
		double psg_len = 0;

		switch (propFunction) {
		case -1:
			if (sigma > doc_len - pos) {
				psg_len += doc_len - pos;
			} else {
				psg_len += sigma;
			}

			if (sigma > pos) {
				psg_len += pos;
			} else {
				psg_len += sigma;
			}
			break;

		case 1:
			psg_len = CosineCDF(doc_len, pos, sigma) - CosineCDF(0, pos, sigma);
			break;
		case 2:
			psg_len = TriangleCDF(doc_len, pos, sigma) - TriangleCDF(0, pos, sigma);
			break;
		case 3:
			psg_len = ArcCDF(doc_len, pos, sigma) - ArcCDF(0, pos, sigma);
			break;
		case 4:
			psg_len = CircleCDF(doc_len, pos, sigma) - CircleCDF(0, pos, sigma);
			break;
		case 0:
		default:
			psg_len = Math.sqrt(2 * pi) * sigma * (GaussianCDF(doc_len, pos, sigma) - GaussianCDF(0, pos, sigma));
			break;
		}

		return psg_len;
	}

	// Cumulative distribution function of Triangle Kernel
	public static double TriangleCDF(double x, double mean, double sigma) {
		double res;
		x = (x - mean) / sigma;
		if (x >= 1) {
			res = sigma;
		} else if (x < -1) {
			res = 0;
		} else if (x < 0) {
			res = sigma * (1 - Math.abs(x)) * (1 - Math.abs(x)) / 2.0;
		} else {
			res = sigma - sigma * (1 - x) * (1 - x) / 2.0;
		}
		return res;
	}
}
