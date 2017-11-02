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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FLogicEngine {

    static Logger logger = Logger.getLogger(FLogicEngine.class);
    /**
     *
     */
    private Map<String, OntoObject> ont = new LinkedHashMap<String, OntoObject>(); // Ontology
    // classes
    /**
     *
     */
    private ArrayList<HornClause> ontoRules = new ArrayList<HornClause>(); // general
    // ontology
    // rules
    /**
     *
     */
    private StringIndex ontoRulesIndex = new StringIndex(); // String -> Integer
    private StringIndex predIndex = new StringIndex();
    private StringIndex[] argsIndex = new StringIndex[FLogicConstants.MAX_ARITY];
    /**
     *
     */
    public HashSet<String> systemPreds; // list of system predicates and
    // operators, i.e. fail, !=, ==
    public HashSet<String> aggregatePreds;
    /**
     *
     */
    private FLogicKB KB = new FLogicKB(); // assertions that are stored in the
    // onotlogy i.e. constants
    private Map<Integer, Integer> classesToCache = new LinkedHashMap<Integer, Integer>();
    private final Pattern FUNC_EXPR = Pattern.compile("(.+)\\((.+)\\)");
    private final Pattern ATTR_EXPR = Pattern.compile("\\$(.+)->(.+)");

    private HashMap<String, String> ancCache; // Ancestor Cache for 1 class

    private ArrayList<String> SymmetricPredicates, TransitivePredicates;
    private HashMap<String, String> InverseOf;
    private Set<String> tabledPreds; // list of predicates to evaluate using
    // linear tabling

    private TemporalReasoner TempReas; // temporal reasoning class

    private MongoDBCluster mgClient;
    private PythonClient pyClient;
    private ElasticSearchCluster esClient;
    // private MySQLClient myClient;

    private JSONObject QueryPatterns;

    private ExecutorService executor;

    // LIVE or RETRO
    private boolean streamMode;

    // Modified by Elmer
    /**
     *
     */
    public FLogicEngine(String configDir, String configFile) {
        systemPreds = new HashSet<String>();
        for (String pred : FLogicConstants.BuiltInPreds.split(" "))
            systemPreds.add(pred);

        aggregatePreds = new HashSet<String>();
        for (String pred : FLogicConstants.AggregatePreds.split(" "))
            aggregatePreds.add(pred);

        ancCache = new HashMap<String, String>();
        SymmetricPredicates = new ArrayList<String>();
        TransitivePredicates = new ArrayList<String>();
        InverseOf = new HashMap<String, String>();
        tabledPreds = new HashSet<String>();

        TempReas = new TemporalReasoner();

        // load flquery-config
        ArrayList<String> Onts = new ArrayList<String>();
        JSONObject FLConfig = null;
        JSONParser jparser = new JSONParser();
        try {
            if (configDir.isEmpty()) {
                FLConfig = (JSONObject) jparser.parse(configFile);
            } else {
                FLConfig = (JSONObject) jparser.parse(new FileReader(configDir + "/" + configFile));
            }
            JSONArray OntList = (JSONArray) FLConfig.get("Ontology");
            for (int l = 0; l < OntList.size(); l++)
                Onts.add(configDir + "/" + (String) OntList.get(l));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // load query patterns
        QueryPatterns = FLConfig;

        Map<String, String> env = System.getenv();

        mgClient = new MongoDBCluster(FLConfig);
        esClient = new ElasticSearchCluster(FLConfig);
        pyClient = new PythonClient(FLConfig, this);
        // myClient = new MySQLClient(FLConfig);

        executor = Executors.newFixedThreadPool(3);

        // load ontology files
        for (String filename : Onts)
            loadOntology(new File(filename));

        // streming mode defaults to false
        streamMode = false;
    }

    // set stream mode to enable stream processing
    public void setStreamMode(boolean mode) {
        this.streamMode = mode;
    }

    public void close() {
        executor.shutdown();
        mgClient.close();
        esClient.close();
    }

    // Disjunction over child and parent
    public boolean isaOr(List<String> child, String parent, FLogicEnvironment flEnv) {
        flEnv.setAnswer(new ArrayList<String>());
        for (String str : child) {
            if (isa(str, parent, flEnv)) {
                flEnv.getAnswer().add(str);
            }
        }

        return (flEnv.getAnswer().size() > 0);
    }

    public boolean isaOr(List<String> child, List<String> parent, FLogicEnvironment flEnv) {
        flEnv.setAnswer(new ArrayList<String>());
        for (String str : child) {
            for (String pstr : parent) {
                if (isa(str, pstr, flEnv)) {
                    flEnv.getAnswer().add(str);
                }
            }
        }

        return (flEnv.getAnswer().size() > 0);
    }

    public void classifyInstance(String term, List<String> entityTypes, List<String> modifiers,
                                 FLogicEnvironment flEnv) {
        Set<String> entityTypeSet = new HashSet<String>();
        List<String> tmpEntityTypes = new ArrayList<String>();
        List<Integer> ts = new ArrayList<Integer>();

        entityTypeSet.addAll(entityTypes);

        // look for new derived entity types for the instance rules
        // instance rules take presidence
        if (tmpEntityTypes.isEmpty()) {
            // Query the KB
            // The instance mapping rules have already been executed
            getInstanceAssertions(term, "<:", "?par", FLogicConstants.DEFAULT_GRAPH, ts, flEnv);
            for (int i = 0; i < ts.size(); i++) {
                N_ary tr = flEnv.getKB().getAssertion(ts.get(i));
                if (!entityTypeSet.contains(tr.getArgs()[1])) {
                    tmpEntityTypes.add(tr.getArgs()[1]);
                }
            }
        }

        if (!tmpEntityTypes.isEmpty()) {
            entityTypes = new ArrayList<String>();
            entityTypes.addAll(tmpEntityTypes);
        }
    }

    public void reduceSemanticTypes(List<String> entityTypes, boolean strict, FLogicEnvironment flEnv) {
        List<String> tmpEntityTypes = new ArrayList<String>();
        tmpEntityTypes.addAll(entityTypes);
        entityTypes.clear();
        for (int i = 0; i < tmpEntityTypes.size(); i++) {
            String parent = tmpEntityTypes.get(i);
            int subsumeCount = 0;
            for (int j = 0; j < tmpEntityTypes.size(); j++) {
                String child = tmpEntityTypes.get(j);
                if (i != j) {
                    if ((strict && subsumes(parent, child, flEnv)) || (!strict && isa(child, parent, flEnv))) {
                        if (!sameAs(child, parent, flEnv)) {
                            subsumeCount++;
                        }
                    }
                }
            }
            if (subsumeCount == 0) {
                entityTypes.add(parent);
            }
        }
    }

    private boolean semanticMatch(String val1, String val2, FLogicEnvironment flEnv) {
        boolean retVal = false;
        if (val1.equals(val2)) {
            retVal = true;
        } else {
            List<String> history = new ArrayList<String>();
            Set<String> searched = new HashSet<String>();
            history.add(val1);
            searched.add(val1);
            while (history.size() > 0) {
                String searchVal = history.remove(0);
                if (searchVal.equals(val2)) {
                    history.clear();
                    searched.clear();
                    retVal = true;
                    break;
                } else {
                    updateEquivalenceChain(searchVal, history, searched, FLogicConstants.DEFAULT_GRAPH, flEnv);
                }
            }
        }
        return retVal;
    }

    // Assumes all ":=:" statements are in memory
    private void updateEquivalenceChain(String searchVal, List<String> history, Set<String> searched, String graph,
                                        FLogicEnvironment flenv) {
        final String EQ_PRED = ":=:";
        FLogicKB kb;

        for (int k = 0; k < 2; k++) {
            if (k == 0)
                kb = KB; // ontology
            else
                kb = flenv.getKB();
            for (int i = 0; i < 2; i++) {
                if (kb.getArgsIndex()[i] != null) {
                    HashSet<Integer> vals = kb.getArgsIndex()[i].getIndexValues(searchVal);
                    if (vals != null) {
                        for (int idx : vals) {
                            N_ary assrt = kb.getAssertion(idx);
                            boolean match = graphMatch(graph, assrt.getGraph());
                            if (match && !assrt.getPred().equals(EQ_PRED)) {
                                match = false;
                            }
                            if (match && !searched.contains(assrt.getArgs()[1 - i])) {
                                history.add(assrt.getArgs()[1 - i]);
                                searched.add(assrt.getArgs()[1 - i]);
                            }
                        }
                    }
                }
            }
        }

        if (flenv.executionMode == FLogicConstants.FULL_MODE) {
            // load data for entities on this list
            String[] args = new String[FLogicConstants.MAX_ARITY];
            for (String ent : history) {
                args[0] = ent;
                args[1] = "?obj";
                // getLocalDBAssertions("?pred", args, 2,
                // FLogicConstants.DEFAULT_GRAPH, flenv);
                getExternalAssertions("?pred", args, 2, FLogicConstants.DEFAULT_GRAPH, flenv);

                args[0] = "?sub";
                args[1] = ent;
                // getLocalDBAssertions("?pred", args, 2,
                // FLogicConstants.DEFAULT_GRAPH, flenv);
                getExternalAssertions("?pred", args, 2, FLogicConstants.DEFAULT_GRAPH, flenv);
            }
        }

    }

    /*
     * Does the parent completely subsume the child Evaluate ?C :: ?Y, ~P :: ?Y,
     * ~?Y :: ?P.
     */
    public boolean subsumes(String parent, String child, FLogicEnvironment flEnv) {
        boolean retVal = false;
        List<OntoObject> scanList = new ArrayList<OntoObject>();
        List<String> ancs = new ArrayList<String>();

        if (!isa(child, parent, flEnv))
            return false;

        OntoObject p = getOnt().get(parent);
        OntoObject c = getOnt().get(child);
        // init Ontology_Environment
        flEnv.getHistory().clear();
        flEnv.getSearched().clear();
        // seed with local super & equivalent classes
        for (OntoObject ocs : c.getSups().values()) {
            flEnv.getSearched().set(ocs.getNodeId());
        }

        while (flEnv.getHistory().size() > 0) {
            c = flEnv.getHistory().remove(0);
            ancs.add(c.getName());
            scanList.clear();
            scanList.addAll(c.getSups().values());
            scanList.addAll(c.getEq().values());
            for (OntoObject ocs : scanList) {
                if (!flEnv.getSearched().get(ocs.getNodeId())) {
                    flEnv.getHistory().add(ocs);
                    flEnv.getSearched().set(ocs.getNodeId());
                }
            }
        }

        // look for ancestor of child that is not a descendant of parent
        for (String s : ancs) {
            if (!isa(parent, s, flEnv) && !isa(s, parent, flEnv)) {
                return false;
            }
        }

        return true;
    }

    public int minDist(List<String> c, List<String> p, FLogicEnvironment flEnv) {
        int minDisIdx = 0;
        int md = FLogicConstants.MAX_ONTOLOGY_DISTANCE;
        for (int i = 0; i < c.size(); i++) {
            for (int j = 0; j < p.size(); j++) {
                int d = distance(c.get(i), p.get(j), flEnv);
                if (d < md) {
                    md = d;
                    minDisIdx = i;
                }
            }
        }

        String tmpC = c.get(0);
        c.set(0, c.get(minDisIdx));
        c.set(minDisIdx, tmpC);

        return md;
    }

    public int distance(String child, String parent, FLogicEnvironment flEnv) {
        int distance = FLogicConstants.MAX_ONTOLOGY_DISTANCE;
        int[] distances = new int[FLogicConstants.MAX_ONTOLOGY_CLASS_CNT];

        OntoObject c = getOnt().get(child);
        OntoObject p = getOnt().get(parent);
        if (p != null && c != null) {
            if (c == p) {
                distance = 0;
            } else {
                flEnv.getHistory().clear();
                flEnv.getSearched().clear();
                for (OntoObject ocs : c.getEq().values()) {
                    flEnv.getHistory().add(ocs);
                    flEnv.getSearched().set(ocs.getNodeId());
                    distances[ocs.getNodeId()] = 0;
                }

                for (OntoObject ocs : c.getSups().values()) {
                    flEnv.getHistory().add(ocs);
                    flEnv.getSearched().set(ocs.getNodeId());
                    distances[ocs.getNodeId()] = 1;
                }

                while (flEnv.getHistory().size() > 0) {
                    OntoObject ocs = flEnv.getHistory().remove(0);
                    if (ocs == p) {
                        distance = distances[ocs.getNodeId()];
                        break;
                    } else {
                        // if (ocs.getEqList().size() > 0) {
                        for (OntoObject a : ocs.getEq().values()) {
                            if (!flEnv.getSearched().get(a.getNodeId())) {
                                flEnv.getHistory().add(a);
                                flEnv.getSearched().set(a.getNodeId());
                                distances[a.getNodeId()] = distances[ocs.getNodeId()];
                            }
                        }
                        // }
                        // if (ocs.getSupList().size() > 0) {
                        for (OntoObject a : ocs.getSups().values()) {
                            if (!flEnv.getSearched().get(a.getNodeId())) {
                                flEnv.getHistory().add(a);
                                flEnv.getSearched().set(a.getNodeId());
                                distances[a.getNodeId()] = distances[ocs.getNodeId()] + 1;
                            }
                        }
                        // }
                    }
                }

            }
        }

        return distance;
    }

    public LinkedHashMap<String, Integer> distanceToAncestors(String cls, FLogicEnvironment flEnv, boolean cache) {
        int distance = FLogicConstants.MAX_ONTOLOGY_DISTANCE;
        LinkedHashMap<String, Integer> DistMap = new LinkedHashMap<String, Integer>();

        String serializedAncs = "";
        synchronized (ancCache) {
            if (ancCache.containsKey(cls)) {
                serializedAncs = ancCache.get(cls);
            }
        }
        if (serializedAncs.length() > 0) {
            String[] Tok = serializedAncs.split(" ");
            for (String pair : Tok) {
                String[] cls_dist = pair.split("=");
                DistMap.put(cls_dist[0], Integer.parseInt(cls_dist[1]));
            }
            return DistMap;
        }

        OntoObject c = getOnt().get(cls);
        if (c != null) {
            flEnv.getHistory().clear();
            flEnv.getSearched().clear();
            DistMap.put(cls, 0);
            for (OntoObject ocs : c.getEq().values()) {
                flEnv.getHistory().add(ocs);
                flEnv.getSearched().set(ocs.getNodeId());
                DistMap.put(ocs.getName(), 0);
            }

            for (OntoObject ocs : c.getSups().values()) {
                flEnv.getHistory().add(ocs);
                flEnv.getSearched().set(ocs.getNodeId());
                DistMap.put(ocs.getName(), 1);
            }

            while (flEnv.getHistory().size() > 0) {
                OntoObject ocs = flEnv.getHistory().remove(0);
                for (OntoObject a : ocs.getEq().values()) {
                    if (!flEnv.getSearched().get(a.getNodeId())) {
                        flEnv.getHistory().add(a);
                        flEnv.getSearched().set(a.getNodeId());
                        DistMap.put(a.getName(), DistMap.get(ocs.getName()));
                    }
                }
                for (OntoObject a : ocs.getSups().values()) {
                    if (!flEnv.getSearched().get(a.getNodeId())) {
                        flEnv.getHistory().add(a);
                        flEnv.getSearched().set(a.getNodeId());
                        DistMap.put(a.getName(), DistMap.get(ocs.getName()) + 1);
                    }
                }
            }
        }

        if (cache) {
            serializedAncs = "";
            for (Map.Entry<String, Integer> entry : DistMap.entrySet()) {
                String anc = entry.getKey();
                int dist = entry.getValue();
                String pair = anc + "=" + dist;
                if (serializedAncs.isEmpty())
                    serializedAncs = pair;
                else
                    serializedAncs = serializedAncs + " " + pair;
            }
            synchronized (ancCache) {
                ancCache.put(cls, serializedAncs);
            }
        }
        return DistMap;
    }

    public int distanceBetweenConcepts(String con1, String con2, FLogicEnvironment flEnv) {
        LinkedHashMap<String, Integer> con1Ancs, con2Ancs;

        OntoObject c;
        int minDist = 34;

        c = getOnt().get(con1);
        if (c == null)
            return FLogicConstants.MAX_ONTOLOGY_DISTANCE;
        c = getOnt().get(con2);
        if (c == null)
            return FLogicConstants.MAX_ONTOLOGY_DISTANCE;

        long sttm = System.currentTimeMillis();
        con1Ancs = distanceToAncestors(con1, flEnv, true);
        con2Ancs = distanceToAncestors(con2, flEnv, true);
        long edtm = System.currentTimeMillis();
        flEnv.funcTime += edtm - sttm;

        // find the closest common ancestor
        for (Map.Entry<String, Integer> entry : con1Ancs.entrySet()) {
            String anc = entry.getKey();
            if (con2Ancs.containsKey(anc)) {
                int dist = entry.getValue() + con2Ancs.get(anc);
                if (dist < minDist) {
                    minDist = dist;
                    break;
                }
            }
        }

        return minDist;
    }

    public boolean inRange(List<String> cls, List<String> range, List<String> negRange, List<String> relaxTo,
                           List<String> negRelaxTo, FLogicEnvironment flEnv) {
        flEnv.getAnswer().clear();
        flEnv.getRelaxToAnswer().clear();
        for (String clsStr : cls) {
            boolean rangeTest = ontoRangeCheck(clsStr, range, flEnv);
            if (rangeTest && !ontoRangeCheck(clsStr, negRange, flEnv)) {
                flEnv.getAnswer().add(clsStr);
            } else {
                rangeTest = false;
            }
            if (!rangeTest) {
                boolean relaxToTest = ontoRangeCheck(clsStr, relaxTo, flEnv);
                if (relaxToTest && !ontoRangeCheck(clsStr, negRelaxTo, flEnv)) {
                    flEnv.getRelaxToAnswer().add(clsStr);
                }
            }
        }

        return !flEnv.getAnswer().isEmpty();
    }

    private boolean ontoRangeCheck(String c, List<String> s, FLogicEnvironment flEnv) {
        boolean retVal = false;

        for (String str : s) {
            if (isa(c, str, flEnv)) {
                retVal = true;
                break;
            }
        }

        return retVal;
    }

    /**
     *
     * @param clsName
     * @return
     */
    public OntoObject getOntologyConcept(String clsName) {
        OntoObject cls;

        if (!ont.containsKey(clsName)) {
            // concept not found so create it
            cls = new OntoObject("CLASS");
            cls.setName(clsName);
            getOnt().put(clsName, cls);
            cls.setNodeId(getOnt().size());
        } else {
            cls = getOnt().get(clsName);
        }
        return cls;
    }

    /**
     *
     * @param from
     * @param to
     * @return
     */
    public OntoObject mapOntologyConcept(String from, String to) {
        OntoObject cls;

        cls = getOntologyConcept(to);
        getOnt().put(from, cls);
        return cls;
    }

    /**
     *
     * @param class1
     * @param class2
     * @param oenv
     * @return
     */
    public boolean sameAs(String class1, String class2, FLogicEnvironment oenv) {
        OntoObject c1, c2;

        if (!ont.containsKey(class1)) {
            return false;
        } else {
            c1 = getOnt().get(class1);
        }

        if (!ont.containsKey(class2)) {
            return false;
        } else {
            c2 = getOnt().get(class2);
        }

        if (c1 == c2) {
            return true;
        }

        oenv.getHistory().clear();
        oenv.getSearched().clear();
        // seed with local equivalent classes
        // for (int i = 0; i < c1.getEqList().size(); i++) {
        // n = c1.getEqList().get(i);
        for (OntoObject n : c1.getEqList()) {
            oenv.getHistory().add(n);
            oenv.getSearched().set(n.getNodeId());
        }
        while (oenv.getHistory().size() > 0) {
            c1 = oenv.getHistory().get(0);
            oenv.getHistory().remove(0);
            if (c1 == c2) {
                return true;
            } else {
                // for (int i = 0; i < c1.getEqList().size(); i++) {
                // n = c1.getEqList().get(i);
                for (OntoObject n : c1.getEqList()) {
                    if (!oenv.getSearched().get(n.getNodeId())) {
                        oenv.getHistory().add(n);
                        oenv.getSearched().set(n.getNodeId());
                    }
                }
            }
        }

        return false;
    }

    /**
     *
     * @param child
     * @param parent
     * @param oenv
     * @return
     */
    public boolean isa(String child, String parent, FLogicEnvironment oenv) {
        OntoObject c, p;
        int i;

        if (!ont.containsKey(child)) {
            return false;
        } else {
            c = getOnt().get(child);
        }

        if (!ont.containsKey(parent)) {
            return false;
        } else {
            p = getOnt().get(parent);
        }

        if (c == p) {
            return true;
        }

        // init
        oenv.getHistory().clear();
        oenv.getSearched().clear();
        // seed with local super & equivalent classes
        // for (i = 0; i < c.getSupList().size(); i++) {
        // n = c.getSupList().get(i);
        for (OntoObject n : c.getSupList()) {
            oenv.getHistory().add(n);
            oenv.getSearched().set(n.getNodeId());
        }
        // for (i = 0; i < c.getEqList().size(); i++) {
        // n = c.getEqList().get(i);
        for (OntoObject n : c.getEqList()) {
            oenv.getHistory().add(n);
            oenv.getSearched().set(n.getNodeId());
        }
        while (oenv.getHistory().size() > 0) {
            OntoObject n = oenv.getHistory().get(0);
            oenv.getHistory().remove(0);
            if (n == p) {
                return true;
            } else {
                for (OntoObject a : n.getEqList()) {
                    // if (n.getEqList().size() > 0) { // search equivalence
                    // links
                    // for (i = 0; i < n.getEqList().size(); i++) {
                    // a = n.getEqList().get(i);
                    if (!oenv.getSearched().get(a.getNodeId())) {
                        oenv.getHistory().add(a);
                        oenv.getSearched().set(a.getNodeId());
                    }
                    // }
                }
                for (OntoObject a : n.getSupList()) {
                    // if (n.getSupList().size() > 0) { // search the super
                    // class links
                    // for (i = 0; i < n.getSupList().size(); i++) {
                    // a = n.getSupList().get(i);
                    if (!oenv.getSearched().get(a.getNodeId())) {
                        oenv.getHistory().add(a);
                        oenv.getSearched().set(a.getNodeId());
                    }
                    // }
                }
            }
        }

        return false;
    }

    public boolean isa(String child, List<String> parents, FLogicEnvironment oenv) {
        BitSet parentBits = new BitSet(FLogicConstants.MAX_ISA_CACHE);
        Set<Integer> parentIds = new HashSet<Integer>();

        OntoObject c = getOnt().get(child);
        if (c == null) {
            return false;
        }

        // get the parent ids
        for (String ps : parents) {
            OntoObject p = getOnt().get(ps);
            if (p == null)
                continue;
            if (c.getNodeId() == p.getNodeId())
                return true;
            parentIds.add(p.getNodeId());
            if (getClassesToCache().containsKey(p.getNodeId())) {
                parentBits.set(getClassesToCache().get(p.getNodeId()));
            }
        }

        if (parentIds.isEmpty())
            return false;

        if (c.getIsaCache().intersects(parentBits))
            return true;

        // init Ontology_Environment
        oenv.getHistory().clear();
        oenv.getSearched().clear();
        // seed with local super & equivalent classes
        c.collectSupsToList(oenv.getHistory(), true);
        for (OntoObject n : oenv.getHistory()) {
            oenv.getSearched().set(n.getNodeId());
        }
        while (oenv.getHistory().size() > 0) {
            OntoObject n = oenv.getHistory().remove(0);
            if (parentIds.contains(n.getNodeId())) {
                if (classesToCache.containsKey(n.getNodeId())) {
                    c.getIsaCache().set(classesToCache.get(n.getNodeId()));
                }
                return true;
            }
            if (n.getIsaCache().intersects(parentBits)) {
                c.getIsaCache().or(n.getIsaCache()); // update the isa cache
                return true;
            }
            List<OntoObject> scanList = new ArrayList<OntoObject>();
            n.collectSupsToList(scanList, true); // include equivalence links
            for (OntoObject a : scanList) {
                if (!oenv.getSearched().get(a.getNodeId())) {
                    oenv.getHistory().add(a);
                    oenv.getSearched().set(a.getNodeId());
                }
            }
        }

        return false;
    }

    /**
     *
     * @param child
     * @param parent
     * @param oenv
     * @return
     */
    public boolean instance(String child, String parent, FLogicEnvironment oenv) {
        ArrayList<Integer> TS;
        boolean ret;

        TS = new ArrayList<Integer>();
        getInstanceAssertions(child, ":", parent, FLogicConstants.DEFAULT_GRAPH, TS, oenv);
        if (TS.size() > 0) {
            ret = true;
        } else {
            ret = false;
        }
        TS.clear();
        TS = null;
        return ret;
    }

    /**
     *
     * @param className
     * @param oenv
     */
    public void getAncestors(String className, FLogicEnvironment oenv) {
        int i;
        OntoObject cls;

        oenv.getHistory().clear();
        oenv.getSearched().clear();
        oenv.getAncs().clear();
        if (getOnt().containsKey(className)) {
            cls = getOnt().get(className);
            oenv.getHistory().add(cls);
            oenv.getSearched().set(cls.getNodeId());
            while (oenv.getHistory().size() > 0) {
                cls = oenv.getHistory().get(0);
                oenv.getHistory().remove(0);
                oenv.getAncs().add(cls);
                // for (i = 0; i < cls.getSupList().size(); i++) {
                // conCls = cls.getSupList().get(i);
                for (OntoObject conCls : cls.getSupList()) {
                    if (!oenv.getSearched().get(conCls.getNodeId())) {
                        oenv.getHistory().add(conCls);
                        oenv.getSearched().set(conCls.getNodeId());
                    }
                }
                // for (i = 0; i < cls.getEqList().size(); i++) {
                // conCls = cls.getEqList().get(i);
                for (OntoObject conCls : cls.getEqList()) {
                    if (!oenv.getSearched().get(conCls.getNodeId())) {
                        oenv.getHistory().add(conCls);
                        oenv.getSearched().set(conCls.getNodeId());
                    }
                }
            }
        }
        // The first entry should be the class for 'className'
        oenv.getAncs().remove(0);
        return;
    }

    public void getDescendants(String className, FLogicEnvironment oenv) {
        int i;
        OntoObject cls;

        oenv.getHistory().clear();
        oenv.getSearched().clear();
        oenv.Decs.clear();
        if (getOnt().containsKey(className)) {
            cls = getOnt().get(className);
            oenv.getHistory().add(cls);
            oenv.getSearched().set(cls.getNodeId());
            while (oenv.getHistory().size() > 0) {
                cls = oenv.getHistory().get(0);
                oenv.getHistory().remove(0);
                oenv.Decs.add(cls);
                for (OntoObject conCls : cls.getSubList()) {
                    if (!oenv.getSearched().get(conCls.getNodeId())) {
                        oenv.getHistory().add(conCls);
                        oenv.getSearched().set(conCls.getNodeId());
                    }
                }
                for (OntoObject conCls : cls.getEqList()) {
                    if (!oenv.getSearched().get(conCls.getNodeId())) {
                        oenv.getHistory().add(conCls);
                        oenv.getSearched().set(conCls.getNodeId());
                    }
                }
            }
        }
        // The first entry should be the class for 'className'
        if (oenv.Decs.size() > 0)
            oenv.Decs.remove(0);
        return;
    }

    /**
     *
     * @param searchGraph
     * @param dataGraph
     * @return
     */
    public boolean graphMatch(String searchGraph, String dataGraph) {
        if (!searchGraph.equals(FLogicConstants.DEFAULT_GRAPH)) {
            if (!dataGraph.equals(FLogicConstants.DEFAULT_GRAPH)) {
                if (!searchGraph.startsWith("?") && !dataGraph.equals(searchGraph)) {
                    return false;
                }
            } else if (!searchGraph.startsWith("?")) {
                return false;
            }
        }
        return true;
    }

    // Determine if subject has on assertion of pred with arity
    // Scan the subject index and compare the predicate labels
    /**
     *
     * @param sub
     * @param pred
     * @param arity
     * @param oenv
     * @return
     */
    public boolean hasAssertion(String sub, String pred, int arity, FLogicEnvironment oenv) {
        N_ary assrt;
        boolean hasAssrt;
        Iterator it;
        int idx;

        hasAssrt = false;
        it = oenv.getKB().getArgsIndex()[0].getValueIterator(sub);
        while (it.hasNext()) {
            idx = (Integer) it.next();
            assrt = oenv.getKB().getAssertion(idx);
            if (assrt.getArity() == arity) {
                if (assrt.getPred().equals(pred) || isa(assrt.getPred(), pred, oenv)) {
                    hasAssrt = true;
                    break;
                }
            }
        }
        return hasAssrt;
    }

    // Query the knowledge base
    // Find triples that satisfy the constrains ?sub[?pred->?obj] or ?sub:?obj
    // if pred=":"
    // pred = "::" is not allowed in the knowledge base
    /**
     *
     * @param sub
     * @param pred
     * @param obj
     * @param graph
     * @param TS
     * @param oenv
     */
    public void getInstanceAssertions(String sub, String pred, String obj, String graph, List<Integer> ts,
                                      FLogicEnvironment oenv) {
        if (!pred.equals(":") && !pred.equals("<:")) {
            oenv.getArgs()[0] = sub;
            oenv.getArgs()[1] = obj;
            // getAssertions(pred, 2, oenv.args, graph, TS, oenv);
        } else {
            // scan all ':' assertions
            if (obj.startsWith("?")) { // Object is bound to a class
                getInstanceAssertionsUnboundObject(sub, pred, obj, graph, ts, oenv);
            } else if (!obj.startsWith("?")) { // Object is unbounded -> need to
                // find all ancestors
                getInstanceAssertionsBoundObject(sub, pred, obj, graph, ts, oenv);
            }
        }
    }

    private void getInstanceAssertionsBoundObject(String sub, String pred, String obj, String graph, List<Integer> TS,
            FLogicEnvironment oenv) {
        boolean match;
        N_ary assrt;
        OntoObject c, n, a;
        String searchPred = null;
        List<Integer> vals = new ArrayList<Integer>();

        if (pred.equals("<:") || (pred.equals(":"))) {
            searchPred = ":";
        }

        // Query the KB 1st
        if (!sub.startsWith("?")) {
            if (oenv.getKB().getArgsIndex()[0].getIndexValues(sub) != null) {
                vals.addAll(oenv.getKB().getArgsIndex()[0].getIndexValues(sub));
            }
        } else {
            if (oenv.getKB().getPredIndex().getIndexValues(searchPred) != null) {
                vals.addAll(oenv.getKB().getPredIndex().getIndexValues(searchPred));
            }
        }
        for (int idx : vals) {
            assrt = oenv.getKB().getAssertion(idx);
            match = graphMatch(graph, assrt.getGraph());
            if (match && assrt.getPred().equals(":") && pred.equals(":") && isa(assrt.getArgs()[1], obj, oenv)) {
                TS.add(idx);
            } else if (match && assrt.getPred().equals(":")
                       && (assrt.getRecordType() == FLogicConstants.DIRECT_ASSERTION
                           || assrt.getRecordType() == FLogicConstants.RULE_INFERENCE)
                       && pred.equals("<:") && sameAs(assrt.getArgs()[1], obj, oenv)) {
                TS.add(idx);
            }
        }

        vals.clear();
        // Query the Ontology 2nd
        if (!sub.startsWith("?")) {
            if (getKB().getArgsIndex()[0].getIndexValues(sub) != null) {
                vals.addAll(getKB().getArgsIndex()[0].getIndexValues(sub));
            }
        } else {
            if (getKB().getPredIndex().getIndexValues(searchPred) != null) {
                vals.addAll(getKB().getPredIndex().getIndexValues(searchPred));
            }
        }
        for (int idx : vals) {
            assrt = getKB().getAssertion(idx);
            match = graphMatch(graph, assrt.getGraph());

            if (match && assrt.getPred().equals(":") && pred.equals(":") && isa(assrt.getArgs()[1], obj, oenv)) {
                oenv.getArgs()[0] = assrt.getArgs()[0];
                oenv.getArgs()[1] = obj;
                oenv.getKB().addAssertion(":", 2, oenv.getArgs(), FLogicConstants.INFERRED_ASSERTION);
                TS.add(oenv.getKB().lastAssertionIdx);
            } else if (match && assrt.getPred().equals(":") && pred.equals("<:")
                       && sameAs(assrt.getArgs()[1], obj, oenv)) {
                oenv.getArgs()[0] = assrt.getArgs()[0];
                oenv.getArgs()[1] = obj;
                oenv.getKB().addAssertion(":", 2, oenv.getArgs(), FLogicConstants.INFERRED_ASSERTION);
                TS.add(oenv.getKB().lastAssertionIdx);
            }
        }
    }

    private void getInstanceAssertionsUnboundObject(String sub, String pred, String obj, String graph, List<Integer> TS,
            FLogicEnvironment oenv) {
        boolean match;
        N_ary assrt;
        OntoObject cls;
        HashSet<Integer> ans;
        String searchPred = null;
        List<Integer> vals = new ArrayList<Integer>();
        searchPred = ":";

        ans = new HashSet<Integer>();

        // Query the KB 1st
        if (!sub.startsWith("?")) {
            vals.addAll(oenv.getKB().getArgsIndex()[0].getIndexValues(sub));
        } else {
            vals.addAll(oenv.getKB().getPredIndex().getIndexValues(searchPred));
        }
        for (int idx : vals) {
            assrt = oenv.getKB().getAssertion(idx);
            match = graphMatch(graph, assrt.getGraph());
            if (match && assrt.getPred().equals(":") && pred.equals(":")) {
                ans.add(idx);
                getAncestors(assrt.getArgs()[1], oenv); // get all ancestors of
                // assrt.obj
                for (int i = 0; i < oenv.getAncs().size(); i++) {
                    cls = oenv.getAncs().get(i);
                    oenv.getArgs()[0] = assrt.getArgs()[0];
                    oenv.getArgs()[1] = cls.getName();
                    oenv.getKB().addAssertion(":", 2, oenv.getArgs(), FLogicConstants.INFERRED_ASSERTION);
                    ans.add(oenv.getKB().lastAssertionIdx);
                }
            } else if ((assrt.getRecordType() == FLogicConstants.DIRECT_ASSERTION
                        || assrt.getRecordType() == FLogicConstants.RULE_INFERENCE) && assrt.getPred().equals(":")
                       && pred.equals("<:")) {
                ans.add(idx);
            }
        }

        // Query the Ontology 2nd
        vals.clear();
        if (getPredIndex().getIndexValues(searchPred) != null) {
            vals.addAll(getPredIndex().getIndexValues(searchPred));
        }
        for (int idx : vals) {
            assrt = getKB().getAssertion(idx);
            match = graphMatch(graph, assrt.getGraph());
            if (match && assrt.getPred().equals(":") && pred.equals(":")) {
                if (sub.startsWith("?") || assrt.getArgs()[0].equals(sub)) {
                    oenv.getKB().addAssertion(":", 2, assrt.getArgs(), FLogicConstants.INFERRED_ASSERTION);
                    ans.add(oenv.getKB().lastAssertionIdx);
                    getAncestors(assrt.getArgs()[1], oenv);
                    for (int i = 0; i < oenv.getAncs().size(); i++) {
                        cls = oenv.getAncs().get(i);
                        oenv.getArgs()[0] = assrt.getArgs()[0];
                        oenv.getArgs()[1] = cls.getName();
                        oenv.getKB().addAssertion(":", 2, oenv.getArgs(), FLogicConstants.INFERRED_ASSERTION);
                        ans.add(oenv.getKB().lastAssertionIdx);
                    }
                }
            } else if (match && assrt.getPred().equals(":") && pred.equals("<:")) {
                if (sub.startsWith("?") || assrt.getArgs()[0].equals(sub)) {
                    oenv.getKB().addAssertion(":", 2, assrt.getArgs(), FLogicConstants.DIRECT_ASSERTION);
                    ans.add(oenv.getKB().lastAssertionIdx);
                }
            }
        }

        // Save the results
        TS.addAll(ans);
        ans.clear();
    }

    // April 8, 2016
    // Merwyn Taylor MITRE: Added to pull data from MongoDB
    public class MGRunnable implements Runnable {
        private String pred;
        private String args[];
        private int arity;
        private String graph;
        private FLogicEnvironment oenv;

        MGRunnable(String pred, String args[], int arity, String graph, FLogicEnvironment oenv) {
            this.pred = pred;
            this.args = args;
            this.arity = arity;
            this.graph = graph;
            this.oenv = oenv;
        }

        @Override
        public void run() {
            long sttm = System.currentTimeMillis();
            getLocalDBAssertions(pred, args, arity, graph, oenv);
            long edtm = System.currentTimeMillis();
            mgClient.Tim = edtm - sttm;
        }

        public void getLocalDBAssertions(String pred, String args[], int arity, String graph, FLogicEnvironment oenv) {
            String sub = args[0];
            String obj = args[1];

            mgClient.results.clear();
            // Search the Local Database
            if (arity == 2 && !(sub.startsWith("?") && obj.startsWith("?"))) { // sub
                // or
                // obj
                // must
                // be
                // constrained
                if (pred.equals(":") && !obj.startsWith("?"))
                    mgClient.getData(sub, pred, oenv.SemTypes); // instance
                // logic already
                // applied to
                // oenv.SemTypes
                else
                    mgClient.getData(sub, pred, obj);
            } else if (arity == 3 && pred.equals("instanceFilter")) {
                String filterObj = args[2];
                // instance filter logic already applied to oenv.SemTypes
                mgClient.results.clear();
                mgClient.getData(sub, ":", oenv.SemTypes);
                int cnt = mgClient.results.size();
                for (int a = 0; a < cnt; a++) {
                    ArrayList<String> res = mgClient.results.get(a);
                    ArrayList<String> resCpy = new ArrayList<String>();
                    resCpy.add(res.get(0));
                    resCpy.add("instanceFilter");
                    resCpy.add(obj);
                    resCpy.add(filterObj);
                    mgClient.results.add(resCpy);
                }
            }
        }
    }


    public void instanceInference(String pred, String args[], int arity, FLogicEnvironment flenv) {
        String obj = args[1];
        String filterObj = args[2];

        flenv.SemTypes.clear();

        if (pred.equals(":") && !obj.startsWith("?")) {
            getDescendants(obj, flenv);
            flenv.SemTypes.add(obj);
            for (OntoObject oo : flenv.Decs)
                flenv.SemTypes.add(oo.getName());
        }

        if (pred.equals("instanceFilter")) {
            // Get all subclasses to pontentially include
            getDescendants(obj, flenv);
            HashSet<String> SemTypesHS = new HashSet<String>();
            SemTypesHS.add(obj);
            for (OntoObject oo : flenv.Decs)
                SemTypesHS.add(oo.getName());

            // Get all subclasses to exclude
            getDescendants(filterObj, flenv);
            for (OntoObject oo : flenv.Decs)
                if (SemTypesHS.contains(oo.getName()))
                    SemTypesHS.remove(oo.getName());
            if (SemTypesHS.contains(filterObj))
                SemTypesHS.remove(filterObj);

            for (String s : SemTypesHS)
                flenv.SemTypes.add(s);
        }
    }

    public void getExternalAssertions(String pred, String args[], int arity, String graph, FLogicEnvironment oenv) {

        Future mgFuture = null;
        boolean mgDone;
        String[] new_args = new String[FLogicConstants.MAX_ARITY];
        long st, ed;
        int runningThreadCnt;

        // Try the EMS
        runningThreadCnt = 0;

        if (pred.equals("instanceFilter") || pred.equals(":"))
            instanceInference(pred, args, arity, oenv);

        st = System.currentTimeMillis();
        pyClient.getData(pred, args, oenv); // This needs to block since it uses
        // the reasoner
        ed = System.currentTimeMillis();
        oenv.pyTim += esClient.Tim + (ed - st);

        mgFuture = executor.submit(new MGRunnable(pred, args, arity, graph, oenv));
        runningThreadCnt++;

        mgDone = false;

        while (runningThreadCnt > 0) {
            // Try MongoDB client
            if (!mgDone && mgFuture.isDone()) {
                st = System.currentTimeMillis();
                for (int a = 0; a < mgClient.results.size(); a++) {
                    ArrayList<String> res = mgClient.results.get(a);
                    new_args[0] = res.get(0);
                    new_args[1] = res.get(2);
                    for (int i = 3; i < res.size() && i - 1 < FLogicConstants.MAX_ARITY; i++)
                        new_args[i - 1] = res.get(i);
                    oenv.getKB().addAssertion(res.get(1), res.size() - 1, new_args, FLogicConstants.DEFAULT_GRAPH,
                                              FLogicConstants.DIRECT_ASSERTION);
                }
                ed = System.currentTimeMillis();
                oenv.mgTim += mgClient.Tim + (ed - st);
                mgDone = true;
                runningThreadCnt--;
            }

        }
    }

    public void clearKB(FLogicEnvironment oenv) {
        if (oenv.executionMode == FLogicConstants.FULL_MODE) {
            ;
        }
        oenv.clear();
        oenv.clearInferred();
    }

    /**
     *
     * @param pred
     * @param args
     * @param arity
     * @param graph
     * @param TS
     * @param oenv
     */
    public void getAssertions(String pred, String args[], int arity, String graph, ArrayList<Integer> TS,
                              FLogicEnvironment oenv) {

        if (pred.equals("FreeTextSearch") || pred.equals("RegexTextSearch"))
            oenv.getKB().fts.clear();

        if (oenv.executionMode == FLogicConstants.FULL_MODE)
            getExternalAssertions(pred, args, arity, graph, oenv);

        if (pred.equals("FreeTextSearch") || pred.equals("RegexTextSearch")) {
            for (int i = 0; i < oenv.getKB().fts.size(); i++)
                TS.add(i);
            return;
        }

        // proceed
        if (pred.equals(":") || pred.equals("<:")) {
            getInstanceAssertions(args[0], pred, args[1], graph, TS, oenv);
            return;
        }

        if (pred.equals("<::")) {
            getDirectSubclassAssertions(args[0], pred, args[1], graph, TS, oenv);
            return;
        }

        if (pred.equals("::")) {
            getSubClass(pred, arity, graph, args, TS, oenv);
            return;
        }

        if (pred.startsWith("?")) {
            getAssertionsUnboundPred(pred, args, arity, graph, TS, oenv);
            return;
        }

        // search for all other patterns
        getAssertionsBoundPred(pred, args, arity, graph, TS, oenv);
    }

    private void getSubClass(String pred, int arity, String graph, String[] args, ArrayList<Integer> TS,
                             FLogicEnvironment oenv) {

        Set<Integer> ans = new HashSet<Integer>();

        if (!args[0].startsWith("?") && !args[1].startsWith("?")) { // child ::
            // parent
            if (isa(args[0], args[1], oenv)) {
                oenv.getArgs()[0] = args[0];
                oenv.getArgs()[1] = args[1];
                oenv.getKB().addAssertion(pred, 2, oenv.getArgs(), FLogicConstants.INFERRED_ASSERTION);
                ans.add(oenv.getKB().lastAssertionIdx);
            }
        }

        TS.addAll(ans);
    }

    /**
     *
     * @param sub
     * @param pred
     * @param obj
     * @param graph
     * @param TS
     * @param oenv
     */
    public void getDirectSubclassAssertions(String sub, String pred, String obj, String graph, ArrayList<Integer> TS,
                                            FLogicEnvironment oenv) {
        HashSet<Integer> ans;
        Iterator it;
        int i;

        ans = new HashSet<Integer>();

        if (!sub.startsWith("?") && obj.startsWith("?")) { // sub <:: ?obj
            if (getOnt().containsKey(sub)) {
                OntoObject cls = getOnt().get(sub);
                oenv.getArgs()[0] = sub;
                // for (i = 0; i < cls.getSupList().size(); i++) {
                // par = cls.getSupList().get(i);
                for (OntoObject par : cls.getSupList()) {
                    oenv.getArgs()[1] = par.getName();
                    oenv.getKB().addAssertion(pred, 2, oenv.getArgs(), FLogicConstants.INFERRED_ASSERTION);
                    ans.add(oenv.getKB().lastAssertionIdx);
                }
            }
        } else if (sub.startsWith("?") && !obj.startsWith("?")) { // ?sub <::
            // obj
            if (getOnt().containsKey(obj)) {
                OntoObject par = getOnt().get(obj);
                oenv.getArgs()[1] = obj;
                // for (i = 0; i < par.getSubs().size(); i++) {
                // cls = par.getSubs().get(i);
                for (OntoObject cls : par.getSubList()) {
                    oenv.getArgs()[0] = cls.getName();
                    oenv.getKB().addAssertion(pred, 2, oenv.getArgs(), FLogicConstants.INFERRED_ASSERTION);
                    ans.add(oenv.getKB().lastAssertionIdx);
                }
            }
        }
        // ?sub <:: ?obj is not supported
        // Save the results
        TS.addAll(ans);
    }

    // Search the kb for assertions on at least one value given that the
    // predicate is unbound
    // May 30, 2012 : As writtent, this does not implement property inheritance
    /**
     *
     * @param pred
     * @param args
     * @param arity
     * @param graph
     * @param TS
     * @param oenv
     */
    public void getAssertionsUnboundPred(String pred, String args[], int arity, String graph, ArrayList<Integer> TS,
                                         FLogicEnvironment oenv) {
        boolean match;
        int indexedArg;
        N_ary assrt;
        HashSet<Integer> ansDirect;
        List<String> history = new ArrayList<String>();
        Set<String> searched = new HashSet<String>();
        ansDirect = new HashSet<Integer>();
        String indexSearchVal = null;

        // Must have at least one argument bound to a value
        indexedArg = -1;
        List<String> keys = new ArrayList<String>();
        keys.addAll(oenv.getKB().getPredIndex().getKeyValues());
        for (int i = 0; i < arity; i++) {
            if (!args[i].startsWith("?")) {
                indexedArg = i;
                history.add(args[i]);
                searched.add(args[i]);
            }
        }

        while (true) {
            if (indexedArg != -1) {
                if (!history.isEmpty()) {
                    indexSearchVal = history.remove(0);
                    keys.clear();
                    keys.add(indexSearchVal);
                } else {
                    break;
                }
            }
            for (String key : keys) {
                HashSet<Integer> vals;
                if (indexedArg == -1)
                    vals = oenv.getKB().getPredIndex().getIndexValues(key);
                else
                    vals = oenv.getKB().getArgsIndex()[indexedArg].getIndexValues(key);
                if (vals != null)
                    for (int idx : vals) {
                        assrt = oenv.getKB().getAssertion(idx);
                        if (assrt.getArity() == arity) {
                            match = graphMatch(graph, assrt.getGraph());
                            for (int i = 0; i < arity && match; i++) {
                                if (i != indexedArg) {
                                    if (!args[i].startsWith("?") && !semanticMatch(assrt.getArgs()[i], args[i], oenv)) {
                                        match = false;
                                    }
                                }
                            }
                            if (match) {
                                ansDirect.add(idx);
                            }
                        }
                    }
            }
            if (indexedArg == -1) {
                break;
            }
            updateEquivalenceChain(indexSearchVal, history, searched, graph, oenv);
        }

        // Save the results
        TS.addAll(ansDirect);
    }

    /**
     *
     * @param pred
     * @param args
     * @param arity
     * @param graph
     * @param TS
     * @param oenv
     */
    public void getAssertionsBoundPred(String pred, String args[], int arity, String graph, ArrayList<Integer> TS,
                                       FLogicEnvironment oenv) {
        boolean usePredIndex, match;
        int idx, indexedArg;
        Iterator it;
        HashSet<Integer> ansDirect, ansMInherit, ansNMInherit;
        ArrayList<String> namesOfClassesWithPred;
        N_ary assrt = null;
        String indexSearchVal = null;

        ansDirect = new HashSet<Integer>();
        ansMInherit = new HashSet<Integer>();
        ansNMInherit = new HashSet<Integer>();
        namesOfClassesWithPred = new ArrayList<String>();
        List<String> history = new ArrayList<String>();
        Set<String> searched = new HashSet<String>();

        usePredIndex = true;
        if (!oenv.isMaterialized(pred, arity)) {
            if (getOnt().containsKey(pred)) {
                OntoObject prop = getOnt().get(pred);
                if (prop.getSubs().size() > 0) {
                    usePredIndex = false;
                }
            }
        }

        // search the knowledge base
        indexedArg = -1;
        boolean fromPredIndex = true;
        // it = oenv.getKB().getPredIndex().iterator();
        List<String> keys = new ArrayList<String>();
        keys.addAll(oenv.getKB().getPredIndex().getKeyValues());

        for (int i = 0; i < arity && indexedArg == -1; i++) {
            if (!args[i].startsWith("?")) {
                indexedArg = i;
                // it = oenv.getKB().argsIndex[i].get(args[i]);
                history.add(args[i]);
                searched.add(args[i]);
            }
        }

        if (indexedArg == -1 && usePredIndex && !pred.startsWith("?")) {
            keys.clear();
            keys.add(pred);
        }

        while (true) {
            if (indexedArg != -1) {
                if (!history.isEmpty()) {
                    indexSearchVal = history.remove(0);
                    keys.clear();
                    keys.add(indexSearchVal);
                    fromPredIndex = false;
                } else {
                    break;
                }
            }
            for (String key : keys) {
                HashSet<Integer> vals;
                if (fromPredIndex) {
                    vals = oenv.getKB().getPredIndex().getIndexValues(key);
                } else {
                    vals = oenv.getKB().getArgsIndex()[indexedArg].getIndexValues(key); // TODO
                    // what
                    // does
                    // this
                    // do?
                }
                if (vals != null) {
                    for (int aIdx : vals) {
                        assrt = oenv.getKB().getAssertion(aIdx);
                        if (assrt.getArity() == arity) {
                            match = graphMatch(graph, assrt.getGraph());
                            if (match && indexedArg != -1) // check the
                                // predicates
                            {
                                if (!assrt.getPred().equals(pred) && !isa(assrt.getPred(), pred, oenv)) {
                                    match = false;
                                }
                            }
                            for (int i = 0; i < arity && match; i++) {
                                if (i != indexedArg) {
                                    if (!args[i].startsWith("?") && !semanticMatch(assrt.getArgs()[i], args[i], oenv)) {
                                        match = false;
                                    }
                                }
                            }
                            if (match) {
                                ansDirect.add(aIdx);
                            }
                        }
                    }
                }
            }

            // Search the ontology and perform inheritance
            // Inherit only to the instances
            keys.clear();
            if (usePredIndex) {
                keys.add(pred);
                // it = getKB().getPredIndex().get(pred);
            } else {
                keys.addAll(getPredIndex().getKeyValues());
                // it = getKB().getPredIndex().iterator();
            }
            for (String key : keys) {
                HashSet<Integer> vals = getPredIndex().getIndexValues(key);
                if (vals != null) {
                    for (int kIdx : vals) {
                        assrt = getKB().getAssertion(kIdx); // get the assertion
                        // Filter
                        if (assrt.getArity() == arity) {
                            match = graphMatch(graph, assrt.getGraph());
                            if (match && (assrt.getPred().equals(pred) || isa(assrt.getPred(), pred, oenv))) {
                                // Need to know which classes have an applicable
                                // assertion
                                namesOfClassesWithPred.add(assrt.getArgs()[0]);
                            } else {
                                match = false;
                            }
                            for (int i = 1; i < arity && match; i++) {
                                if (!args[i].startsWith("?") && !semanticMatch(assrt.getArgs()[i], args[i], oenv)) {
                                    match = false;
                                }
                            }
                            if (match) {
                                // save the assertion from the ontology to the
                                // kb
                                oenv.getKB().addAssertion(pred, arity, assrt.getArgs(),
                                                          FLogicConstants.INFERRED_ASSERTION);
                                if (args[0].startsWith("?")) {
                                    if (assrt.getInhSem() == FLogicConstants.MONOTONIC) {
                                        ansMInherit.add(oenv.getKB().lastAssertionIdx);
                                    } else if (assrt.getInhSem() == FLogicConstants.NON_MONOTONIC) {
                                        ansNMInherit.add(oenv.getKB().lastAssertionIdx);
                                    }
                                    ansDirect.add(oenv.getKB().lastAssertionIdx);
                                } else if (!args[0].startsWith("?") && assrt.getArgs()[0].equals(args[0])) // subject
                                    // is
                                    // a
                                    // value
                                {
                                    ansDirect.add(oenv.getKB().lastAssertionIdx);
                                } else {
                                    if (assrt.getInhSem() == FLogicConstants.MONOTONIC) {
                                        ansMInherit.add(oenv.getKB().lastAssertionIdx);
                                    } else if (assrt.getInhSem() == FLogicConstants.NON_MONOTONIC) {
                                        ansNMInherit.add(oenv.getKB().lastAssertionIdx);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (indexedArg == -1) {
                break;
            }
            updateEquivalenceChain(indexSearchVal, history, searched, graph, oenv);
        }

        // Perform monotonic inheritance 1st before non-monotonic inheritance
        // to determine if there are any instances that will inherit values
        // that should not be altered
        if (ansMInherit.size() > 0) {
            applyMonotonicInheritance(args[0], pred, arity, ansDirect, ansMInherit, oenv);
        }
        if (ansNMInherit.size() > 0) {
            applyNonMonotonicInheritance(args[0], pred, arity, namesOfClassesWithPred, ansDirect, ansNMInherit, oenv);
        }

        // Save the results
        it = ansDirect.iterator();
        while (it.hasNext()) {
            TS.add((Integer) it.next());
        }
    }

    // Apply inheritance method to the assertions in ansInherit
    // Save the inferences in ansDirect

    private void applyMonotonicInheritance(String instance, String pred, int arity, HashSet<Integer> ansDirect,
                                           HashSet<Integer> ansInherit, FLogicEnvironment oenv) {
        int i, j, idx;
        Iterator it;
        ArrayList<Integer> instances;
        N_ary assrt, inst;

        instances = new ArrayList<Integer>();

        it = ansInherit.iterator();
        while (it.hasNext()) {
            idx = (Integer) it.next();
            assrt = oenv.getKB().getAssertion(idx);
            instances.clear();
            // get instancs of assrt.sub
            getInstanceAssertions(instance, ":", assrt.getArgs()[0], FLogicConstants.DEFAULT_GRAPH, instances, oenv);
            for (i = 0; i < instances.size(); i++) {
                idx = instances.get(i);
                inst = oenv.getKB().getAssertion(idx);
                oenv.getArgs()[0] = inst.getArgs()[0];
                for (j = 1; j < arity; j++) {
                    oenv.getArgs()[j] = assrt.getArgs()[j];
                }
                oenv.getKB().addAssertion(pred, arity, oenv.getArgs(), FLogicConstants.INFERRED_ASSERTION);
                ansDirect.add(oenv.getKB().lastAssertionIdx);
            }
        }
    }

    // Apply inheritance method to the assertions in ansInherit
    // Save the inferences in ansDirect
    private void applyNonMonotonicInheritance(String instance, String pred, int arity,
            ArrayList<String> namesOfClassesWithPred, HashSet<Integer> ansDirect, HashSet<Integer> ansInherit,
            FLogicEnvironment oenv) {
        BitSet classesWithPred = new BitSet(FLogicConstants.MAX_ONTOLOGY_CLASS_CNT);
        N_ary assrt;
        int[] distances;
        OntoObject cls;
        StringIndex classAnsInherit_index = new StringIndex(); // Map string ->
        // int
        char index; // the index that is used
        InheritedAssertion ia, ia_ins;

        // Mark all classes that have a value asserted
        Iterator<Integer> it = ansInherit.iterator();
        while (it.hasNext()) {
            int idx = it.next();
            assrt = oenv.getKB().getAssertion(idx);
            if (getOnt().containsKey(assrt.getArgs()[0])) {
                cls = getOnt().get(assrt.getArgs()[0]);
                classesWithPred.set(cls.getNodeId());
                classAnsInherit_index.addToIndex(cls.getName(), idx);
            }
        }
        for (int i = 0; i < namesOfClassesWithPred.size(); i++) {
            if (getOnt().containsKey(namesOfClassesWithPred.get(i))) {
                cls = getOnt().get(namesOfClassesWithPred.get(i));
                classesWithPred.set(cls.getNodeId());
            }
        }

        if (!instance.startsWith("?")) {
            it = oenv.getKB().getArgsIndex()[0].getValueIterator(instance);
            index = 'S';
        } else { // iterate through all instance assertions
            it = oenv.getKB().getPredIndex().getValueIterator(":");
            index = 'P';
        }
        distances = new int[FLogicConstants.MAX_ONTOLOGY_CLASS_CNT];
        Map<String, List<InheritedAssertion>> IAs = new LinkedHashMap<String, List<InheritedAssertion>>();
        while (it.hasNext()) {
            int idx = it.next();
            assrt = oenv.getKB().getAssertion(idx);
            if ((index == 'S' && assrt.getPred().equals(":")) || index == 'P') {
                if (!hasAssertion(assrt.getArgs()[0], pred, arity, oenv)) { // assertion
                    // on
                    // sub
                    // using
                    // pred?
                    // find the class
                    if (getOnt().containsKey(assrt.getArgs()[1])) {
                        cls = getOnt().get(assrt.getArgs()[1]);
                        // follow paths to top of ontology
                        oenv.getHistory().clear();
                        oenv.getSearched().clear();
                        for (int i = 0; i < FLogicConstants.MAX_ONTOLOGY_CLASS_CNT; i++) {
                            distances[i] = 0;
                        }
                        // seed with local super & equivalent classes
                        oenv.getHistory().add(cls);
                        distances[cls.getNodeId()] = 1;
                        // find the 1st class in the classesWithPred list --
                        // this is the closest class
                        while (oenv.getHistory().size() > 0) {
                            cls = oenv.getHistory().get(0);
                            oenv.getHistory().remove(0);
                            if (classesWithPred.get(cls.getNodeId())) {
                                if (classAnsInherit_index.containsKey(cls.getName())
                                        && !classAnsInherit_index.getIndexValues(cls.getName()).isEmpty()) {
                                    ia = new InheritedAssertion();
                                    ia.setInstanceLbl(assrt.getArgs()[0]);
                                    ia.setClassLbl(cls.getName());
                                    ia.setDistFromSrcClass(distances[cls.getNodeId()]);
                                    if (!IAs.containsKey(ia.getInstanceLbl())) {
                                        IAs.put(ia.getInstanceLbl(), new ArrayList<InheritedAssertion>());
                                    }
                                    IAs.get(ia.getInstanceLbl()).add(ia);
                                }
                            } else {
                                // for (int i = 0; i < cls.getEqList().size();
                                // i++) {
                                // pcls = cls.getEqList().get(i);
                                for (OntoObject pcls : cls.getEqList()) {
                                    if (!oenv.getSearched().get(pcls.getNodeId())) {
                                        oenv.getHistory().add(pcls);
                                        oenv.getSearched().set(pcls.getNodeId());
                                        distances[pcls.getNodeId()] = distances[pcls.getNodeId()] + 1;
                                    }
                                }

                                for (OntoObject pcls : cls.getSupList()) {
                                    if (!oenv.getSearched().get(pcls.getNodeId())) {
                                        oenv.getHistory().add(pcls);
                                        oenv.getSearched().set(pcls.getNodeId());
                                        distances[pcls.getNodeId()] = distances[pcls.getNodeId()] + 1;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // assert inferences
        ia = new InheritedAssertion();
        ia.setInstanceLbl("zzzz");
        if (!IAs.containsKey("zzzz")) {
            IAs.put("zzzz", new ArrayList<InheritedAssertion>());
        }
        IAs.get("zzzz").add(ia);
        boolean first = true;
        String oldInstanceLbl = null;
        int minDistance = 0;
        for (String key : IAs.keySet()) {
            if (first) {
                ia = IAs.get(key).get(0);
                oldInstanceLbl = ia.getInstanceLbl();
                minDistance = ia.getDistFromSrcClass();
            }
            Iterator<InheritedAssertion> iait = IAs.get(key).iterator();
            while (iait.hasNext()) {
                ia = iait.next();
                if (!ia.getInstanceLbl().equals(oldInstanceLbl)) { // apply this
                    // assertion
                    Iterator<InheritedAssertion> iaita = IAs.get(oldInstanceLbl).iterator();
                    while (iaita.hasNext()) {
                        ia_ins = iaita.next(); // instance ia
                        if (ia_ins.getDistFromSrcClass() == minDistance) { // apply
                            // this
                            // assertion
                            // get assertions for this class
                            it = classAnsInherit_index.getValueIterator(ia_ins.getClassLbl());
                            while (it.hasNext()) {
                                int idx = it.next();
                                assrt = oenv.getKB().getAssertion(idx);
                                oenv.getArgs()[0] = oldInstanceLbl;
                                for (int j = 1; j < assrt.getArity(); j++) {
                                    oenv.getArgs()[j] = assrt.getArgs()[j];
                                }
                                oenv.getKB().addAssertion(assrt.getPred(), assrt.getArity(), oenv.getArgs(),
                                                          FLogicConstants.INFERRED_ASSERTION);
                                ansDirect.add(oenv.getKB().lastAssertionIdx);
                            }
                        }
                    }
                    // prepare for next instance
                    minDistance = ia.getDistFromSrcClass();
                    oldInstanceLbl = ia.getInstanceLbl();
                } else {
                    minDistance = Math.min(minDistance, ia.getDistFromSrcClass());
                }
            }
        }

        // clear temporary space
        IAs.clear();
        classAnsInherit_index.clear();
    }

    public void loadClassesToCache(String fname) {
        int bitIdx = 0;

        File f = new File(fname);
        try {
            BufferedReader bf = new BufferedReader(new FileReader(f));
            String str;
            while ((str = bf.readLine()) != null) {
                if (getOnt().containsKey(str)) {
                    getClassesToCache().put(getOnt().get(str).getNodeId(), bitIdx++);
                }
                if (bitIdx >= FLogicConstants.MAX_ISA_CACHE) {
                    break;
                }
            }
            bf.close();
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
        }
    }

    public void init(File ontologyDir) throws IOException {
        File initFile = new File(ontologyDir, "ontology.init");
        if (!initFile.exists()) {
            initFile = new File(ontologyDir, "reasoner.init");
        }
        try {
            BufferedReader br = new BufferedReader(new FileReader(initFile));
            String line = null;
            String currentSection = null;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("Section:")) {
                    String section = line.substring(8).trim();
                    currentSection = section;
                    continue;
                }
                if (line.startsWith("EndSection")) {
                    currentSection = null;
                    continue;
                }

                line = line.trim();

                if ("ontology".equals(currentSection)) {
                    loadOntology(new File(ontologyDir, line));
                }

            }
            br.close();
        } catch (IOException ioe) {
            logger.error(ioe.getMessage(), ioe);
        }
    }

    /// Load content from F-Logic Expressions
    /**
     *
     * @param fname
     */
    public void loadOntology(File file) {
        FLogicDriver driver = new FLogicDriver();
        driver.setFlengine(this);
        driver.setOenv(null);
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            driver.parse(in);
        } catch (Exception e) {
            logger.error("Error while loading file: " + file.getAbsolutePath(), e);
        } finally {
            try {
                in.close();
            } catch (Throwable t) {}
        }
    }

    // Modified by Elmer
    public void loadOntology(String ontologies) {
        InputStream in = new ByteArrayInputStream(ontologies.getBytes(StandardCharsets.UTF_8));
        FLogicDriver driver = new FLogicDriver();
        driver.setFlengine(this);
        driver.setOenv(null);
        try {
            driver.parse(in);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param fname
     * @param oenv
     */
    public void loadKB(File file, FLogicEnvironment oenv) {
        FLogicDriver driver = new FLogicDriver();
        driver.setFlengine(this);
        driver.setOenv(oenv);
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            driver.parse(in);
        } catch (Exception e) {
            logger.error("Error while loading file: " + file.getAbsolutePath(), e);
        } finally {
            try {
                in.close();
            } catch (Throwable t) {
            }
        }
    }

    /**
     *
     * @param stmt
     * @param oenv
     */
    public void loadStatement(String stmt, FLogicEnvironment oenv) {
        FLogicDriver driver = new FLogicDriver();
        driver.setFlengine(this);
        driver.setOenv(oenv);
        try {
            driver.parse(stmt);
        } catch (Exception e) {
            logger.error("Error while loading statement: " + stmt, e);
        }
    }

    /**
     *
     * @param goalExpr
     * @param oenv
     * @return
     */
    public Goal parseGoal(String goalExpr, FLogicEnvironment oenv) {
        if (oenv == null) {
            return null;
        }

        FLogicDriver driver = new FLogicDriver();
        driver.setFlengine(this);
        driver.setOenv(oenv);
        try {
            driver.parse(goalExpr);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error while parsing expression: " + goalExpr, e);
        }
        ;
        if (oenv.getGoals().size() > 0) {
            Goal g = oenv.getGoals().get(0);
            oenv.getGoals().clear();
            return g;
        }
        return null;
    }

    /**
     *
     * @param subLabel
     * @param predLabel
     * @param objLabel
     * @param inhSem
     * @param graph
     * @param oenv
     */
    public void addToOntology(String subLabel, String predLabel, String objLabel, String inhSem, String graph,
                              FLogicEnvironment oenv) {
        OntoObject sub, obj;

        if (predLabel.equals("::") || // subClassOf
                predLabel.equals(":->:")) { // subPropertyOf
            sub = getOntologyConcept(subLabel);
            obj = getOntologyConcept(objLabel);
            sub.addSup(obj);
            obj.addSub(sub);
        } else if (predLabel.equals(":=:") && oenv == null) { // equivalence
            sub = getOntologyConcept(subLabel);
            obj = getOntologyConcept(objLabel);
            sub.addEq(obj);
            obj.addEq(sub);
        } else {
            if (oenv == null) {
                addTriple(subLabel, predLabel, objLabel, inhSem);
            } else {
                // Pattern func_expr = Pattern.compile("(.+)\\((.+)\\)");
                String[] args = new String[FLogicConstants.MAX_ARITY];
                int argCnt = 2;
                args[0] = subLabel;
                args[1] = objLabel;
                // Matcher matches = func_expr.matcher(predLabel);
                Matcher matches = FUNC_EXPR.matcher(predLabel);
                if (matches.find()) {
                    predLabel = matches.group(1);
                    String predArgs = matches.group(2);
                    String[] tmpArgs = predArgs.split(",");
                    String T = "";
                    for (int i = 0; i < tmpArgs.length && i < FLogicConstants.MAX_ARITY - 2; i++) {
                        Matcher matches_attr = ATTR_EXPR.matcher(tmpArgs[i]);
                        if (matches_attr.find()) {
                            if (T.length() == 0)
                                T = "\"" + matches_attr.group(1) + "\":\"" + matches_attr.group(2) + "\"";
                            else
                                T = T + ", \"" + matches_attr.group(1) + "\":\"" + matches_attr.group(2) + "\"";
                        } else
                            args[argCnt++] = tmpArgs[i];
                    }
                    if (T.length() > 1)
                        args[argCnt++] = "{\"T\":{" + T + "}}";
                }
                oenv.getKB().addAssertion(predLabel, argCnt, args, graph, FLogicConstants.DIRECT_ASSERTION);
            }
        }
    }

    public void addTriple(String sub, String pred, String obj, String inhSem) {
        N_ary assertion = new N_ary();
        assertion.setPred(pred);
        assertion.getArgs()[0] = sub;
        assertion.getArgs()[1] = obj;
        assertion.setArity(2);

        if (inhSem.equals("->")) {
            assertion.setInhSem(FLogicConstants.NOT_INHERITABLE);
        } else if (inhSem.equals("*m->")) {
            assertion.setInhSem(FLogicConstants.MONOTONIC);
        } else {
            assertion.setInhSem(FLogicConstants.NON_MONOTONIC);
        }
        assertion.setGraph(FLogicConstants.DEFAULT_GRAPH);
        int idx = getKB().kb.size();
        getKB().kb.add(assertion);

        getPredIndex().addToIndex(pred, idx);
    }

    /**
     *
     * @param pred_label
     * @param inhSem
     * @param domain
     * @param range
     * @param oenv
     */
    public void addPredicateToOntology(String pred_label, String inhSem, String domain, ArrayList<String> range,
                                       FLogicEnvironment oenv) {
        OntoObject pred;

        pred = getOntologyConcept(pred_label);
        pred.setInhSem(inhSem);
        pred.getDomain().add(domain);
        for (int i = 0; i < range.size(); i++) {
            pred.getRange().add(range.get(i));
        }
    }

    public void addPredicateSemantics(String pred, String infSemantics, String pred_inv) {
        if (infSemantics.equals("symmetric"))
            SymmetricPredicates.add(pred);
        else if (infSemantics.equals("transitive"))
            TransitivePredicates.add(pred);
        else if (infSemantics.equals("inverseOf")) {
            InverseOf.put(pred, pred_inv);
            InverseOf.put(pred_inv, pred);
        }
    }

    /**
     *
     * @param subLabel
     * @param predLabel
     * @param objLabel
     * @param graph
     * @param oenv
     */
    public void delFromOntology(String subLabel, String predLabel, String objLabel, String graph,
                                FLogicEnvironment oenv) {
        if (predLabel.equals("::") || predLabel.equals(":=:")) {
            OntoObject sub, obj;

            sub = getOntologyConcept(subLabel);
            obj = getOntologyConcept(objLabel);
            if (predLabel.equals("::")) {
                sub.delSup(obj); // retract child -> parent
                obj.delSub(sub); // retract parent -> child
            } else {
                // retract sub :=: obj
                sub.delEq(obj);
                obj.delEq(sub);
            }
        }
        if (oenv != null) {
            Pattern func_expr = Pattern.compile("(.+)\\((.+)\\)");
            String[] args = new String[FLogicConstants.MAX_ARITY];
            int argCnt = 2;
            args[0] = subLabel;
            args[1] = objLabel;
            Matcher matches = func_expr.matcher(predLabel);
            if (matches.find()) {
                predLabel = matches.group(1);
                String predArgs = matches.group(2);
                String[] tmpArgs = predArgs.split(",");
                for (int i = 0; i < tmpArgs.length && i < FLogicConstants.MAX_ARITY - 2; i++) {
                    args[i + 2] = tmpArgs[i];
                    argCnt++;
                }
            }
            if (oenv != null) {
                oenv.getKB().delAssertion(predLabel, argCnt, args);
            }
        }
    }

    /// Rule Engine
    /**
     *
     * @param head
     * @param body
     */
    public void loadFLogicRule(Expression head, Expression body) {
        HornClause hc = new HornClause(head, body);
        hc.id = ontoRules.size();
        hc.updateVariableNames();
        ontoRules.add(hc);
        getOntoRulesIndex().addToIndex(hc.head.getPredicate().getLabel(), hc.id);
    }

    /**
     *
     * @param clauses
     * @param oenv
     */
    public void addGoal(Expression clauses, FLogicEnvironment oenv) {
        Goal g = new Goal(clauses);
        oenv.getGoals().add(g);
    }

    /// Backward chaining rule engine
    private boolean unifiable(Atom a1, Atom a2) {
        if (a1.getType() == FLogicConstants.VAR || a2.getType() == FLogicConstants.VAR) {
            return true;
        }
        if (a1.getLabel().equals(a2.getLabel())) {
            return true;
        }
        return false;
    }

    // Unify rule head with a literal from a goal
    // ArrayList<String> queryvars,
    private void unify(Literal literal, Literal hcliteral, BindingSet bindSet, FLogicEnvironment oenv) {
        int b, c, i, j;
        String exprs[][], predLabel;
        boolean done, subst;

        exprs = new String[2][FLogicConstants.MAX_ARITY];

        if (literal.getPredicate().getLabel().equals("<:")) {
            predLabel = ":";
        } else {
            predLabel = literal.getPredicate().getLabel();
        }
        bindSet.clear();
        // predicate must match
        if ((hcliteral.getPredicate().getLabel().equals(predLabel)
                || isa(hcliteral.getPredicate().getLabel(), predLabel, oenv))
                && hcliteral.getArity() == literal.getArity()) {
            // copy literals
            for (i = 0; i < literal.getArity(); i++) {
                exprs[0][i] = literal.getArgs()[i].getLabel();
                exprs[1][i] = hcliteral.getArgs()[i].getLabel();
            }
            // copy named graphs
            exprs[0][i] = literal.getGraph().getLabel();
            exprs[1][i] = hcliteral.getGraph().getLabel();

            done = false;
            subst = false;
            c = 0;
            while (!done) {
                subst = false;
                if (!exprs[0][c].equals(exprs[1][c])) { // different
                    if (predLabel.equals("FreeTextSearch") || predLabel.equals("RegexTextSearch")
                            || exprs[0][c].startsWith("?") || exprs[1][c].startsWith("?")) {
                        bindSet.addBinding(exprs[1][c], exprs[0][c]);
                        subst = true;
                    } else {
                        bindSet.clear();
                        done = true;
                    }
                } else {
                    c++;
                }
                if (subst) {
                    b = bindSet.getSize() - 1; // last binding added
                    for (i = 0; i < 2; i++) {
                        for (j = 0; j < literal.getArity() + 1; j++) {
                            if (bindSet.getDestination()[b].equals(exprs[i][j])) {
                                exprs[i][j] = bindSet.getSource()[b];
                            }
                        }
                    }
                    c = 0;
                }
                if ((c == (literal.getArity() + 1)) && !subst) {
                    done = true;
                }
            }
        }
        bindSet.reduce();
    }

    // Unify with knowledge base
    // Special case unify variables with data
    private void unify(Literal literal, N_ary assertion, BindingSet bindSet, FLogicEnvironment oenv) {
        int i, j;
        String predLabel;

        if (literal.getPredicate().getLabel().equals("<:")) {
            predLabel = ":";
        } else {
            predLabel = literal.getPredicate().getLabel();
        }
        bindSet.clear();
        if (predLabel.startsWith("?") || predLabel.equals(assertion.getPred())
                || isa(assertion.getPred(), predLabel, oenv)) {
            if (literal.getArity() > 1 && literal.getArgs()[0].getLabel().equals(literal.getArgs()[1].getLabel())) {
                if (assertion.getArgs()[0].equals(assertion.getArgs()[1])) // sub
                    // ==
                    // obj
                {
                    bindSet.addBinding(literal.getArgs()[0].getLabel(), assertion.getArgs()[0]);
                }
            } else {
                for (i = 0; i < literal.getArity(); i++) {
                    if (literal.getArgs()[i].getType() == FLogicConstants.VAR) {
                        bindSet.addBinding(literal.getArgs()[i].getLabel(), assertion.getArgs()[i]);
                    }
                }
            }
            if (literal.getGraph().getType() == FLogicConstants.VAR) {
                bindSet.addBinding(literal.getGraph().getLabel(), assertion.getGraph());
            }
            if (literal.getPredicate().getType() == FLogicConstants.VAR) {
                bindSet.addBinding(literal.getPredicate().getLabel(), assertion.getPred());
            }
        }
        if (bindSet.getSize() > 0) {// apply attributes
            for (Map.Entry me : literal.atts.entrySet()) {
                String attr = (String) me.getKey();
                Atom atm = (Atom) me.getValue();
                // If the assertion has the attr then bind
                String attr_val = assertion.getAttribute(attr);
                if (attr_val != null)
                    bindSet.addBinding(atm.getLabel(), attr_val);
            }
        }
        bindSet.reduce();
    }

    private Tuple createNewTuple(Goal goal, BindingSet bindSet, ArrayList<String> variables) {
        int i, j;
        Tuple t;
        boolean fnd;
        String value;

        // create a new tuple
        t = new Tuple();

        fnd = true;
        for (i = 0; i < variables.size() && fnd; i++) {
            // look for the ith variable in the bindings
            fnd = false;
            value = goal.getBindSet().getBinding(variables.get(i));
            if (value.length() > 0 && !value.startsWith("?")) {
                t.getCol()[i] = value;
                fnd = true;
            } else {
                value = bindSet.getBinding(variables.get(i));
                if (value.length() > 0 && !value.startsWith("?")) {
                    t.getCol()[i] = value;
                    fnd = true;
                }
            }
        }
        if (!fnd) {
            t = null;
            return null;
        }

        return t;
    }

    private ArrayList<String> produceTrace(Goal curGoal, String hisSrc, FLogicEnvironment oenv) {
        ArrayList<String> Trace = produceTrace(curGoal, oenv);
        if (hisSrc.startsWith("D")) {
            int assrt_id = Integer.parseInt(hisSrc.substring(1));
            N_ary assrt = oenv.getKB().getAssertion(assrt_id);
            Trace.add(assrt.toString());
        }
        return Trace;
    }

    private ArrayList<String> produceTrace(Goal curGoal, FLogicEnvironment oenv) {
        ArrayList<String> Trace = new ArrayList<String>();
        for (int i = 0; i < curGoal.getHistory().size(); i++) {
            String hisSrc = curGoal.getHistory().get(i);
            if (hisSrc.startsWith("D")) {
                int assrt_id = Integer.parseInt(hisSrc.substring(1));
                N_ary assrt = oenv.getKB().getAssertion(assrt_id);
                Trace.add(assrt.toString());
            } else if (hisSrc.startsWith("R")) {
                int rule_id = Integer.parseInt(hisSrc.substring(1));
                HornClause rule = ontoRules.get(rule_id);
                Trace.add(rule.toString());
            }
        }
        return Trace;
    }

    private void tableResults(Goal origGoal, Tuple tuple, ArrayList<String> queryvars, FLogicEnvironment oenv) {
        String args[], predLabel, graphLabel;
        int c, a, v;
        Literal lit;
        boolean varFnd = false;

        if (tuple == null) {
            return;
        }

        args = new String[FLogicConstants.MAX_ARITY];
        // Find an expression in the original query
        for (c = 0; c < origGoal.getClauses().size(); c++) {
            lit = origGoal.getClauses().get(c);
            graphLabel = FLogicConstants.DEFAULT_GRAPH;
            // don't save negated literals and literals expressed on system
            // predicates
            if (lit.getPredicate().getType() != FLogicConstants.VAR && !lit.isNegated()
                    && !systemPreds.contains(lit.getPredicate().getLabel())) {
                // Find bindings for this expression
                for (a = 0; a < lit.getArity(); a++) {
                    if (lit.getArgs()[a].getType() == FLogicConstants.VAR) {
                        varFnd = false;
                        for (v = 0; v < queryvars.size(); v++) {
                            if (queryvars.get(v).equals(lit.getArgs()[a].getLabel())) {
                                args[a] = tuple.getCol()[v];
                                varFnd = true;
                            }
                        }
                    } else {
                        args[a] = lit.getArgs()[a].getLabel();
                    }
                }
                if (lit.getGraph().getType() == FLogicConstants.VAR) {
                    for (v = 0; v < queryvars.size(); v++) {
                        if (queryvars.get(v).equals(lit.getGraph().getLabel())) {
                            graphLabel = tuple.getCol()[v];
                        }
                    }
                } else {
                    graphLabel = lit.getGraph().getLabel();
                }
                if (varFnd) {
                    if (lit.getPredicate().getLabel().equals("<:")) {
                        predLabel = ":";
                    } else {
                        predLabel = lit.getPredicate().getLabel();
                    }
                    oenv.getKB().addAssertion(predLabel, lit.getArity(), args, graphLabel,
                                              FLogicConstants.INFERRED_ASSERTION);
                }
            }
        }
    }

    private void solveViaKB(Goal origGoal, Goal curGoal, GoalStack GS, ResultSet RS, FLogicEnvironment oenv) {
        int i, id;
        String pred, searchArgs[];
        N_ary assertion;
        String extSrc;
        Tuple tpl;
        Literal literal;
        BindingSet bindSet;

        literal = curGoal.getClauses().get(0);
        searchArgs = new String[FLogicConstants.MAX_ARITY];
        bindSet = new BindingSet();

        if (curGoal.getRuleMode() == FLogicConstants.RULE_MODE_RULES_ONLY) {
            curGoal.getDataChoicePoints().setCalcChoicePoints(true);
        }
        if (!curGoal.getDataChoicePoints().isCalcChoicePoints()) {
            // get a list of tuples that unify with this literal
            for (i = 0; i < literal.getArity(); i++) {
                searchArgs[i] = literal.getArgs()[i].getLabel();
            }
            oenv.curGoal = curGoal;
            getAssertions(literal.getPredicate().getLabel(), searchArgs, literal.getArity(),
                          literal.getGraph().getLabel(), curGoal.getDataChoicePoints().getChoicePoints(), oenv);
            curGoal.getDataChoicePoints().setCalcChoicePoints(true);
            if (literal.getArgs()[0].getType() == FLogicConstants.VALUE
                    && literal.getArgs()[1].getType() == FLogicConstants.VALUE
                    && curGoal.getDataChoicePoints().getChoicePoints().size() > 0) {
                // don't search the rules
                curGoal.getRuleChoicePoints().setCalcChoicePoints(true);
            }
        }
        pred = literal.getPredicate().getLabel();
        while (curGoal.getDataChoicePoints().getChoicePoints().size() > 0) {
            // remove choice point on top
            id = curGoal.getDataChoicePoints().getChoicePoints().remove(0);
            assertion = oenv.getKB().getAssertion(id, pred);
            unify(literal, assertion, bindSet, oenv); // should always unify
            // merge bindings with current goal bindings
            if (pred.equals("RegexTextSearch") || pred.equals("FreeTextSearch"))
                extSrc = "T" + id;
            else
                extSrc = "D" + id;
            if (curGoal.getClauses().size() > 1) {
                addNewGoal(GS, curGoal.deriveNewGoal(extSrc, bindSet, RS, oenv), oenv);
            } else { // save answer
                tpl = createNewTuple(curGoal, bindSet, RS.getVariables());
                tableResults(origGoal, tpl, RS.getVariables(), oenv);
                RS.addTuple(tpl);
            }
            oenv.setSkiprest(true);
        }
    }

    private void solveViaRules(Goal origGoal, Goal curGoal, GoalStack GS, ResultSet RS, FLogicEnvironment oenv,
                               boolean linearTabling) {
        int i, id;
        HornClause hc;
        Literal literal;
        BindingSet bindSet;
        String extSrc;
        Tuple tpl;
        boolean allVarsBound;

        literal = curGoal.getClauses().get(0);
        bindSet = new BindingSet();

        if (!curGoal.getRuleChoicePoints().isCalcChoicePoints()) {
            if (!linearTabling) {
                // get a list of rules for which the rule heads unify with the
                // literal
                // is this a fully tabled literal
                if (!oenv.isMaterialized(literal.getPredicate().getLabel(), literal.getArity())) {
                    getRules(literal, curGoal.getRuleChoicePoints().getChoicePoints(), oenv);
                }
            } else {
                if (!GS.isRecursiveCall(curGoal)) {
                    getRules(literal, curGoal.getRuleChoicePoints().getChoicePoints(), oenv);
                    StringBuffer sb = new StringBuffer();
                    sb.append(literal.getPredicate().getLabel());
                    sb.append("/");
                    sb.append(literal.getArity());
                    if (tabledPreds.contains(sb.toString()))
                        GS.addNewTabledGoal(literal); // only added if it is not
                    // subsumed by an
                    // existing goal
                }
            }
            curGoal.getRuleChoicePoints().setCalcChoicePoints(true);
        }
        if (curGoal.getRuleChoicePoints().getChoicePoints().size() > 0) {
            // remove choice point on top
            id = curGoal.getRuleChoicePoints().getChoicePoints().remove(0);
            hc = ontoRules.get(id);
            // unify(literal, hc.head, bindSet, RS.getVariables(), oenv);
            unify(literal, hc.head, bindSet, oenv);
            if (hc.body.size() > 0 || curGoal.getClauses().size() > 1) {
                extSrc = "R" + id;
                boolean added = addNewGoal(GS, curGoal.deriveNewGoal(hc, extSrc, bindSet, RS, systemPreds, oenv), oenv);
            } else if (curGoal.getClauses().size() == 1) { // save answer
                allVarsBound = true;
                for (i = 0; i < bindSet.getSize() && allVarsBound; i++) {
                    if (bindSet.getSource()[i].startsWith("?"))
                        allVarsBound = false;
                }
                if (allVarsBound) {
                    tpl = createNewTuple(curGoal, bindSet, RS.getVariables());
                    if (GS.isLT)
                        tableResults(origGoal, tpl, RS.getVariables(), oenv);
                    RS.addTuple(tpl);
                }
                if (literal.getArgs()[0].getType() == FLogicConstants.VALUE
                        && literal.getArgs()[1].getType() == FLogicConstants.VALUE) {
                    curGoal.getRuleChoicePoints().getChoicePoints().clear();
                }
            }
            oenv.setSkiprest(true);
        }
    }

    private HashSet<String> evaluateAggregate(Literal ag_lit, FLogicEnvironment oenv) {
        int i;
        ResultSet AG_RS = new ResultSet();
        Goal AG_Goal = new Goal();
        for (i = 0; i < ag_lit.getDisjuncts().size(); i++) {
            Literal AG_Lit = new Literal(ag_lit.getDisjuncts().get(i));
            AG_Goal.getClauses().add(AG_Lit);
        }
        GoalStack AG_GS = prepareGoal(AG_Goal, AG_RS, oenv);
        solveGoal(AG_Goal, AG_RS, AG_GS, oenv);
        // collect distinct variable values
        HashSet<String> distinctVals = new HashSet<String>();
        for (i = 0; i < AG_RS.getTuples().size(); i++) {
            String varRes = AG_RS.getValue(i, ag_lit.aggregateVariable);
            distinctVals.add(varRes);
        }

        return distinctVals;
    }

    private void evaluateSystemPredicate(Goal origGoal, Goal curGoal, GoalStack GS, ResultSet RS,
                                         FLogicEnvironment oenv) {
        Literal lit;
        String pred;
        BindingSet bindSet;
        Tuple tpl;
        int i;
        N_ary nary;
        ArrayList<N_ary> preds;
        boolean eval = false; // system predicate evaluation defaults to false
        boolean cont;

        /// Init
        bindSet = new BindingSet();
        lit = curGoal.getClauses().get(0);

        pred = lit.getPredicate().getLabel();
        // != //
        if (pred.equals("!=")) {
            // subject & object must be bound
            if (lit.getArgs()[0].getType() == FLogicConstants.VALUE
                    && lit.getArgs()[1].getType() == FLogicConstants.VALUE) {
                if (!lit.getArgs()[0].getLabel().equals(lit.getArgs()[1].getLabel())) {
                    eval = true;
                }
            }
            GS.invalidateGoal(curGoal); // don't evaluate any further
        } // str_find
        else if (pred.equals("str_find")) {
            if (lit.getArgs()[0].getType() == FLogicConstants.VALUE
                    && lit.getArgs()[1].getType() == FLogicConstants.VALUE) {
                if (lit.getArgs()[0].getLabel().indexOf(lit.getArgs()[1].getLabel()) != -1)
                    eval = true;
            }
            GS.invalidateGoal(curGoal);
        } // str_remove
        else if (pred.equals("str_remove")) {
            nary = new N_ary();
            nary.setPred(lit.getPredicate().getLabel());
            for (i = 0; i < 3; i++)
                nary.getArgs()[i] = lit.getArgs()[i].getLabel();
            nary.getArgs()[1] = nary.getArgs()[0].replaceAll(nary.getArgs()[2], "");
            nary.setArity(3);
            unify(lit, nary, bindSet, oenv);
            eval = true;
            GS.invalidateGoal(curGoal); // don't evaluate any further
        } // _setof_ //
        else if (pred.equals("setof")) {
            HashSet<String> distinctVals = evaluateAggregate(lit, oenv);
            String Vars = "";
            for (String dv : distinctVals) {
                if (Vars.length() == 0)
                    Vars = dv;
                else
                    Vars = Vars.concat("," + dv);
            }
            nary = new N_ary();
            nary.setPred("setof");
            nary.getArgs()[0] = "[" + Vars + "]";
            nary.setArity(1);
            unify(lit, nary, bindSet, oenv);
            eval = true;
            GS.invalidateGoal(curGoal); // don't evaluate any further
        } // lengthof //
        else if (pred.equals("lengthof")) {
            HashSet<String> distinctVals = evaluateAggregate(lit, oenv);
            nary = new N_ary();
            nary.setPred("lengthof");
            nary.getArgs()[0] = "" + distinctVals.size() + "";
            nary.setArity(1);
            unify(lit, nary, bindSet, oenv);
            eval = true;
            GS.invalidateGoal(curGoal); // don't evaluate any further
        } // == //
        else if (lit.getPredicate().getLabel().equals("==")) {
            // subject & object must be bound
            if (lit.getArgs()[0].getType() == FLogicConstants.VALUE
                    && lit.getArgs()[1].getType() == FLogicConstants.VALUE) {
                if (lit.getArgs()[0].getLabel().equals(lit.getArgs()[1].getLabel())) {
                    eval = true;
                }
            }
            GS.invalidateGoal(curGoal); // don't evaluate any further
        } // < or > //
        else if (lit.getPredicate().getLabel().equals("<") || lit.getPredicate().getLabel().equals(">")) {
            // subject & object must be bound
            if (lit.getArgs()[0].getType() == FLogicConstants.VALUE
                    && lit.getArgs()[1].getType() == FLogicConstants.VALUE) {
                // comparing numbers
                float f_arg0 = 0, f_arg1 = 0;
                cont = true;
                try {
                    f_arg0 = Float.valueOf(lit.getArgs()[0].getLabel()).floatValue();
                    f_arg1 = Float.valueOf(lit.getArgs()[1].getLabel()).floatValue();
                } catch (Exception e) {
                    cont = false;
                }
                if (cont) {
                    if (lit.getPredicate().getLabel().equals("<")) {
                        if (f_arg0 < f_arg1) {
                            eval = true;
                        }
                    }
                    if (lit.getPredicate().getLabel().equals(">")) {
                        if (f_arg0 > f_arg1) {
                            eval = true;
                        }
                    }
                }
            }
            GS.invalidateGoal(curGoal); // don't evaluate any further
        } // ontoMap // only valid for use if args0,1 are values
        else if (lit.getPredicate().getLabel().equals("ontoMap")) {
            // subject & object must be bound
            if (lit.getArgs()[0].getType() == FLogicConstants.VALUE
                    && lit.getArgs()[1].getType() == FLogicConstants.VALUE) {
                if (isa(lit.getArgs()[0].getLabel(), lit.getArgs()[1].getLabel(), oenv)
                        || instance(lit.getArgs()[0].getLabel(), lit.getArgs()[1].getLabel(), oenv)) {
                    eval = true;
                }
            }
            GS.invalidateGoal(curGoal); // don't evaluate any further
        } // fail //
        else if (lit.getPredicate().getLabel().equals("fail")) {
            // this goal fails and the goal that it refers to fails
            GS.invalidateGoal(lit.getNegBTGoal());
            GS.invalidateGoal(curGoal.getId());
        } // materialize //
        else if (lit.getPredicate().getLabel().equals("table")) {
            if (lit.getArgs()[1].getLabel().equals("true")) {
                for (i = 0; i < curGoal.getClauses().size(); i++) {
                    lit = curGoal.getClauses().get(i);
                    if (lit.getPredicate().getLabel().equals("table")) {
                        // mark this predicate for tabling
                        setTabledPred(lit.getArgs()[0].getLabel(), Integer.parseInt(lit.getArgs()[2].getLabel()));
                    }
                }
            }
            GS.invalidateGoal(curGoal); // don't evaluate any further
        } else if (TempReas.Comps.contains(lit.getPredicate().getLabel())) {
            if (lit.getArgs()[0].getType() == FLogicConstants.VALUE
                    && lit.getArgs()[1].getType() == FLogicConstants.VALUE)
                eval = TempReas.callPred(lit.getPredicate().getLabel(), lit.getArgs()[0].getLabel(),
                                         lit.getArgs()[1].getLabel());
            GS.invalidateGoal(curGoal); // don't evaluate any further
        }

        // Save Results //
        if (eval == true) {
            if (curGoal.getClauses().size() > 1) {
                addNewGoal(GS, curGoal.deriveNewGoal("", bindSet, RS, oenv), oenv);
            } else { // save answer
                tpl = createNewTuple(curGoal, bindSet, RS.getVariables());
                tableResults(origGoal, tpl, RS.getVariables(), oenv);
                RS.addTuple(tpl);
            }
        }
    }

    public void evaluate(String query, ResultSet RS, FLogicEnvironment flenv) {
        Goal g = parseGoal(query, flenv);
        if (g != null) {
            RS.query = query;
            evaluate(g, RS, flenv);
        }
    }

    // function to evaluate a query
    /**
     *
     * @param origGoal
     * @param RS
     * @param oenv
     */
    public void evaluate(Goal origGoal, ResultSet RS, FLogicEnvironment oenv) {
        if (oenv.getKB().kb.size() > FLogicConstants.MAX_KB_SIZE)
            clearKB(oenv);

        /// Cache taxonomy rules ///
        if (!oenv.isTaxonomyRulesCached()) {
            applyTaxonomyRules(oenv);
        }

        if (SymmetricPredicates.size() + TransitivePredicates.size() + InverseOf.size() > 0)
            createPredicateSemantics();

        // rewrite the goal
        // rewriteGoal(origGoal, oenv);

        // solve the goal
        oenv.mgTim = 0;
        oenv.esTim = 0;
        oenv.pyTim = 0;
        GoalStack GS = prepareGoal(origGoal, RS, oenv);
        solveGoal(origGoal, RS, GS, oenv);

        if (GS.tabPred.length() > 0) { // materialized predicates
            oenv.setMaterialized(GS.tabPred, GS.tabPredArity);
        }

        // save if in stream mode
    }

    public class MatchDecision {
        public int pc, oc;
        public BindingSet bindings;
        public ArrayList<Literal> literalsToDelete;

        public MatchDecision(int pc, int oc, BindingSet bSet, ArrayList<Literal> listToDel) {
            this.pc = pc;
            this.oc = oc;
            bindings = new BindingSet();
            bindings.append(bSet);
            literalsToDelete = new ArrayList<Literal>();
            for (Literal lit : listToDel)
                literalsToDelete.add(lit);
        }
    }

    // May 20, 2015 : Match portions of a goal and replace with an optimal
    // pattern
    // : Needs to support backtracking
    // May 21, 2015 : Backtracking add
    private void rewriteGoal(Goal origGoal, FLogicEnvironment oenv) {
        int pc = 0, pcLim = 0, oc = 0, ocLim = 0;
        MatchDecision mDec;
        Goal cpGoal = null; // copy of original pattern
        BindingSet bindSet = new BindingSet();
        BindingSet finalBindSet = new BindingSet();
        ArrayList<Literal> literalsToDelete = new ArrayList<Literal>();
        LinkedList<MatchDecision> mdStack = new LinkedList<MatchDecision>();
        ArrayList<MatchDecision> finalMatches = new ArrayList<MatchDecision>();
        try {
            JSONArray patternList = (JSONArray) QueryPatterns.get("Patterns");
            for (int p = 0; p < patternList.size(); p++) {
                JSONObject Pattern = (JSONObject) patternList.get(p);
                String pattern = (String) Pattern.get("pattern");
                if (pattern.length() > 0)
                    pattern = "?- " + pattern;
                Goal pGoal = parseGoal(pattern, oenv);
                finalBindSet.clear();
                literalsToDelete.clear();
                mdStack.clear();
                boolean match = true;
                boolean contRewrite = true;
                if (pGoal == null)
                    contRewrite = false;
                else {
                    pc = 0;
                    pcLim = pGoal.getClauses().size();
                    oc = 0;
                    ocLim = origGoal.getClauses().size();
                    cpGoal = new Goal();
                    for (Literal lit : pGoal.getClauses())
                        cpGoal.getClauses().add(new Literal(lit));
                }
                while (contRewrite) {
                    Literal pLit = cpGoal.getClauses().get(pc); // match this
                    // literal
                    Literal oLit = origGoal.getClauses().get(oc);
                    match = false;
                    if (oLit.isNegated() == pLit.isNegated()) {
                        unify(oLit, pLit, bindSet, oenv);
                        if (bindSet.getSize() > 0) {
                            // save state for backtracking
                            mDec = new MatchDecision(pc, oc, finalBindSet, literalsToDelete);
                            mdStack.addFirst(mDec);

                            // update state
                            finalBindSet.append(bindSet);
                            for (int i = pc; i < cpGoal.getClauses().size(); i++)
                                cpGoal.getClauses().get(i).substituteBindings(bindSet);
                            literalsToDelete.add(oLit);
                            match = true;
                            if (pc == pcLim - 1) {
                                mDec = new MatchDecision(pc, oc, finalBindSet, literalsToDelete);
                                finalMatches.add(mDec); // save new completed
                                // goal
                            }
                        }
                    }
                    if (match) {
                        oc = ocLim;
                    } // start next pattern clause
                    else
                        oc++; // move forward
                    while (true) {
                        if (oc >= ocLim) {
                            oc = 0;
                            pc++;
                        }
                        if (pc >= pcLim) {
                            if (mdStack.size() > 0) { // backtrack
                                mDec = mdStack.pollFirst();
                                pc = mDec.pc;
                                oc = mDec.oc + 1;
                                finalBindSet.clear();
                                finalBindSet.append(mDec.bindings);
                                cpGoal.getClauses().clear();
                                for (Literal lit : pGoal.getClauses())
                                    cpGoal.getClauses().add(lit);
                            } else {
                                contRewrite = false;
                                break;
                            }
                        } else
                            break;
                    }
                }
                if (finalMatches.size() > 0) {
                    MatchDecision md = finalMatches.get(0);
                    String optimalPattern = (String) Pattern.get("optimal_pattern");
                    pGoal = parseGoal("?- " + optimalPattern, oenv);
                    for (int i = 0; i < pGoal.getClauses().size(); i++) {
                        pGoal.getClauses().get(i).substituteBindings(md.bindings);
                        origGoal.getClauses().add(pGoal.getClauses().get(i));
                    }
                    for (Literal lit : md.literalsToDelete) {
                        for (int l = 0; l < origGoal.getClauses().size(); l++) {
                            Literal oLit = origGoal.getClauses().get(l);
                            if (lit == oLit) {
                                origGoal.getClauses().remove(l);
                                break;
                            }
                        }
                    }
                    origGoal.optimize();
                    finalMatches.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean addNewGoal(GoalStack GS, Goal g, FLogicEnvironment flenv) {
        if (GS.addNewGoal(g)) {
            if (flenv.executionMode == FLogicConstants.FULL_MODE) {
                if (g.getClauses().size() <= 1 && g.getClauses().size() > 0) {
                    Literal lit = g.getClauses().get(0);
                    if (lit.getType() == 0 || lit.getType() == 1) {
                        Atom arg = lit.getArgs()[0]; // subject
                        if (arg.getType() == FLogicConstants.VALUE)
                            ;
                        else {
                            arg = lit.getArgs()[1]; // object
                            if (arg.getType() == FLogicConstants.VALUE)
                                ;
                        }
                    }
                }
            }
            return true;
        } else
            return false;
    }

    private GoalStack prepareGoal(Goal origGoal, ResultSet RS, FLogicEnvironment oenv) {
        GoalStack GS;
        Literal lit, disj;
        boolean match;

        oenv.setGoal_id(0);
        origGoal.setId(oenv.getAndIncrementGoal_id());

        // Get the list of variables in the goal
        RS.clear();
        for (int i = 0; i < origGoal.getClauses().size(); i++) {
            lit = origGoal.getClauses().get(i);
            if (lit.getType() == 0) {
                if (lit.getPredicate().getType() == FLogicConstants.VAR
                        && !lit.getPredicate().getLabel().startsWith("?_")) {
                    RS.addVariable(lit.getPredicate().getLabel());
                }
                for (int a = 0; a < lit.getArity(); a++) {
                    if (lit.getArgs()[a].getType() == FLogicConstants.VAR
                            && !lit.getArgs()[a].getLabel().startsWith("?_")) {
                        RS.addVariable(lit.getArgs()[a].getLabel());
                    }
                }
                for (Map.Entry me : lit.atts.entrySet()) {
                    Atom atm = (Atom) me.getValue();
                    if (atm.getType() == FLogicConstants.VAR && !atm.getLabel().startsWith("?_"))
                        RS.addVariable(atm.getLabel());
                }
                if (lit.getGraph().getType() == FLogicConstants.VAR && !lit.getGraph().getLabel().startsWith("?_")) {
                    RS.addVariable(lit.getGraph().getLabel());
                }
            } else if (lit.getType() == 1) {
                for (int l = 0; l < lit.getDisjuncts().size(); l++) {
                    disj = lit.getDisjuncts().get(l);
                    for (int a = 0; a < disj.getArity(); a++) {
                        if (disj.getArgs()[a].getType() == FLogicConstants.VAR
                                && !disj.getArgs()[a].getLabel().startsWith("?_")) {
                            RS.addVariable(disj.getArgs()[a].getLabel());
                        }
                    }
                }
            } else if (lit.getType() == 2) {
                for (int a = 0; a < lit.getArity(); a++) {
                    if (lit.getArgs()[a].getType() == FLogicConstants.VAR
                            && !lit.getArgs()[a].getLabel().startsWith("?_")) {
                        RS.addVariable(lit.getArgs()[a].getLabel());
                    }
                }
            }
        }

        // Initialize the goal stack
        GS = new GoalStack();
        GS.addNewGoal(origGoal);

        GS.tabPred = ""; // don't register as fully tabled if tabPred is empty;
        GS.tabPredArity = -1; // Java needs to see this
        if (origGoal.getClauses().size() == 1 && origGoal.getClauses().get(0).getType() == 0) {
            lit = origGoal.getClauses().get(0);
            match = true;
            for (int i = 0; i < lit.getArity(); i++) {
                if (lit.getArgs()[i].getType() == FLogicConstants.VAR && !lit.getArgs()[i].getLabel().startsWith("?_"))
                    ;
                else {
                    match = false;
                }
            }
            if (match) {
                // only true if no new data is added
                GS.tabPred = lit.getPredicate().getLabel();
                GS.tabPredArity = lit.getArity();
            }
        }

        return GS;
    }

    // August 10, 2015
    public void resumeGoal(String fl_goal_id, String goalJSON, String fl_id, ResultSet RS, FLogicEnvironment flenv) {
        JSONParser jparser = new JSONParser();
        JSONObject jgoal = null;
        try {
            jgoal = (JSONObject) jparser.parse(goalJSON);
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        // get the query
        String query = (String) jgoal.get("Query");
        Goal goal = parseGoal(query, flenv);
        goal.setUUID(fl_goal_id);
        flenv.fl_id = fl_id;
        // copy bindings
        JSONArray jbindings = (JSONArray) jgoal.get("Bindings");
        for (int b = 0; b < jbindings.size(); b++) {
            JSONObject jbinding = (JSONObject) jbindings.get(b);
            String src = (String) jbinding.get("src");
            String dst = (String) jbinding.get("dest");
            goal.getBindSet().addBinding(src, dst);
        }
        // create a new goal
        GoalStack GS = new GoalStack();
        GS.addNewGoal(goal);
        // copy original variables
        JSONArray jvariables = (JSONArray) jgoal.get("Variables");
        for (int v = 0; v < jvariables.size(); v++)
            RS.addVariable((String) jvariables.get(v));
        // solve goal
        RS.query = null;
        solveGoal(goal, RS, GS, flenv);
    }

    // Sept 14, 2010
    // If the 1st literal in origGoal is unbounded then results for that literal
    // are cached in the kb
    private void solveGoal(Goal origGoal, ResultSet RS, GoalStack GS, FLogicEnvironment oenv) {
        Goal curGoal;
        BindingSet bindSet;
        Literal lit;
        boolean match;
        int i, a, l;
        Tuple tpl;
        String extSrc; // source for extending goal, D? or R?
        long st;

        st = System.currentTimeMillis(); // start time
        bindSet = new BindingSet();

        while (GS.getGoalsToEval().size() > 0 && RS.getIterations() < RS.getMaxIterations()) {
            oenv.setSkiprest(false);
            lit = null; // Java needs to see this
            curGoal = GS.getTopGoal();
            if (oenv.executionMode == FLogicConstants.FULL_MODE && curGoal != origGoal) {
                ;
            }
            if (GS.getInvalidGoals().get(curGoal.getId()) || curGoal.exhaustedChoicePoints()) {
                GS.removeTopGoal();
            } else {
                if (curGoal.getClauses().size() == 0 && curGoal.isSaveResults()) {
                    tpl = createNewTuple(curGoal, bindSet, RS.getVariables());
                    tableResults(origGoal, tpl, RS.getVariables(), oenv);
                    RS.addTuple(tpl);
                    GS.removeTopGoal();
                    oenv.setSkiprest(true);
                } else {
                    lit = curGoal.getClauses().get(0);
                }

                // are all variables bound to values ? and tuple exists stop
                if (!oenv.isSkiprest() && RS.solutionExists(curGoal)) {
                    GS.invalidateGoal(curGoal);
                    oenv.setSkiprest(true);
                }

                if (!oenv.isSkiprest()) {
                    RS.setIterations(RS.getIterations() + 1);
                }

                // is literal negated
                if (!oenv.isSkiprest() && lit.isNegated()) {
                    bindSet.setSize(0);
                    Goal g1 = curGoal.deriveNewGoal("", bindSet, RS, oenv);
                    if (g1 != null) {
                        if (g1.getClauses().size() == 0) {
                            g1.setSaveResults(true); // if we backtrack to this
                        }
                        Goal g2 = curGoal.deriveNewGoalForNegation(g1, oenv);
                        GS.removeTopGoal();
                        if (addNewGoal(GS, g1, oenv)) {
                            addNewGoal(GS, g2, oenv);
                        } else {
                            GS.invalidateGoal(g2);
                        }
                    }
                    oenv.setSkiprest(true);
                }

                // PREPARE FOR DISJUNCTIVE CLAUSES
                if (!oenv.isSkiprest() && lit.getType() == 1) {
                    for (l = 0; l < lit.getDisjuncts().size(); l++) {
                        Goal gd = curGoal.deriveNewGoalForDisjunction(lit.getDisjuncts().get(l), oenv);
                        addNewGoal(GS, gd, oenv);
                    }
                    GS.invalidateGoal(curGoal);
                    oenv.setSkiprest(true);
                }

                // CHECK FOR BUILTIN PREDICATES //
                if (!oenv.isSkiprest() && (systemPreds.contains(lit.getPredicate().getLabel())
                                           || aggregatePreds.contains(lit.getPredicate().getLabel()))) {
                    evaluateSystemPredicate(origGoal, curGoal, GS, RS, oenv);
                    oenv.setSkiprest(true);
                }

                if (!oenv.isSkiprest()) {
                    String ss = lit.getPredicate().getLabel() + "/" + lit.getArity();
                    if (!tabledPreds.contains(ss)) {
                        // CHECK THE KB
                        if (!oenv.isSkiprest())
                            solveViaKB(origGoal, curGoal, GS, RS, oenv);

                        // CHECK THE RULEBASE
                        if (!oenv.isSkiprest())
                            solveViaRules(origGoal, curGoal, GS, RS, oenv, false);
                    } else {
                        // Check the RULEBASE for tabled predicates
                        if (!oenv.isSkiprest()) {
                            solveGoalLT(curGoal, oenv);
                            solveViaKB(origGoal, curGoal, GS, RS, oenv);
                            oenv.setMaterialized(curGoal.getClauses().get(0).toNormalizedString());
                            GS.invalidateGoal(curGoal);
                        }
                    }
                }
            }
        }
        RS.setGoalCount(oenv.getGoal_id());
        if (RS.getIterations() < RS.getMaxIterations()) {
            RS.setComplete(true);
        } else {
            RS.setComplete(false);
        }
        RS.setRunTime(System.currentTimeMillis() - st);
    }

    public void applyTaxonomyRules(FLogicEnvironment oenv) {
        int i, a, v, ruleIdx;
        int varPos[];
        String args[];
        HornClause rule;
        Goal g;
        Literal lit;
        ResultSet RS;

        if (oenv.isTaxonomyRulesCached()) {
            return;
        }
        RS = new ResultSet();
        if (getOntoRulesIndex().containsKey(":")) {
            for (Iterator it = getOntoRulesIndex().getValueIterator(":"); it.hasNext();) {
                ruleIdx = (Integer) it.next();
                rule = ontoRules.get(ruleIdx);
                g = new Goal();
                for (i = 0; i < rule.body.size(); i++) {
                    lit = new Literal(rule.body.get(i));
                    g.getClauses().add(lit);
                }
                GoalStack GS = prepareGoal(g, RS, oenv);
                solveGoal(g, RS, GS, oenv); // save results in KB

                // find positions of variables
                varPos = new int[FLogicConstants.MAX_ARITY];
                args = new String[FLogicConstants.MAX_ARITY];
                for (a = 0; a < rule.head.getArity(); a++) {
                    if (rule.head.getArgs()[a].getType() == FLogicConstants.VAR) {
                        varPos[a] = -1;
                        for (v = 0; v < RS.getVariables().size(); v++) {
                            if (RS.getVariables().get(v).equals(rule.head.getArgs()[a].getLabel())) {
                                varPos[a] = v;
                            }
                        }
                    } else {
                        varPos[a] = -2;
                        args[a] = rule.head.getArgs()[a].getLabel();
                    }
                }
                for (i = 0; i < RS.getTuples().size(); i++) {
                    // fill in arguments to assert with predicate
                    for (a = 0; a < rule.head.getArity(); a++) {
                        if (rule.head.getArgs()[a].getType() == FLogicConstants.VAR) {
                            if (varPos[a] > -1) {
                                args[a] = RS.getTuples().get(i).getCol()[varPos[a]];
                            } else {
                                args[a] = "undefined";
                            }
                        } else {
                            args[a] = rule.head.getArgs()[a].getLabel();
                        }
                    }
                    oenv.getKB().addAssertion(rule.head.getPredicate().getLabel(), rule.head.getArity(), args,
                                              FLogicConstants.RULE_INFERENCE);
                }
            }
        }
        RS.clear();
        oenv.setTaxonomyRulesCached(true);
    }

    private void getRules(Literal literal, ArrayList<Integer> TS, FLogicEnvironment oenv) {
        int i, a, ruleIdx;
        HornClause rule;
        boolean match;
        String predLabel;

        // check for direct instance
        if (literal.getPredicate().getLabel().equals("<:")) {
            predLabel = ":";
        } else {
            predLabel = literal.getPredicate().getLabel();
        }

        if (predLabel.equals(":") && oenv.isTaxonomyRulesCached()) {
            return;
        }

        for (String key : getOntoRulesIndex().getKeyValues()) {
            for (Iterator it = getOntoRulesIndex().getValueIterator(key); it.hasNext();) {
                ruleIdx = (Integer) it.next();
                rule = ontoRules.get(ruleIdx);
                if (predLabel.equals(rule.head.getPredicate().getLabel())
                        || isa(rule.head.getPredicate().getLabel(), predLabel, oenv)) {
                    if (rule.head.getArity() == literal.getArity()) { // predicates
                        // match
                        match = true;
                        for (a = 0; a < literal.getArity() && match; a++) {
                            match = unifiable(rule.head.getArgs()[a], literal.getArgs()[a]);
                        }
                        if (match) {
                            TS.add(rule.id);
                        }
                    }
                }
            }
        }
    }

    //// Linear Tabling
    // Implementation of linear tabling to materialize predicates
    /**
     *
     * @param predicates
     * @param oenv
     */
    public void solveGoalLT(Goal curGoalIn, FLogicEnvironment oenv) {
        Goal curGoal, newGoal, origGoal;
        GoalStack GS;
        ResultSet curRS;
        BindingSet bindSet;
        Literal literal;
        long st;
        String extSrc;
        ArrayList<String> args;
        int l, id, i, old_goal_id;
        int rssize, newrssize;
        Tuple tpl;

        old_goal_id = oenv.getGoal_id(); // save for later

        st = System.currentTimeMillis(); // start time

        GS = new GoalStack();
        GS.isLT = true;
        GS.addNewTabledGoal(curGoalIn.getClauses().get(0));
        GS.curGoalIdx = 0;
        origGoal = GS.getNextTabledGoal();
        curRS = GS.getNextTabledGoalRS();
        oenv.setGoal_id(1);

        rssize = 0;
        while (curRS.getIterations() < curRS.getMaxIterations()) {
            oenv.setSkiprest(false);

            curGoal = GS.getTopGoal();

            if (curGoal == origGoal && curGoal.exhaustedChoicePoints()) { // reset
                // the
                // top
                // goal
                // Count the number of assertions for each tabled predicate
                rssize = GS.getTabledGoalResultsCnt();

                // clear all choice points
                curGoal.getDataChoicePoints().setCalcChoicePoints(false);
                curGoal.getDataChoicePoints().getChoicePoints().clear();
                curGoal.getRuleChoicePoints().setCalcChoicePoints(false);
                curGoal.getRuleChoicePoints().getChoicePoints().clear();

                curRS.setGoalCount(curRS.getGoalCount() + oenv.getGoal_id());
                oenv.setGoal_id(1);

                // Switch goals
                origGoal = GS.getNextTabledGoal();
                curRS = GS.getNextTabledGoalRS();
                curGoal = origGoal;
            }

            if (GS.getInvalidGoals().get(curGoal.getId()) || curGoal.exhaustedChoicePoints()) {
                GS.removeTopGoal();
                oenv.setSkiprest(true);
            }

            literal = null;
            bindSet = null;
            if (!oenv.isSkiprest() && curGoal.isSaveResults()) {
                // we have backtracked to a goal that was negated and the
                // negation is true with no other clauses
                // save results
                tpl = createNewTuple(curGoal, bindSet, curRS.getVariables());
                tableResults(origGoal, tpl, curRS.getVariables(), oenv);
                GS.removeTopGoal();
                oenv.setSkiprest(true);
            } else if (!oenv.isSkiprest())
                literal = curGoal.getClauses().get(0);

            if (!oenv.isSkiprest())
                curRS.setIterations(curRS.getIterations() + 1);

            // is literal negated
            if (!oenv.isSkiprest() && literal.isNegated()) {
                bindSet.setSize(0);
                Goal g1 = curGoal.deriveNewGoal("", bindSet, curRS, oenv);
                if (g1 != null) {
                    if (g1.getClauses().size() == 0)
                        g1.setSaveResults(true); // if we backtrack to this
                    Goal g2 = curGoal.deriveNewGoalForNegation(g1, oenv);
                    GS.removeTopGoal();
                    if (GS.addNewGoal(g1))
                        GS.addNewGoal(g2);
                    else
                        GS.invalidateGoal(g2);
                }
                oenv.setSkiprest(true);
            }

            // PREPARE FOR DISJUNCTIVE CLAUSES
            if (!oenv.isSkiprest() && literal.getType() == 1) {
                for (l = 0; l < literal.getDisjuncts().size(); l++) {
                    Goal gd = curGoal.deriveNewGoalForDisjunction(literal.getDisjuncts().get(l), oenv);
                    GS.addNewGoal(gd);
                }
                GS.invalidateGoal(curGoal);
                oenv.setSkiprest(true);
            }

            // CHECK FOR BUILTIN PREDICATES //
            if (!oenv.isSkiprest() && (systemPreds.contains(literal.getPredicate().getLabel())
                                       || aggregatePreds.contains(literal.getPredicate().getLabel()))) {
                evaluateSystemPredicate(origGoal, curGoal, GS, curRS, oenv);
                oenv.setSkiprest(true);
            }

            // CHECK THE KB
            if (!oenv.isSkiprest())
                solveViaKB(origGoal, curGoal, GS, curRS, oenv);

            // CHECK THE RULEBASE
            if (!oenv.isSkiprest())
                solveViaRules(origGoal, curGoal, GS, curRS, oenv, true);

            // CHECK for termination
            if (GS.curGoalIdx == GS.LT_Goals.size() - 1) {
                newrssize = GS.getTabledGoalResultsCnt();
                if (GS.getTopGoal() == origGoal && origGoal.exhaustedChoicePoints() && rssize == newrssize)
                    break;
            }
        }

        // clear
        GS.clearTabledGoals();

        oenv.setGoal_id(old_goal_id); // restore old goal id
    }

    private void setTabledPred(String pred, int arity) {
        StringBuffer SB = new StringBuffer();
        SB.append(pred);
        SB.append("/");
        SB.append(arity);
        tabledPreds.add(SB.toString());
    }

    private void createPredicateSemantics() {
        HornClause rule;
        int ruleIdx, a;
        int[] rule_arity = new int[10];
        String expr, context;
        StringBuilder SB;
        Set<String> InferenceRuleSet;

        InferenceRuleSet = new HashSet<String>();

        /// Symmetric Properties ///
        for (String symProp : SymmetricPredicates) {
            for (a = 0; a < 10; a++)
                rule_arity[a] = 0; // clear
            if (getOntoRulesIndex().containsKey(symProp))
                for (Iterator it = getOntoRulesIndex().getValueIterator(symProp); it.hasNext();) {
                    ruleIdx = (Integer) it.next();
                    rule = ontoRules.get(ruleIdx);
                    if (rule_arity[rule.head.getArity()] == 0) { // pattern not
                        // created
                        context = "";
                        SB = new StringBuilder();
                        if (rule.head.getArity() > 2) { // copy context
                            SB.append("(?C2");
                            for (a = 3; a < rule.head.getArity(); a++) {
                                SB.append(",?C");
                                SB.append(a);
                            }
                            SB.append(")");
                            context = SB.toString();
                        }
                        rule_arity[rule.head.getArity()] = 1; // pattern created
                        expr = "?X[" + symProp + context + "->?Y] :- ?Y[" + symProp + context + "->?X].";
                        InferenceRuleSet.add(expr);
                        setTabledPred(symProp, rule.head.getArity());
                    }
                }
            if (rule_arity[2] == 0) { // binary case
                expr = "?X[" + symProp + "->?Y] :- ?Y[" + symProp + "->?X].";
                InferenceRuleSet.add(expr);
                setTabledPred(symProp, 2);
            }
        }
        SymmetricPredicates.clear();

        /// Transitive Properties ///
        for (String tranProp : TransitivePredicates) {
            for (a = 0; a < 10; a++)
                rule_arity[a] = 0; // clear
            if (getOntoRulesIndex().containsKey(tranProp))
                for (Iterator it = getOntoRulesIndex().getValueIterator(tranProp); it.hasNext();) {
                    ruleIdx = (Integer) it.next();
                    rule = ontoRules.get(ruleIdx);
                    if (rule_arity[rule.head.getArity()] == 0) { // pattern not
                        // created
                        context = "";
                        SB = new StringBuilder();
                        if (rule.head.getArity() > 2) { // copy context
                            SB.append("(?C2");
                            for (a = 3; a < rule.head.getArity(); a++) {
                                SB.append(",?C");
                                SB.append(a);
                            }
                            SB.append(")");
                            context = SB.toString();
                        }
                        rule_arity[rule.head.getArity()] = 1; // pattern created
                        expr = "?X[" + tranProp + context + "->?Z] :- ?X[" + tranProp + context + "->?Y], ?Y["
                               + tranProp + context + "->?Z], ?X!=?Z.";
                        InferenceRuleSet.add(expr);
                        setTabledPred(tranProp, rule.head.getArity());
                    }
                }
            if (rule_arity[2] == 0) { // binary case
                expr = "?X[" + tranProp + "->?Z] :- ?X[" + tranProp + "->?Y], ?Y[" + tranProp + "->?Z], ?X!=?Z.";
                InferenceRuleSet.add(expr);
                setTabledPred(tranProp, 2);
            }
        }
        TransitivePredicates.clear();

        /// InverseOf ///
        for (Map.Entry<String, String> entry : InverseOf.entrySet()) {
            String prop = entry.getKey();
            String invProp = entry.getValue();
            for (a = 0; a < 10; a++)
                rule_arity[a] = 0; // clear
            if (getOntoRulesIndex().containsKey(prop))
                for (Iterator it = getOntoRulesIndex().getValueIterator(prop); it.hasNext();) {
                    ruleIdx = (Integer) it.next();
                    rule = ontoRules.get(ruleIdx);
                    if (rule_arity[rule.head.getArity()] == 0) { // pattern not
                        // created
                        context = "";
                        SB = new StringBuilder();
                        if (rule.head.getArity() > 2) { // copy context
                            SB.append("(?C2");
                            for (a = 3; a < rule.head.getArity(); a++) {
                                SB.append(",?C");
                                SB.append(a);
                            }
                            SB.append(")");
                            context = SB.toString();
                        }
                        rule_arity[rule.head.getArity()] = 1; // pattern created
                        expr = "?X[" + prop + context + "->?Y] :- ?X[" + invProp + context + "->?Y].";
                        InferenceRuleSet.add(expr);
                        setTabledPred(prop, rule.head.getArity());
                    }
                }
            if (rule_arity[2] == 0) { // binary case
                expr = "?X[" + prop + "->?Y] :- ?X[" + invProp + "->?Y].";
                InferenceRuleSet.add(expr);
                setTabledPred(prop, 2);
            }
        }
        InverseOf.clear();

        /// Add inferencing rules ///
        FLogicDriver driver = new FLogicDriver();
        driver.setFlengine(this);
        for (String infRule : InferenceRuleSet) {
            try {
                driver.parse(infRule);
            } catch (Exception e) {
                logger.error("Error while parsing rule semantics: " + infRule, e);
            }
        }
        InferenceRuleSet.clear();
    }

    /**
     * @return the classesToCache
     */
    public Map<Integer, Integer> getClassesToCache() {
        return classesToCache;
    }

    /**
     * @return the ont
     */
    public Map<String, OntoObject> getOnt() {
        return ont;
    }

    /**
     * @return the KB
     */
    public FLogicKB getKB() {
        return KB;
    }

    /**
     * @return the ontoRulesIndex
     */
    public StringIndex getOntoRulesIndex() {
        return ontoRulesIndex;
    }

    /**
     * @param ontoRulesIndex
     *            the ontoRulesIndex to set
     */
    public void setOntoRulesIndex(StringIndex ontoRulesIndex) {
        this.ontoRulesIndex = ontoRulesIndex;
    }

    /**
     * @return the predIndex
     */
    public StringIndex getPredIndex() {
        return predIndex;
    }

    /**
     * @return the argsIndex
     */
    public StringIndex[] getArgsIndex() {
        return argsIndex;
    }
}
