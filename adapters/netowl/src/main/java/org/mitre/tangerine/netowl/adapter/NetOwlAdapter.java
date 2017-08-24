package org.mitre.tangerine.netowl.adapter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.mitre.tangerine.netowl.adapter.NetOwlDataMapModel.Mappings;
import org.mitre.tangerine.netowl.parser.Content;
import org.mitre.tangerine.netowl.parser.Property;
import org.mitre.tangerine.netowl.parser.Content.Document.Entity;
import org.mitre.tangerine.netowl.parser.Content.Document.Link;
import org.mitre.tangerine.netowl.parser.Content.Document.Sentiment.EntityArg;

import com.google.gson.Gson;

//TODO create second constructor
public class NetOwlAdapter extends Adapter<Content, Document> {
	
	private final String name = "NO";
	private final String canonicalName = "NetOwl Analytic";
	private String refUuid, refEntity, collection;
	
public NetOwlAdapter() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public ResponseModel adapt(String collection, Content data) throws AETException {
		ResponseModel response = new ResponseModel();
		this.collection = collection;
		Map<String, HashMap<String, String>> ontologyMappings = new HashMap<String, HashMap<String, String>>();
		Map<String, HashMap<String, String>> argumentMappings = new HashMap<String, HashMap<String, String>>();		
		response.setCollection(collection);
		
		BufferedReader read = new BufferedReader(new InputStreamReader(this.loadFile(this.getClass(), "NetOwlDataMap.json")));
		NetOwlDataMapModel dpm = new Gson().fromJson(read, NetOwlDataMapModel.class);

		ArrayList<Mappings> om = dpm.getOntologyMappings();
		for (Mappings mappings : om) {
			
			ontologyMappings.put(mappings.getKey(), mappings.getMapping().getAll());
		}
		ArrayList<Mappings> am = dpm.getArgumentMappings();
		for (Mappings mappings : am) {
			argumentMappings.put(mappings.getKey(), mappings.getMapping().getAll());
			System.out.println(mappings.getKey());
			Iterator<String> i = mappings.getMapping().getAll().keySet().iterator();
			while (i.hasNext()) {
				String key = i.next();
				String val = mappings.getMapping().getAll().get(key);
				System.out.println("..."+key+":"+val);
			}
		}
		
		response = entities(data.getDocument().getEntity(), argumentMappings, ontologyMappings, response);
		response = links(data.getDocument().getLink(), ontologyMappings, response);
		
		return response;
	}

	public ResponseModel adapt(String collection, Content data, String uuid, String entity) throws AETException {

		this.refUuid = uuid;
		this.refEntity = entity;
		return this.adapt(collection, data);
	}

	private ResponseModel entities(List<Entity> entities, Map<String, HashMap<String, String>> argumentMappings, Map<String, HashMap<String, String>> ontologyMappings, ResponseModel response) {
		String lat = "", lon = "";
		String entityId, ontology, uuid;
		HashMap<String, String> ontologyMapping;
		int sequence = 0;

		for (int i = 0; i < entities.size(); i++) {
			uuid = collection + "_" + sequence;
			if (entities.get(i).getOntology().equals(this.refEntity)) {
				entityId = this.refUuid;
			} else {
				entityId = collection + "_" + entities.get(i).getId();
			}
			ontology = entities.get(i).getOntology();

			if (ontologyMappings.containsKey(ontology)) {
				ontologyMapping = ontologyMappings.get(ontology);

				// Geo entity
				if (entities.get(i).getGeodetic() != null) {
					lat = entities.get(i).getGeodetic().getLatitude().toString();
					lon = entities.get(i).getGeodetic().getLongitude().toString();
					response.addAssertion(new AssertionModel(entityId, "AE#hasLatitude", lat, ASSERT_TYPE.OBJECT));
					response.addAssertion(new AssertionModel(entityId, "AE#hasLongitude", lon, ASSERT_TYPE.OBJECT));
					response.addAssertion(new AssertionModel(entityId, "a", ontologyMapping.get("onto"), lat, lon));
				} else {
					response.addAssertion(new AssertionModel(entityId, "a", ontologyMapping.get("onto"), ASSERT_TYPE.OBJECT));
				}

				// get label
				response.addAssertion(new AssertionModel(entityId, "label", entities.get(i).getValue(), ASSERT_TYPE.DATA));

				// get entity arguments
				response = entityArg(entities.get(i).getEntityArg(), entityId, argumentMappings, response);

				// Human entity
				if (ontologyMapping.get("onto").equals("sumo#Human") && entities.get(i).getValueType().equals("name")) {
					response.addAssertion(new AssertionModel(uuid, "a", "sumo#Name", ASSERT_TYPE.OBJECT));
					response.addAssertion(new AssertionModel(uuid, "label", entities.get(i).getValue(), ASSERT_TYPE.DATA));
					response.addAssertion(new AssertionModel(entityId, "AE#hasName", uuid, ASSERT_TYPE.OBJECT));
					// property entity
					response = properties(entities.get(i).getProperty(), uuid, argumentMappings, response, "$Name");
				}

				// property entity (unspecified subject)
				response = properties(entities.get(i).getProperty(), entityId, argumentMappings, response, null);
				
			}
			sequence += 1;
		}
		
		return response;
	}

