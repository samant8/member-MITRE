package org.mitre.tangerine.esri.adapter;

import static org.junit.Assert.*;


import java.util.Map;

import org.junit.Test;
import org.mitre.tangerine.esri.adapter.EsriAdapter;
import org.mitre.tangerine.esri.parser.EsriParser;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;

import com.google.gson.GsonBuilder;

public class EsriAdapterTest {

	@Test
	public void testAdapt() {
		Map<String, String> input = new EsriParser().parse(this.getClass().getClassLoader().getResourceAsStream("esri.csv"));
		for (Map.Entry<String, String> entry : input.entrySet()) {
			System.out.println("Key : " + entry.getKey() + " Value : " + entry.getValue());
		}
		
		EsriAdapter ad = new EsriAdapter();
		ResponseModel rep = null;
		
		try {
			rep = ad.adapt("collection", input, "Travelling_UUID", "AE#Travelling");
		} catch (AETException e) {
			fail();
		}
		
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(rep);		
		System.out.println(json);
		assert(json.contains("AE#hasDistance"));
		assert(json.contains("\"collection\": \"collection\""));
		
	}

}
