package org.mitre.tangerine.pii.adapter;

import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.pii.adapter.PIIAdapter;
import org.mitre.tangerine.pii.parser.PIIParser;

import com.google.gson.GsonBuilder;

public class PIIAdapterTest {
	
	@Test
	public void testAdapt() {
		Map<String, String> input = new PIIParser().parse(this.getClass().getClassLoader().getResourceAsStream("pii.csv"));
		for (Map.Entry<String, String> entry : input.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}
		
		PIIAdapter ad = new PIIAdapter();
		ResponseModel rep = null;
		
		try {
			rep = ad.adapt("collection", input, "Human_UUID");
		} catch (AETException e) {
			fail();
		}
		
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(rep);		
		System.out.println(json);
		
	}
}
