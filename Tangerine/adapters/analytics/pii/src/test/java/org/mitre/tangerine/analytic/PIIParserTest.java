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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mitre.tangerine.exception.AETException;

import junit.framework.TestCase;

public class PIIParserTest extends TestCase {

    public void testParse() throws IOException, AETException {

        Map<String, String> parsy = new PiiAdapter().parse(this.getClass().getClassLoader().getResourceAsStream("pii.csv") );
        Set<String> keys = new HashSet<String>();
        keys.addAll(Arrays.asList("HasLienRecord","HasBankruptcyRecord","HasArrestRecord","HasCriminalRecord"));

        assert(parsy.keySet().containsAll(keys));

    }

}
