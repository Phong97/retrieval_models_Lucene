package utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * SearchResult lưu kết quả tìm được, gồm internal ID (docid), external ID (docno),
 * và relevance score
 */
public class SearchResult {
	
	protected int docid;
	protected String docno;
	protected double score;
	
	public SearchResult(int docid, String docno, double score) {
		this.docid = docid;
		this.docno = docno;
		this.score = score;
	}
	
	public int getDocid() {
		return docid;
	}
	
	public String getDocno() {
		return docno;
	}
	
	public Double getScore() {
		return score;
	}
	
	public SearchResult setScore(double score) {
		this.score = score;
		return this;
	}
	
		
	/**
	 * Ghi các kết quả tìm được theo format của TREC
	 *
	 * @param writer  output
	 * @param queryid query id
	 * @param runname Tên mỗi thuật toán
	 * @param results Danh sách kết quả
	 * @param n       Chỉ lấy top-n kết quả
	 */
	public static void writeTRECFormat(PrintStream writer, String queryid, String runname, List<SearchResult> results, int n) {
		for (int ix = 0; ix < results.size() && ix < n; ix++) {
			int rank = ix + 1;
			writer.printf("%s 0 %s %d %.8f %s\n", queryid, results.get(ix).docno, rank, results.get(ix).score, runname);
			rank++;
			if (rank >= n) {
				break;
			}
		}
	}
	
	/**
	 * Đọc kết quả theo format của TREC
	 *
	 * @param file kết quả theo format của TREC
	 * @return Một ánh xạ từ queryid sang danh sách kết quả
	 * @throws IOException
	 */
	public static Map<String, List<SearchResult>> readTRECFormat(File file) throws IOException {
		Map<String, List<SearchResult>> results = new TreeMap<>();
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		String line;
		while((line = reader.readLine()) != null) {
			String[] splits = line.split("\\s+");
			String qid = splits[0];
			String docno = splits[2];
			double score = Double.parseDouble(splits[4]);
			results.putIfAbsent(qid, new ArrayList<>());
			results.get(qid).add(new SearchResult(-1, docno, score));
		}
		reader.close();
		return results;
	}
	
}