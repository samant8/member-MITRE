/*
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *                   NOTICE
 *
 * This software was produced for the U. S. Government
 * under Basic Contract No. FA8702-17-C-0001, and is
 * subject to the Rights in Noncommercial Computer Software
 * and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (MAY 2013)
 *
 * (c)2016-2017 The MITRE Corporation. All Rights Reserved.
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
/*

   _____                .__               .__
  /  _  \   ____ _____  |  | ___.__. _____|__| ______
 /  /_\  \ /    \\__  \ |  |<   |  |/  ___/  |/  ___/
/    |    \   |  \/ __ \|  |_\___  |\___ \|  |\___ \
\____|__  /___|  (____  /____/ ____/____  >__/____  >
        \/     \/     \/     \/         \/        \/
___________             .__
\_   _____/__  ___ ____ |  |__ _____    ____    ____   ____
 |    __)_\  \/  // ___\|  |  \\__  \  /    \  / ___\_/ __ \
 |        \>    <\  \___|   Y  \/ __ \|   |  \/ /_/  >  ___/
/_______  /__/\_ \\___  >___|  (____  /___|  /\___  / \___  >
        \/      \/    \/     \/     \/     \//_____/      \/

 */
package org.mitre.tangerine.voucher.adapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.mitre.tangerine.adapter.Adapter;
import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.AssertionModel;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.models.AssertionModel.ASSERT_TYPE;
import org.mitre.tangerine.voucher.adapter.VoucherDataMapModel.OntologyMappings;

import com.google.gson.Gson;

// create base loader abstract class.
// move utilities class as a external library & base adapter class
// document what the letters are	
public class VoucherAdapter extends Adapter<Map<String, String>, Document> {
	private final String name = "VO", canonicalName = "Voucher Adapter";

	public VoucherAdapter() {
		super();
	}

	protected class Tuple {
		String uuid;
		String object;

		public Tuple(String uuid, String object) {
			this.uuid = uuid;
			this.object = object;
		}

		public String getUuid() {
			return uuid;
		}

		public String getObject() {
			return object;
		}
	}

	/**
	 * Load the voucher mapping... beit from JSON, plain text, OWL, etc.
	 * 
	 * @throws AETException
	 */
	@Override
	public ResponseModel adapt(String id, Map<String, String> vouchers) throws AETException {
		BufferedReader read = new BufferedReader(
				new InputStreamReader(this.loadFile(this.getClass(), "VoucherDataMap.json")));
		ResponseModel ret = new ResponseModel();
		Map<String, Tuple> instances = new HashMap<String, Tuple>();
		int seq = 0;

		List<OntologyMappings> ontolist = new Gson().fromJson(read, VoucherDataMapModel.class).getOntologyMappings();

		Map<String, String> instance = null;

		ret.setCollection(id);

		Tuple tuple = new Tuple(id + "_" + seq, "AE#VATravelVoucher");
		instances.put("$Voucher", tuple);

		ret.addAssertion((new AssertionModel(id + "_" + seq, "a", "AE#VATravelVoucher", ASSERT_TYPE.OBJECT)));
		seq += 1;

		// create mappings per voucher by iterating over unparsed mapping
		// file
		for (int count = 0; count < ontolist.size(); count++) {
			instance = ontolist.get(count).getMapping().getInstances();
			// iterates over instances and stores them
			if (instance != null)
				for (String s : instance.keySet()) {
					instances.put(s, new Tuple(id + "_" + seq, instance.get(s)));
					seq += 1;
					ret.addAssertion(
							(new AssertionModel(instances.get(s).getUuid(), "a", instance.get(s), ASSERT_TYPE.OBJECT)));
				}
			instance = null;
		}
		for (int j = 0; j < ontolist.size(); j++) {
			// gets a mapping for a specific key
			String key = ontolist.get(j).getKey();
			List<Map<String, String>> asserts = ontolist.get(j).getMapping().getAsserts();

			for (int k = 0; k < asserts.size(); k++, seq++) {

				Tuple subject = instances.get(asserts.get(k).get("S"));
				String predicate = asserts.get(k).get("P");
				String obj = asserts.get(k).get("O");

				System.out.println(subject.getObject());
				System.out.println(subject.getUuid());
				System.out.println(predicate);
				System.out.println(obj);
				System.out.println(instances.get(obj));
				System.out.println("##########################");
				
				if (!obj.equals("<value>")) {
					ret.addAssertion((new AssertionModel(subject.getUuid(), predicate, instances.get(obj).getUuid(),
							ASSERT_TYPE.OBJECT)));
				} else {
					ret.addAssertion(
							(new AssertionModel(subject.getUuid(), predicate, vouchers.get(key), ASSERT_TYPE.DATA)));
				}
			}

		}
		return ret;
	}

	@Override
	public ResponseModel updateDB(ResponseModel data, AETDatabase db) throws AETException {
		int elems = 0;
		List<AssertionModel> elements = data.getAssertions();
		String collection = data.getCollection();
		db.openConnection();
		db.accessDatabase();
		db.accessSelection(collection);
		for (AssertionModel assertion : elements) {
			try {
				db.updateSelection(Document.parse(new Gson().toJson(assertion)));
			} catch (AETException e) {
				data.setError(true);
				data.setMessage(e.getMessage());
			}
			elems++;
		}
		db.indexSelection();
		db.closeConnection();
		data.setNumberOfElements(elems);
		return data;

	}

	@Override
	public String getCanonicalName() {
		return this.canonicalName;
	}

	@Override
	public String getName() {
		return this.name;
	}

}
