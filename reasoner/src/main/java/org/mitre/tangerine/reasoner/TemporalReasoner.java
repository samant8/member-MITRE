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
import java.util.HashSet;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

// Temporal reasoning class

public class TemporalReasoner {
    private SimpleDateFormat ft_iso, ft_short_iso;
    public HashSet<String> Comps; // predicates that are supported
    private String CompsList = "temp_lt temp_gt temp_eq";

    public TemporalReasoner() {
        ft_iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        ft_short_iso = new SimpleDateFormat("yyyy-MM-dd");
        Comps = new HashSet<String>();
        for(String c: CompsList.split(" ")) Comps.add(c);
    }

    public boolean callPred(String pred, String t1, String t2) {
        boolean rtn = false;
        switch(pred) {
        case "temp_lt":
            rtn = temp_lt(t1, t2);
            break;
        case "temp_gt":
            rtn = temp_gt(t1, t2);
            break;
        case "temp_eq":
            rtn = temp_eq(t1, t2);
            break;
        }
        return rtn;
    }

    private Date parseISODate(String date_expr) {
        Date t = null;
        try {
            t = ft_iso.parse(date_expr);
            return t;
        } catch (ParseException e) {
            ;
        }

        try {
            t = ft_short_iso.parse(date_expr);
            return t;
        } catch (ParseException e) {
            ;
        }

        return t;
    }

    public boolean temp_lt(String t1, String t2) {
        Date d1, d2;
        d1 = parseISODate(t1);
        d2 = parseISODate(t2);
        if(d1 != null && d2 != null) {
            int ct = d1.compareTo(d2);
            if(ct < 0) return true;
        }
        return false;
    }

    public boolean temp_gt(String t1, String t2) {
        Date d1, d2;
        d1 = parseISODate(t1);
        d2 = parseISODate(t2);
        if(d1 != null && d2 != null) {
            int ct = d1.compareTo(d2);
            if(ct > 0) return true;
        }
        return false;
    }

    public boolean temp_eq(String t1, String t2) {
        Date d1, d2;
        d1 = parseISODate(t1);
        d2 = parseISODate(t2);
        if(d1 != null && d2 != null) {
            int ct = d1.compareTo(d2);
            if(ct == 0) return true;
        }
        return false;
    }
}
