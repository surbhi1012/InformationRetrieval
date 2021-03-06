package ir.algorithms.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * @author Surbhi Gupta
 */
public class Analyzer {
	private static final String RESULT_FILE_TYPE = ".txt";
	private static ArrayList<DocRelevance> DocRelList = new ArrayList<>();
	private static int totalNumberofRel = 0;
	private static ArrayList<QueryStats> QueryStatsList = new ArrayList<>();
	private static double MAP = 0.0;
	private static double MRR = 0.0;

	/**
	 * The method to generate the performance statistics of the Search Engine and writing the results to file
	 * @param searchEngineResultFilePrefix is the relative path and prefix to the output files of the Search Engine
	 * @param relevanceFile is the relative path to the relevance judgement file
	 * @param outputFile is the relative path and prefix to the output analysis files generated by this method
	 * @param qCount is the number of queries processed by the search engine in this run
     * @throws IOException
     */
	public static void generateStats(String searchEngineResultFilePrefix, String relevanceFile, String outputFile, int qCount) throws IOException {
		System.out.println("Generating the stats...");

		int q = 1;
		while(q < qCount)
		{
			if(! shouldQueryBeExcluded(q, relevanceFile))
			{
				generateStatsForQuery(q, searchEngineResultFilePrefix, relevanceFile, outputFile);
				DocRelList.clear();
				totalNumberofRel = 0;
			}

			DocRelList.clear();
			totalNumberofRel = 0;
			q++;

		}

		for(QueryStats qs : QueryStatsList)
		{
			MAP += (Double.isNaN(qs.averagePrecision) ? 0d : qs.averagePrecision);
			MRR += (Double.isNaN(qs.reciprocalRank) ? 0d : qs.reciprocalRank);
		}

		MAP = MAP / QueryStatsList.size();
		MRR = MRR / QueryStatsList.size();

		printSearchEngineResults(outputFile, MAP, MRR);

		System.out.println("All stats generated");
	}

