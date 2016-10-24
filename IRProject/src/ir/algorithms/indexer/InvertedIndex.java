package ir.algorithms.indexer;

import ir.commons.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvertedIndex {
	private static final String INDEX_NAME_SUFFIX = "gram.txt";
	private static final String CORPUS_FILE_TYPE = ".txt";
	private static final String CACM_FILE_PREFIX = "CACM";
	private static final int NGRAM = 1;			// 1 for unigram
	
	private static Map<String, HashMap<String, Integer>> invIndex = new HashMap<>();

	/**
	 * The method to create and write inverted index for the given corpus at the given relative path
	 * @param corpusDirPath is the relative directory path of the corpus
	 * @param invIndexPrefix is the relative path and prefix of the inverted index to be created
	 * @throws IOException
     */
	public static void invertedIndexGenerator(String corpusDirPath, String invIndexPrefix) throws IOException {
		List<File> fileList = Utils.readFilesFromDirectory(corpusDirPath, CACM_FILE_PREFIX, "");
		
		System.out.println("Processing files for indexing...");
		for (File file : fileList) {
			readFileAndAddToIndex(file, NGRAM);
		}
		
		System.out.println("Printing inverted index...");
		printIndex(invIndexPrefix + NGRAM + INDEX_NAME_SUFFIX);
		
		System.out.println("Index generated");
	}

	/**
	 * The method to create n-grams from the consecutive words in the given file and store in the inverted index
	 * @param file is the file to be read for n-gramming creation
	 * @param nGram is the n-gramming factor, e.g: 2 for bigrams, 3 for trigrams
     */
	private static void readFileAndAddToIndex(File file, int nGram) {
		String fileContent = "";

		try {
			//parse the entire file content into a string
			fileContent = new String(Files.readAllBytes(file.toPath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
		String fName = file.getName().replaceAll(CORPUS_FILE_TYPE, "");
		String[] nGramsList = getNGram(fileContent, nGram);
		
		for (String word : nGramsList) {
			
			if (invIndex.get(word) == null) {
				//word appears for the first time
				HashMap<String, Integer> values = new HashMap<>();
				values.put(fName, 1);
				invIndex.put(word, values);
			} else {
				HashMap<String, Integer> values = invIndex.get(word);
				if (values.keySet().contains(fName)) {
					//doc entry already exists
					values.put(fName, values.get(fName) + 1);
					invIndex.put(word, values);
				} else {
					values.put(fName, 1);
					invIndex.put(word, values);
				}
			}
		}
	}

	/**
	 * The helper method to create n-grams from the consecutive words in the given file
	 * @param fileContent is the text content file to be read for n-gramming
	 * @param nGram is the n-gramming factor, e.g: 2 for bigrams, 3 for trigrams
     * @return the array of n-grams created from the text content of the file
     */
	private static String[] getNGram(String fileContent, int nGram) {
		String[] wList = fileContent.split("\\s+");
		String[] nGramList = new String[wList.length - nGram + 1];
		
		for (int i = 0; i < wList.length - nGram + 1; i++) {
			String s = "";
			for (int j = 0; j < nGram; j++)
				s += wList[i + j] + " ";
			nGramList[i] = s.trim();
		}
		
		return nGramList;
	}

	/**
	 * The utility method to write the inverted index file in a file
	 * @param fileName is the relative path of the inverted index file
	 * @throws IOException
     */
	private static void printIndex(String fileName) throws IOException {
		try (FileWriter writer = new FileWriter(fileName)) {
			for (String word : invIndex.keySet()) {
				String s = "";
				HashMap<String, Integer> values = invIndex.get(word);
				for (String doc : values.keySet()) {
					s += "[" + doc + ":" + values.get(doc) + "], ";
				}

				writer.write(word + " : " + s.substring(0, s.length() - 2) + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
