package ir.algorithms.analysis;

/**
 * The class to store aggregated analysis results for a query
 * : Precision, Recall, Average Precision, Reciprocal Rank, Precision at Rank 5, Precision at Rank 20
 * @author Surbhi Gupta
 */
public class QueryStats {
       public int queryId;
       public double precision;
       public double recall;
       public double averagePrecision;
       public double reciprocalRank;
       public double pAt5;
       public double pAt20;
       
   	public QueryStats(int queryId, double precision, double recall, double averagePrecision, double reciprocalRank,
					  double pAt5, double pAt20) {
		this.queryId = queryId;
		this.precision = precision;
		this.recall = recall;
		this.averagePrecision = averagePrecision;
		this.reciprocalRank = reciprocalRank;
		this.pAt5 = pAt5;
		this.pAt20 = pAt20;
	}
}
