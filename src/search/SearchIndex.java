package search;

import utils.Utils;
import utils.SearchResult;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SearchIndex {

	private static List<SearchResult> list;

	public static void main(String[] args) {
		try {

			// index path
			String indexPath = "C:\\Users\\phong\\eclipse-workspace\\lab3\\index";
			// folder Ä‘á»ƒ xuáº¥t káº¿t quáº£
			String outputPath = "C:\\Users\\phong\\eclipse-workspace\\lab3\\output";

			// query vÃ  field cáº§n tÃ¬m kiáº¿m
			String field = "text";
			String query = "retrieval evaluation";

			// Analyzer includes options for text processing
			Analyzer analyzer = new Analyzer() {
				@Override
				protected TokenStreamComponents createComponents(String fieldName) {
					// BÆ°á»›c 1: tokenization (DÃ¹ng StandardTokenizer cá»§a Lucene)
					TokenStreamComponents ts = new TokenStreamComponents(new StandardTokenizer());
					// BÆ°á»›c 2: Chuyá»ƒn cÃ¡c token sang chá»¯ thÆ°á»�ng (lowercased)
					ts = new TokenStreamComponents(ts.getTokenizer(), new LowerCaseFilter(ts.getTokenStream()));
					// BÆ°á»›c 3: Loáº¡i bá»� cÃ¡c stop word
					ts = new TokenStreamComponents(ts.getTokenizer(),
							new StopFilter(ts.getTokenStream(), StopAnalyzer.ENGLISH_STOP_WORDS_SET));
					// BÆ°á»›c 4: Ã�p dá»¥ng stemming
					ts = new TokenStreamComponents(ts.getTokenizer(), new PorterStemFilter(ts.getTokenStream()));
					// ts = new TokenStreamComponents(ts.getTokenizer(), new
					// KStemFilter(ts.getTokenStream()));
					return ts;
				}
			};

			// TÃ¡ch query thÃ nh cÃ¡c token (dÃ¹ng analyzer giá»‘ng bÆ°á»›c indexing)
			List<String> queryTerms = Utils.tokenize(query, analyzer);

			Directory dir = FSDirectory.open(new File(indexPath).toPath());
			IndexReader index = DirectoryReader.open(dir);

			List<SearchResult> resultsBooleanAND = searchBooleanAND(index, field, queryTerms);
			List<SearchResult> resultsTFIDF = searchTFIDF(index, field, queryTerms);
			List<SearchResult> resultsVSMCosine = searchVSMCosine(index, field, queryTerms);

			// KhÃ´ng thay Ä‘á»•i cÃ¡c thiáº¿t láº­p output sau
			File dirOutput = new File(outputPath);
			dirOutput.mkdirs();

			{
				PrintStream writer = new PrintStream(new FileOutputStream(new File(dirOutput, "results_BooleanAND")));
				SearchResult.writeTRECFormat(writer, "0", "BooleanAND", resultsBooleanAND, resultsBooleanAND.size());
				SearchResult.writeTRECFormat(System.out, "0", "BooleanAND", resultsBooleanAND, 10);
				writer.close();
			}

			{
				PrintStream writer = new PrintStream(new FileOutputStream(new File(dirOutput, "results_TFIDF")));
				SearchResult.writeTRECFormat(writer, "0", "TFIDF", resultsTFIDF, resultsTFIDF.size());
				SearchResult.writeTRECFormat(System.out, "0", "TFIDF", resultsTFIDF, 10);
				writer.close();
			}

			{
				PrintStream writer = new PrintStream(new FileOutputStream(new File(dirOutput, "results_VSMCosine")));
				SearchResult.writeTRECFormat(writer, "0", "VSMCosine", resultsVSMCosine, resultsVSMCosine.size());
				SearchResult.writeTRECFormat(System.out, "0", "VSMCosine", resultsVSMCosine, 10);
				writer.close();
			}

			index.close();
			dir.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Hiá»‡n thá»±c Boolean AND
	 *
	 * @param index      Lucene index reader
	 * @param field      index field Ä‘á»ƒ tÃ¬m query
	 * @param queryTerms Danh sÃ¡ch cÃ¡c query term
	 * @return Danh sÃ¡ch káº¿t quáº£ (sáº¯p xáº¿p theo docno)
	 */
	public static List<SearchResult> searchBooleanAND(IndexReader index, String field, List<String> queryTerms)
			throws Exception {
		// Viáº¿t pháº§n hiá»‡n thá»±c cá»§a báº¡n á»Ÿ Ä‘Ã¢y
		IndexSearcher searcher = new IndexSearcher(index);
		searcher.setSimilarity(new BM25Similarity());
		Builder builder = new BooleanQuery.Builder();
		QueryParser parser = new QueryParser(field, new StandardAnalyzer());
		for (String string : queryTerms) {
			Query query = parser.parse(string);
			builder.add(query, BooleanClause.Occur.MUST);
		}
		BooleanQuery booleanQuery = builder.build();
		int top = index.numDocs();
		TopDocs docs = searcher.search(booleanQuery, top);
		list = new ArrayList<SearchResult>();
		for (ScoreDoc scoreDoc : docs.scoreDocs) {
			int docID = scoreDoc.doc;
			double score = scoreDoc.score;
			Document doc = index.document(docID);
			String docno = doc.get("docno");
			list.add(new SearchResult(docID, docno, score));
		}
		Collections.sort(list, new Comparator<SearchResult>() {
			@Override
			public int compare(SearchResult o1, SearchResult o2) {
				return o1.getDocno().compareTo(o2.getDocno());
			}
		});
		return list;
	}

	/**
	 * Hiá»‡n thá»±c TFxIDF
	 *
	 * @param index      Lucene index reader
	 * @param field      index field Ä‘á»ƒ tÃ¬m query
	 * @param queryTerms Danh sÃ¡ch cÃ¡c query term
	 * @return Danh sÃ¡ch káº¿t quáº£ (sáº¯p xáº¿p theo Ä‘á»™ liÃªn quan)
	 * @throws ParseException 
	 */
	public static List<SearchResult> searchTFIDF(IndexReader index, String field, List<String> queryTerms)
			throws IOException, ParseException {
		// Viáº¿t pháº§n hiá»‡n thá»±c cá»§a báº¡n á»Ÿ Ä‘Ã¢y
		IndexSearcher searcher = new IndexSearcher(index);

		searcher.setSimilarity(new ClassicSimilarity());
		QueryParser parser = new QueryParser(field, new StandardAnalyzer());
		String queryText = null;
		for (String string : queryTerms) {
			queryText += " " + string; 
		}
		Query query = parser.parse(queryText);
		int top = index.numDocs();
		TopDocs docs = searcher.search(query, top);
		list = new ArrayList<SearchResult>();
		for (ScoreDoc scoreDoc : docs.scoreDocs) {
			int docID = scoreDoc.doc;
			double score = scoreDoc.score;
			Document doc = index.document(docID);
			String docno = doc.get("docno");
			list.add(new SearchResult(docID, docno, score));
		}
		return list;
	}

	/**
	 * Hiá»‡n thá»±c VSM (cosine similarity)
	 *
	 * @param index      Lucene index reader
	 * @param field      index field Ä‘á»ƒ tÃ¬m query
	 * @param queryTerms Danh sÃ¡ch cÃ¡c query term
	 * @return Danh sÃ¡ch káº¿t quáº£ (sáº¯p xáº¿p theo Ä‘á»™ liÃªn quan)
	 * @throws ParseException 
	 */
	public static List<SearchResult> searchVSMCosine(IndexReader index, String field, List<String> queryTerms)
			throws IOException, ParseException {
		// Viáº¿t pháº§n hiá»‡n thá»±c cá»§a báº¡n á»Ÿ Ä‘Ã¢y
		IndexSearcher searcher = new IndexSearcher(index);

		searcher.setSimilarity(new ClassicSimilarity());
		QueryParser parser = new QueryParser(field, new StandardAnalyzer());
		String queryText = null;
		for (String string : queryTerms) {
			queryText += " " + string; 
		}
		Query query = parser.parse(queryText);
		int top = index.numDocs();
		TopDocs docs = searcher.search(query, top);
		list = new ArrayList<SearchResult>();
		for (ScoreDoc scoreDoc : docs.scoreDocs) {
			int docID = scoreDoc.doc;
			double score = scoreDoc.score;
			Document doc = index.document(docID);
			String docno = doc.get("docno");
			list.add(new SearchResult(docID, docno, score));
		}
		return list;
	}

}