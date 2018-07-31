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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author jpevan2
 * @author mgtayl3
 */
public class StringIndex {
    private Map<String, HashSet<Integer>> index = new LinkedHashMap<String, HashSet<Integer>>();

    public void addToIndex(String key, int i) {
        if (!index.containsKey(key)) {
            index.put(key, new HashSet<Integer>());
        }
        index.get(key).add(i);
    }

    public void addToIndex(String key, Integer i) {
        addToIndex(key, i.intValue());
    }

    public Set<String> getKeyValues() {
        return index.keySet();
    }

    public HashSet<Integer> getIndexValues(String key) {
        return index.get(key);
    }

    public void deleteIndexKey(String key) {
        if (index.containsKey(key)) {
            index.remove(key);
        }
    }

    public void deleteIndexValue(String key, int value) {
        if (index.containsKey(key)) {
            index.get(key).remove(value);
        }
    }

    public void release() {
        index.clear();
        index=null;
    }

    public void clear() {
        index = new LinkedHashMap<String, HashSet<Integer>>();
    }

    public Iterator<Integer> getValueIterator(String key) {
        return index.get(key).iterator();
    }

    public Iterator<String> getKeyIterator() {
        return index.keySet().iterator();
    }

    public int getIndexSize(String key) {
        int idxSize = 0;
        if (index.containsKey(key)) {
            idxSize = index.get(key).size();
        }
        return idxSize;
    }

    public boolean containsKey(String key) {
        return index.containsKey(key);
    }

    /**
     * @return the index
     */
    public Map<String, HashSet<Integer>> getIndex() {
        return index;
    }
}
