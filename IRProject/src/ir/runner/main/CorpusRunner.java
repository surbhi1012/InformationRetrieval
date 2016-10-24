package ir.runner.main;

import java.io.IOException;

import ir.algorithms.corpus.CorpusGenerator;

/**
 * @author Surbhi Gupta
 */
public class CorpusRunner {
	public static void main(String[] args) throws IOException {
		//create corpus
		CorpusGenerator.corpus();
	}
}
