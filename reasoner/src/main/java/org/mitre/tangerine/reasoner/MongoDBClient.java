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

package org.mitre.tangerine.reasoner;
import java.util.HashSet;
import java.util.ArrayList;

import com.mongodb.MongoClient;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;
import com.mongodb.ServerAddress;

public class MongoDBClient {
    public ArrayList<ArrayList<String>> results;
    private ArrayList<String> singleObj;
    private ArrayList<DBObject> JSONAssrts;
    public long Tim;

    private HashSet<String> UUID_History;       // History of UUID pulled
    private HashSet<String> UUID_NFHistory;     // History of UUID pulled for labels
    private HashSet<String> UUID_AssocHistory;  // History of Associated UUID pulled
    private HashSet<String> NameForm_History;   // History of NameForm pulled
    private HashSet<String> Graph_History;      // Graphs in memory
    private HashSet<String> TextIndexPredicates; // List of predicates for which assertions are indexed via text index
    private HashSet<String> InstanceOfPredicates; // List of instance of predicates
    private HashSet<String> EnrichmentPredicates; // List of predicates for which assertions may appear in the enrichment portion of an assertion

    private String[] clientAssertion   = new String[10];  // individual components of JSON documents

    // MongoDB Interface
    public MongoClient mongoCl;
    public DB          mongoDb;
    private String kbsrc;
    public int invoked, submitToMongo;

