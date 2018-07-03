package mx.nic.lab.rpki.sqlite.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Read queries from .sql files
 */
public class QueryGroup {

	/** Directory where the queries files will be found */
	private static final String DEFAULT_SQL_FILES_DIR = "META-INF/sql/";

	/** String to represent the schema in the queries */
	private static final String SCHEMA_KEY = "{schema}";
	private static final Pattern SCHEMA_PATTERN = Pattern.compile(Pattern.quote(SCHEMA_KEY));

	/** The queries, indexed by means of their names. */
	private Map<String, String> queries;

	/**
	 * Loads the query group described by file <code>file</code>.
	 * 
	 * @param file
	 *            name of the file where the queries are stored, minus extension.
	 *            The function will assume that the file can be found in the
	 *            classpath, under the "META-INF/sql/" directory.
	 * @throws IOException
	 *             Problems reading the <code>file</code> file.
	 */
	public QueryGroup(String file, String schema) throws IOException {
		String filePath = DEFAULT_SQL_FILES_DIR + file + ".sql";
		InputStream in = QueryGroup.class.getClassLoader().getResourceAsStream(filePath);

		queries = new HashMap<String, String>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {

			StringBuilder querySB = new StringBuilder();
			String queryName = null;
			String currentLine;

			while ((currentLine = reader.readLine()) != null) {
				if (currentLine.startsWith("#")) {
					queryName = currentLine.substring(1).trim();
				} else {
					querySB.append(currentLine);
					if (currentLine.trim().endsWith(";")) {
						// If the query has no name, it will be ignored.
						if (queryName != null) {
							String queryString = querySB.toString();
							if (schema != null) {
								queryString = SCHEMA_PATTERN.matcher(queryString).replaceAll(schema);
							}
							String oldQuery = queries.put(queryName, queryString);
							if (oldQuery != null) {
								throw new IllegalArgumentException("There is more than one '" + queryName + "' query.");
							}
						}
						querySB.setLength(0);
					} else {
						querySB.append(" ");
					}
				}
			}
		}
	}

	/**
	 * Returns the query whose name is <code>name</code>.
	 * 
	 * @param name
	 *            label that (in the file) prefixes the query along with a '#'
	 *            symbol.
	 * @return the query whose name is <code>name</code>.
	 */
	public String getQuery(String name) {
		return queries.get(name);
	}

	/**
	 * @return the queries map
	 */
	public Map<String, String> getQueries() {
		return queries;
	}

}
