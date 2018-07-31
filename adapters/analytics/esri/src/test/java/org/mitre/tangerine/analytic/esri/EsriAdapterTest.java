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



package org.mitre.tangerine.analytic.esri;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.mitre.tangerine.analytic.EsriAdapter;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;
import com.google.gson.GsonBuilder;

public class EsriAdapterTest {

    @Test
    public void testAdapt() throws IOException, AETException {
        EsriAdapter ad = new EsriAdapter();
        ResponseModel rep = null;
        Map<String, String> map = new HashMap<String, String>();
        map.put("uuid",  "Travelling_UUID");
        map.put("entity",  "AE#Travelling");

        try {
            rep = ad.adapt("collection", this.getClass().getClassLoader().getResourceAsStream("esri.csv"), map);
        } catch (IOException e) {
            fail();
        }

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(rep);
        assert (json.contains("AE#hasDistance"));
        assert (json.contains("\"collection\": \"collection\""));
    }
}
