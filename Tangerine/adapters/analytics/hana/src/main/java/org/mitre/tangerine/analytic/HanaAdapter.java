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
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.mitre.tangerine.adapter.Analytic;
import org.mitre.tangerine.analytic.HanaDataMapModel.OntologyMappings;
import org.mitre.tangerine.analytic.HanaInputModel.Results.Result;
import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.AssertionModel;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.models.AssertionModel.ASSERT_TYPE;
import org.slf4j.Logger;

import com.google.gson.Gson;

public class HanaAdapter extends Analytic {
    private String uuid, entity;
    private final String name = "HA", canonicalName = "org.mitre.tangerine.analytic.HanaAdapter", description = "This adapter ...";

    public HanaAdapter() {
        super();
        uuid = "";
        entity = "";
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

    private ResponseModel adapt(String id, HanaInputModel data) {
        List<Map<String, String>> asserts = null;
        List<OntologyMappings> ontolist = null;
        Map<String, String> instanceId = null;
        Map<String, String> variables = null;
        Map<String, Tuple> instances = null;
        Map<String, String> instance = null;
        InputStreamReader read = null;
        HanaDataMapModel hdmm = null;
        String methodResult = null;
        String instanceVal = null;
        String[] objParts = null;
        String[] varParts = null;
        ResponseModel re = null;
        String predicate = null;
        Tuple subject = null;
        Result result = null;
        Method method = null;
        String obj = null;
        String key = null;
        int seq = 0;

        Logger log = this.getLogger();
        log.info("Loading Datamap");
        read = new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("HanaDataMap.json"));
        log.info("Datamap Loaded");
        hdmm = new Gson().fromJson(read, HanaDataMapModel.class);
        ontolist = hdmm.getOntologyMappings();
        re = new ResponseModel();
        re.setCollection(id);
        instances = new HashMap<String, Tuple>();

        log.info("Running Results");
        for (int i = 0; i < data.getD().getResults().size(); i++) {
            result = data.getD().getResults().get(i);

            for (int j = 0; j < ontolist.size(); j++) {
                asserts = ontolist.get(j).getMapping().getAssertions();
                key = ontolist.get(j).getKey();
                try {
                    method = result.getClass().getMethod("get" + key);
                    methodResult = (String) method.invoke(result);
                } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
                             | InvocationTargetException e) {
                    e.printStackTrace();
                }

                if (ontolist.get(j).getMapping().getInstanceID() != null) {
                    instanceId = ontolist.get(j).getMapping().getInstanceID();
                    for (Entry<String, String> entry : instanceId.entrySet()) {
                        instanceId.put(entry.getKey(), methodResult);
                    }
                    continue;
                }

                if (ontolist.get(j).getMapping().getVariables() != null) {
                    variables = ontolist.get(j).getMapping().getVariables();
                    for (Entry<String, String> entry : variables.entrySet()) {
                        if (entry.getValue().startsWith("<map>")) {
                            varParts = entry.getValue().split(" ");
                            try {
                                Method hdmmMethod = hdmm.getClass().getMethod("get" + varParts[1]);
                                Map<String, String> hdmmMethodResult = (Map<String, String>) hdmmMethod.invoke(hdmm);
                                variables.put(entry.getKey(), hdmmMethodResult.get(methodResult));
                            } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                                         | IllegalArgumentException | InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        } else {
                            variables.put(entry.getKey(), methodResult);
                        }
                    }
                    continue;
                }

                log.info("Running Instances");
                instance = ontolist.get(j).getMapping().getInstances();
                if (instance != null) {
                    for (String s : instance.keySet()) {
                        instanceVal = null;

                        if (instanceId != null) {
                            if (instanceId.containsKey(instance.get(s))) {
                                instanceVal = instanceId.get(instance.get(s));
                            }
                        }
                        if (variables != null) {
                            if (variables.containsKey(instance.get(s))) {
                                instanceVal = variables.get(instance.get(s));
                            }
                        }
                        if (instanceVal == null) {
                            instanceVal = instance.get(s);
                        }

                        if (this.entity.equals(instance.get(s))) {
                            instances.put(s, new Tuple(this.uuid, instanceVal));
                        } else {
                            instances.put(s, new Tuple(id + "_" + seq, instanceVal));
                            log.info("Instance - Adding Assertion: " + s + "(" + instances.get(s).getUuid() + ", a, "
                                     + instanceVal + "," + ASSERT_TYPE.OBJECT + ")");
                            re.addAssertion((new AssertionModel(instances.get(s).getUuid(), "a", instanceVal,
                                                                ASSERT_TYPE.OBJECT)));
                            seq += 1;
                        }
                    }
                }
                instance = null;
                log.info("Finished Running Instances");

                log.info("Running Assertions");
                for (int k = 0; k < asserts.size(); k++) {
                    subject = instances.get(asserts.get(k).get("S"));
                    predicate = asserts.get(k).get("P");
                    obj = asserts.get(k).get("O");

                    String subjectId = null;
                    if (subject == null) {
                        if (instanceId != null) {
                            if (instanceId.containsKey(asserts.get(k).get("S"))) {
                                subjectId = instanceId.get(asserts.get(k).get("S"));
                            }
                        }
                        if (variables != null) {
                            if (variables.containsKey(asserts.get(k).get("S"))) {
                                subjectId = variables.get(asserts.get(k).get("S"));
                            }
                        }
                    } else {
                        subjectId = subject.getUuid();
                    }

                    if (obj.equals("<value>")) {
                        log.info("<value> - Adding Assertion: (" + subjectId + ", " + predicate + ", " + methodResult
                                 + ", " + ASSERT_TYPE.DATA + ")");
                        re.addAssertion((new AssertionModel(subjectId, predicate, methodResult, ASSERT_TYPE.DATA)));
                    } else if (obj.contains("$")) {
                        log.info("$ - Adding Assertion: (" + subjectId + ", " + predicate + ", "
                                 + instances.get(obj).getUuid() + ", " + ASSERT_TYPE.OBJECT + ")");
                        re.addAssertion((new AssertionModel(subjectId, predicate, instances.get(obj).getUuid(),
                                                            ASSERT_TYPE.OBJECT)));
                    } else if (obj.startsWith("<map>")) {
                        objParts = obj.split(" ");
                        try {
                            log.info("Calling - get" + objParts[1]);
                            Method hdmmMethod = hdmm.getClass().getMethod("get" + objParts[1]);
                            Map<String, String> hdmmMethodResult = (Map<String, String>) hdmmMethod.invoke(hdmm);
                            log.info("<map> - Adding Assertion: (" + subjectId + ", " + predicate + ", "
                                     + hdmmMethodResult.get(methodResult) + ", " + ASSERT_TYPE.OBJECT + ")");
                            re.addAssertion(new AssertionModel(subjectId, predicate, hdmmMethodResult.get(methodResult),
                                                               ASSERT_TYPE.OBJECT));
                        } catch (NoSuchMethodException | SecurityException | IllegalAccessException
                                     | IllegalArgumentException | InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    } else {
                        log.info("Label - Adding assertion: (" + subjectId + ", " + predicate + ", " + obj + ", "
                                 + ASSERT_TYPE.DATA + ")");
                        re.addAssertion((new AssertionModel(subjectId, predicate, obj, ASSERT_TYPE.DATA)));
                    }
                }
            }
        }
        log.info("Finished Running Assertions");
        return re;
    }

    private HanaInputModel HanaParser(InputStream data) throws UnsupportedEncodingException {
        HanaInputModel him = null;
        Reader reader = null;
        reader = new InputStreamReader(data, "UTF-8");
        him = new Gson().fromJson(reader, HanaInputModel.class);
        return him;
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
            this.getClass().getClassLoader().getResourceAsStream("HanaDataMap.json"));
        String ret = "";
        int val = 0;
        while ((val = read.read()) != -1) {
            ret += (char) val;
        }
        read.close();
        return ret;
    }

    @Override
    public ResponseModel adapt(String collection, InputStream data, Map<String, String> params)
    throws AETException, IOException {
        return this.adapt(collection, this.HanaParser(data));
    }
}
