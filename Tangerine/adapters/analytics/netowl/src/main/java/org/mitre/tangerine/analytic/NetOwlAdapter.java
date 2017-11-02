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



package org.mitre.tangerine.analytic;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.bson.Document;
import org.mitre.tangerine.adapter.Analytic;
import org.mitre.tangerine.analytic.NetOwlDataMapModel.Mappings;
import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.AssertionModel;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.models.AssertionModel.ASSERT_TYPE;
import org.mitre.tangerine.netowl.parser.Content;
import org.mitre.tangerine.netowl.parser.Property;
import org.slf4j.Logger;
import org.mitre.tangerine.netowl.parser.Content.Document.Entity;
import org.mitre.tangerine.netowl.parser.Content.Document.Link;
import org.mitre.tangerine.netowl.parser.Content.Document.Sentiment.EntityArg;

import com.google.gson.Gson;

public class NetOwlAdapter extends Analytic {

    private final String name = "NO";
    private final String canonicalName = this.getClass().getName();
    private final String description = "This adapter...";
    private final Logger log = this.getLogger();

    private ResponseModel adapt(String collection, Content data, String refUuid, String refEntity) {
        ResponseModel response = new ResponseModel();
        Map<String, HashMap<String, String>> ontologyMappings = new HashMap<String, HashMap<String, String>>();
        Map<String, HashMap<String, String>> argumentMappings = new HashMap<String, HashMap<String, String>>();
        // TODO review response model set collection
        response.setCollection(collection);

        log.info("Reading data map");
        InputStreamReader read = new InputStreamReader(
            this.getClass().getClassLoader().getResourceAsStream("NetOwlDataMap.json"));
        NetOwlDataMapModel dpm = new Gson().fromJson(read, NetOwlDataMapModel.class);

        ArrayList<Mappings> om = dpm.getOntologyMappings();
        for (Mappings mappings : om) {

            ontologyMappings.put(mappings.getKey(), mappings.getMapping().getAll());
        }
        ArrayList<Mappings> am = dpm.getArgumentMappings();
        for (Mappings mappings : am) {
            argumentMappings.put(mappings.getKey(), mappings.getMapping().getAll());
        }

        response = entities(data.getDocument().getEntity(), argumentMappings, ontologyMappings, response, refUuid,
                            refEntity, collection);
        response = links(data.getDocument().getLink(), ontologyMappings, response, collection);

        return response;
    }

