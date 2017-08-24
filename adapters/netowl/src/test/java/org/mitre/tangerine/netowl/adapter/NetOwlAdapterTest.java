package org.mitre.tangerine.netowl.adapter;

import static org.junit.Assert.*;


import java.util.Map;

import org.junit.Test;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.netowl.adapter.NetOwlAdapter;
import org.mitre.tangerine.netowl.parser.Content;
import org.mitre.tangerine.netowl.parser.NetOwlParser;

import com.google.gson.GsonBuilder;

public class NetOwlAdapterTest {

	// 8/1/2017 - Runs a test of the voucher adapter
	
	@Test
	public void testAdaptName() throws AETException {

		Content input = new NetOwlParser().parse(this.getClass().getClassLoader().getResourceAsStream("netowl_name.xml"));
		NetOwlAdapter ad = new NetOwlAdapter();
		ResponseModel rep = null;
		
		try {
			rep = ad.adapt("collection", input, "Human_UUID", "entity:person");
		} catch (AETException e) {
			fail();
		}
		
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(rep);		
		//System.out.println(json);
		
	}
	
	@Test
	public void testAdaptAddress() throws AETException {

		Content input = new NetOwlParser().parse(this.getClass().getClassLoader().getResourceAsStream("netowl_address.xml"));
		NetOwlAdapter ad = new NetOwlAdapter();
		ResponseModel rep = null;
		
		try {
			rep = ad.adapt("collection", input, "PostalAddress_UUID", "entity:address:mail");
		} catch (AETException e) {
			fail();
		}
		
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(rep);		
		//System.out.println(json);
		
	}

}
