package ir.algorithms.snippet;

import ir.commons.SearchQuery;
import ir.commons.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Surbhi Gupta
 */
public class SnippetGenerator {
	private String resultPath;
	private List<SearchQuery> queryList;
	private String corpusPath;
	private String snippetFileSavePrefix;

	private static final int SNIPPET_SIZE = 25;
	private static final int WINDOW_SIZE = 8;
	private static final String CORPUS_FILE_TYPE = ".txt";

	public SnippetGenerator(String resultPath, List<SearchQuery> queryList, String corpusPath, String snippetFileSavePrefix) {
		this.resultPath = resultPath.substring(0, resultPath.lastIndexOf("/"));
		this.queryList = queryList;
		this.corpusPath = corpusPath;
		this.snippetFileSavePrefix = snippetFileSavePrefix;
	}

	/**
	 * a private class to score the snippet for a query
	 */
	private static class SnippetFragmentScore implements Comparable<SnippetFragmentScore> {
		String fragmentText;
		double score;
		
		SnippetFragmentScore(String fragment, double score) {
			this.fragmentText = fragment;
			this.score = score;
		}

		@Override
		public int compareTo(SnippetFragmentScore sfc) {
			return (score < sfc.score ? 1 : score > sfc.score ? -1 : 0);
		}
	}

	/**
	 * method to create and write snippets to file(s): 1 file per query
	 * this being an optional step in the Search process, it reads the Search results from the disk
 	 * @throws IOException
     */
	public void generateSnippet() throws IOException {
		System.out.println("Generating snippets...");

		// get all the file names from the directory
		List<File> fileList = Utils.readFilesFromDirectory(resultPath, "", "");
		
		// read all the first run results	-	1 file here is for 1 query
		int qIndex = 0;
		for (File f : fileList) {
			List<String> snippetList = new ArrayList<>();
			
			//getting the query text
        	String qText = queryList.get(qIndex).queryText;
        	
			//reading a result file
			BufferedReader buffer = new BufferedReader(new FileReader(f.getAbsolutePath()));
	        String line;
	        while ((line = buffer.readLine()) != null) {
	        	String docID = line.split(" ")[1];
	        	String docPath = corpusPath + docID + CORPUS_FILE_TYPE;
	        	
	        	//reading the doc file from the corpus dir
	        	File d = new File(docPath);
	        	String docContent = new String(Files.readAllBytes(d.toPath()));
	        	
	        	String snippet = getSnippetFromContent(qText, docContent);
	        	snippetList.add(docID + " : " + snippet);
	        }
	        buffer.close();
	        
	        // create a new file with this snippet list
	        writeSnippetList(snippetList, qIndex + 1);
	        qIndex++;
		}
		System.out.println("All snippets generated");
	}

	/**
	 * helper method to write snippets to file for the given query id
	 * this being an optional step in the Search process, it reads the Search results from the disk
	 * @param snippetList is the list of snippets generated for the given querty id and to be written in a file
	 * @param qID is the query id for which the snippets are written in a file
     */
	private void writeSnippetList(List<String> snippetList, int qID) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(snippetFileSavePrefix + qID + CORPUS_FILE_TYPE);
			for (String snippet : snippetList) {
				writer.write(snippet + "\n"); 
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
	 * The method to generate snippets from the document content
	 * @param queryText is the text of the query for which the snippet has to be generated
	 * @param content is the document content as String
     * @return snippet generated for the given query text from the document content
     */
	private String getSnippetFromContent(String queryText, String content) {
		//init a list of fragments, all with a score of 0
		List<SnippetFragmentScore> fragments = getFragments(content);

		String[] qTerms = queryText.split(" ");
		
		//loop through and update score
		for (SnippetFragmentScore sf : fragments) {
			double score = 0d;
			for (String qTerm : qTerms) {
				if (!qTerm.equals("")) {
					String[] parts = sf.fragmentText.split(" ");
					for (String part : parts) {
						if (part.trim().equals(qTerm.trim())) {
							score += 1d;
						}
					}
				}
			}
			
			sf.score = score;
		}
		
		Collections.sort(fragments);
		
		String snippet = "";
		int snippetLength = 0;
		for (SnippetFragmentScore sf : fragments) {
			if (snippetLength + WINDOW_SIZE <= SNIPPET_SIZE) {
				snippet += highlightFragment(qTerms, sf.fragmentText.trim()) + "...";
				snippetLength += WINDOW_SIZE;
			} else {
				String[] words = sf.fragmentText.split(" ");
				for (String word : words) {
					snippet += (Utils.listContainsTextIgnoreCase(qTerms, word) ? "[" + word + "]" : word);
					snippetLength += 1;
					if (snippetLength == SNIPPET_SIZE) break;
				}
			}
			if (snippetLength == SNIPPET_SIZE) break;
		}
		
		return snippet.trim();
	}

	/**
	 * The aesthetic method to highlight all the query terms in the generated snippet fragment
	 * @param qTerms is the array of query terms
	 * @param fragment is the generated fragment for the given query
     * @return the highlighted fragment
     */
	private String highlightFragment(String[] qTerms, String fragment) {
		String[] words = fragment.split(" ");
		fragment = "";

		for (String word : words) {
			fragment += (Utils.listContainsTextIgnoreCase(qTerms, word) ? "[" + word + "]" : word) + " ";
		}
		
		return fragment.trim();
	}

	/**
	 * The method to split the given document text into fragments of length WINDOW_SIZE
	 * @param fileContent is the is the document content as String
	 * @return the list of fragments generated
     */
	private List<SnippetFragmentScore> getFragments(String fileContent) {
		List<SnippetFragmentScore> wordList = new ArrayList<>();
		
		String[] wList = fileContent.split("\\s+");
		
		for (int i = 0; i < wList.length; i += WINDOW_SIZE) {
			String accumulator = "";
			for (int j = 0; j < WINDOW_SIZE; j++) {
				if (i + j < wList.length) {
					accumulator += wList[i + j] + " ";
				}
			}
			SnippetFragmentScore sf = new SnippetFragmentScore(accumulator, 0d);	//init score with 0
			wordList.add(sf);
		}
		
		return wordList;
	}
}
