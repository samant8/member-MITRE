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
import java.util.HashMap;

public class N_ary {

    private String pred;
    private String[] args= new String[FLogicConstants.MAX_ARITY];
    private int arity;
    private byte recordType;  // asserted=0 inference=1  deleted=-1
    private String graph;     // the graph that this assertions is in or default graph
    private byte inhSem;      // not-inheritable=0 monotonic=1 non-monotonic=2

    private HashMap<String, String> attrs; // assertion attributes

    public N_ary() {
        attrs = new HashMap<String, String>();
    }

    public void release() {
        args = null;
        attrs.clear();
        attrs=null;
    }

    /**
     * @return the pred
     */
    public String getPred() {
        return pred;
    }

    /**
     * @param pred the pred to set
     */
    public void setPred(String pred) {
        this.pred = pred;
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
     * @return the arity
     */
    public int getArity() {
        return arity;
    }

    /**
     * @param arity the arity to set
     */
    public void setArity(int arity) {
        this.arity = arity;
    }

    /**
     * @return the recordType
     */
    public byte getRecordType() {
        return recordType;
    }

    /**
     * @param recordType the recordType to set
     */
    public void setRecordType(byte recordType) {
        this.recordType = recordType;
    }

    /**
     * @return the graph
     */
    public String getGraph() {
        return graph;
    }

    /**
     * @param graph the graph to set
     */
    public void setGraph(String graph) {
        this.graph = graph;
    }

    /**
     * @return the inhSem
     */
    public byte getInhSem() {
        return inhSem;
    }

    /**
     * @param inhSem the inhSem to set
     */
    public void setInhSem(byte inhSem) {
        this.inhSem = inhSem;
    }

    @Override
    public String toString() {
        String ss="";

        ss = args[0] + "[" + pred;
        if(arity>2) {
            ss += "(";
            for(int a=2; a<arity; a++) {
                if(a!=2) ss += ",";
                ss += args[a];
            }
            ss += ")";
        }
        ss += "->" + args[1] +"]";
        return ss;
    }

    public void clearAttributes() {
        attrs.clear();
    }

    public void setAttribute(String attr_name, String attr_val) {
        attrs.put(attr_name,attr_val);
    }

    public String getAttribute(String attr_name) {
        if(attrs.containsKey(attr_name)) return attrs.get(attr_name);
        else {
            if(attr_name.equals("temp_st")) return FLogicConstants.firstDate;
            if(attr_name.equals("temp_ed"))   return FLogicConstants.lastDate;
        }
        return null; // Should be an exception
    }
}
