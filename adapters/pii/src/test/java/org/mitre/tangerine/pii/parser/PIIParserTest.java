package org.mitre.tangerine.pii.parser;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mitre.tangerine.pii.parser.PIIParser;

import junit.framework.TestCase;

public class PIIParserTest extends TestCase {

	public void testParse() {
		
		Map<String, String> parsy = new PIIParser().parse(this.getClass().getClassLoader().getResourceAsStream("pii.csv"));
		Set<String> keys = new HashSet<String>();
		keys.addAll(Arrays.asList("HasLienRecord","HasBankruptcyRecord","HasArrestRecord","HasCriminalRecord"));

		assert(parsy.keySet().containsAll(keys));
			
	}

}
