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



package org.mitre.tangerine.netowl.adapter;


import java.io.IOException;
import java.util.HashMap;

import org.junit.Test;
import org.mitre.tangerine.analytic.NetOwlAdapter;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.netowl.parser.Content;


public class NetOwlAdapterTest {

    @Test
    public void testAdaptName() throws AETException {

        NetOwlAdapter ad = new NetOwlAdapter();
        try {
            ad.adapt("collection", this.getClass().getClassLoader().getResourceAsStream("netowl_name.xml"), new HashMap<String, String>());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Test
    public void testAdaptAddress() throws AETException {

        NetOwlAdapter ad = new NetOwlAdapter();

        try {
            ad.adapt("collection", this.getClass().getClassLoader().getResourceAsStream("netowl_address.xml"), new HashMap<String, String>());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
