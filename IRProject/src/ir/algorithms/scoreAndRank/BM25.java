package ir.algorithms.scoreAndRank;

import ir.commons.SearchQuery;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Surbhi Gupta
 */
public class BM25 {
	private static List<DocScore> docScoreList;				 // result list of all the documents with their BM25 scores
	private static Map<String, Integer> queryInDocsCount; 	 // mapping of queries to their document frequency
	private static int N;								  	 // number of documents in the corpus
	private static double avdl;							  	 // average document length of the corpus
	private static Map<String, Integer> queryRelDocCountMap; // mapping of queries to their relevant document frequency

	private static final String SYSTEM_NAME = "BM25";
	private static final String RESULT_FILE_TYPE = ".txt";

	/**
	 * The method to fetch the inverted index of a word from the relative file name of the index file
	 * @param term is the word from the query whose inverted index has to be fetched
	 * @param indexPath is the relative path where the inverted indices are stored
     * @return the inverted index of this word as a Map from the word to the
     */
	private static Map<String, Integer> getInvertedListForTerm(String term, String indexPath) {
		BufferedReader buffer = null;
		
		try {
			buffer = new BufferedReader(new FileReader(indexPath));
			String line;
			while ((line = buffer.readLine()) != null) {
				if (line.startsWith(term + " : ")) {
					String[] parts = line.trim().split(" : ");
					String[] docTF = parts[1].split(", ");

					Map<String, Integer> hm = new HashMap<>();
					for (String docTFPart : docTF) {
						String s = docTFPart.substring(docTFPart.indexOf("[") + 1, docTFPart.indexOf("]"));
						hm.put(s.split(":")[0], Integer.parseInt(s.split(":")[1]));
					}
					queryInDocsCount.put(term, docTF.length);

					return hm;
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("Index file not found");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
	        try {
				if (buffer != null) {
					buffer.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * The utility method to check if a document in the docScoreList has the given docID
	 * @param docID is the id of the document to be searched in the docScoreList
	 * @return TRUE if a document with the given docID is present in the docScoreList
     */
	private static boolean docPresentInDocScoreList(String docID) {
		boolean present = false;
		
		for (DocScore ds : docScoreList) {
			if (ds.docID.equals(docID)) {
				present = true;
				break;
			}
		}

		return present;
	}

	/**
	 * The utility method to check if a document in the docScoreList has the given docID
	 * @param docID is the id of the document to be searched in the docScoreList
	 * @return the matched document, otherwise NULL
     */
	private static DocScore docFromDocScoreList(String docID) {
		for (DocScore ds : docScoreList) {
			if (ds.docID.equals(docID))
				return ds;
		}
		
		return null;
	}

	/**
	 * The method to set document lengths of all documents in docScoreList and also find the average document length
	 * @param docLengthPath is the relative path for the file containing the document lengths of all the documents in
	 *                      the corpus
     */
	private static void setDocLengthAndAVDL(String docLengthPath) {
		BufferedReader buffer = null;
		Map<String, Integer> docLengthHM = new HashMap<>();
		
		try {
			buffer = new BufferedReader(new FileReader(docLengthPath));
	        String line;
	        N = 0;
	        while ((line = buffer.readLine()) != null) {
	        	String[] parts = line.split(" : ");    	
	        	String docID = parts[0];
	        	int dl = Integer.parseInt(parts[1]);
	        	avdl += dl;
	        	N++;
	        	
	        	docLengthHM.put(docID, dl);
	        }
	        avdl /= N;
		} catch (NumberFormatException | IOException e) {
			e.printStackTrace();
		} finally {
	        try {
				if (buffer != null) {
					buffer.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for (DocScore ds : docScoreList) {
			ds.docLength = docLengthHM.get(ds.docID);
		}
	}

	/**
	 * The method to initialize the docScoreList for a query with all the documents it is present in by setting the
	 * score as 0.0 and doc lengths as 1
	 * @param invertedListForAllTerms is the inverted list of all the query terms from a given query
     */
	private static void initDocScoreList(Map<String, Map<String, Integer>> invertedListForAllTerms) {
		for (String term : invertedListForAllTerms.keySet()) {
			Map<String, Integer> docTF = invertedListForAllTerms.get(term);
			
			if (docTF == null) continue;
			
			for (String docID : docTF.keySet()) {
				if (docPresentInDocScoreList(docID)) {
					DocScore ds = docFromDocScoreList(docID);
					if (ds != null) {
						ds.tfMap.put(term, docTF.get(docID));
					}

				} else {
					DocScore ds = new DocScore();
					ds.docID = docID;
					ds.docScore = 0.0;
					ds.docLength = 1;
					HashMap<String, Integer> tfMap = new HashMap<>();
					tfMap.put(term, docTF.get(docID));
					ds.tfMap = tfMap;
					
					docScoreList.add(ds);
				}
			}
		}
	}

	/**
	 * The method to calculate the Okapi BM25 score for all documents that the given SearchQuery appears in
	 * Formula used as given in https://en.wikipedia.org/wiki/Okapi_BM25
	 * @param searchQuery is the SearchQuery whose relevant documents have to be scored
	 * @param qTermRelDocCountMap is the mapping of all the query terms with their relevant document frequencies
     * @param R is the number of relevant documents for the given SearchQuery
     */
	private static void setBM25Score(SearchQuery searchQuery, Map<String, Integer> qTermRelDocCountMap, double R) {
		/*
		All variable names here are in line with the standard notations used in the formula
		 */
		for (DocScore ds : docScoreList) {
			double sumBM = 0d;
			for (String term : queryInDocsCount.keySet()) {
				double k1 = 1.2d;
				int k2 = 100;
				double b = 0.75d;
				double K = k1 * ((1 - b) + (b * ds.docLength / avdl));
				double qfi = getTermCountInQuery(searchQuery.queryText, term) / 1d;
				double ri = (qTermRelDocCountMap != null && qTermRelDocCountMap.keySet().contains(term) ? qTermRelDocCountMap.get(term) : 0d);
				double ni = queryInDocsCount.get(term);
				double fi = (ds.tfMap.keySet().contains(term) ? ds.tfMap.get(term) : 0d);
				
				double part1BM = ((ri + 0.5) * (N - ni - R + ri + 0.5)) / ((R - ri + 0.5) * (ni - ri + 0.5));
				double part2BM = ((k1 + 1) * fi) / (K + fi);
				double part3BM = ((k2 + 1) * qfi) / (k2 + qfi);
				double partBM = Math.log(part1BM) * part2BM * part3BM;
				
				sumBM += partBM;
			}
			
			ds.docScore = sumBM;
		}
	}

	/**
	 * The utility method to find the number of times the given term appears in the query text
	 * @param queryText is the text of the given query
	 * @param term is the word whose count is to be calculated
     * @return the count of occurances of the given term in the given query text
     */
	private static int getTermCountInQuery(String queryText, String term) {
		String[] words = queryText.split(" ");
		int count = 0;

		for (String word : words) {
			if (word.equals(term)) count++;
		}
		
		return count;
	}

	/**
	 * The utility method to print the BM25 scores of all the documents of the given query id in a single file
	 * @param queryNo is the id of the query whose
	 * @param limit is the #results desired in the output
	 * @param fileNamePrefix is the path and prefix of the file to be created for this query
     */
	private static void printDocScore(int queryNo, int limit, String fileNamePrefix) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(fileNamePrefix + queryNo + RESULT_FILE_TYPE);
			
			int rank = 1;
			for (DocScore ds : docScoreList) {
				DecimalFormat numberFormat = new DecimalFormat("#.00");
				writer.write(queryNo + " " + ds.docID + " " + rank++ + " " + numberFormat.format(ds.docScore) + " " +
				SYSTEM_NAME + "\n");
				if (--limit == 0) break; 
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			if (writer != null) {
				writer.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The method to get the relevance judgement on all the queries in the given queryList
	 * @param queryList is the list of SearchQuery objects for whom the relevance judgement has be to read
	 * @param relFeedbackFilePath is the relative path of the relative judgements for all the queries
	 * @param corpusPath is the relative path of the corpus dir where all the corpus files are stored
	 * @return the mapping of all the query terms with the map of document id and the term frequency of that query term
	 * in that document
	 * @throws IOException
     */
	private static Map<String, Map<String, Integer>> getQTermRelDocCountMap(List<SearchQuery> queryList, String relFeedbackFilePath,
			String corpusPath) throws IOException {
		
		if (relFeedbackFilePath.equals(""))
			return null;
		
		Map<String, Map<String, Integer>> qTermRelDocCountMap = new HashMap<>();
		queryRelDocCountMap = new HashMap<>();
		
		//reading the rel. feedback file
		BufferedReader buffer = new BufferedReader(new FileReader(relFeedbackFilePath));
        String line;
        while ((line = buffer.readLine()) != null) {
        	String qID = (line.split(" ")[0]);
        	String docID = line.split(" ")[2];
        	
        	//setting queryRelDocCountMap
        	if (queryRelDocCountMap.keySet().contains(qID)) {
            	queryRelDocCountMap.put(qID, queryRelDocCountMap.get(qID) + 1);
        	} else {
            	queryRelDocCountMap.put(qID, 1);
        	}
        	
        	//reading the doc content
        	if (docID.length() < 9) {
        		String zeros = "";
        		for (int i = 0; i < 9 - docID.length(); i++) {
        			zeros += "0";
        		}
        		docID = docID.substring(0, docID.indexOf("-") + 1) + zeros + docID.substring(docID.indexOf("-") + 1);
        	}
        	File d = new File(corpusPath + docID + RESULT_FILE_TYPE);
        	String docContent = new String(Files.readAllBytes(d.toPath()));
        	
        	//getting the query text
        	String qText = "";
        	for (SearchQuery sq : queryList) {
        		if (sq.id == Integer.parseInt(qID)) { qText = sq.queryText; break; }
        	}
        	
        	String[] qTerms = qText.split(" ");
        	Set<String> qTermSet = new HashSet<>();
			for (String qTerm : qTerms) {
				qTermSet.add(qTerm.trim());
			}
			//check if the qTerm appears in the doc
			qTermSet.stream().filter(qTerm -> !qTerm.equals("") && countOccurancesOfWordInText(docContent, qTerm) > 0).forEach(qTerm -> {
				if (qTermRelDocCountMap.keySet().contains(qID)) {
					Map<String, Integer> map = qTermRelDocCountMap.get(qID);

					if (map.keySet().contains(qTerm)) {
						map.put(qTerm, map.get(qTerm) + 1);
						qTermRelDocCountMap.put(qID, map);
					} else {
						map.put(qTerm, 1);
						qTermRelDocCountMap.put(qID, map);
					}
				} else {
					Map<String, Integer> map = new HashMap<>();
					map.put(qTerm, 1);
					qTermRelDocCountMap.put(qID, map);
				}
			});
        }
		buffer.close();
        
		return qTermRelDocCountMap;
	}

	/**
	 * The utility method to count the occurances of the given word in the given sentence
	 * @param sentence is the text in which the given word has to be searched
	 * @param word is the text to be searched in the given sentence
     * @return the number of times the word appears in the sentence
     */
	private static int countOccurancesOfWordInText(String sentence, String word) {
		int numInstances = 0;
		
		String[] words = sentence.split(" ");
		for (String word1 : words) {
			if (!word1.equals("") && word1.equals(word)) numInstances++;
		}
		
		return numInstances;
	}

	/**
	 * The method to search for the given queries in the corpus, score documents based on BM25 scores and write the
	 * results in file(s) (one per query)
	 * @param queryList is the list of SearchQuery objects to be searched in the corpus
	 * @param n is the maximum number of results desired in the final output
	 * @param corpusPath is the relative path to the corpus directory
	 * @param indexPath is the relative path to the inverted index file
	 * @param docLengthPath is the relative path to the document length file
	 * @param fileNamePrefix is the prefix to be used while saving the writing the final scored documents for each query
	 * @param relFeedbackFilePath is the relative path to the relevant judgement provided for the queries
     * @throws IOException
     */
	public static void scoreAndRetrieve(List<SearchQuery> queryList, int n, String corpusPath,
								 String indexPath, String docLengthPath, String fileNamePrefix,
								 String relFeedbackFilePath, int queryCount) throws IOException {
		docScoreList = new ArrayList<>();
		queryInDocsCount = new HashMap<>();

		//getting relevance judgements for this query
		Map<String, Map<String, Integer>> qTermRelDocCountMap = getQTermRelDocCountMap(queryList, relFeedbackFilePath, corpusPath);
		
		System.out.println("Searching for all queries (" + queryCount + " in total)");
		for (SearchQuery q : queryList) {
			System.out.print(".");

			//reseting the doc score list for this query
        	docScoreList.clear();
    		queryInDocsCount.clear();
        	
    		//getting the inverted indexes for all the query terms and clubbing them
    		Map<String, Map<String, Integer>> indexListForAllTerms = new HashMap<>();
    		String[] parts = q.queryText.split(" +");
			for (String term : parts) {
				Map<String, Integer> invertedListForTerm = getInvertedListForTerm(term, indexPath);
				indexListForAllTerms.put(term, invertedListForTerm);
			}
        	
        	//setting the docScoreList 
        	initDocScoreList(indexListForAllTerms);
        	setDocLengthAndAVDL(docLengthPath);
        	if (qTermRelDocCountMap == null) {
            	setBM25Score(q, null, 0);
        	} else {
            	setBM25Score(q, qTermRelDocCountMap.get("" + q.id),
            			(queryRelDocCountMap.keySet().contains("" + q.id) ? queryRelDocCountMap.get("" + q.id) : 0));
        	}
        	
        	//printing the details
			Collections.sort(docScoreList);
        	printDocScore(q.id, n, fileNamePrefix);
		}
    	System.out.println("\nBM25 results generated for all queries");
	}
}
