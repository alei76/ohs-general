package ohs.ir.medical.trec.cds_2014;

import java.text.DecimalFormat;

public class Test {

	public static void main(String[] args) {

		String qId = "1";

		System.out.println(new DecimalFormat("00").format(Double.parseDouble(qId)));
	}
}
