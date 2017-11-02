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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class BindingSet {
    private String[] source = new String[FLogicConstants.MAX_BINDINGS];;
    private String[] destination = new String[FLogicConstants.MAX_BINDINGS];;
    private int size = 0;

    public BindingSet() {}

    public void clear() {
        setSize(0);
    }

    public void release() {
        source=null;
        destination=null;
    }

    public void addBinding(String src, String dst) {
        int i;
        String bsrc, bdst;
        boolean dup = false;

        if (getSize() < FLogicConstants.MAX_BINDINGS) {
            bsrc = src;
            bdst = dst; // default
            if (src.startsWith("?") && !dst.startsWith("?")) {
                bsrc = dst;
                bdst = src;
            }

            // Does src already exist
            for (i = 0; i < getSize() && !dup; i++) {
                if (getSource()[i].equals(bsrc)
                        && getDestination()[i].equals(bdst)) {
                    dup = true;
                }
            }
            if (!dup) {
                getSource()[getSize()] = bsrc;
                getDestination()[getSize()] = bdst;
                setSize(getSize() + 1);
            }
        }
    }

    public String getBinding(String var) {
        int i;

        while (true) {
            for (i = 0; i < getSize(); i++) {
                if (getDestination()[i].equals(var)) {
                    if (getSource()[i].startsWith("?")) {
                        var = getSource()[i];
                        break;
                    } else {
                        return getSource()[i];
                    }
                }
            }
            if (i == getSize()) {
                return "";
            }
        }
    }

    @Override
    public String toString() {
        int i;
        String ss;

        ss = "";
        for (i = 0; i < getSize(); i++) {
            ss += getSource()[i] + "/" + getDestination()[i] + " ";
        }
        return ss;
    }

    public JSONArray toJSON() {
        JSONArray bds = new JSONArray();
        for(int i=0; i<getSize(); i++) {
            JSONObject bd = new JSONObject();
            bd.put("src",  getSource()[i]);
            bd.put("dest", getDestination()[i]);
            bds.add(bd);
        }
        return bds;
    }

    public void append(BindingSet bs) {
        int i;

        if (getSize() + bs.getSize() < FLogicConstants.MAX_BINDINGS) {
            for (i = 0; i < bs.getSize(); i++) {
                getSource()[getSize()] = bs.getSource()[i];
                getDestination()[getSize()] = bs.getDestination()[i];
                setSize(getSize() + 1);
            }
        }
    }

    // look for ?x / ?c && value / ?x
    public void reduce() {
        int i, j;

        for (i = 0; i < getSize(); i++) {
            for (j = 0; j < getSize(); j++) {
                if (getDestination()[j].equals(getSource()[i])) {
                    getSource()[i] = getSource()[j];
                }
            }
        }
    }

    /**
     * @return the source
     */
    public String[] getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String[] source) {
        this.source = source;
    }

    /**
     * @return the destination
     */
    public String[] getDestination() {
        return destination;
    }

    /**
     * @param destination the destination to set
     */
    public void setDestination(String[] destination) {
        this.destination = destination;
    }

    /**
     * @return the size
     */
    public int getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    public void setSize(int size) {
        this.size = size;
    }
}
