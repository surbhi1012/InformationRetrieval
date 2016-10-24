package ir.algorithms.scoreAndRank;

import java.util.HashMap;

/**
 * Utility class to store the BM25 document score of documents, along with their length and a mapping of document terms
 * to their frequencies
 */
public class DocScore implements Comparable<DocScore> {
	public String docID;
	public double docScore;
	public int docLength;
	public HashMap<String, Integer> tfMap;

	@Override
	public int compareTo(DocScore ds) {
		return (docScore < ds.docScore ? 1 : docScore > ds.docScore ? -1 : 0);
	}
}
