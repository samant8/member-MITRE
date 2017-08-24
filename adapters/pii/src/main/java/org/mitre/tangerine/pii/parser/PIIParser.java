package org.mitre.tangerine.pii.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.mitre.tangerine.parser.Parser;

/**
 * Parses a CSV and returns a list of entries
 * 
 * @author atrivedi@mitre.org
 *
 */
public class PIIParser extends Parser<Map<String, String>> {
	/**
	 * @param input
	 * @param delimiter
	 * @return A list of maps; each entry in the list corresponds to an entry in
	 *         the csv (with the exception of the first row). The map consists
	 *         of keys generated from the csv's column headers (first row) and
	 *         the value for that entry.
	 */
	@Override
	public Map<String, String> parse(InputStream input) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		Map<String, String> temp = new HashMap<String, String>();
		String[] keys, data;
		char delimiter = ',';

		try {
			keys = reader.readLine().split(",");
			data = reader.readLine().split(delimiter + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

			// TODO if(data.length != keys.length) throw new AETException();

			for (int i = 0; i < data.length; i++) {
				temp.put(keys[i], data[i]);
			}
		} catch (IOException e) {
			e.printStackTrace();
			// TODO throw new AETException();
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// TODO throw new AETException();
				e.printStackTrace();
			}
		}
		return temp;
	}
}
