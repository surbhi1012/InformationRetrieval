package ir.algorithms.corpus;

import ir.algorithms.scoreAndRank.DocScore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

/**
 * @author Surbhi Gupta
 */
class DocLength {
	private static final String ILLEGAL_NAME = ".DS_Store";
	private static final String UTF8 = "UTF-8";
	private static final String CORPUS_FILE_TYPE = ".txt";

	/**
	 * The utility method to generate document lengths for all the documents in the corpus and write in a file
	 * @param corpusFilePath is the relative path to the corpus files directory
	 * @param docLengthFilePath is the relative path to the file containing all the document lengths
	 * @param stopWords is the set of stop words to be excluded from any consideration
	 * @throws IOException
     */
	static void docLengthGenerator(String corpusFilePath, String docLengthFilePath, HashSet<String> stopWords) throws IOException {
		ArrayList<DocScore> docScoreList = new ArrayList<>();
		
		File folder = new File(corpusFilePath);
		File[] listOfFiles = folder.listFiles();

		if (listOfFiles != null) {
			for (File file : listOfFiles) {
                if (file.isFile() && !file.getName().equals(ILLEGAL_NAME)) {
                    Scanner sc = null;
                    String fileContent = "";
                    try {
                        sc = new Scanner(file, UTF8);
                        fileContent = new String(Files.readAllBytes(file.toPath()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (sc != null) {
                            sc.close();
                        }
                    }

                    String[] words = fileContent.replace(" +", " ").trim().split(" ");
                    DocScore d = new DocScore();
                    d.docID = file.getName().replaceAll(CORPUS_FILE_TYPE, "");
                    d.docScore = 0.0;
                    d.docLength = words.length;

                    if (stopWords != null) {
                        for (String word : words) {
                             if (stopWords.contains(word)) d.docLength--;
                        }
                    }

                    d.tfMap = null;
                    docScoreList.add(d);
                }
            }
		}

		FileWriter writer = null;
		try {
			writer = new FileWriter(docLengthFilePath);
			int N = 0;
			double sumDL = 0.0;
			
			for (DocScore ds : docScoreList) {
				N++;
				sumDL += ds.docLength;
				writer.write(ds.docID + " : " + ds.docLength + "\n");
			}
			System.out.println("Total files: " + N + ", Average DL: " + sumDL / N);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer != null) {
			writer.close();
		}
	}
}
