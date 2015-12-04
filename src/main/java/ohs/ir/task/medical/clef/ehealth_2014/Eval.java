package ohs.ir.task.medical.clef.ehealth_2014;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math.stat.inference.TTestImpl;

import ohs.io.IOUtils;
import ohs.types.Counter;
import ohs.types.CounterMap;

public class Eval {

	public static void doSignificanceTests() throws Exception {
		String dirName = EHPath.TASK_THREE_DIR + "evals/";

		List<CounterMap<String, String>> runs = new ArrayList<CounterMap<String, String>>();
		String[] metricNames = { "P_10", "ndcg_cut_10" };

		for (int i = 0; i < 7; i++) {
			String fn1 = dirName + String.format("KISTI_EN_RUN.%d.dat", i + 1);
			String fn2 = dirName + String.format("KISTI_EN_RUN.%d.dat.ndcg", i + 1);

			CounterMap<String, String> cm = new CounterMap<String, String>();

			Counter<String> c1 = getValues(fn1, metricNames[0]);
			Counter<String> c2 = getValues(fn2, metricNames[1]);

			cm.setCounter(metricNames[0], c1);
			cm.setCounter(metricNames[1], c1);

			runs.add(cm);
		}
		
		
		
		

		CounterMap<String, String> baseline = runs.get(0);

		StringBuffer sb = new StringBuffer();
		sb.append("Run ID\tP(0.05)\tP(0.01)\tNDCG(0.05)\tNDCG(0.01)\n");

		for (int i = 1; i < 7; i++) {
			CounterMap<String, String> run = runs.get(i);
			boolean[] resForP10 = tests(baseline.getCounter(metricNames[0]), run.getCounter(metricNames[0]));
			boolean[] resForNDCG10 = tests(baseline.getCounter(metricNames[1]), run.getCounter(metricNames[1]));

			sb.append(String.format("RUN%d", i + 1));

			for (int j = 0; j < resForP10.length; j++) {
				sb.append(String.format("\t%s", resForP10[j]));
			}

			for (int j = 0; j < resForNDCG10.length; j++) {
				sb.append(String.format("\t%s", resForNDCG10[j]));
			}

			if (i != 7) {
				sb.append("\n");
			}
		}

		System.out.println(sb.toString());
	}
 
	public static Counter<String> getValues(String fn, String type) throws Exception {
		Counter<String> ret = new Counter<String>();

		List<String> lines = IOUtils.readLines(fn);

		for (String line : lines) {
			String[] parts = line.split("\t");

			for (int i = 0; i < parts.length; i++) {
				parts[i] = parts[i].trim();
			}

			if (parts[0].equals(type)) {
				String qId = parts[1];
				double value = Double.parseDouble(parts[2]);
				ret.setCount(qId, value);
			}
		}
		ret.removeKey("all");
		return ret;
	}

	public static void main(String[] args) throws Exception {
		System.out.println("process begins. ");
		doSignificanceTests();

		System.out.println("process ends. ");
	}

	public static boolean[] tests(Counter<String> c1, Counter<String> c2) throws Exception {
		Set<String> set = new TreeSet<String>();
		set.addAll(c1.keySet());
		set.addAll(c2.keySet());

		List<String> keys = new ArrayList<String>(set);
		double[] v1 = new double[keys.size()];
		double[] v2 = new double[keys.size()];

		for (int i = 0; i < keys.size(); i++) {
			v1[i] = c1.getCount(keys.get(i));
			v2[i] = c2.getCount(keys.get(i));
		}

		TTestImpl tt = new TTestImpl();
		boolean isSignificantlyImproved1 = tt.pairedTTest(v1, v2, 0.05);
		boolean isSignificantlyImproved2 = tt.pairedTTest(v1, v2, 0.01);
		return new boolean[] { isSignificantlyImproved1, isSignificantlyImproved2 };
	}
}