    public MongoDBClient(String dbname, String kbsrc, String mongodb_ip, ArrayList<String> textPreds, ArrayList<String> enrichPreds) {
        ArrayList<ServerAddress> mongo_set = new ArrayList<ServerAddress>();
        for(String addr : mongodb_ip.split(","))
            mongo_set.add(new ServerAddress(addr, 27017));
        this.kbsrc = kbsrc;
        mongoDb = null;
        try {
            mongoCl = new MongoClient( mongo_set );
            mongoDb = mongoCl.getDB(dbname);
        } catch(Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        results   = new ArrayList<ArrayList<String>>();
        singleObj = new ArrayList<String>();

        UUID_History         = new HashSet<String>();
        UUID_NFHistory       = new HashSet<String>();
        UUID_AssocHistory    = new HashSet<String>();
        Graph_History        = new HashSet<String>();
        TextIndexPredicates  = new HashSet<String>();
        EnrichmentPredicates = new HashSet<String>();
        InstanceOfPredicates = new HashSet<String>();
        for(String pred : textPreds) TextIndexPredicates.add(pred);
        for(String pred : enrichPreds) EnrichmentPredicates.add(pred);
        for(String pred : FLogicConstants.InstanceOfPreds.split(" ")) InstanceOfPredicates.add(pred);

        JSONAssrts = new ArrayList<DBObject>();

        // capture usage statistic
        invoked       = 0;
        submitToMongo = 0;
    }

    public void release() {
        clearKB();
        results=null;
        singleObj=null;
        clientAssertion=null;
        mongoCl.close();
        mongoCl=null;
        UUID_History=null;
        UUID_NFHistory=null;
        UUID_AssocHistory=null;
        Graph_History=null;
        TextIndexPredicates=null;
        InstanceOfPredicates=null;
    }

    public void clearKB() {
        singleObj.clear();
        results.clear();
        UUID_History.clear();
        UUID_NFHistory.clear();
        UUID_AssocHistory.clear();
        Graph_History.clear();
        TextIndexPredicates.clear();
        InstanceOfPredicates.clear();
    }

    public boolean getAssertionFromJSONObject(DBObject assrt) {
        try {
            JSONAssrts.clear();

            JSONAssrts.add((DBObject) assrt.get("A"));
            // Enrichment content
            if(assrt.containsKey("E")) {
                BasicDBList EA = (BasicDBList) assrt.get("E");
                for(int e=0; e<EA.size(); e++) {
                    DBObject EAssrt = (DBObject) EA.get(e);
                    String sub  = (String) EAssrt.get("S");
                    String pred = (String) EAssrt.get("P");
                    if(sub.contains("NAMEDESIGNATOR")) UUID_History.add(sub);
                    UUID_History.add(sub+pred);
                    JSONAssrts.add(EAssrt);
                }
            }

            BasicDBList Prov = (BasicDBList) assrt.get("provenance");
            DBObject EDH = (DBObject) assrt.get("edhHeader");

            for(DBObject A : JSONAssrts) {
                clientAssertion[0] = (String) A.get("S");
                String p = (String) A.get("P");
                if(p.equals("a") || p.equals("RDF#Type") || p.equals("RDF#type")) p = ":";
                else if(p.equals("RDF#sameAs") || p.equals("OWL#sameAs")) p = ":=:";
                clientAssertion[1] = p;
                if(A.containsKey("O"))      clientAssertion[2] = (String) A.get("O");  // non text search value
                else if(A.containsKey("D")) clientAssertion[2] = (String) A.get("D");  // text search value
                else clientAssertion[2] = "--"; //error in json document

                for(int a=0; a<3; a++) if(clientAssertion[a] == null) return false; // check for nulls in assertions

                ArrayList<String> res = new ArrayList<String>();
                for(int a=0; a<3; a++) res.add(clientAssertion[a]);

                BasicDBList C = (BasicDBList) assrt.get("C");
                if(C != null)
                    for(int c=0; c<C.size(); c++) res.add((String) C.get(c));

                DBObject T  = (DBObject) assrt.get("T");
                DBObject ER = (DBObject) assrt.get("ER");
                String T_str = null;
                if(T != null)
                    if(T.toString().length()>4) T_str = T.toString();
                if(ER != null)
                    if(ER.toString().length()>4) {
                        if(T_str == null) T_str = ER.toString();
                        else T_str = T_str + " , " + ER.toString();
                    }
                if(T_str != null) res.add("{\"T\":" + T_str + "}");

                DBObject V = (DBObject) assrt.get("V");
                if(V != null)
                    if(V.toString().length()>4) res.add("{\"V\":" + V.toString() + "}");

                if(Prov != null)
                    res.add("{\"P\":" + Prov.toString() + "}");

                if(EDH != null)
                    res.add("{\"E\":" + EDH.toString() + "}");


                results.add(res);
                A=null;
            }
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void getData(String sub, String pred, String obj) {
        singleObj.clear();
        singleObj.add(obj);
        getData(sub, pred, singleObj);
    }

    public void getData(String sub, String pred, ArrayList<String> obj_lst) {
        String obj;
        DBCollection mongoColl;
        boolean getFullGraph = false, textSearch = false;
        BasicDBObject qry;
        HashSet<String> graphsToLoad = null;

        invoked++;

        if(pred.equals("FreeTextSearch"))  {
            getData_fts(sub, pred, obj_lst);
            return;
        }
        if(pred.equals("RegexTextSearch")) {
            getData_regex(sub, pred, obj_lst);
            return;
        }

        if(!InstanceOfPredicates.contains(pred)) obj = obj_lst.get(0);
        else obj = "";

        mongoColl = mongoDb.getCollection(kbsrc);
        if(TextIndexPredicates.contains(pred)) textSearch = true;

        qry = new BasicDBObject();
        String feat;
        if(!sub.startsWith("?")) {
            if(UUID_History.contains(sub)) return;
            feat = "A.S";
            if(EnrichmentPredicates.contains(pred) || sub.contains("~NAMEDESIGNATOR")) feat = "E.S";
            qry.put(feat, sub);
            if(pred.startsWith("?")) { // no pred
                UUID_History.add(sub);
            } else { // pred specified
                if(UUID_History.contains(sub+pred)) return;
                if(InstanceOfPredicates.contains(pred)) qry.append("A.P", "a"); //translate
                else {
                    feat = "A.P";
                    if(EnrichmentPredicates.contains(pred) || sub.contains("~NAMEDESIGNATOR")) feat = "E.P";
                    qry.append(feat, pred);
                }
                UUID_History.add(sub+pred);
            }
        } else if(!obj.startsWith("?")) {
            if(InstanceOfPredicates.contains(pred)) {
                getData_inst(obj_lst);
                return;
            } else {
                if(pred.startsWith("?")) {
                    if(UUID_AssocHistory.contains(obj)) return;
                    getData_ems(sub, pred, obj);
                    qry.put("A.O", obj);
                    UUID_AssocHistory.add(obj);
                } else {
                    if(UUID_AssocHistory.contains(obj+pred)) return;
                    UUID_AssocHistory.add(obj+pred);
                    if(!textSearch) {
                        qry.put("A.O", obj);
                        qry.append("A.P", pred);
                    } else {
                        getData_ems(sub, pred, obj);
                        return;
                    }
                }
            }
        } else return; // no bound variables

        submitToMongo++;

        DBCursor mongoCr = mongoColl.find(qry);
        while(mongoCr.hasNext()) {
            DBObject assrt = mongoCr.next();
            if(getFullGraph) {
                String graph_id = (String) assrt.get("G");
                if(!Graph_History.contains(graph_id)) {
                    graphsToLoad.add(graph_id);
                    Graph_History.add(graph_id);
                }
            } else
                getAssertionFromJSONObject(assrt);
        }
        mongoCr.close();

        if(getFullGraph && graphsToLoad.size()>0) {
            // load assertions in graphs
            BasicDBList gr_lst = new BasicDBList();
            gr_lst.addAll(graphsToLoad);
            BasicDBObject gr_qry = new BasicDBObject("$in", gr_lst);
            qry = new BasicDBObject("G", gr_qry);

            // retrieve assertions
            mongoCr = mongoDb.getCollection(kbsrc).find(qry);
            while(mongoCr.hasNext()) {
                DBObject assrt = mongoCr.next();
                getAssertionFromJSONObject(assrt);
            }
            mongoCr.close();

            gr_lst.clear();
            gr_lst=null;
            gr_qry.clear();
            gr_qry=null;
        }
    }

    // Exact Match Search
    public void getData_ems(String sub, String pred, String obj) {
        ArrayList<BasicDBObject> orlst = new ArrayList<BasicDBObject>();
        orlst.add(new BasicDBObject("A.D", obj));
        orlst.add(new BasicDBObject("E.D", obj));
        BasicDBObject txt_qry;
        txt_qry = new BasicDBObject("$text", new BasicDBObject("$search","\"" + obj + "\"")).append("$or", orlst);
        if(!pred.startsWith("?"))
            txt_qry.append("A.P", pred);

        DBCollection mongoColl = mongoDb.getCollection(kbsrc);
        DBCursor mongoCr = mongoColl.find(txt_qry);
        while(mongoCr.hasNext()) {
            DBObject assrt = mongoCr.next();
            boolean extractAssertion = false;

            // Is the match in A.D
            DBObject A = (DBObject) assrt.get("A");
            String txt = (String) A.get("D");
            if(txt != null && txt.equals(obj)) extractAssertion = true;

            // Is the match in E.D
            if(!extractAssertion) {
                BasicDBList E = (BasicDBList) assrt.get("E");
                if(E != null) {
                    for(int e=0; e<E.size() && !extractAssertion; e++) {
                        A = (DBObject) E.get(e);
                        txt = (String) A.get("D");
                        if(txt != null && txt.equals(obj)) extractAssertion = true;
                    }
                }
            }

            if(extractAssertion) getAssertionFromJSONObject(assrt);
        }
        mongoCr.close();
    }

    // Free Text Search
    public void getData_fts(String sub, String pred, ArrayList<String> obj_lst) {
        String names = null;
        for(String name : obj_lst)
            if(names == null) names = name;
            else names = names + " " + name;
        names = "\"" + names + "\"";
        BasicDBObject txt_qry;
        if(!sub.startsWith("?"))
            txt_qry = new BasicDBObject("A.S", sub).append("$text", new BasicDBObject("$search", names));
        else txt_qry = new BasicDBObject("$text", new BasicDBObject("$search", names));

        DBCollection mongoColl = mongoDb.getCollection(kbsrc);

        submitToMongo++;

        DBCursor mongoCr = mongoColl.find(txt_qry);
        while(mongoCr.hasNext()) {
            DBObject assrt = mongoCr.next();
            // change predicate to FreeTextSearch
            DBObject A = (DBObject) assrt.get("A");
            A.removeField("P");
            A.put("P", "FreeTextSearch");
            getAssertionFromJSONObject(assrt);
        }
        mongoCr.close();
    }

    // Regular Expression Text Search
    public void getData_regex(String sub, String pred, ArrayList<String> obj_lst) {
        String regex_expr = obj_lst.get(0);
        BasicDBObject txt_qry;
        if(!sub.startsWith("?"))
            txt_qry = new BasicDBObject("A.S", sub).append("A.D", new BasicDBObject("$regex", regex_expr));
        else txt_qry = new BasicDBObject("A.D", new BasicDBObject("$regex", regex_expr));

        DBCollection mongoColl = mongoDb.getCollection(kbsrc);

        submitToMongo++;

        DBCursor mongoCr = mongoColl.find(txt_qry);
        while(mongoCr.hasNext()) {
            DBObject assrt = mongoCr.next();
            // change predicate to RegexTextSearch
            DBObject A = (DBObject) assrt.get("A");
            A.removeField("P");
            A.put("P", "RegexTextSearch");
            getAssertionFromJSONObject(assrt);
        }
        mongoCr.close();
    }

    // Get all instances of semTypes
    public void getData_inst(ArrayList<String> semTypes) {
        BasicDBList ent_lst = new BasicDBList();
        ent_lst.addAll(semTypes);
        BasicDBObject ent_qry = new BasicDBObject("$in", ent_lst);

        BasicDBObject qry = new BasicDBObject("A.O", ent_qry).append("A.P", "a");
        DBCollection mongoColl = mongoDb.getCollection(kbsrc);

        DBCursor mongoCr = mongoColl.find(qry);
        while(mongoCr.hasNext()) {
            DBObject assrt = mongoCr.next();
            getAssertionFromJSONObject(assrt);
        }
        mongoCr.close();
    }
}
