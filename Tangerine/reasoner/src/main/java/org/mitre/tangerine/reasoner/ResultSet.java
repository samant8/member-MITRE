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
import java.util.HashSet;
import java.util.LinkedHashMap;

import java.io.StringWriter;
import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/// MongoDB for Streams storage
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBList;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;

import java.util.UUID;

public class ResultSet {

    private int goalCount = 0;
    private int iterations = 0;
    // number of interations to solve goal
    private int maxIterations = FLogicConstants.MAX_ITERATIONS;
    private boolean complete;  // did the process complete within time limits
    public boolean displayOnAdd; // display result when added
    private long runTime;
    private ArrayList<String> variables = new ArrayList<String>();
    private ArrayList<Tuple> tuples = new ArrayList<Tuple>();
    private ArrayList<ArrayList<String>> traces = new ArrayList<ArrayList<String>>();
    // map tuple to string for duplicate detection
    private HashSet<String> resmap = new HashSet<String>();
    // map a variable name to a position on variables list
    private LinkedHashMap<String, Integer> variableMap = new LinkedHashMap<String, Integer>();
    public String query;

    public ResultSet() {
        displayOnAdd = false; // don't display results when they are added
        query=null;
    }

    public void clear() {
        getVariables().clear();
        getVariableMap().clear();
        getTuples().clear();
        getResmap().clear();
        traces.clear();
        setIterations(0);
        setGoalCount(0);
    }

    public void addVariable(String in_var) {
        boolean varFnd = false;
        for (String var : getVariables()) {
            if (var.equals(in_var)) {
                varFnd = true;
            }
        }

        if (!varFnd) {
            getVariables().add(in_var);
        }
    }

    public boolean tupleExists(Tuple tuple) {
        String ss;

        ss = "";
        for (int i = 0; i < getVariables().size(); i++) {
            ss += tuple.getCol()[i];
        }
        // search the map for this result
        if (!resmap.contains(ss)) {
            return false;
        } else {
            return true;
        }
    }

    public void addTuple(Tuple tuple) {
        this.addTuple(tuple, null);
    }

    public void addTuple(Tuple tuple, ArrayList<String> tuple_trace) {
        String ss;

        if (tuple == null) {
            return;
        }

        ss = "";
        for (int i = 0; i < getVariables().size(); i++) {
            ss += tuple.getCol()[i];
        }
        if (!resmap.contains(ss)) {
            getTuples().add(tuple);
            getResmap().add(ss);
            traces.add(tuple_trace);
        } else { // duplicate
            tuple = null;
        }
    }

    public String getValue(Integer row, String variable) {
        int j;

        if (getVariableMap().size() == 0) {
            // build variable index
            for (j = 0; j < getVariables().size(); j++) {
                getVariableMap().put(getVariables().get(j), j);
            }
        }

        if (row < getTuples().size()) {
            if (getVariableMap().containsKey(variable)) {
                int colNum = getVariableMap().get(variable);
                Tuple tpl = getTuples().get(row);
                return tpl.getCol()[colNum];
            }
        }
        return "";
    }


    public String toJSONString() {
        JSONObject json_res = new JSONObject();
        JSONArray res = new JSONArray();
        for(int i=0; i<getTuples().size(); i++) {
            JSONObject tupl_res = new JSONObject();
            Tuple tpl = getTuples().get(i);
            for(int j=0; j<getVariables().size(); j++)
                tupl_res.put(getVariables().get(j), tpl.getCol()[j]);

            ArrayList<String> Trace = traces.get(i);
            JSONArray tupl_trace = new JSONArray();
            for(String trace : Trace) tupl_trace.add(trace);

            JSONObject res_i = new JSONObject();
            res_i.put("Result", tupl_res);
            res_i.put("Trace", tupl_trace);

            res.add(res_i);
        }
        json_res.put("Results", res);
        json_res.put("Total", getTuples().size());
        json_res.put("Time", getRunTime());

        StringWriter out = new StringWriter();
        try {
            JSONValue.writeJSONString(json_res, out);
        } catch (IOException e) {
            return "bad result string";
        }
        return out.toString();
    }

