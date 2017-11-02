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

import org.mitre.tangerine.reasoner.FLogicConstants;

public class Atom {

    private String label;
    private char type;

    public Atom(String str) {
        label = str;
        if (label.startsWith("?")) {
            type = FLogicConstants.VAR;
        } else {
            type = FLogicConstants.VALUE;
        }
    }

    public void update(String str) {
        setLabel(str);
        if (getLabel().startsWith("?")) {
            setType(FLogicConstants.VAR);
        } else {
            setType(FLogicConstants.VALUE);
        }
    }

    public String toFLogic() {
        if(type == FLogicConstants.VAR) return label;
        else return "'" + label + "'";
    }
    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * @param label the label to set
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the type
     */
    public char getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(char type) {
        this.type = type;
    }
}