	/**
	 * The method to generate the performance statistics for the given query
	 * @param queryId is the query id for the given query
	 * @param searchEngineResultFilePrefix is the relative path and prefix to the output files of the Search Engine
	 * @param relevanceFile is the relative path to the relevance judgement file
	 * @param outputFile is the relative path and prefix to the output analysis files generated by this method
     * @throws IOException
     */
	private static void generateStatsForQuery(int queryId, String searchEngineResultFilePrefix,
			String relevanceFile, String outputFile) throws IOException {

		File resultFile = new File(searchEngineResultFilePrefix + queryId + RESULT_FILE_TYPE);

		try (BufferedReader br = new BufferedReader(new FileReader(resultFile)))
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				String[] words = line.split(" ");
				String doc = words[1];

				DocRelevance DocRel = new DocRelevance(doc, false, 0.0, 0.0);

				DocRelList.add(DocRel);
			}
		}
		setRelevanceBool(queryId, relevanceFile, outputFile);
	}

	/**
	 * The method that calculates Precision and Recall for all the documents relevant to this query
	 * @param queryId is the query id for the given query
	 * @param relFile is the relative path to the relevance judgement file
	 * @param outputFile is the relative path and prefix to the output analysis files generated by this method
	 * @throws IOException
     */
	private static void setRelevanceBool(int queryId, String relFile, String outputFile) throws IOException {
		File relevanceFile = new File(relFile);
		try (BufferedReader br = new BufferedReader(new FileReader(relevanceFile)))
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				String[] words = line.split(" ");

				if(Integer.parseInt(words[0]) == queryId)
				{
					totalNumberofRel++;
					DocRelList.stream().filter(dr -> words[2].equals(dr.docID)).forEach(dr -> dr.relevance = true);
				}
			}
		}
		calculatePrecisionandRecall(queryId, outputFile);
	}

	/**
	 * Helper method to calculate Precision and Recall for all the documents relevant to this query
	 * @param queryId is the query id for the given query
	 * @param outputFile is the relative path and prefix to the output analysis files generated by this method
	 * @throws IOException
     */
	private static void calculatePrecisionandRecall(int queryId, String outputFile) throws IOException{
		int RelNumber = 0;
		int i = 0;
		double precisionSum = 0.0;
		double reciRank = 0.0;
		double precisionAt5 = 0.0;
		double precisionAt20 = 0.0;
		int count = 0;

		for(int idx = 0; idx < DocRelList.size(); idx++)
		{
			if(DocRelList.get(idx).relevance)
			{
				RelNumber += 1;
				i++;
				DocRelList.get(idx).precision = (double)RelNumber / i;
				DocRelList.get(idx).recall = (double)RelNumber / totalNumberofRel;
				precisionSum += DocRelList.get(idx).precision;

				if(count == 0)
				{
					reciRank = 1d / (idx + 1);
					count++;
				}

				if(i == 5)
				{
					precisionAt5 = DocRelList.get(idx).precision;
				}

				if(i == 20)
				{
					precisionAt20 = DocRelList.get(idx).precision;
				}
			}
			else
			{
				i++;
				DocRelList.get(idx).precision = (double)RelNumber / i;
				DocRelList.get(idx).recall = (double)RelNumber / totalNumberofRel;

				if(i == 5)
				{
					precisionAt5 = DocRelList.get(idx).precision;
				}

				if(i == 20)
				{
					precisionAt20 = DocRelList.get(idx).precision;
				}
			}

		}

		QueryStats qs = new QueryStats(queryId, DocRelList.get(DocRelList.size() - 1).precision, DocRelList.get(DocRelList.size() - 1).recall,
				precisionSum / RelNumber, reciRank, precisionAt5, precisionAt20);

		QueryStatsList.add(qs);

		printQueryResults(queryId, outputFile, DocRelList.get(DocRelList.size() - 1).precision, DocRelList.get(DocRelList.size() - 1).recall,
				precisionSum / RelNumber, reciRank, precisionAt5, precisionAt20);

		printPandRTables(queryId, outputFile);

	}

	/**
	 * Utility method to write Precision and Recall tables in file
	 * @param queryId is the query id for the given query
	 * @param outputFile is the relative path and prefix to the output analysis files generated by this method
	 * @throws IOException
     */
	private static void printPandRTables(int queryId, String outputFile) throws IOException {
		FileWriter writer = new FileWriter(outputFile + "_forQuery_" + queryId + ".txt", true);
		DecimalFormat numberFormat = new DecimalFormat("#.00");

		for(DocRelevance dr :DocRelList){
			writer.write("DOCID: " + dr.docID +
					", DocREL: " + dr.relevance +
					", DocPrecision: " + numberFormat.format(dr.precision) +
					", DocRecall: " + numberFormat.format(dr.recall));
			writer.write("\r\n");
		}
		writer.close();
	}

	/**
	 * Utility method to print final analysis results aggregated over all documents for this query
	 * @param queryId is the query id for the given query
	 * @param outputFile is the relative path and prefix to the output analysis files generated by this method
	 * @param precision is the Precision for the query
	 * @param recall is the Recall for the query
	 * @param avePrecision is the Average Precision for the query
	 * @param RR is the Reciprocal Rank for the query
	 * @param pAt5 is the Precision at Rank 5 for the query
     * @param pAt20 is the Precision at Rank 20 for the query
     * @throws IOException
     */
	private static void printQueryResults(int queryId, String outputFile, double precision, double recall,
										  double avePrecision, double RR, double pAt5, double pAt20)
			throws IOException {
		FileWriter writer = new FileWriter(outputFile + "_forQuery_" + queryId + ".txt");
		DecimalFormat numberFormat = new DecimalFormat("#.00");

		writer.write("Precision: " + numberFormat.format(precision) +
				", Recall: " + numberFormat.format(recall) +
				", Average Precision: " + numberFormat.format(avePrecision) +
				", Reciprocal Rank: " + numberFormat.format(RR) +
				", Precision At Rank 5: " + numberFormat.format(pAt5) +
				", Precision At Rank 20: " + numberFormat.format(pAt20));
		writer.write("\r\n");

		writer.close();
	}

	/**
	 * The utility method to print the final Search Engine analysis results
	 * @param outputFile is the relative path and prefix to the output analysis files generated by this method
	 * @param MAP is the Mean Average Precision of this Search Engine, aggregated over all queries
	 * @param MRR is the Mean Reciprocal Rank of this Search Engine, aggregated over all queries
	 * @throws IOException
     */
	private static void printSearchEngineResults(String outputFile, double MAP, double MRR) throws IOException {
		FileWriter writer = new FileWriter(outputFile + RESULT_FILE_TYPE);
		DecimalFormat numberFormat = new DecimalFormat("#.00");

		writer.write("Mean Average Precision: " + numberFormat.format(MAP) +
				", Mean Reciprocal Rank: " + numberFormat.format(MRR));
		writer.close();
	}

	/**
	 * Utility method for checking if a specific query has to be excluded from analysis
	 * @param queryId is the id of the query in contention
	 * @param relFile is the relative path to the relevance judgements file
	 * @return TRUE if the given query has to be excluded from analysis
	 * @throws IOException
     */
	private static boolean shouldQueryBeExcluded(int queryId, String relFile) throws IOException {
		File RelFile = new File(relFile);
		boolean shouldExclude = true;

		try (BufferedReader br = new BufferedReader(new FileReader(RelFile)))
		{
			String line;
			while ((line = br.readLine()) != null)
			{
				String[] words = line.split(" ");
				String qID = words[0];

				if(Integer.parseInt(qID) == queryId)
				{
					shouldExclude = false;
				}
			}
		}
		return shouldExclude;
	}
}