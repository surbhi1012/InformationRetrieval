package ir.algorithms.corpus;

import ir.commons.Utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Surbhi Gupta
 */
public class CorpusGenerator {
	private static final String CORPUS_DIR_PATH = "data/input/cacm/";
	private static final String CACM_FILE_PREFIX = "CACM";
	private static final String CACM_FILE_TYPE = ".html";
	private static final String CORPUS_FILE_TYPE = ".txt";
	private static final String READ_START_TAG = "<pre>";
	private static final String READ_END_TAG = "</pre>";
	private static final String UTF8 = "UTF-8";
	private static final String DESTI_DIR_NAME = "data/output/cacm_corpus/";
	private static final String CORPUS_PATH = "data/output/cacm_corpus/";
	private static final String DOC_LENGTH_PATH = "data/output/docLength.txt";
	
	/**
	 * The method to read all the corpus files in the given path name and store them as the desired type after
	 * processing
	 * @throws IOException
     */
	public static void corpus() throws IOException {
		List<File> fileList = Utils.readFilesFromDirectory(CORPUS_DIR_PATH, CACM_FILE_PREFIX, CACM_FILE_TYPE );
		
		System.out.println("Processing files for corpus generation...");
		for (File file : fileList) {
			readFileAndAddToCorpus(file);
		}
		DocLength.docLengthGenerator(CORPUS_PATH, DOC_LENGTH_PATH, null);
		
		System.out.println("Corpus generated");
	}

	/**
	 * The method to read the given file and add it to the corpus after processing
	 * @param file is the file to be read and processed
	 * @throws IOException
     */
	private static void readFileAndAddToCorpus(File file) throws IOException {
		Scanner sc = new Scanner(file, UTF8);
		String fileContent = new String(Files.readAllBytes(file.toPath()));	//parse the entire file content into a string
		sc.close();
    	
		String fileName = DESTI_DIR_NAME + processFileName(file.getName() + CORPUS_FILE_TYPE);
		String content = fileContent.substring(fileContent.indexOf(READ_START_TAG) + 5, fileContent.indexOf(READ_END_TAG)).trim();
		content = Utils.processContent(content);
		
		Pattern p1 = Pattern.compile("[c][a][0-9][0-9][0-9][0-9][0-9][0-9] [a-z][a-z]");
		Pattern p2 = Pattern.compile("[c][a][0-9][0-9][0-9][0-9][0-9][0-9][a-z][a-z]");
		Pattern p3 = Pattern.compile("[c][a][0-9][0-9][0-9][0-9][0-9][0-9][a-z] [a-z][a-z]");
		Pattern p4 = Pattern.compile("[c][a][0-9][0-9][0-9][0-9][0-9][0-9][a-z][a-z][a-z]");
		Matcher m1 = p1.matcher(content);
		Matcher m2 = p2.matcher(content);
		Matcher m3 = p3.matcher(content);
		Matcher m4 = p4.matcher(content);
		
		if (m1.find()) { int position = m1.start(); content = content.substring(0, position).trim(); }
		else if (m2.find()) { int position = m2.start(); content = content.substring(0, position).trim(); }
		else if (m3.find()) { int position = m3.start(); content = content.substring(0, position).trim(); }
		else if (m4.find()) { int position = m4.start(); content = content.substring(0, position).trim(); }
		
		addToCorpus(fileName, content);
	}

	/**
	 * The utility method to format the file name as per asked requirement
	 * @param fileName is the file name to be formatted as per requirement
	 * @return the file name after formatting
     */
	private static String processFileName(String fileName) {
		return fileName.replace(CACM_FILE_TYPE, "");
	}

	/**
	 * The utility method to write a given text in a new file at the given path
	 * @param fileName is the path and name of the new file to be created
	 * @param content is the content of the new file to be created
	 * @throws IOException
     */
	private static void addToCorpus(String fileName, String content) throws IOException {
		FileWriter writer = new FileWriter(fileName); 
		writer.write(content);
		writer.close();
	}
}
