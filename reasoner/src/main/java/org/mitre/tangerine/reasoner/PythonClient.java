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
import java.util.HashMap;
import java.util.Map;

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.OutputStreamWriter;
import java.io.InputStreamReader;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class PythonClient {
    private JSONParser jparser;

    public ArrayList<ArrayList<String>> results;
    public long Tim;

    private JSONArray PyScripts;
    private String    PyScriptsDir;
    private String    PythonExe;
    private String    uDN;
    private String    iDN;

    private FLogicEngine      flengine;
    private FLogicEnvironment flenv;

    public PythonClient(JSONObject FLConfig, FLogicEngine flengine) {
        results = new ArrayList<ArrayList<String>>();

        // create a JSON parser
        jparser = new JSONParser();

        // create a lite versio of the reasoning engine
        this.flengine = flengine;
        flenv = new FLogicEnvironment();

        // get host name
        Map<String, String> env = System.getenv();

        PythonExe    = (String)    FLConfig.get("PythonExe"); // default location of python
        PyScripts    = (JSONArray) FLConfig.get("Plugins");
        PyScriptsDir = (String)    FLConfig.get("PluginsDir");
    }

    private boolean useScript(JSONObject script, String predicate, FLogicEnvironment flenv) {
        // Predicate Match
        String pname = (String) script.get("name");
        if(!pname.equals(predicate)) return false;

        // Is call to the service blocked
        if(flenv.AnalyticsToIgnore.contains(predicate)) return false;

        // Otherwise
        return true;
    }

    public void getData(String predicate, String Params[], FLogicEnvironment flenv) {
        try {
            for(int p=0; p<PyScripts.size(); p++) {
                JSONObject script = (JSONObject) PyScripts.get(p);
                if(useScript(script, predicate, flenv)) {
                    if(script.containsKey("F-Logic")) executeFLWF(script, predicate, Params, flenv);
                    else if(script.containsKey("Clause")) executeFunc(script, predicate, Params, flenv);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // run a f-logic query; send results to a script; return script results as assertions
    public void executeFLWF(JSONObject script, String predicate, String Params[], FLogicEnvironment flenv) {
        ResultSet RS = new ResultSet();
        Goal goal;
        String[] new_args = new String[FLogicConstants.MAX_ARITY];
        HashMap<String, String> varMap = new HashMap<String, String>();

        try {
            // build the query
            String SQry = (String) script.get("F-Logic");
            JSONArray ArgMap = (JSONArray) script.get("argMap");
            for(int a=0; a<ArgMap.size(); a++) {
                String arg   = (String) ArgMap.get(a);
                String param = Params[a];
                if(!param.startsWith("?")) {
                    SQry = SQry.replace(arg, param);
                    varMap.put(arg, param);
                }
            }
            // evaluate the query
            goal = flengine.parseGoal(SQry, flenv);
            if(goal != null) {
                int old_goal_id = flenv.getGoal_id();
                flengine.evaluate(goal, RS, flenv);
                flenv.setGoal_id(old_goal_id);
                // send query results to python
                String qry_results = RS.toJSONString();
                String pyScript    = (String) script.get("pythonScript");

                // run the script
                ProcessBuilder pb = new ProcessBuilder(PythonExe, PyScriptsDir+"/"+pyScript);
                Process P = pb.start();

                // write qry_results
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(P.getOutputStream()));
                writer.write(qry_results);
                writer.newLine();
                writer.close();

                // read the results
                BufferedReader reader = new BufferedReader(new InputStreamReader(P.getInputStream()));
                String s, scr_results="";
                while((s = reader.readLine()) != null) scr_results = scr_results.concat(s);

                // generate assertions
                JSONArray assertTemplate = (JSONArray) script.get("assert");
                JSONObject Asserts       = (JSONObject) jparser.parse(scr_results);
                JSONArray Results        = (JSONArray) Asserts.get("Results");
                String param, val;
                for(int r=0; r<Results.size(); r++) {
                    JSONObject result  = (JSONObject) Results.get(r);
                    JSONObject binding = (JSONObject) result.get("Results");
                    for(int a=0; a<assertTemplate.size(); a++) {
                        param = (String) assertTemplate.get(a);
                        if(binding.containsKey(param)) val = (String) binding.get(param);
                        else val = varMap.get(param);
                        new_args[a] = val;
                    }
                    flenv.getKB().addAssertion(predicate, assertTemplate.size(), new_args,
                                               FLogicConstants.DEFAULT_GRAPH,
                                               FLogicConstants.DIRECT_ASSERTION);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // run a script that generates a single value
    private void executeFunc(JSONObject script, String predicate, String Params[],
                             FLogicEnvironment flenv) {
        String[] new_args = new String[FLogicConstants.MAX_ARITY];
        String pyScript = (String) script.get("pythonScript");

        // run the script
        ArrayList Cmd = new ArrayList();
        Cmd.add(PythonExe);
        Cmd.add(PyScriptsDir+"/"+pyScript);
        Cmd.add(Params[0] + "|" + predicate + "|" + Params[1]);
        String[] cmd_call = (String []) Cmd.toArray(new String[Cmd.size()]);

        String s;
        ArrayList<String> scr_results = new ArrayList<String>();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd_call);
            Process p = pb.start();

            // read the results
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while((s = reader.readLine()) != null) scr_results.add(s);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // generate assertions
        String param;
        for(String scr : scr_results) {
            String[] SCR = scr.split("\\|");
            predicate = SCR[1];
            new_args[0] = SCR[0];
            for(int a=2; a<SCR.length; a++)	new_args[a-1] = SCR[a];
            flenv.getKB().addAssertion(predicate, SCR.length-1, new_args,
                                       FLogicConstants.DEFAULT_GRAPH,
                                       FLogicConstants.DIRECT_ASSERTION);
        }
    }

}
