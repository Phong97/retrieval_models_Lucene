package index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.util.Calendar;

/**
 * This is an example of accessing corpus statistics and corpus-level term
 * statistics.
 *
 * @author phong97
 * @version 2019-03
 */
public class CorpusStats {

	public static void main(String[] args) {
		try {

			String indexPath = "C:\\Users\\phong\\eclipse-workspace\\lab3\\index";

			// Thá»‘ng kÃª cho tá»« "retrieval" trong "text" field
			String field = "text";
			String term1 = "information";
			String term2 = "retrieval";
			String term3 = "evaluation";

			Directory dir = FSDirectory.open(new File(indexPath).toPath());
			IndexReader index = DirectoryReader.open(dir);

			// Viáº¿t thÃªm pháº§n mÃ£ Ä‘á»ƒ thá»±c hiá»‡n cÃ¡c thá»‘ng kÃª á»Ÿ Ä‘Ã¢y
			long begin = Calendar.getInstance().getTimeInMillis();
			// 1.Có bao nhiêu tài liệu trong kho tài liệu?
			int numDoc = index.numDocs(); // tổng số tài liệu
			System.out.println("Total docs: " + numDoc);
			
			// 2.Độ dài trung bình của text field và title field của các tài liệu trong cả
			// kho tài liệu?
			long numTextField = index.getSumTotalTermFreq("text"); // tổng chiều dài của trường text
			long numTitleField = index.getSumTotalTermFreq("title"); // tổng chiều dài trường title
			System.out.println("avgTextField = " + numTextField*1.0/numDoc);
			System.out.println("avgTitleField = " + numTitleField*1.0/numDoc);
			
			// 3. Có tất cả bao nhiêu index term trong cả kho tài liệu (của cả title và text
			// field, không tính docno)?
			long numIndexTerm = index.getSumTotalTermFreq("all");
			System.out.println("Total index term : " + numIndexTerm);
			
			// 4. Tính document frequency (DF) và invert document frequency (IDF) của index
			// term “information” và “retrieval”.
			// DF(w) là số tài liệu chứa từ w.
			long nTerm1 = index.docFreq(new Term(field, term1)); // số tài liệu chứa từ "information"
			long nTerm2 = index.docFreq(new Term(field, term2)); // số tài liệu chứa từ "retrieval"
			double idfTerm1 = Math.log((numDoc + 1.0) / (nTerm1 + 1.0));
			double idfTerm2 = Math.log((numDoc + 1.0) / (nTerm2 + 1.0));
			System.out.println("idf information : " + idfTerm1);
			System.out.println("idf retrieval : " + idfTerm2);
			
			// 5. Tìm tất cả các tài liệu chứa cả từ “retrieval” và “evaluation”.
            IndexSearcher searcher = new IndexSearcher( index );
            searcher.setSimilarity( new BM25Similarity() );
            // Just like building an index, we also need an Analyzer to process the query strings
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
			// Build a Query object
			QueryParser parser = new QueryParser(field, analyzer);
			Query query1 = parser.parse(term2);
			Query query2 = parser.parse(term3);
			BooleanQuery query = new BooleanQuery.Builder()
				    .add(query1, BooleanClause.Occur.MUST)
				    .add(query2, BooleanClause.Occur.MUST)
				    .build();
			
			int top = numDoc; 
            TopDocs docs = searcher.search( query, top ); 

            System.out.printf("%-10s%-20s%-10s%s\n", "Rank", "DocNo", "Score", "Title");
            int rank = 1;
            for ( ScoreDoc scoreDoc : docs.scoreDocs ) {
                int docID = scoreDoc.doc;
                double score = scoreDoc.score;
                Document doc = index.document(docID);
                String docno = doc.get("docno");
                String title = doc.get("title");
                System.out.printf("%-10d%-20s%-10.4f%s\n", rank, docno, score, title);
                rank++;
            }
            System.out.printf("Total: %d document", rank);
			// Ä�Ã³ng index reader vÃ  directory
			index.close();
			dir.close();
			
			// 6. In ra thời gian để thực hiện các yêu cầu từ 1 đến 5.
			long end = Calendar.getInstance().getTimeInMillis();
			System.out.println();
			System.out.println("Executed Time: " + (end - begin) + " Millisecond");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
