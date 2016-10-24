package ir.commons;

/**
 * Utility class to store a query with its id
 * @author Surbhi Gupta
 */
public class SearchQuery {
	public int id;
	public String queryText;
	
	public SearchQuery(int id, String queryText) {
		this.id = id;
		this.queryText = queryText;
	}

	public void setQueryText(String newText) {
		queryText = newText;
	}
}
