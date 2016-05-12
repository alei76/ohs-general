package ohs.eden.keyphrase;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import ohs.io.FileUtils;
import ohs.io.TextFileReader;
import ohs.types.Counter;
import ohs.types.SetMap;
import ohs.types.StrPair;
import ohs.utils.Generics;
import ohs.utils.StopWatch;
import ohs.utils.StrUtils;

public class KeywordLoader {

	public static void main(String[] args) throws Exception {
		System.out.println("process begins.");

		// {
		// KeywordData kwdData = new KeywordData();
		// kwdData.read(KPPath.KEYWORD_DATA_SER_FILE.replace("_data", "_data_clusters"));
		//
		// KeywordLoader l = new KeywordLoader();
		// l.load(kwdData);
		// }

		{
			KeywordLoader l = new KeywordLoader();
			l.loadPatentKeywordsMap(KPPath.KEYWORD_PATENT_FILE);
		}

		System.out.println("process ends.");
	}

	private Connection con;

	private int batch_size = 10000;

	public KeywordLoader() {

	}

	private void deleteTable(String tableName) throws Exception {
		Statement stmt = con.createStatement();
		String query = "delete from " + tableName;
		int deletedRows = stmt.executeUpdate(query);

		if (deletedRows > 0) {
			System.out.println("Deleted All Rows In The Table Successfully...");
		} else {
			System.out.println("Table already empty.");
		}

		con.commit();
		stmt.close();
	}

	public void load(KeywordData kwdData) throws Exception {
		open();

		loadKeywords(kwdData);
		loadKeywordsMap(kwdData);

		con.close();

	}

	private void loadKeywords(KeywordData kwdData) throws Exception {
		printMetaData("Keywords");
		deleteTable("Keywords");

		SetMap<Integer, Integer> clusterToKwds = kwdData.getClusterToKeywords();
		Map<Integer, Integer> clusterToLabel = kwdData.getClusterToLabel();

		int num_kwds = 0;

		/*
		 * kwdid, kor_kwd, eng_kwd, cid, is_label, kwd_freq
		 */
		String sql = "insert into Keywords values (?,?,?,?,?,?)";
		PreparedStatement pstmt = con.prepareStatement(sql);

		StopWatch stopWatch = StopWatch.newStopWatch();

		List<Integer> cids = Generics.newArrayList();

		{
			Counter<Integer> c = Generics.newCounter();
			for (int cid : clusterToKwds.keySet()) {
				c.setCount(cid, clusterToKwds.get(cid).size());
			}

			cids = c.getSortedKeys();
		}

		for (int cid : cids) {
			Counter<Integer> c = Generics.newCounter();

			for (int kwdid : clusterToKwds.get(cid)) {
				int kwd_freq = kwdData.getKeywordFreqs()[kwdid];
				c.setCount(kwdid, kwd_freq);
			}

			for (int kwdid : c.getSortedKeys()) {
				StrPair kwdp = kwdData.getKeywordIndexer().getObject(kwdid);
				String[] two = kwdp.asArray();

				two = StrUtils.unwrap(two);

				String korKwd = two[0];
				String engKwd = two[1];

				// if (korKwd.length() == 0) {
				// continue;
				// }

				int is_label = clusterToLabel.get(cid) == kwdid ? 1 : 0;
				int kwd_freq = kwdData.getKeywordFreqs()[kwdid];

				// System.out.println(kwdStr);

				if (++num_kwds % batch_size == 0) {
					int[] res = pstmt.executeBatch();
					con.commit();
					System.out.printf("\r[%d, %s]", num_kwds, stopWatch.stop());
				}

				pstmt.setInt(1, kwdid);
				pstmt.setString(2, korKwd);
				pstmt.setString(3, engKwd);
				pstmt.setInt(4, cid);
				pstmt.setBoolean(5, is_label > 0 ? true : false);
				pstmt.setInt(6, kwd_freq);
				pstmt.addBatch();
			}
		}

		pstmt.executeBatch();
		con.commit();
		pstmt.close();

		System.out.printf("\r[%d, %s]\n", num_kwds, stopWatch.stop());

	}

	private void loadKeywordsMap(KeywordData kwdData) throws Exception {
		printMetaData("Keywords_Map");
		deleteTable("Keywords_Map");

		StopWatch stopWatch = StopWatch.newStopWatch();

		/*
		 * cn, kwdid
		 */
		String sql = "insert into Keywords_Map values (?,?)";
		PreparedStatement pstmt = con.prepareStatement(sql);
		int num_kwds = 0;

		for (int kwdid : kwdData.getKeywordToDocs().keySet()) {
			for (int docid : kwdData.getKeywordToDocs().get(kwdid)) {
				String cn = kwdData.getDocumentIndxer().getObject(docid);

				if (++num_kwds % batch_size == 0) {
					pstmt.executeBatch();
					con.commit();
					System.out.printf("\r[%d, %s]", num_kwds, stopWatch.stop());
				}

				pstmt.setString(1, cn);
				pstmt.setInt(2, kwdid);
				pstmt.addBatch();
			}
		}

		pstmt.executeBatch();
		con.commit();
		pstmt.close();

		System.out.printf("\r[%d, %s]\n", num_kwds, stopWatch.stop());

	}

	public void loadPatentKeywordsMap(String fileName) throws Exception {
		open();

		TextFileReader reader = new TextFileReader(fileName);
		reader.setPrintNexts(false);
		reader.setPrintSize(batch_size);

		List<String> lines = Generics.newArrayList();

		while (reader.hasNext()) {
			reader.printProgress();

			String line = reader.next();

			if (lines.size() == batch_size) {
				/*
				 * cn, kwdid
				 */
				String sql = "insert into Keywords_Map values (?,?)";
				PreparedStatement pstmt = con.prepareStatement(sql);

				for (String s : lines) {
					String[] parts = s.split("\t");
					String cn = parts[0];
					int kwdid = Integer.parseInt(parts[1]);

					pstmt.setString(1, cn);
					pstmt.setInt(2, kwdid);
					pstmt.addBatch();
				}

				pstmt.executeBatch();
				con.commit();
				pstmt.close();

				lines = null;
				lines = Generics.newArrayList();
			} else {
				lines.add(line);
			}
		}
		reader.printProgress();
		reader.close();

		con.close();
	}

	private void open() throws Exception {
		Class.forName("com.mysql.jdbc.Driver");
		String[] lines = FileUtils.readText(KPPath.DB_ACCOUNT_FILE).split("\t");

		StrUtils.trim(lines);

		String url = String.format("jdbc:mysql://%s:3306/authority", lines[0]);
		String id = lines[1];
		String pwd = lines[2];

		con = DriverManager.getConnection(url, id, pwd);
		con.setAutoCommit(false);
	}

	private void printMetaData(String tableName) throws Exception {
		DatabaseMetaData meta = con.getMetaData();
		ResultSet resultSet = meta.getColumns(null, null, "Keywords", null);
		while (resultSet.next()) {
			String name = resultSet.getString("COLUMN_NAME");
			String type = resultSet.getString("TYPE_NAME");
			int size = resultSet.getInt("COLUMN_SIZE");

			System.out.println("Column name: [" + name + "]; type: [" + type + "]; size: [" + size + "]");
		}
	}

}
