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
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class FLogicEnvironment {

    private ArrayList<OntoObject> history = new ArrayList<OntoObject>();
    public  ArrayList<OntoObject> Ancs = new ArrayList<OntoObject>();
    public  ArrayList<OntoObject> Decs;
    public  ArrayList<String> SemTypes; // temp storage of instanceFilter optimization
    private BitSet searched = new BitSet(FLogicConstants.MAX_ONTOLOGY_CLASS_CNT);
    private int thread;
    private boolean match;
    private ArrayList<String> answer = new ArrayList<String>();
    private ArrayList<String> relaxToAnswer = new ArrayList<String>();
    private ArrayList<Goal> Goals = new ArrayList<Goal>();
    private float rangeScore;
    private LinkedHashMap<String, LinkedHashMap<String, Integer>> ancDis =
        new LinkedHashMap<String, LinkedHashMap<String, Integer>>();   // distance to ancestors

    // KnowledgeBase
    private FLogicKB KB = new FLogicKB();
    private boolean taxonomyRulesCached;
    // list of predicates that are completely materialized
    private Map<String, Integer> materializedPreds = new HashMap<String, Integer>();
    private String args[] = new String[FLogicConstants.MAX_ARITY];
    private String new_args[] = new String[FLogicConstants.MAX_ARITY];

    // Rule Evaluation
    private int goal_id;
    private boolean skiprest;
    public long funcTime, mgTim, esTim, pyTim;

    public int executionMode; // query execution mode
    public String fl_id; // id of the current query

    public Goal curGoal; // the goal context for data request

    public HashSet<Integer> RulesToIgnore; // Rules to ignore

    public HashSet<String> AnalyticsToIgnore; // Analytics to ignore

    public FLogicEnvironment() {
        funcTime=0;
        Decs = new ArrayList<OntoObject>();
        executionMode = FLogicConstants.FULL_MODE;
        SemTypes = new ArrayList<String>();

        RulesToIgnore     = new HashSet<Integer>();
        AnalyticsToIgnore = new HashSet<String>();
    }

    public void setMaterialized(String predExpr) {
        if (!getMaterializedPreds().containsKey(predExpr)) {
            getMaterializedPreds().put(predExpr, 0);
        }
    }

    public void setMaterialized(String pred, int arity) {
        String ss = pred + "/" + arity;
        setMaterialized(ss);
    }

    public boolean isMaterialized(String predExpr) {
        return getMaterializedPreds().containsKey(predExpr);
    }

    public boolean isMaterialized(String pred, int arity) {
        String ss = pred + "/" + arity;
        return isMaterialized(ss);
    }

    public void clear() {
        getHistory().clear();
        getAnswer().clear();
        getRelaxToAnswer().clear();
        getMaterializedPreds().clear();
        setTaxonomyRulesCached(false);
        getKB().clearKB();
        SemTypes.clear();
    }

    public void clearInferred() {
        getKB().delInferred();
        getMaterializedPreds().clear();
        setTaxonomyRulesCached(false);
    }

    /**
     * @return the history
     */
    public ArrayList<OntoObject> getHistory() {
        return history;
    }

    /**
     * @param history the history to set
     */
    public void setHistory(ArrayList<OntoObject> history) {
        this.history = history;
    }

    /**
     * @return the Ancs
     */
    public ArrayList<OntoObject> getAncs() {
        return Ancs;
    }

    /**
     * @param Ancs the Ancs to set
     */
    public void setAncs(ArrayList<OntoObject> Ancs) {
        this.Ancs = Ancs;
    }

    /**
     * @return the searched
     */
    public BitSet getSearched() {
        return searched;
    }

    /**
     * @param searched the searched to set
     */
    public void setSearched(BitSet searched) {
        this.searched = searched;
    }

    /**
     * @return the thread
     */
    public int getThread() {
        return thread;
    }

    /**
     * @param thread the thread to set
     */
    public void setThread(int thread) {
        this.thread = thread;
    }

    /**
     * @return the match
     */
    public boolean isMatch() {
        return match;
    }

    /**
     * @param match the match to set
     */
    public void setMatch(boolean match) {
        this.match = match;
    }

    /**
     * @return the answer
     */
    public ArrayList<String> getAnswer() {
        return answer;
    }

    /**
     * @param answer the answer to set
     */
    public void setAnswer(ArrayList<String> answer) {
        this.answer = answer;
    }

    /**
     * @return the relaxToAnswer
     */
    public ArrayList<String> getRelaxToAnswer() {
        return relaxToAnswer;
    }

    /**
     * @param relaxToAnswer the relaxToAnswer to set
     */
    public void setRelaxToAnswer(ArrayList<String> relaxToAnswer) {
        this.relaxToAnswer = relaxToAnswer;
    }

    /**
     * @return the Goals
     */
    public ArrayList<Goal> getGoals() {
        return Goals;
    }

    /**
     * @param Goals the Goals to set
     */
    public void setGoals(ArrayList<Goal> Goals) {
        this.Goals = Goals;
    }

    /**
     * @return the rangeScore
     */
    public float getRangeScore() {
        return rangeScore;
    }

    /**
     * @param rangeScore the rangeScore to set
     */
    public void setRangeScore(float rangeScore) {
        this.rangeScore = rangeScore;
    }

    /**
     * @return the ancDis
     */
    public LinkedHashMap<String, LinkedHashMap<String, Integer>> getAncDis() {
        return ancDis;
    }

    /**
     * @param ancDis the ancDis to set
     */
    public void setAncDis(LinkedHashMap<String, LinkedHashMap<String, Integer>> ancDis) {
        this.ancDis = ancDis;
    }

    /**
     * @return the KB
     */
    public FLogicKB getKB() {
        return KB;
    }

    /**
     * @param KB the KB to set
     */
    public void setKB(FLogicKB KB) {
        this.KB = KB;
    }

    /**
     * @return the taxonomyRulesCached
     */
    public boolean isTaxonomyRulesCached() {
        return taxonomyRulesCached;
    }

    /**
     * @param taxonomyRulesCached the taxonomyRulesCached to set
     */
    public void setTaxonomyRulesCached(boolean taxonomyRulesCached) {
        this.taxonomyRulesCached = taxonomyRulesCached;
    }

    /**
     * @return the materializedPreds
     */
    public Map<String, Integer> getMaterializedPreds() {
        return materializedPreds;
    }

    /**
     * @param materializedPreds the materializedPreds to set
     */
    public void setMaterializedPreds(Map<String, Integer> tabledPreds) {
        this.materializedPreds = tabledPreds;
    }

    /**
     * @return the args
     */
    public String[] getArgs() {
        return args;
    }

    /**
     * @param args the args to set
     */
    public void setArgs(String[] args) {
        this.args = args;
    }

    /**
     * @return the goal_id
     */
    public int getGoal_id() {
        return goal_id;
    }

    /**
     * @param goal_id the goal_id to set
     */
    public void setGoal_id(int goal_id) {
        this.goal_id = goal_id;
    }

    public int getAndIncrementGoal_id() {
        int id = this.goal_id;
        this.goal_id++;
        return id;
    }

    /**
     * @return the skiprest
     */
    public boolean isSkiprest() {
        return skiprest;
    }

    /**
     * @param skiprest the skiprest to set
     */
    public void setSkiprest(boolean skiprest) {
        this.skiprest = skiprest;
    }
}
