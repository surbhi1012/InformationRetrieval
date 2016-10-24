package ir.runner.main;

import java.io.IOException;

import ir.algorithms.indexer.InvertedIndex;

/**
 * @author Surbhi Gupta
 */
public class InvertedIndexRunner {
	private final static String CORPUS_DIR_NAME = "data/output/cacm_corpus/";
	private final static String INDEX_NAME_PREFIX = "data/output/invertedIndex";
	
	public static void main(String[] args) throws IOException {
		//create index
		InvertedIndex.invertedIndexGenerator(CORPUS_DIR_NAME, INDEX_NAME_PREFIX);
	}
}
