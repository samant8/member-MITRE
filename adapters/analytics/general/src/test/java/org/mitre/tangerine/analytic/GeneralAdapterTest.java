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

import org.junit.Test;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;

import com.google.gson.GsonBuilder;

public class GeneralAdapterTest {


    @Test
    public void testAdapt() throws IOException, AETException {
        GeneralAdapter ad = new GeneralAdapter();
        ResponseModel rep = null;

        rep = ad.adapt("collection", this.getClass().getClassLoader().getResourceAsStream("currency.txt"), null);

        String json = new GsonBuilder().setPrettyPrinting().create().toJson(rep);
    }

    @Test
    public void testAdaptFail()  {
        GeneralAdapter ad = new GeneralAdapter();
        ResponseModel rep = null;

        try {
            rep = ad.adapt("collection", this.getClass().getClassLoader().getResourceAsStream("error.txt"), null);
        } catch (AETException | IOException e) {
            assert(true);
        }

    }
}
