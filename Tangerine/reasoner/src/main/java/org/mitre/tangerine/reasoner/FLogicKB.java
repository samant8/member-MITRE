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
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class FLogicKB {

    public int lastAssertionIdx;                                  // index of last triple inserted or found
    public Stack<Integer> freeKBIdx  = new Stack<Integer>();      // list of free N_ary objects
    public List<Integer> inferredIdx = new ArrayList<Integer>();  // list of assertions that were inferred
    private StringIndex predIndex    = new StringIndex();
    private StringIndex[] argsIndex  = new StringIndex[FLogicConstants.MAX_ARITY]; // indices into the assertions
    public List<N_ary> kb            = new ArrayList<N_ary>();   // collection of assertions
    public List<N_ary> fts           = new ArrayList<N_ary>();   // free text search assertions
    public JSONParser jparser        = new JSONParser();
    static Logger logger             = Logger.getLogger(FLogicKB.class);

    public FLogicKB() {
        for (int i = 0; i < FLogicConstants.MAX_ARITY; i++) {
            argsIndex[i] = new StringIndex();
        }
    }

    public void release() {
        jparser=null;
        clearKB();
        freeKBIdx=null;
        inferredIdx=null;
        predIndex.release();
        predIndex=null;
        for(int i=0; i<FLogicConstants.MAX_ARITY; i++) {
            argsIndex[i].release();
            argsIndex[i]=null;
        }
        argsIndex=null;
        kb=null;
    }

    public void clearKB() {
        int i;

        // clear fts
        for(i=0; i<fts.size(); i++) {
            N_ary na = kb.get(i);
            na = null;
        }
        fts.clear();

        // clear the kb
        for(i=0; i<kb.size(); i++) {
            N_ary na = kb.get(i);
            na = null;
        }
        kb.clear();

        // clear the indices
        getPredIndex().clear();
        for (i = 0; i < FLogicConstants.MAX_ARITY; i++) {
            getArgsIndex()[i].clear();
        }
        freeKBIdx.clear();
        inferredIdx.clear();
    }

    public void addAssertion(String pred, int arity, String args[], byte recordType) {
        addAssertion(pred, arity, args, FLogicConstants.DEFAULT_GRAPH, "->", recordType);
    }

    public void addAssertion(String pred, int arity, String args[], String graph, byte recordType) {
        addAssertion(pred, arity, args, graph, "->", recordType);
    }

    public void addAssertion(String pred, int arity, String args[], String graph, String inhSem, byte recordType) {
        N_ary na;
        int idx;
        String attrs=null, prov=null;

        // Free Text Search results
        if(pred.equals("FreeTextSearch") || pred.equals("RegexTextSearch")) {
            na = new N_ary();
            na.setGraph(graph);
            na.setPred(pred);
            for (int i = 0; i < arity && i < FLogicConstants.MAX_ARITY; i++) na.getArgs()[i] = args[i];
            na.setArity(arity);
            na.setRecordType(recordType);
            na.setInhSem(FLogicConstants.NOT_INHERITABLE);
            fts.add(na);
            return;
        }

        if(arity > 2) {
            int origArity = arity;
            for(int i=2; i<origArity; i++) {
                if(args[i].startsWith("{\"T\":{"))      {
                    attrs = args[i];    // attributes
                    arity--;
                } else if(args[i].startsWith("{\"V\":{")) {
                    prov  = args[i];    // provenance
                    arity--;
                }
            }
        }

        // avoid adding duplicates
        if (attrs==null && assertionExists(pred, arity, args)) return;

        if (freeKBIdx.size() == 0) {
            na = new N_ary();  // create n-ary
            idx = kb.size(); 	 // save assertion
            kb.add(na);
        } else {  // re-use existing n_ary that was previously freed
            idx = freeKBIdx.pop();
            na = kb.get(idx);
        }

        na.setGraph(graph);
        na.setPred(pred);
        for (int i = 0; i < arity && i < FLogicConstants.MAX_ARITY; i++) {
            na.getArgs()[i] = args[i];
        }
        na.setArity(arity);
        na.setRecordType(recordType);
        if (inhSem.equals("->")) {
            na.setInhSem(FLogicConstants.NOT_INHERITABLE);
        } else if (inhSem.equals("*m->")) {
            na.setInhSem(FLogicConstants.MONOTONIC);
        } else {
            na.setInhSem(FLogicConstants.NON_MONOTONIC);
        }

        na.clearAttributes();
        if(attrs != null) { // parse attributes
            try {
                JSONObject Attrs = (JSONObject) jparser.parse(attrs);
                JSONObject T     = (JSONObject) Attrs.get("T");
                if(T!=null)
                    for(Object key : T.keySet()) {
                        String attr_name = (String) key;
                        String attr_val  = (String) T.get(key);
                        na.setAttribute(attr_name, attr_val);
                    }
            } catch (java.lang.ClassCastException e) {
                ;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        lastAssertionIdx = idx;

        // index assertion
        getPredIndex().addToIndex(pred, idx);
        for (int i = 0; i < arity && i < FLogicConstants.MAX_ARITY; i++) {
            getArgsIndex()[i].addToIndex(args[i], idx);
        }

        if (recordType == FLogicConstants.INFERRED_ASSERTION
                || recordType == FLogicConstants.RULE_INFERENCE) {
            inferredIdx.add(idx);
        } else {
            delInferred();
        }
    }

    public N_ary getAssertion(int idx, String pred) {
        N_ary na;

        if(pred.equals("FreeTextSearch") || pred.equals("RegexTextSearch")) na = fts.get(idx);
        else na = kb.get(idx);
        return na;
    }

    public N_ary getAssertion(int idx) {
        N_ary na;

        na = kb.get(idx);
        return na;
    }

    public void delAssertion(String pred, int arity, String args[]) {
        // does this assertion exist
        if (!assertionExists(pred, arity, args)) {
            return;
        }

        // lastAssertionIdx contains the location of this assertion
        delAssertion(lastAssertionIdx);

        // some cached inferences are now no longer valid
        // clear all inferred assertions since there is no way to determine
        // which inferred assertions are affected by the deletion
        delInferred();
    }

    public void delAssertion(int idx) {
        boolean erase;
        N_ary assrt;

        assrt = kb.get(idx);

        // save the position of the assertion
        freeKBIdx.push(idx);

        // remove from the predicate index
        getPredIndex().deleteIndexValue(assrt.getPred(), idx);
        for (int i = 0; i < FLogicConstants.MAX_ARITY; i++) {
            getArgsIndex()[i].deleteIndexValue(assrt.getArgs()[i], idx);
        }
    }

    public void delInferred() {
        for (int idx : inferredIdx) {
            delAssertion(idx);
        }
        inferredIdx.clear();
    }

    public boolean assertionExists(String pred, int arity, String args[]) {
        N_ary na;
        int idx, matchCnt;

        // use the 'sub' index to search
        HashSet<Integer> argList = getArgsIndex()[0].getIndexValues(args[0]);
        if (argList != null) {
            for (Iterator it = getArgsIndex()[0].getIndexValues(args[0]).iterator(); it.hasNext();) {
                matchCnt = 1;
                idx = (Integer) it.next();
                na = kb.get(idx);
                if (na.getArity() == arity && na.getPred().equals(pred)) {
                    for (int i = 1; i < arity; i++) {
                        if (na.getArgs()[i].equals(args[i])) {
                            matchCnt++;
                        }
                    }
                    if (matchCnt == arity) {
                        lastAssertionIdx = idx;
                        return true;
                    }
                }
            }
        }
        return false;
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
