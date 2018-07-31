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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OntoObject {

    private String name;
    private String ns;
    private int nodeId;
    private Map<Integer, OntoObject> subs = new LinkedHashMap<Integer, OntoObject>();
    private Map<Integer, OntoObject> sups = new LinkedHashMap<Integer, OntoObject>();
    private Map<Integer, OntoObject> eq = new LinkedHashMap<Integer, OntoObject>();
    private List<String> domain = new ArrayList<String>();
    private List<String> range = new ArrayList<String>();
    private String inhSem;
    private BitSet isaCache = new BitSet(FLogicConstants.MAX_ISA_CACHE);

    public OntoObject(String name) {
        this.name = name;
    }

    public void collectSupsToList(List<OntoObject> ooList, boolean includeEq) {
        collectToList(ooList, getSups(), includeEq);
    }

    public void collectSubsToList(List<OntoObject> ooList, boolean includeEq) {
        collectToList(ooList, getSubs(), includeEq);
    }

    public void collectEqsToList(List<OntoObject> ooList) {
        collectToList(ooList, getEq(), false);
    }

    private void collectToList(List<OntoObject> ooList, Map<Integer, OntoObject> src, boolean includeEq) {
        ooList.addAll(src.values());
        if (includeEq) {
            ooList.addAll(getEq().values());
        }
    }

    public void addSub(OntoObject sub) {
        addOnto(sub, getSubs());
    }

    public void delSub(OntoObject sub) {
        delOnto(sub, getSubs());
    }

    public void addSup(OntoObject sup) {
        addOnto(sup, getSups());
    }

    public void delSup(OntoObject sup) {
        delOnto(sup, getSups());
    }

    public void addEq(OntoObject cls) {
        addOnto(cls, getEq());
    }

    public void delEq(OntoObject cls) {
        delOnto(cls, getEq());
    }

    private void delOnto(OntoObject oo, Map<Integer, OntoObject> map) {
        if (map.containsKey(oo.getNodeId())) {
            map.remove(oo.getNodeId());
        }
    }

    private void addOnto(OntoObject oo, Map<Integer, OntoObject> map) {
        if (!map.containsKey(oo.getNodeId())) {
            map.put(oo.getNodeId(), oo);
        }
    }

    public Collection<OntoObject> getSupList() {
        return getSups().values();
    }

    public Collection<OntoObject> getSubList() {
        return getSubs().values();
    }

    public Collection<OntoObject> getEqList() {
        return getEq().values();
    }

    /**
     * @return the subs
     */
    public Map<Integer, OntoObject> getSubs() {
        return subs;
    }

    /**
     * @return the sups
     */
    public Map<Integer, OntoObject> getSups() {
        return sups;
    }

    /**
     * @return the eq
     */
    public Map<Integer, OntoObject> getEq() {
        return eq;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the ns
     */
    public String getNs() {
        return ns;
    }

    /**
     * @param ns the ns to set
     */
    public void setNs(String ns) {
        this.ns = ns;
    }

    /**
     * @return the nodeId
     */
    public int getNodeId() {
        return nodeId;
    }

    /**
     * @param nodeId the nodeId to set
     */
    public void setNodeId(int nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * @return the inhSem
     */
    public String getInhSem() {
        return inhSem;
    }

    /**
     * @param inhSem the inhSem to set
     */
    public void setInhSem(String inhSem) {
        this.inhSem = inhSem;
    }

    /**
     * @return the domain
     */
    public List<String> getDomain() {
        return domain;
    }

    /**
     * @param domain the domain to set
     */
    public void setDomain(List<String> domain) {
        this.domain = domain;
    }

    /**
     * @return the range
     */
    public List<String> getRange() {
        return range;
    }

    /**
     * @param range the range to set
     */
    public void setRange(List<String> range) {
        this.range = range;
    }

    /**
     * @return the isaCache
     */
    public BitSet getIsaCache() {
        return isaCache;
    }
}