    public String toJSONString(String query, FLogicEnvironment flenv) {
        JSONObject json_res = new JSONObject();
        JSONArray res = new JSONArray();
        for(int i=0; i<getTuples().size(); i++) {
            JSONObject tupl_res = new JSONObject();
            Tuple tpl = getTuples().get(i);
            for(int j=0; j<getVariables().size(); j++)
                tupl_res.put(getVariables().get(j), tpl.getCol()[j]);

            ArrayList<String> Trace = traces.get(i);
            JSONArray tupl_trace = new JSONArray();
            for(String trace : Trace) tupl_trace.add(trace);

            JSONObject res_i = new JSONObject();
            res_i.put("Result", tupl_res);
            res_i.put("Trace", tupl_trace);

            res.add(res_i);
        }
        json_res.put("Results", res);
        json_res.put("Total", getTuples().size());
        JSONObject rTime = new JSONObject();
        rTime.put("Total", getRunTime());
        rTime.put("MGTime",  flenv.mgTim);
        json_res.put("Time", rTime);
        json_res.put("Query", query);

        StringWriter out = new StringWriter();
        try {
            JSONValue.writeJSONString(json_res, out);
        } catch (IOException e) {
            return "bad result string";
        }
        return out.toString();
    }

    public void mongoSave(DB mongoDb, String flqueryid) {
        try {
            DBCollection mongoColl = mongoDb.getCollection("FL_QUERY_RESULT");
            for(int i=0; i<getTuples().size(); i++) {
                Tuple tuple = getTuples().get(i);
                ArrayList<String> tuple_trace = traces.get(i);
                BasicDBObject bindings = new BasicDBObject();
                for(int j=0; j<getVariables().size(); j++)
                    bindings.put(getVariables().get(j), tuple.getCol()[j]);
                BasicDBObject search  = new BasicDBObject();
                // does this result already exist
                search.put("fl_query_id", flqueryid);
                search.put("bindings", bindings);
                DBCursor mongoCr = mongoColl.find(search);
                if(!mongoCr.hasNext()) {
                    BasicDBObject results = new BasicDBObject();
                    results.put("fl_query_id", flqueryid);
                    results.put("result_id", UUID.randomUUID().toString());
                    results.put("bindings", bindings);
                    if(tuple_trace != null) {
                        BasicDBList trace = new BasicDBList();
                        for(String step : tuple_trace) trace.add(step);
                        results.put("trace", trace);
                    }
                    mongoColl.insert(results);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }
    }

    public int size() {
        return getTuples().size();
    }

    public boolean solutionExists(Goal goal) {
        int i, j;
        Tuple t;
        boolean fnd;
        String value;

        t = new Tuple();
        fnd = true;
        for (i = 0; i < getVariables().size() && fnd; i++) {
            // look for the ith variable in the bindings
            fnd = false;
            value = goal.getBindSet().getBinding(getVariables().get(i));
            if (value.length() > 0 && !value.startsWith("?")) {
                t.getCol()[i] = value;
                fnd = true;
            }
        }
        // check result set for this tuple
        if (fnd && tupleExists(t)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the goalCount
     */
    public int getGoalCount() {
        return goalCount;
    }

    /**
     * @param goalCount the goalCount to set
     */
    public void setGoalCount(int goalCount) {
        this.goalCount = goalCount;
    }

    /**
     * @return the iterations
     */
    public int getIterations() {
        return iterations;
    }

    /**
     * @param iterations the iterations to set
     */
    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    /**
     * @return the maxIterations
     */
    public int getMaxIterations() {
        return maxIterations;
    }

    /**
     * @param maxIterations the maxIterations to set
     */
    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * @return the complete
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * @param complete the complete to set
     */
    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    /**
     * @return the runTime
     */
    public long getRunTime() {
        return runTime;
    }

    /**
     * @param runTime the runTime to set
     */
    public void setRunTime(long runTime) {
        this.runTime = runTime;
    }

    /**
     * @return the variables
     */
    public ArrayList<String> getVariables() {
        return variables;
    }

    /**
     * @param variables the variables to set
     */
    public void setVariables(ArrayList<String> variables) {
        this.variables = variables;
    }

    /**
     * @return the tuples
     */
    public ArrayList<Tuple> getTuples() {
        return tuples;
    }

    /**
     * @param tuples the tuples to set
     */
    public void setTuples(ArrayList<Tuple> tuples) {
        this.tuples = tuples;
    }

    /**
     * @return the resmap
     */
    public HashSet<String> getResmap() {
        return resmap;
    }

    /**
     * @param resmap the resmap to set
     */
    public void setResmap(HashSet<String> resmap) {
        this.resmap = resmap;
    }

    /**
     * @return the variableMap
     */
    public LinkedHashMap<String, Integer> getVariableMap() {
        return variableMap;
    }

    /**
     * @param variableMap the variableMap to set
     */
    public void setVariableMap(LinkedHashMap<String, Integer> variableMap) {
        this.variableMap = variableMap;
    }
}
