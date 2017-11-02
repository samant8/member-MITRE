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



package org.mitre.tangerine.analytic;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mitre.tangerine.analytic.PiiAdapter;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;

import com.google.gson.GsonBuilder;

public class PIIAdapterTest {

    @Test
    public void testAdapt() {
        Map<String, String> input = null;

        PiiAdapter ad = new PiiAdapter();
        ResponseModel rep = null;

        try {
            rep = ad.adapt("collection", this.getClass().getClassLoader().getResourceAsStream("pii.csv"), new HashMap<String, String>());
        } catch (IOException | AETException e1) {
        }
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(rep);

        ad = new PiiAdapter();
        rep = null;

        Map<String, String> map = new HashMap<String, String>();
        map.put("uuid", "Human_UUID");
        map.put("entity", "SSN");
        map.put("key", "301-27-6586");

        try {
            rep = ad.adapt("collection", this.getClass().getClassLoader().getResourceAsStream("pii2.csv"), map);
        } catch (AETException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        json = new GsonBuilder().setPrettyPrinting().create().toJson(rep);

    }
}
