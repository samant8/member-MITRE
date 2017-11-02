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
import java.io.InputStream;
import org.junit.Test;
import org.mitre.tangerine.analytic.HanaAdapter;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;

import com.google.gson.GsonBuilder;

import junit.framework.TestCase;

public class HanaAdapterTest extends TestCase {

    @Test
    public void testParse() throws AETException, IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("boko.json");
        HanaAdapter ha = new HanaAdapter();
        ResponseModel re = ha.adapt("foobar", is, null);
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(re);
        System.out.println(json);
        System.out.println("done");
    }
}
