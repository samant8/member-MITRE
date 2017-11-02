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
import org.mitre.tangerine.analytic.EsriDataMapModel.OntologyMappings;
import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.AssertionModel;
import org.mitre.tangerine.models.ResponseModel;
import org.slf4j.Logger;
import org.mitre.tangerine.models.AssertionModel.ASSERT_TYPE;

import com.google.gson.Gson;

public class EsriAdapter extends Analytic {

    private final String name = "ES", canonicalName = this.getClass().getName(),
                         description = "This adapter ...";

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

    private ResponseModel adapt(String collection, Map<String, String> data, String uuid, String entity)
    throws IOException {
        Logger log = this.getLogger();
        log.info("Loading Datamap");
        InputStreamReader read = new InputStreamReader(
            this.getClass().getClassLoader().getResourceAsStream("EsriDataMap.json"));
        log.info("Datamap Loaded");
        ResponseModel ret = new ResponseModel();
        Map<String, Tuple> instances = new HashMap<String, Tuple>();
        int seq = 0;

        List<OntologyMappings> ontolist = new Gson().fromJson(read, EsriDataMapModel.class).getOntologyMappings();
        Map<String, String> instance = null;
        ret.setCollection(collection);

        for (int count = 0; count < ontolist.size(); count++) {
            instance = ontolist.get(count).getMapping().getInstances();
            // iterates over instances and stores them
            if (instance != null)
                for (String s : instance.keySet()) {
                    if (entity.equals(instance.get(s))) {
                        instances.put(s, new Tuple(uuid, instance.get(s)));
                    } else {
                        instances.put(s, new Tuple(collection + "_" + seq, instance.get(s)));
                        seq += 1;
                        log.info("Adding assertion: (" + instances.get(s).getUuid() + ", a, " + instance.get(s) + ","
                                 + ASSERT_TYPE.OBJECT + ")");
                        ret.addAssertion((new AssertionModel(instances.get(s).getUuid(), "a", instance.get(s),
                                                             ASSERT_TYPE.OBJECT)));
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
                    log.info("Adding assertion: (" + subject.getUuid() + ", " + predicate + ", " + data.get(key) + ", "
                             + ASSERT_TYPE.DATA + ")");
                    ret.addAssertion(
                        (new AssertionModel(subject.getUuid(), predicate, data.get(key), ASSERT_TYPE.DATA)));
                } else if (obj.contains("$")) {
                    log.info("Adding assertion: (" + subject.getUuid() + ", " + predicate + ", " + instances.get(obj)
                             + ", " + ASSERT_TYPE.OBJECT + ")");
                    ret.addAssertion((new AssertionModel(subject.getUuid(), predicate, instances.get(obj).getUuid(),
                                                         ASSERT_TYPE.OBJECT)));
                } else {
                    log.info("Adding assertion: (" + subject.getUuid() + ", " + predicate + ", " + data.get(key) + ", "
                             + ASSERT_TYPE.DATA + ")");
                    ret.addAssertion((new AssertionModel(subject.getUuid(), predicate, obj, ASSERT_TYPE.DATA)));
                }
            }
        }
        return ret;
    }

    @Override
    public void updateDB(ResponseModel data, AETDatabase db) throws AETException {
        Logger log = this.getLogger();
        List<AssertionModel> elements = data.getAssertions();
        String collection = data.getCollection();
        log.info("Opening connection to database");
        db.open();
        log.info("Accessing collection");
        db.access(collection);
        log.info("Adding assertions to database");
        for (AssertionModel assertion : elements) {
            db.update(Document.parse(new Gson().toJson(assertion)));
        }
        log.info("index database");
        db.index();
        log.info("Close connection to database");
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
            this.getClass().getClassLoader().getResourceAsStream("EsriDataMap.json"));
        String ret = "";
        int val = 0;
        while ((val = read.read()) != -1) {
            ret += (char) val;
        }
        read.close();

        return ret;
    }

    public Map<String, String> parse(InputStream is) throws AETException, IOException {
        Logger log = this.getLogger();
        log.info("Reading CSV file");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        Map<String, String> temp = new HashMap<String, String>();
        String[] keys, data;
        char delimiter = ',';

        keys = reader.readLine().split(",");
        data = reader.readLine().split(delimiter + "(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

        if (data.length != keys.length) {
            reader.close();
            log.info("Malformed CSV");
            throw new AETException("Could not parse file.");
        }

        for (int i = 0; i < data.length; i++) {
            temp.put(keys[i], data[i]);
        }
        log.info("Parsed CSV");
        return temp;
    }


    @Override
    public ResponseModel adapt(String collection, InputStream data, Map<String, String> params) throws IOException, AETException {
        String uuid = params.containsKey("uuid") ? params.get("uuid") : "";
        String entity  = params.containsKey("uuid") ? params.get("entity") : "";
        return this.adapt(collection, this.parse(data), uuid, entity);
    }
}
