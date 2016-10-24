package ir.commons;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Surbhi Gupta
 */
public class Utils {
	private static final String ILLEGAL_FILE_NAME = ".DS_Store";
	private static final String UTF8 = "UTF-8";
	private static final String ROOT_XML_START_TAG = "<root>";
	private static final String ROOT_XML_END_TAG = "</root>";
	private static final String DOC_ELEMENT_NAME = "DOC";
	private static final String DOC_NUMBER_ELEMENT_NAME = "DOCNO";

	/**
	 * The method to create a list of SearchQuery objects reading from an XML file
	 * @param fileName is the relative name of the file that contains the XML of the input queries
	 * @return a list of the queries as objects of the SearchQuery class
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
     */
	public static List<SearchQuery> parseXML(String fileName) throws IOException, ParserConfigurationException, SAXException {
		List<SearchQuery> queryList = new ArrayList<>();
		
		File fXmlFile = new File(fileName);
		Scanner sc = new Scanner(fXmlFile, UTF8);
		String fileContent = new String(Files.readAllBytes(fXmlFile.toPath()));	//parse the entire file content into a string
		sc.close();
		
		String xmlContent = ROOT_XML_START_TAG + fileContent + ROOT_XML_END_TAG;
		DocumentBuilder newDocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = newDocumentBuilder.parse(new ByteArrayInputStream(xmlContent.getBytes()));

		// get a list of all the nodes in the XML that denote a query
		NodeList nList = doc.getElementsByTagName(DOC_ELEMENT_NAME);
		for (int i = 0; i < nList.getLength(); i++) {
			Element e = (Element) nList.item(i);

			String idText = e.getElementsByTagName(DOC_NUMBER_ELEMENT_NAME).item(0).getTextContent().trim();
			int id = Integer.parseInt(idText);
			String text = e.getTextContent().trim().substring(idText.length()).trim();
			
			if (text.length() > 0) {
				SearchQuery q = new SearchQuery(id, processContent(text));
				queryList.add(q);
			}
		}
		
		return queryList;
	}

	/**
	 * The utility method to strip a wikipedia page of extra white spaces, special characters and citations
	 * @param content is the page content as String
	 * @return processed content after removing extra white spaces, special characters and citations
     */
	public static String processContent(String content) {
		content = content
				.toLowerCase()
				.replaceAll("\n", " ")					//remove all carriage returns
				.replaceAll("\\s+", " ")				//strip all extra spaces
				.replaceAll("[^0-9a-z-,. ]+", "")		//allow only numbers/alphabets/-/./,
				.replaceAll("\\[[0-9]+\\]", "")			//remove citations i.e; [a number]
				.replaceAll("(?<!\\d)\\.(?!\\d)|(?<!\\d),(?!\\d)", " ")		// removing patterns like a.a or a,a
				.replaceAll("(?<=\\d)\\.(?!\\d)|(?<=\\d),(?!\\d)", " ")		// removing patterns like 1.a or 1,a
				.replaceAll("(?<!\\d)\\.(?=\\d)|(?<!\\d),(?=\\d)", " ")		// removing patterns like a.1 or a,1
				.trim();
		
		return content;
	}

	/**
	 * The utility method to read all allowed files from a directory into a list of files
	 * @param dir is the relative path of the directory to read from
	 * @return list of read file
     */
	public static List<File> readFilesFromDirectory(String dir, String prefix, String postfix) {
		List<File> fileList = new ArrayList<>();
		File folder = new File(dir);
		File[] listOfFiles = folder.listFiles();

		if (listOfFiles != null) {
			for (File file : listOfFiles) {
				if (file.isFile() && shouldReadFile(file, prefix, postfix)) {
					fileList.add(file);
				}
			}
		}

		return fileList;
	}

	/**
	 * The utility method to tell if the file is to be read or ignored
	 * @param f is the file to be evaluated for reading
	 * @return TRUE if the file has to be read
     */
	private static boolean shouldReadFile(File f, String prefix, String postfix) {
		if (f.getName().contains(ILLEGAL_FILE_NAME))
			return false;
		if (!prefix.isEmpty()) {
			if (!f.getName().startsWith(prefix))
				return false;
		}
		if (!postfix.isEmpty()) {
			if (!f.getName().endsWith(postfix))
				return false;
		}
		return true;

	}

	/**
	 * The utility method to check if a text is present in a list of texts, ignoring case
	 * @param list is the list of strings to look into
	 * @param text is the string to search for
     * @return TRUE is the given string is present in the list, ignoring case
     */
	public static boolean listContainsTextIgnoreCase(String[] list, String text) {
		boolean present = false;

		for (String aList : list) {
			if (aList.trim().equals(text)) {
				present = true;
				break;
			}
		}

		return present;
	}
}
