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


package org.mitre.tangerine.models;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;
import org.mitre.tangerine.models.ReasonerModel.Data;
import org.mitre.tangerine.models.ReasonerModel.Data.Results;
import org.mitre.tangerine.models.ReasonerModel.Data.Results.Result;
import org.junit.Test;

public class ReasonerModelTest {

    @Test
    public void testReasonerModel() {
        Gson gson = new Gson();
        ReasonerModel m = new ReasonerModel();

        Data d = m.new Data();
        Results r = d.new Results();

        List<Result> test = new ArrayList<Result>();
        Result l = r.new Result();
        Result n = r.new Result();
        l.setKey("test1");
        l.setValue("test2");
        n.setKey("test1");
        n.setValue("test2");
        test.add(l);
        test.add(n);
        r.setResult(test);
        d.setQuery("test3");
        List<Results> test2 = new ArrayList<Results>();
        test2.add(r);
        test2.add(r);
        d.setResults(test2);
        List<Data> test3 = new ArrayList<Data>();
        test3.add(d);
        test3.add(d);
        m.setDataList(test3);

        assert (gson.toJson(m).equals(
                    "{\"dataList\":[{\"query\":\"test3\",\"resultList\":[{\"result\":[{\"key\":\"test1\",\"val\":\"test2\"},{\"key\":\"test1\",\"val\":\"test2\"}]},{\"result\":[{\"key\":\"test1\",\"val\":\"test2\"},{\"key\":\"test1\",\"val\":\"test2\"}]}]},{\"query\":\"test3\",\"resultList\":[{\"result\":[{\"key\":\"test1\",\"val\":\"test2\"},{\"key\":\"test1\",\"val\":\"test2\"}]},{\"result\":[{\"key\":\"test1\",\"val\":\"test2\"},{\"key\":\"test1\",\"val\":\"test2\"}]}]}]}"
                    + ""));

    }

}
