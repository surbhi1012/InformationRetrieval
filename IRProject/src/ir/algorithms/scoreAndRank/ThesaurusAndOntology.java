package ir.algorithms.scoreAndRank;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;
import ir.commons.SearchQuery;
import ir.commons.Utils;

/**
 * @author Surbhi Gupta
 */
public class ThesaurusAndOntology {
	private static final int NUM_SYNONYMS_TO_ADD = 20;
	private static final String WORDNET_DB_PATH = "wordnet.database.dir";
	private static final String WORDNET_DICT_PATH = "WordNet-3.0/dict";
	private static final String[] relevantDomains = {"(computer science)", "(electronics)", "(communication theory)",
			"(digital communication)", "(computing)"};

	private Set<String> stopWords;

	public ThesaurusAndOntology(String stopListFile) throws IOException {
		stopWords = new HashSet<>();

		File stopWordsFile = new File(stopListFile);
		try (BufferedReader br = new BufferedReader(new FileReader(stopWordsFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] words = line.split(" ");
				stopWords.add(words[0]);
			}
		}
	}

	/**
	 * The method to expand the individual queries with at most 20 synonyms of its non-stop terms
	 * @param queries is the list of SearchQuery objects representing the original ones without any modification
	 * @return the same list of SearchQuery expanded with synonyms of each non-stop terms
     */
	public List<SearchQuery> getExpandedQueries_THR(List<SearchQuery> queries)
	{
		System.out.println("Expanding queries...");

		for (SearchQuery sq : queries) {
			Set<String> totalExpansionWords = new HashSet<>();
			String expansionString = "";

			String[] qTerms = sq.queryText.split(" ");

			for (String qTerm : qTerms) {
				Set<String> expansionWords = new HashSet<>();
				if (!stopWords.contains(qTerm))
					expansionWords = getExpansionWords(qTerm);
				totalExpansionWords.addAll(expansionWords.stream().filter(e -> !stopWords.contains(e)).collect(Collectors.toList()));
			}

			if (totalExpansionWords.isEmpty()) {
				Set<String> expansionTerms = getSynonyms(sq);
				totalExpansionWords.addAll(expansionTerms.stream().filter(e -> !stopWords.contains(e)).collect(Collectors.toList()));
			}

			int k = 0;
			for (String words : totalExpansionWords) {
				if (k < NUM_SYNONYMS_TO_ADD)
					expansionString += words + " ";
				k++;
			}

			sq.setQueryText(sq.queryText.trim() + " " + expansionString.trim());
		}
		
		return queries;
	}

	/**
	 * The method to find and return the set of synonyms of the query terms of the SearchQuery
	 * @param sq is a SearchQuery whose synonyms are to be found
	 * @return the set of synonyms for the terms of the SearchQuery
     */
	private Set<String> getSynonyms(SearchQuery sq) {
		Set<String> words = new HashSet<>();
		String query = sq.queryText;
		String[] qTerms = query.split(" ");

		WordNetDatabase database = WordNetDatabase.getFileInstance();

		for(String qTerm : qTerms){
			Synset[] synonymSets = database.getSynsets(qTerm);

			if(synonymSets.length > 0){
				for (Synset synonymSet : synonymSets) {
					for (String synonyms : synonymSet.getWordForms()) {
						String[] synonym = synonyms.split(" ");

						for (String w : synonym) {
							if (!stopWords.contains(w))
								words.add(w);
						}
					}
				}
			}
		}
		return words;
	}

	/**
	 * The method to return the synonyms of a word from relevant domains
	 * @param term is the word for which the synonyms are to be collected
	 * @return a set of synonyms from relevant domains for this term
     */
	private Set<String> getExpansionWords(String term){
		Set<String> expansionWords = new HashSet<>();
		System.setProperty(WORDNET_DB_PATH, WORDNET_DICT_PATH);

		WordNetDatabase database = WordNetDatabase.getFileInstance();

		Synset[] synsets = database.getSynsets(term);
		String content = "";
		if (synsets.length > 0){
			for (Synset synset : synsets) {
				boolean ofRelevantDomain = false;
				for (String relevantDomain : relevantDomains) {
					if (synset.getDefinition().contains(relevantDomain)) {
						ofRelevantDomain = true;
						break;
					}
				}
				if (ofRelevantDomain)
					content += synset.getDefinition();
			}
		}
		else{
			content = "";
		}
		content = Utils.processContent(content);
		String[] expansionTerms = content.split(" ");
		for(String s : expansionTerms)
		{
			if(! stopWords.contains(s) && ! s.equals(""))
				expansionWords.add(s);
		}
		return expansionWords;
	}
}