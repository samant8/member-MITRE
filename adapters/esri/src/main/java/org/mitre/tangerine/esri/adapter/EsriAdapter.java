package org.mitre.tangerine.esri.adapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.mitre.tangerine.adapter.Adapter;
import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.esri.adapter.EsriDataMapModel.OntologyMappings;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.AssertionModel;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.models.AssertionModel.ASSERT_TYPE;

import com.google.gson.Gson;

public class EsriAdapter extends Adapter<Map<String, String>, Document> {
	// private MongoQueries mongoQueries;
	private String uuid, entity;
	private String name = "ES", canonicalName = "Environmental Systems Research Institute Analytic";

	public EsriAdapter() {
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

	public ResponseModel adapt(String collection, Map<String, String> data, String uuid, String entity) throws AETException {
		this.uuid = uuid;
		this.entity = entity;
		return this.adapt(collection, data);
	}

	@Override
	public ResponseModel adapt(String id, Map<String, String> data) throws AETException {
		BufferedReader read = new BufferedReader(
				new InputStreamReader(this.loadFile(this.getClass(), "EsriDataMap.json")));
		ResponseModel ret = new ResponseModel();
		Map<String, Tuple> instances = new HashMap<String, Tuple>();
		int seq = 0;

		List<OntologyMappings> ontolist = new Gson().fromJson(read, EsriDataMapModel.class).getOntologyMappings();
		Map<String, String> instance = null;
		ret.setCollection(id);		

		for (int count = 0; count < ontolist.size(); count++) {
			instance = ontolist.get(count).getMapping().getInstances();
			// iterates over instances and stores them
			if (instance != null)
				for (String s : instance.keySet()) {
					if (this.entity.equals(instance.get(s))) {
						instances.put(s, new Tuple(this.uuid, instance.get(s)));
					} else{
						instances.put(s, new Tuple(id + "_" + seq, instance.get(s)));
						seq += 1;
						ret.addAssertion(
								(new AssertionModel(instances.get(s).getUuid(), "a", instance.get(s), ASSERT_TYPE.OBJECT)));
					}					
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

				if (obj.equals("<value>")) {
					ret.addAssertion(
							(new AssertionModel(subject.getUuid(), predicate, data.get(key), ASSERT_TYPE.DATA)));
				} else if (obj.contains("$")) {
					ret.addAssertion((new AssertionModel(subject.getUuid(), predicate, instances.get(obj).getUuid(),
							ASSERT_TYPE.OBJECT)));				
				} else {
					ret.addAssertion((new AssertionModel(subject.getUuid(), predicate, obj, ASSERT_TYPE.DATA)));
				}
			}
		}
		return ret;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getCanonicalName() {
		return this.canonicalName;
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
}
