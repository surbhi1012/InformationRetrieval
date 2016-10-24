package ir.runner.main;

import java.io.IOException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import ir.algorithms.analysis.Analyzer;
import ir.algorithms.scoreAndRank.BM25;
import ir.commons.SearchQuery;
import ir.algorithms.scoreAndRank.ThesaurusAndOntology;
import ir.algorithms.snippet.SnippetGenerator;
import ir.commons.Utils;

/**
 * @author Surbhi Gupta
 */
public class SearchEngineRunner {
	private static final int RESULT_SIZE = 100;

	private static final String STOP_LIST_PATH = "data/input/common_words";
	private static final String QUERY_PATH = "data/input/cacm.query";
	private static final String REL_FEEDBACK_PATH = "data/input/cacm.rel";
	private static final String CORPUS_PATH = "data/output/cacm_corpus/";
	private static final String INDEX_PATH = "data/output/invertedIndex1gram.txt";
	private static final String DOC_LENGTH_PATH = "data/output/docLength.txt";
	private static final String RESULT_FILE_PREFIX_BM25_THR = "data/output/result/bm25_thr/resultBM25_THR_forQuery_";
	private static final String SNIPPET_FILE_PREFIX_BM25_THR = "data/output/snippet/bm25_thr/snippetBM25_THR_forQuery_";
	private static final String ANALYSIS_FILE_PREFIX_BM25_THR = "data/output/analysis/bm25_thr/analysisBM25_THR";

	/**
	 * The main method for this Search Engine project
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
     */
	public static void main(String[] args) throws IOException, ParserConfigurationException, SAXException {
		System.out.println("Search Engine");
		System.out.println("uses BM25 for scoring, Thesaurus for expanding queries and Relevance Feedback for evaluation");

		// read the queries from the XML file into a List
		List<SearchQuery> queryList = Utils.parseXML(QUERY_PATH);

		// perform query expansion by referring to the query terms in a dictionary for their synonyms
		ThesaurusAndOntology thr = new ThesaurusAndOntology(STOP_LIST_PATH);
		List<SearchQuery> queryListAfterTHR = thr.getExpandedQueries_THR(queryList);

		// perform Search on the expanded query list
		BM25.scoreAndRetrieve(queryListAfterTHR, RESULT_SIZE, CORPUS_PATH, INDEX_PATH, DOC_LENGTH_PATH,
				RESULT_FILE_PREFIX_BM25_THR, REL_FEEDBACK_PATH, queryList.size());

		/**
		 * generate snippets for the results
		 * this being an optional step in the Search process, it reads the Search results from the disk
		 */
		SnippetGenerator sg = new SnippetGenerator(RESULT_FILE_PREFIX_BM25_THR, queryList, CORPUS_PATH,
				SNIPPET_FILE_PREFIX_BM25_THR);
		sg.generateSnippet();

		/**
		 * create an analysis report of the final result
		 * this being an optional step in the Search process, it reads the Search results from the disk
		 */
		Analyzer.generateStats(RESULT_FILE_PREFIX_BM25_THR, REL_FEEDBACK_PATH, ANALYSIS_FILE_PREFIX_BM25_THR,
				queryList.size());
	}
}