    private ResponseModel entities(List<Entity> entities, Map<String, HashMap<String, String>> argumentMappings,
                                   Map<String, HashMap<String, String>> ontologyMappings, ResponseModel response, String refUuid,
                                   String refEntity, String collection) {
        String lat = "", lon = "";
        String entityId, ontology, uuid;
        HashMap<String, String> ontologyMapping;
        int sequence = 0;

        for (int i = 0; i < entities.size(); i++) {
            uuid = collection + "_" + sequence;
            if (entities.get(i).getOntology().equals(refEntity)) {
                entityId = refUuid;
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

                    log.info("Added assertion: (" + entityId + ", AE#hasLatitude, " + lat + ", " + ASSERT_TYPE.OBJECT
                             + ")");
                    log.info("Added assertion: (" + entityId + ", AE#hasLongitude, " + lon + ", " + ASSERT_TYPE.OBJECT
                             + ")");
                    log.info("Added assertion: (" + entityId + ", a, " + ontologyMapping.get("onto") + ", " + lat + ", "
                             + lon + ")");
                    response.addAssertion(new AssertionModel(entityId, "AE#hasLatitude", lat, ASSERT_TYPE.OBJECT));
                    response.addAssertion(new AssertionModel(entityId, "AE#hasLongitude", lon, ASSERT_TYPE.OBJECT));
                    response.addAssertion(new AssertionModel(entityId, "a", ontologyMapping.get("onto"), lat, lon));
                } else {
                    log.info("Added assertion: (" + entityId + ", a, " + ontologyMapping.get("onto") + ", "
                             + ASSERT_TYPE.OBJECT + ")");
                    response.addAssertion(
                        new AssertionModel(entityId, "a", ontologyMapping.get("onto"), ASSERT_TYPE.OBJECT));
                }

                // get label
                log.info("Added assertion: (" + entityId + ", label, " + entities.get(i).getValue() + ", "
                         + ASSERT_TYPE.DATA + ")");
                response.addAssertion(
                    new AssertionModel(entityId, "label", entities.get(i).getValue(), ASSERT_TYPE.DATA));

                // get entity arguments
                response = entityArg(entities.get(i).getEntityArg(), entityId, argumentMappings, response, collection);

                // Human entity
                if (ontologyMapping.get("onto").equals("sumo#Human") && entities.get(i).getValueType().equals("name")) {
                    log.info("Added assertion: (" + uuid + ", a, sumo#Name, " + ASSERT_TYPE.OBJECT + ")");
                    response.addAssertion(new AssertionModel(uuid, "a", "sumo#Name", ASSERT_TYPE.OBJECT));
                    log.info("Added assertion: (" + uuid + ", label, " + entities.get(i).getValue() + ", "
                             + ASSERT_TYPE.OBJECT + ")");
                    response.addAssertion(
                        new AssertionModel(uuid, "label", entities.get(i).getValue(), ASSERT_TYPE.DATA));
                    log.info("Added assertion: (" + entityId + ", AE#hasName, " + uuid + ", " + ASSERT_TYPE.OBJECT
                             + ")");
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

    private ResponseModel entityArg(List<EntityArg> entityArg, String entityId,
                                    Map<String, HashMap<String, String>> argumentMappings, ResponseModel resp, String collection) {
        HashMap<String, String> argument;

        for (int i = 0; i < entityArg.size(); i++) {
            if (argumentMappings.containsKey(entityArg.get(i).getRole())) {
                argument = argumentMappings.get(entityArg.get(i).getRole());
                if (argument.get("use").equals("idref")) {
                    log.info("Added assertion: (" + entityId + "," + argument.get("pred") + " , " + collection + "_"
                             + ((Entity) entityArg.get(i).getIdref()).getId() + ", " + ASSERT_TYPE.OBJECT + ")");
                    resp.addAssertion(new AssertionModel(entityId, argument.get("pred"),
                                                         collection + "_" + ((Entity) entityArg.get(i).getIdref()).getId(), ASSERT_TYPE.OBJECT));
                } else {
                    log.info("Added assertion: (" + entityId + "," + argument.get("pred") + " , "
                             + entityArg.get(i).getValue() + ", " + ASSERT_TYPE.OBJECT + ")");
                    resp.addAssertion(new AssertionModel(entityId, argument.get("pred"), entityArg.get(i).getValue(),
                                                         ASSERT_TYPE.OBJECT));
                }
            }
        }

        return resp;
    }

    private ResponseModel properties(List<Property> properties, String entityId,
                                     Map<String, HashMap<String, String>> argumentMappings, ResponseModel resp, String expectedSubject) {
        String name, value, pred, subj;
        HashMap<String, String> argument;

        for (int i = 0; i < properties.size(); i++) {
            name = properties.get(i).getName();
            value = properties.get(i).getValue();
            if (argumentMappings.containsKey(name)) {
                argument = argumentMappings.get(name);
                pred = argument.get("pred");
                subj = argument.get("subject");
                if ((subj == null) && (expectedSubject == null)) {
                    log.info("Added assertion: (" + entityId + "," + pred + ", " + value + ", " + ASSERT_TYPE.DATA
                             + ")");
                    resp.addAssertion(new AssertionModel(entityId, pred, value, ASSERT_TYPE.DATA));
                } else if ((expectedSubject != null) && (expectedSubject.equals(subj))) {
                    log.info("Added assertion: (" + entityId + "," + pred + ", " + value + ", " + ASSERT_TYPE.DATA
                             + ")");
                    resp.addAssertion(new AssertionModel(entityId, pred, value, ASSERT_TYPE.DATA));
                }
            }
        }

        return resp;
    }

    private ResponseModel links(List<Link> links, Map<String, HashMap<String, String>> ontologyMappings,
                                ResponseModel resp, String collection) {
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
                    log.info("Added assertion: (" + event_id + ", a, " + ontologyMapping.get("eventType") + ", "
                             + ASSERT_TYPE.OBJECT + ")");
                    resp.addAssertion(
                        new AssertionModel(event_id, "a", ontologyMapping.get("eventType"), ASSERT_TYPE.OBJECT));
                    log.info("Added assertion: (" + event_id + ", " + ontologyMapping.get("source") + ", " + S + ", "
                             + ASSERT_TYPE.OBJECT + ")");
                    resp.addAssertion(
                        new AssertionModel(event_id, ontologyMapping.get("source"), S, ASSERT_TYPE.OBJECT));
                    log.info("Added assertion: (" + event_id + ", " + ontologyMapping.get("target") + ", " + T + ", "
                             + ASSERT_TYPE.OBJECT + ")");
                    resp.addAssertion(
                        new AssertionModel(event_id, ontologyMapping.get("target"), T, ASSERT_TYPE.OBJECT));
                } else {
                    AssertionModel provenance = new AssertionModel(S, ontology, T, ASSERT_TYPE.OBJECT);
                    provenance.setV(links.get(i).getId(), collection);
                    resp.addAssertion(provenance);
                    log.info("Added provenance: ( " + provenance + ")");
                }
            }
        }

        return resp;
    }

    @Override
    public void updateDB(ResponseModel data, AETDatabase db) throws AETException {
        List<AssertionModel> elements = data.getAssertions();
        String collection = data.getCollection();
        log.info("Opening connection to database");
        db.open();
        log.info("Accessing " + collection);
        db.access(collection);
        log.info("Adding assertions");
        for (AssertionModel assertion : elements) {
            db.update(Document.parse(new Gson().toJson(assertion)));
        }
        log.info("Indexing database");
        db.index();
        log.info("Closing connection to database");
        db.close();
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
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getDatamap() throws IOException {
        InputStreamReader read = new InputStreamReader(
            this.getClass().getClassLoader().getResourceAsStream("NetOwlDataMap.json"));
        String ret = "";
        int val = 0;
        while ((val = read.read()) != -1) {
            ret += (char) val;
        }
        read.close();

        return ret;
    }

    public Content parse(InputStream is) throws AETException {
        Logger log = this.getLogger();
        Content elem = null;
        JAXBContext jc = null;
        Unmarshaller u = null;
        log.info("Parsing XML");
        try {
            jc = JAXBContext.newInstance("org.mitre.tangerine.netowl.parser");
            u = jc.createUnmarshaller();
            elem = (Content) u.unmarshal(is);
        } catch (JAXBException e) {
            log.error("Error Parsing XML", e);
            throw new AETException("Could not parse xml : " + e.getMessage());
        }

        return elem;
    }

    @Override
    public ResponseModel adapt(String collection, InputStream data, Map<String, String> params)
    throws AETException, IOException {
        String uuid = params.containsKey("uuid") ? params.get("uuid") : "";
        String entity = params.containsKey("entity") ? params.get("entity") : "";

        return this.adapt(collection, this.parse(data), uuid, entity);
    }

}
