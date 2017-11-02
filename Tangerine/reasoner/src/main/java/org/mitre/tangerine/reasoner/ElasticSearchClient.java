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

import java.lang.StringBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.OutputStream;
import java.io.Writer;

import java.net.URL;
import java.net.HttpURLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ElasticSearchClient {
    public ArrayList<ArrayList<String>> results;
    private ArrayList<String> singleObj;
    public long Tim;

    private HashSet<String> UUID_History;       // History of UUID pulled
    private HashSet<String> UUID_NFHistory;     // History of UUID pulled for labels
    private HashSet<String> UUID_AssocHistory;  // History of Associated UUID pulled
    private HashSet<String> NameForm_History;   // History of NameForm pulled
    private HashSet<String> Graph_History;      // Graphs in memory

    private String[] clientAssertion   = new String[10];  // individual components of JSON documents

    private String kbsrc, elastic_ip;

    private JSONParser jparser;
    private ArrayList<JSONObject> respHits;
    private int scrollSize;

    HttpURLConnection conn_query, conn_scroll;
    OutputStream out_query, out_scroll;

    // April 9, 2016: Use the scroll api to get result sets that are more than 10000
    //                hits.hits is empty on last batch
    // April 9, 2016: kbsrc can be a comma seperated list of sources
    // ES will search all sources in parallel and return the top <size> matches
    public ElasticSearchClient(String kbsrc, String elastic_ip, int elastic_ss) {
        this.kbsrc      = kbsrc;
        this.elastic_ip = elastic_ip;
        jparser = new JSONParser();
        this.results = new ArrayList<ArrayList<String>>();
        this.respHits = new ArrayList<JSONObject>();
        if(elastic_ss > 0) this.scrollSize = elastic_ss;
        else this.scrollSize = 1000; //default

        singleObj = new ArrayList<String>();

        UUID_History      = new HashSet<String>();
        UUID_NFHistory    = new HashSet<String>();
        UUID_AssocHistory = new HashSet<String>();
        Graph_History     = new HashSet<String>();

        try {
            URL url = new URL("http://"+elastic_ip+":9200/"+kbsrc+"/_search?scroll=1m");
            conn_query = (HttpURLConnection) url.openConnection();
            initConnection(conn_query);

            url = new URL("http://"+elastic_ip+":9200/_search/scroll");
            conn_scroll = (HttpURLConnection) url.openConnection();
            initConnection(conn_scroll);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initConnection(HttpURLConnection conn) {
        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "keep-alive");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            conn.setRequestProperty("Content-Type","application/json");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearKB() {
        singleObj.clear();
        results.clear();
        UUID_History.clear();
        UUID_NFHistory.clear();
        UUID_AssocHistory.clear();
        Graph_History.clear();
    }

    // use scroll api to get all results
    // save all results in an ArrayList of JSONObject
    public void callServer(String query) {
        String restReq=null, restPost=null, scroll_id=null;
        int round=0;
        long st=0,ed=0;

        //st = System.currentTimeMillis();
        try {
            while (true) {
                if(round==0) {
                    restReq  = "http://"+elastic_ip+":9200/"+kbsrc+"/_search?scroll=1m";
                    restPost = query;
                } else {
                    restReq  = "http://"+elastic_ip+":9200/_search/scroll";
                    restPost = "{\"scroll\":\"1m\", \"scroll_id\":\""+scroll_id+"\"}";
                }
                URL url = new URL(restReq);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "keep-alive");
                conn.setDoOutput(true);
                conn.setDoInput(true);
                conn.setUseCaches(false);
                conn.setAllowUserInteraction(false);
                conn.setRequestProperty("Content-Type","application/json");
                conn.setRequestProperty("Content_Length", ""+restPost.length()+"");
                OutputStream out = conn.getOutputStream();
                Writer writer = new OutputStreamWriter(out, "UTF-8");
                writer.write(restPost);
                writer.close();
                out.close();

                if (conn.getResponseCode() != 200) break;

                BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = "";
                StringBuilder sb = new StringBuilder();
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }

                //System.out.println(sb.toString());
                JSONObject JRES = (JSONObject) jparser.parse(sb.toString());
                sb=null;
                //get scroll id for next round
                scroll_id = (String) JRES.get("_scroll_id");
                JRES = (JSONObject) JRES.get("hits");
                JSONArray hits = (JSONArray) JRES.get("hits");
                if(hits.size()==0) break;
                for(int h=0; h<hits.size(); h++) {
                    JSONObject assrt = (JSONObject) hits.get(h);
                    respHits.add(assrt);
                }
                if(hits.size() <= scrollSize) break;
                round++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //ed = System.currentTimeMillis();
        //System.out.println(query + " Hits " + respHits.size() + " Time:" + (ed-st));
    }

    public boolean getAssertionFromJSONObject(JSONObject assrt_in) {
        JSONObject assrt = (JSONObject) assrt_in.get("_source");
        assrt = (JSONObject) assrt.get("A");
        String p = (String) assrt.get("P");
        if(p.equals("a") || p.equals("RDF#Type") || p.equals("RDF#type")) p = ":";
        else if(p.equals("RDF#sameAs") || p.equals("OWL#sameAs")) p = ":=:";
        ArrayList<String> res = new ArrayList<String>();
        res.add((String) assrt.get("S"));
        res.add(p);
        res.add((String) assrt.get("O"));
        results.add(res);
        return true;
    }

    public void getData(String sub, String pred, String obj) {
        singleObj.clear();
        singleObj.add(obj);
        getData(sub, pred, singleObj);
    }

    public void getData(String sub, String pred, ArrayList<String> obj_lst) {
        String obj, query_base, query_pred=null, query=null;
        boolean getFullGraph = false, nfSearch = false;
        HashSet<String> graphsToLoad = null;

        if(pred.equals("FreeTextSearch"))   {
            getData_fts(sub, pred, obj_lst);
            return;
        }
        if(pred.equals("RegexTextSearch"))  {
            getData_fts(sub, pred, obj_lst);
            return;
        }

        if(!pred.equals(":")) obj = obj_lst.get(0);
        else obj = "";

        // set base query command
        query_base = "{\"size\":"+scrollSize+", \"query\":{\"query_string\":{\"query\":\"";

        if(!pred.startsWith("?")) {
            if(pred.equals(":")) query_pred = "A.P:\\\"a\\\"";
            else query_pred = "A.P:\\\"" + pred + "\\\"";
        }

        if(!sub.startsWith("?")) {
            if(query_pred == null && UUID_History.contains(sub)) return;
            query = "A.S:\\\"" + sub + "\\\"";
            if(query_pred == null) {
                UUID_History.add(sub);
                graphsToLoad = new HashSet<String>();
                getFullGraph = true;
            }
        } else if(!obj.startsWith("?")) {
            if(pred.equals(":")) {
                getData_inst(obj_lst);
                return;
            } else {
                if(query_pred == null && UUID_AssocHistory.contains(obj)) return;
                query = "A.O:\\\"" + obj + "\\\"";
                if(query_pred == null) UUID_AssocHistory.add(obj);
            }
        }

        // build complete query
        if(query_pred==null) {
            query_base = query_base + query + "\"}}}";
        } else {
            query_base = query_base +query+" AND "+query_pred+"\"}}}";
        }
        callServer(query_base);
        try {
            for(int h=0; h<respHits.size(); h++) {
                JSONObject assrt = respHits.get(h);
                if(getFullGraph) {
                    assrt = (JSONObject) assrt.get("_source");
                    String graph_id = (String) assrt.get("G");
                    if(!Graph_History.contains(graph_id)) {
                        graphsToLoad.add(graph_id);
                        Graph_History.add(graph_id);
                    }
                } else
                    getAssertionFromJSONObject(assrt);
            }
            respHits.clear();

            if(getFullGraph && graphsToLoad.size()>0) {
                query_base = "{\"size\":"+scrollSize+", \"query\":{\"query_string\":{\"query\":\"G:";
                for(String gid : graphsToLoad) query_base = query_base.concat(gid + " ");
                query_base = query_base.concat("\"}}}");
                callServer(query_base);
                for(int h=0; h<respHits.size(); h++) {
                    JSONObject assrt = respHits.get(h);
                    getAssertionFromJSONObject(assrt);
                }
                respHits.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Free Text Search & Regex Search
    // pred is RegexTextSearch FreeTextSearch
    public void getData_fts(String sub, String pred, ArrayList<String> obj_lst) {
        String fts = obj_lst.get(0);
        String query = "{\"size\":"+scrollSize+", \"query\":{\"query_string\":{\"query\":\"A.O:" + fts + "\", \"default_operator\":\"AND\"}}}";
        callServer(query);
        try {
            for(int h=0; h<respHits.size(); h++) {
                JSONObject assrt = respHits.get(h);
                JSONObject new_assrt = (JSONObject) assrt.get("_source");
                new_assrt = (JSONObject) new_assrt.get("A");
                new_assrt.put("P", pred);
                getAssertionFromJSONObject(assrt);
            }
            respHits.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getData_inst(ArrayList<String> semTypes) {
        String semTypeLst="";
        for(String st : semTypes)
            if(semTypeLst=="") semTypeLst="\\\""+st+"\\\"";
            else semTypeLst = semTypeLst.concat(" \\\""+st+"\\\"");
        String query = "{\"size\":"+scrollSize+", \"query\":{\"query_string\":{\"query\":\"(A.P:\\\"a\\\") AND (A.O:"+semTypeLst+")\"}}}";
        callServer(query);
        try {
            for(int h=0; h<respHits.size(); h++) {
                JSONObject assrt = respHits.get(h);
                getAssertionFromJSONObject(assrt);
            }
            respHits.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
