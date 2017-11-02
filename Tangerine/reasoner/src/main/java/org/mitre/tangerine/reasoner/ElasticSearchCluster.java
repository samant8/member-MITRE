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
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ElasticSearchCluster {
    public ArrayList<ArrayList<String>> results;
    private ArrayList<ElasticSearchClient> esClients;

    private ExecutorService executor;

    public long Tim;

    public ElasticSearchCluster(JSONObject FLConfig) {
        results   = new ArrayList<ArrayList<String>>();
        esClients = new ArrayList<ElasticSearchClient>();

        int scroll_sz = -1;
        String elastic_ip = (String) FLConfig.get("ElasticSearch_IP");
        if(FLConfig.containsKey("ElasticSearchScrollSize")) {
            Long l = (Long) FLConfig.get("ElasticSearchScrollSize");
            scroll_sz = Integer.valueOf(l.intValue());
        }
        JSONArray kbList = (JSONArray) FLConfig.get("ESKBs");
        for(int l=0; l<kbList.size(); l++) {
            String kbname = (String) kbList.get(l);
            esClients.add(new ElasticSearchClient(kbname, elastic_ip, scroll_sz));
        }

        if(esClients.size()>0)
            executor = Executors.newFixedThreadPool(esClients.size());
    }

    public void close() {
        if(esClients.size()>0) executor.shutdown();
    }

    public void clearKB() {
        results.clear();
        for(int es=0; es<esClients.size(); es++) esClients.get(es).clearKB();
    }

    public void getData(String sub, String pred, String obj) {
        ArrayList<String> obj_lst = new ArrayList<String>();
        obj_lst.add(obj);
        getData(sub, pred, obj_lst);
    }

    public void getData(String sub, String pred, ArrayList<String> obj_lst) {
        if(esClients.size()==0) { // No work to do
            Tim = 0;
            return;
        }

        int runningThreadCnt;
        long sttm = System.currentTimeMillis();

        Future[] esFuture = new Future[ esClients.size() ];
        boolean[] esDone  = new boolean[ esClients.size() ];

        // Start threads
        runningThreadCnt=0;
        for(int es=0; es<esClients.size(); es++) {
            esFuture[es] = executor.submit(new ESClusterRunnable("getData", es, sub, pred, obj_lst));
            runningThreadCnt++;
            esDone[es] = false;
        }

        // Aggregate results
        while(runningThreadCnt > 0) {
            for(int es=0; es<esClients.size(); es++) {
                if(!esDone[es] && esFuture[es].isDone()) {
                    for(int a=0; a<esClients.get(es).results.size(); a++) {
                        ArrayList<String> res = esClients.get(es).results.get(a);
                        results.add( res );
                    }
                    esClients.get(es).results.clear(); // no longer needed
                    esDone[es] = true;
                    runningThreadCnt--;
                }
            }
        }

        long edtm = System.currentTimeMillis();
        Tim = edtm-sttm;
    }

    public class ESClusterRunnable implements Runnable {
        private String method; // getData or getData_Cache
        private String sub, pred;
        private ArrayList<String> obj_lst;
        private ElasticSearchClient esClient;

        ESClusterRunnable(String method, int clientIdx,
                          String sub, String pred, ArrayList<String> obj_lst) {
            this.method   = method;
            this.esClient = esClients.get(clientIdx);
            this.sub      = sub;
            this.pred     = pred;
            this.obj_lst  = obj_lst;
        }

        @Override
        public void run() {
            if(method.equals("getData")) esClient.getData(sub, pred, obj_lst);
            //else if(method.equals("getData_Cache")) esClient.getData_Cache();
        }
    }

}
