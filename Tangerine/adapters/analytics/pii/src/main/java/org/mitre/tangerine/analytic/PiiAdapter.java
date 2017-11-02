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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.mitre.tangerine.adapter.Analytic;
import org.mitre.tangerine.analytic.PIIDataMapModel.OntologyMappings;
import org.mitre.tangerine.analytic.PIIDataMapModel.ValueMappings;
import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.AssertionModel;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.models.AssertionModel.ASSERT_TYPE;
import org.slf4j.Logger;

import com.google.gson.Gson;

public class PiiAdapter extends Analytic {
    private final String name = "PiiAdapter", canonicalName = this.getClass().getName(),
                         description = "This adapter...";

    public PiiAdapter() {
        super();
    }


    private ResponseModel adapt(String collection, Map<String, String> pii, String uuid, String entity,
                                String foreignKey) {
        Logger log = this.getLogger();
        ResponseModel response = new ResponseModel();
        log.info("Reading data map");
        InputStream input = this.getClass().getClassLoader().getResourceAsStream("PIIDataMap.json");
        BufferedReader read = new BufferedReader(new InputStreamReader(input));
        PIIDataMapModel dataModel = new Gson().fromJson(read, PIIDataMapModel.class);
        List<OntologyMappings> ontolist = dataModel.getOntologyMappings();
        List<ValueMappings> vallist = dataModel.getValueMappings();

        response.setCollection(collection);

        // check for missing arguments and for the correct ssn
        if (uuid == null || entity == null || foreignKey == null || !foreignKey.equals(pii.get(entity))) {
            uuid = collection;
            log.info("Adding assertion: ( " + collection + ", a, #sumoHuman, " + ASSERT_TYPE.OBJECT + ")");
            response.addAssertion(new AssertionModel(collection, "a", "sumo#Human", ASSERT_TYPE.OBJECT));
        }

        for (int j = 0; j < ontolist.size(); j++) {
            // gets a mapping for a specific key
            String key = ontolist.get(j).getKey();
            List<Map<String, String>> asserts = ontolist.get(j).getMapping().getAsserts();

            for (int k = 0; k < asserts.size(); k++) {
                String subject = uuid;
                String predicate = asserts.get(k).get("P");
                String object = asserts.get(k).get("O");

                if (object.equals("<value>")) {
                    log.info("Adding assertion: ( " + subject + ", " + predicate + ", " + pii.get(key) + ", "
                             + ASSERT_TYPE.DATA + ")");
                    response.addAssertion(new AssertionModel(subject, predicate, pii.get(key), ASSERT_TYPE.DATA));
                } else if (object.startsWith("<map>")) {
                    String[] objParts = object.split(" ");
                    for (int i = 0; i < vallist.size(); i++) {
                        if (vallist.get(i).getKey().equals(objParts[1])) {
                            String value = vallist.get(i).getMapping().get(pii.get(key).toLowerCase());
                            log.info("Adding assertion: ( " + subject + ", " + predicate + ", " + value + ", "
                                     + ASSERT_TYPE.OBJECT + ")");
                            response.addAssertion(new AssertionModel(subject, predicate, value, ASSERT_TYPE.OBJECT));
                        }
                    }
                }
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
    public void updateDB(ResponseModel data, AETDatabase db) throws AETException {
        List<AssertionModel> elements = data.getAssertions();
        String collection = data.getCollection();
        db.open();
        db.access(collection);
        for (AssertionModel assertion : elements) {
            db.update(Document.parse(new Gson().toJson(assertion)));
        }
        db.index();
        db.close();
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getDatamap() throws IOException {
        InputStreamReader read = new InputStreamReader(
            this.getClass().getClassLoader().getResourceAsStream("PIIDataMap.json"));
        String ret = "";
        int val = 0;
        while ((val = read.read()) != -1) {
            ret += (char) val;
        }
        read.close();

        return ret;
    }

    public Map<String, String> parse(InputStream input) throws IOException, AETException {
        Logger log = this.getLogger();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        Map<String, String> temp = new HashMap<String, String>();
        String[] keys, data;
        String delimiter = ",";

        log.info("Parsing CSV");

        keys = reader.readLine().split(delimiter);
        data = reader.readLine().split(delimiter + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        if (data.length != keys.length) {
            reader.close();
            log.info("Malformed CSV");
            throw new AETException("Could not parse CSV");
        }

        for (int i = 0; i < data.length; i++) {
            temp.put(keys[i], data[i]);
        }
        reader.close();
        log.info("Parsed CSV");
        return temp;
    }

    @Override
    public ResponseModel adapt(String collection, InputStream data, Map<String, String> params)
    throws AETException, IOException {
        String uuid = params.containsKey("uuid") ? params.get("uuid") : "";
        String entity = params.containsKey("entity") ? params.get("entity") : "";
        String key = params.containsKey("key") ? params.get("key") : "";

        return this.adapt(collection,  this.parse(data),  uuid, entity, key);
    }
}
