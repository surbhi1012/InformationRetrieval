package ir.algorithms.analysis;

/**
 * The class to store analysis results (Precision and Recall) for a document along with the binary relevance
 * @author Surbhi Gupta
 */
public class DocRelevance {
	public String docID;
	public boolean relevance;
	public double precision;
	public double recall;
	
	public DocRelevance(String docID, boolean relevance, double precision, double recall) {
		this.docID = docID;
		this.relevance = relevance;
		this.precision = precision;
		this.recall = recall;
	}
}
