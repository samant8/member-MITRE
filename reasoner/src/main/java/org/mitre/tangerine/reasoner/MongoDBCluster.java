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

public class MongoDBCluster {

    public ArrayList<ArrayList<String>> results;
    private ArrayList<MongoDBClient> mgClients;

    private ExecutorService executor;

    public long Tim;

    public MongoDBCluster(JSONObject FLConfig) {
        results   = new ArrayList<ArrayList<String>>();
        mgClients = new ArrayList<MongoDBClient>();
        ArrayList<String> textPreds = new ArrayList<String>();
        ArrayList<String> enrichPreds = new ArrayList<String>();

        String mongodb_ip = (String) FLConfig.get("MongoDB_IP");

        String dbname = (String) FLConfig.get("MongoDB_Name");

        JSONArray idxPredList = (JSONArray) FLConfig.get("MongoDB_TextIndexPredicates");
        for(int l=0; l<idxPredList.size(); l++) textPreds.add( (String) idxPredList.get(l) );

        JSONArray enrPredList = (JSONArray) FLConfig.get("MongoDB_EnrichmentPredicates");
        for(int l=0; l<enrPredList.size(); l++) enrichPreds.add( (String) enrPredList.get(l) );

        JSONArray kbList = (JSONArray) FLConfig.get("KBs");
        for(int l=0; l<kbList.size(); l++) {
            String kbname = (String) kbList.get(l);
            mgClients.add(new MongoDBClient(dbname, kbname, mongodb_ip, textPreds, enrichPreds));
        }

        if(mgClients.size()>0)
            executor = Executors.newFixedThreadPool(mgClients.size());
    }

    public void close() {
        if(mgClients.size()>0) executor.shutdown();
        for(int mg=0; mg<mgClients.size(); mg++) mgClients.get(mg).release();
    }

    public void clearKB() {
        results.clear();
        for(int mg=0; mg<mgClients.size(); mg++) mgClients.get(mg).clearKB();
    }

    public void getData(String sub, String pred, String obj) {
        ArrayList<String> obj_lst = new ArrayList<String>();
        obj_lst.add(obj);
        getData(sub, pred, obj_lst);
    }

    public void getData(String sub, String pred, ArrayList<String> obj_lst) {
        if(mgClients.size()==0) { // No work to do
            Tim = 0;
            return;
        }

        int runningThreadCnt;
        long sttm = System.currentTimeMillis();

        Future[] mgFuture = new Future[ mgClients.size() ];
        boolean[] mgDone  = new boolean[ mgClients.size() ];

        // Start threads
        runningThreadCnt=0;
        for(int mg=0; mg<mgClients.size(); mg++) {
            mgFuture[mg] = executor.submit(new MGClusterRunnable("getData", mg, sub, pred, obj_lst));
            runningThreadCnt++;
            mgDone[mg] = false;
        }

        // Aggregate results
        while(runningThreadCnt > 0) {
            for(int mg=0; mg<mgClients.size(); mg++) {
                if(!mgDone[mg] && mgFuture[mg].isDone()) {
                    for(int a=0; a<mgClients.get(mg).results.size(); a++) {
                        ArrayList<String> res = mgClients.get(mg).results.get(a);
                        results.add( res );
                    }
                    mgClients.get(mg).results.clear(); // no longer needed
                    mgDone[mg] = true;
                    runningThreadCnt--;
                }
            }
        }

        long edtm = System.currentTimeMillis();
        Tim = edtm-sttm;
    }

    public class MGClusterRunnable implements Runnable {
        private String method; // getData or getData_Cache
        private String sub, pred;
        private ArrayList<String> obj_lst;
        private MongoDBClient mgClient;

        MGClusterRunnable(String method, int clientIdx,
                          String sub, String pred, ArrayList<String> obj_lst) {
            this.method   = method;
            this.mgClient = mgClients.get(clientIdx);
            this.sub      = sub;
            this.pred     = pred;
            this.obj_lst  = obj_lst;
        }

        @Override
        public void run() {
            if(method.equals("getData")) mgClient.getData(sub, pred, obj_lst);
            //else if(method.equals("getData_Cache")) mgClient.getData_Cache();
        }
    }

}
