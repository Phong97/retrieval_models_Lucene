package utils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Một số hàm tiện ích
 */
public class Utils {
	
		/**
	 * Tìm document trong index theo DOCNO (external ID)
	 * Hàm trả về DOCID (internal ID) của document hoặc -1 nếu không tìm thấy
	 *
	 * @param index      index reader
	 * @param docnoField Tên field được sử dụng để lưu DOCNO (external document IDs)
	 * @param docno      DOCNO (external ID) cần tìm
	 * @return DOCNO (external ID) của document trong index hoặc -1 nếu không tìm thấy
	 * @throws IOException
	 */
	public static int findByDocno(IndexReader index, String docnoField, String docno) throws IOException {
		BytesRef term = new BytesRef(docno);
		PostingsEnum posting = MultiFields.getTermDocsEnum(index, docnoField, term, PostingsEnum.NONE);
		if (posting != null) {
			int docid = posting.nextDoc();
			if (docid != PostingsEnum.NO_MORE_DOCS) {
				return docid;
			}
		}
		return -1;
	}

	/**
	 * @param index      index reader
	 * @param docnoField Tên field được sử dụng để lưu DOCNO (external document IDs)
	 * @param docid      DOCID (internal ID) của document
	 * @return DOCNO (external ID) của document
	 * @throws IOException
	 */
	public static String getDocno(IndexReader index, String docnoField, int docid) throws IOException {
		Set<String> fieldset = new HashSet<>();
		fieldset.add(docnoField);
		Document d = index.document(docid, fieldset);
		return d.get(docnoField);
	}
	
	/**
	 * Tach (tokenize) input string thành các token (word) dùng Lucene analyzer
	 *
	 * @param text     input string
	 * @param analyzer Lucene Analyzer
	 * @return Danh sách các token (word)
	 */
	public static List<String> tokenize(String text, Analyzer analyzer) throws IOException {
		List<String> tokens = new ArrayList<>();
		TokenStream ts = analyzer.tokenStream("", new StringReader(text));
		CharTermAttribute attr = ts.getAttribute(CharTermAttribute.class);
		ts.reset();
		while (ts.incrementToken()) {
			tokens.add( attr.toString());
		}
		ts.end();
		ts.close();
		return tokens;
	}

}