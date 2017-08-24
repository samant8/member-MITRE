package org.mitre.tangerine.pii.adapter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.mitre.tangerine.adapter.Adapter;
import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.AssertionModel;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.models.AssertionModel.ASSERT_TYPE;
import org.mitre.tangerine.pii.adapter.PIIDataMapModel.OntologyMappings;

import com.google.gson.Gson;

public class PIIAdapter extends Adapter<Map<String, String>, Document> {
	//private MongoQueries mongoQueries;
	private String uuid;
	private String name = "PI", canonicalName = "Personally Identifiable Information Analytic";

	public PIIAdapter() {
		super();
	}

	public ResponseModel adapt(String collection, Map<String, String> pii, String uuid) throws AETException {
		this.uuid = uuid;
		return this.adapt(collection, pii);
		
	}


	@Override
	public ResponseModel adapt(String collection, Map<String, String> pii) throws AETException {
		ResponseModel response = new ResponseModel();
		InputStream input = this.getClass().getClassLoader().getResourceAsStream("PIIDataMap.json");
		BufferedReader read = new BufferedReader(new InputStreamReader(input));
		Gson gson = new Gson();
		List<OntologyMappings> ontolist = gson.fromJson(read, PIIDataMapModel.class).getOntologyMappings();

		response.setCollection(collection);
		
		for (int j = 0; j < ontolist.size(); j++) {
			// gets a mapping for a specific key
			String key = ontolist.get(j).getKey();
			List<Map<String, String>> asserts = ontolist.get(j).getMapping().getAsserts();

			Iterator<String> keyseti = pii.keySet().iterator();
			while (keyseti.hasNext()) {
				String k = keyseti.next();
				System.out.println(k+":"+pii.get(k));
			}
			
			for (int k = 0; k < asserts.size(); k++) {
				String subject = this.uuid;
				String predicate = asserts.get(k).get("P");
								
				response.addAssertion(new AssertionModel(subject, predicate, pii.get(key), ASSERT_TYPE.DATA));
			}

		}
		return response;
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
		for(AssertionModel assertion : elements){
			try{
			db.updateSelection(Document.parse(new Gson().toJson(assertion)));
			} catch(AETException e){
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