	private ResponseModel entityArg(List<EntityArg> entityArg, String entityId, Map<String, HashMap<String, String>> argumentMappings, ResponseModel resp) {
		HashMap<String, String> argument;

		for (int i = 0; i < entityArg.size(); i++) {
			if (argumentMappings.containsKey(entityArg.get(i).getRole())) {
				argument = argumentMappings.get(entityArg.get(i).getRole());
				if (argument.get("use").equals("idref")) {
					resp.addAssertion(new AssertionModel(entityId, argument.get("pred"),
							collection + "_" + ((Entity) entityArg.get(i).getIdref()).getId(), ASSERT_TYPE.OBJECT));
				} else {
					resp.addAssertion(new AssertionModel(entityId, argument.get("pred"), entityArg.get(i).getValue(), ASSERT_TYPE.OBJECT));
				}
			}
		}
		
		return resp;
	}

	private ResponseModel properties(
			List<Property> properties, 
			String entityId, 
			Map<String, HashMap<String, String>> argumentMappings, 
			ResponseModel resp,
			String expectedSubject) {
		String name, value, pred, subj;
		HashMap<String, String> argument;

		for (int i = 0; i < properties.size(); i++) {
			name = properties.get(i).getName();
			value = properties.get(i).getValue();
			if (argumentMappings.containsKey(name)) {
				argument = argumentMappings.get(name);
				pred = argument.get("pred");
				subj = argument.get("subject");
				//System.out.println(argument.get("voodoo"));
				if ((subj==null) && (expectedSubject==null)) {
					resp.addAssertion(new AssertionModel(entityId, pred, value, ASSERT_TYPE.DATA));
				} else if ((expectedSubject!=null) && (expectedSubject.equals(subj))) {
					resp.addAssertion(new AssertionModel(entityId, pred, value, ASSERT_TYPE.DATA));
				}
			}
		}
		
		return resp;
	}

	private ResponseModel links(List<Link> links, Map<String, HashMap<String, String>> ontologyMappings, ResponseModel resp) {
		String linkId, linkOnto, ontology, event_id;
		String S, T;
		List<EntityArg> entityArgs;
		HashMap<String, String> ontologyMapping;

		for (int i = 0; i < links.size(); i++) {
			linkId = collection + "_" + links.get(i).getId();

			// get link type
			linkOnto = links.get(i).getOntology();
			if (ontologyMappings.containsKey(linkOnto)) {
				S = "";
				T = "";
				ontologyMapping = ontologyMappings.get(linkOnto);
				ontology = ontologyMapping.get("onto");

				// Skip link if the ontology has not been created
				if (ontology.equals("AE#")) {
					continue;
				}

				// get link entity arguments
				entityArgs = links.get(i).getEntityArg();
				for (int j = 0; j < entityArgs.size(); j++) {
					if (entityArgs.get(j).getRoleType().equals("source")) {
						S = collection + "_" + ((Entity) entityArgs.get(j).getIdref()).getId();
					} else if (entityArgs.get(j).getRoleType().equals("target")) {
						T = collection + "_" + ((Entity) entityArgs.get(j).getIdref()).getId();
					}
				}

				// get link events
				if (ontology.equals("event")) {
					event_id = "Event_" + linkId;
					resp.addAssertion(new AssertionModel(event_id, "a", ontologyMapping.get("eventType"), ASSERT_TYPE.OBJECT));
					resp.addAssertion(new AssertionModel(event_id, ontologyMapping.get("source"), S, ASSERT_TYPE.OBJECT));
					resp.addAssertion(new AssertionModel(event_id, ontologyMapping.get("target"), T, ASSERT_TYPE.OBJECT));
				} else {
					AssertionModel provenance = new AssertionModel(S, ontology, T, ASSERT_TYPE.OBJECT);
					provenance.setV(links.get(i).getId(), collection);
					resp.addAssertion(provenance);
				}
			}
		}
		
		return resp;
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

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getCanonicalName() {
		return this.canonicalName;
	}


}
