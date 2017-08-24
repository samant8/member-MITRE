package org.mitre.tangerine.voucher.adapter;

import static org.junit.Assert.*;


import java.util.Map;

import org.junit.Test;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.voucher.adapter.VoucherAdapter;
import org.mitre.tangerine.voucher.parser.VoucherParser;

import com.google.gson.GsonBuilder;

public class VoucherAdapterTest {

	// 8/1/2017 - Runs a test of the voucher adapter
	
	@Test
	public void testAdapt() {

		Map<String, String> input = new VoucherParser().parse(this.getClass().getClassLoader().getResourceAsStream("Claim.csv"));
		VoucherAdapter ad = new VoucherAdapter();
		ResponseModel rep = null;
		
		try {
			rep = ad.adapt("collection", input);
		} catch (AETException e) {
			fail();
		}
		
		String json = new GsonBuilder().setPrettyPrinting().create().toJson(rep);		
		System.out.println(json);
		assert(json.contains("AE#VATravelVoucher"));
		assert(json.contains("\"collection\": \"collection\""));
		
	}

}
