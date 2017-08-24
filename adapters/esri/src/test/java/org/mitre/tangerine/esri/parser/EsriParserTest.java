package org.mitre.tangerine.esri.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mitre.tangerine.esri.parser.EsriParser;

import junit.framework.TestCase;

public class EsriParserTest extends TestCase {

	public void testParse() {
		Map<String, String> parsy = new EsriParser().parse(this.getClass().getClassLoader().getResourceAsStream("esri.csv"));
		for (Map.Entry<String, String> entry : parsy.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}
		
		Set<String> keys = new HashSet<String>();		
		keys.addAll(Arrays.asList("StartLatitude","StartLongitude","EndLatitude","EndLongitude"));
		assert(parsy.keySet().containsAll(keys));			
	}

}
