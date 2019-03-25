package index;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BuildIndex {

	public static void main(String[] args) {
		try {

			String docsPath = "C:\\Users\\phong\\eclipse-workspace\\lab3\\docs"; // Ä‘Æ°á»�ng dáº«n Ä‘áº¿n thÆ° má»¥c
																					// TREC collection
			String indexPath = "C:\\Users\\phong\\eclipse-workspace\\lab3\\index"; // Ä‘Æ°á»�ng dáº«n Ä‘áº¿n thÆ° má»¥c
																					// index

			Directory dir = FSDirectory.open(Paths.get(indexPath));

			// Táº¡o custom Analyzer Ä‘á»ƒ xá»­ lÃ½ text
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

			IndexWriterConfig config = new IndexWriterConfig(analyzer);
			// ChÃº Ã½: IndexWriterConfig.OpenMode.CREATE sáº½ ghi Ä‘Ã¨ (override) cÃ¡c file
			// index hiá»‡n cÃ³ trong thÆ° má»¥c index
			config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

			IndexWriter ixwriter = new IndexWriter(dir, config);

			// CÃ¡c thiáº¿t láº­p cho metadata field
			FieldType fieldTypeMetadata = new FieldType();
			fieldTypeMetadata.setOmitNorms(true);
			fieldTypeMetadata.setIndexOptions(IndexOptions.DOCS);
			fieldTypeMetadata.setStored(true);
			fieldTypeMetadata.setTokenized(false);
			fieldTypeMetadata.freeze();

			// CÃ¡c thiáº¿t láº­p cho text field
			FieldType fieldTypeText = new FieldType();
			fieldTypeText.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
			fieldTypeText.setStoreTermVectors(true);
			fieldTypeText.setStoreTermVectorPositions(true);
			fieldTypeText.setTokenized(true);
			fieldTypeText.setStored(true);
			fieldTypeText.freeze();

			// Báº¡n cáº§n duyá»‡t tá»«ng tÃ i liá»‡u (<DOC>...</DOC>) tá»« TREC collection,
			// táº¡o má»™t Document object cho má»—i tÃ i liá»‡u, vÃ  add
			// Document object Ä‘Ã³ vÃ o index dÃ¹ng addDocument().
			final Path docDir = Paths.get(docsPath);
			if (!Files.isReadable(docDir)) {
				System.out.println("Document directory '" + docDir.toAbsolutePath()
						+ "' does not exist or is not readable, please check the path");
				System.exit(1);
			}
			indexDocs(ixwriter, docDir);
			// Ä�Ã³ng index writer vÃ  directory
			ixwriter.close();
			dir.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Indexes the given file using the given writer, or if a directory is given,
	 * recurses over files and directories found under the given directory.
	 * 
	 * NOTE: This method indexes one document per input file. This is slow. For good
	 * throughput, put multiple documents into your input file(s). An example of
	 * this is in the benchmark module, which can create "line doc" files, one
	 * document per line, using the <a href=
	 * "../../../../../contrib-benchmark/org/apache/lucene/benchmark/byTask/tasks/WriteLineDocTask.html"
	 * >WriteLineDocTask</a>.
	 * 
	 * @param writer Writer to the index where the given file/dir info will be
	 *               stored
	 * @param path   The file to index, or the directory to recurse into to find
	 *               files to index
	 * @throws IOException If there is a low-level I/O error
	 */
	static void indexDocs(final IndexWriter writer, Path path) throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					try {
						indexDoc(writer, file, attrs.lastModifiedTime().toMillis());
					} catch (IOException ignore) {
						// don't index files that can't be read.
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			indexDoc(writer, path, Files.getLastModifiedTime(path).toMillis());
		}
	}

	/** Indexes a single document */
	static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
		try (InputStream stream = Files.newInputStream(file)) {
			// make a new, empty document
			Document doc = new Document();

			// Add the path of the file as a field named "path". Use a
			// field that is indexed (i.e. searchable), but don't tokenize
			// the field into separate words and don't index term frequency
			// or positional information:
			Field pathField = new StringField("path", file.toString(), Field.Store.YES);
			doc.add(pathField);

			// Add the last modified date of the file a field named "modified".
			// Use a LongPoint that is indexed (i.e. efficiently filterable with
			// PointRangeQuery). This indexes to milli-second resolution, which
			// is often too fine. You could instead create a number based on
			// year/month/day/hour/minutes/seconds, down the resolution you require.
			// For example the long value 2011021714 would mean
			// February 17, 2011, 2-3 PM.
			doc.add(new LongPoint("modified", lastModified));

			// Add the contents of the file to a field named "contents". Specify a Reader,
			// so that the text of the file is tokenized and indexed, but not stored.
			// Note that FileReader expects the file to be in UTF-8 encoding.
			// If that's not the case searching for special characters will fail.
			doc.add(new TextField("contents",
					new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))));
			// index field
			Pattern pattern = Pattern.compile(
					"<DOC>.+?<DOCNO>(.+?)</DOCNO>.+?<TI>(.+?)</TI>.+?<TEXT>(.+?)</TEXT>.+?</DOC>",
					Pattern.CASE_INSENSITIVE + Pattern.MULTILINE + Pattern.DOTALL);
			String corpusText = new String( IOUtils.toByteArray( stream ), "UTF-8" );
			Matcher matcher = pattern.matcher(corpusText);
			while (matcher.find()) {

				String docno = matcher.group(1).trim();
				String title = matcher.group(2).trim();
				String text = matcher.group(3).trim();

				// Add each field to the document with the appropriate field type options
				doc.add(new TextField("docno", docno, Field.Store.YES));
				doc.add(new TextField("title", title, Field.Store.YES));
				doc.add(new TextField("text", text, Field.Store.YES));
				doc.add(new TextField("all", text, Field.Store.YES));
				doc.add(new TextField("all", title, Field.Store.YES));
			}

			if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
				// New index, so we just add the document (no old document can be there):
				System.out.println("adding " + file);
				writer.addDocument(doc);
			} else {
				// Existing index (an old copy of this document may have been indexed) so
				// we use updateDocument instead to replace the old one matching the exact
				// path, if present:
				System.out.println("updating " + file);
				writer.updateDocument(new Term("path", file.toString()), doc);
			}
		}
	}
}