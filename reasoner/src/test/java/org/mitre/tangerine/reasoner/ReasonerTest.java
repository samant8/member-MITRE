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

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;

import org.mitre.tangerine.reasoner.Reasoner;

import org.junit.Test;

public class ReasonerTest {

    @Test
    public void test() {
        HashMap<String, String> csv = new HashMap<String, String>();
        ArrayList<Tuple> tuples = new ArrayList<Tuple>();
        Tuple tuple = new Tuple();

        String[] col = { "val1-1", "val1,2", "val1\"3", "val1\",4",
                         "VO_ad3bb310_a110_4f01_9189_104f596fe6c7_1503938567367_0", "?val1-6"
                       };
        tuple.setCol(col);
        tuples.add(tuple);

        ArrayList<String> variables = new ArrayList<String>();
        variables.add("key1");
        variables.add("key,2");
        variables.add("key\"3");
        variables.add("key\",4");
        variables.add("key5");
        variables.add("?key6");

        ResultSet RS = new ResultSet();
        RS.setVariables(variables);
        RS.setTuples(tuples);

        for (int i = 0; i < RS.getTuples().size(); i++) {
            Tuple tpl = RS.getTuples().get(i);
            for (int j = 0; j < RS.getVariables().size(); j++) {
                System.out.println(tpl.getCol()[j]);
                System.out.println(RS.getVariables().get(j));
            }
        }
    }

}
