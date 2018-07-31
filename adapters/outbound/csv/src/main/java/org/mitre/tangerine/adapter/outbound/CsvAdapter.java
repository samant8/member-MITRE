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

package org.mitre.tangerine.adapter.outbound;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;

import org.mitre.tangerine.adapter.OutboundAdapter;
import org.mitre.tangerine.models.ReasonerModel;
import org.mitre.tangerine.models.ReasonerModel.Data;
import org.mitre.tangerine.models.ReasonerModel.Data.Results;
import org.mitre.tangerine.models.ReasonerModel.Data.Results.Result;
import org.mitre.tangerine.models.ResponseModel;
import org.slf4j.Logger;

public class CsvAdapter extends OutboundAdapter {

    private final String name = "CSVAdapter", canonicalName = this.getClass().getName(),
                         description = "This adapter converts Reasoner Data to CSV.";

    @Override
    public ResponseModel transform(ReasonerModel data) {
        Logger log = this.getLogger();
        log.info("Converting ReasonerModel to CSV using " + canonicalName);
        Map<String, String> csv = new HashMap<String, String>();
        Map<String, Integer> dupes = new HashMap<String, Integer>();
        List<Data> d = data.getDataList();
        ResponseModel response = new ResponseModel();

        for (Data elem : d) {
            if (elem.getResults().size() == 0) {
                String key = elem.getQuery();
                key = key.substring(key.lastIndexOf("?") + 1, key.lastIndexOf("]"));
                csv.put(key, "");
            } else {
                for (Results res : elem.getResults()) {
                    for (Result r : res.getResult()) {
                        String key = r.getKey().substring(1);
                        String value = r.getValue();
                        if (value.matches("\\w{2}_\\w{8}_\\w{4}_\\w{4}_\\w{4}_\\w{12}_\\w{13}_.*")
                                || value.matches("\\w+#.+"))
                            continue;
                        if (value.contains(",") && (value.charAt(0) != '"' && value.charAt(value.length() - 1) != '"'))
                            value = "\"" + value + "\"";
                        log.info("Adding key: " + key + " value: " + value);
                        if (csv.containsKey(key)) {
                            if (dupes.containsKey(key)) {
                                dupes.put(key, dupes.get(key) + 1);
                            } else {
                                dupes.put(key, 1);
                            }
                            key += dupes.get(key);
                        }
                        log.info("Adding key: " + key + " value: " + value);
                        csv.put(key, value);
                    }
                }
            }
        }

        log.info("Adding CSV to ResponseModel");
        List<String> keys = new ArrayList<String>();
        keys.addAll(csv.keySet());
        Collections.sort(keys);
        String row1 = "", row2 = "";
        for (String key : keys) {
            row1 += key + ",";
            row2 += csv.get(key) + ",";
        }
        response.setMessage(row1.substring(0, row1.length() - 1) + "\n" + row2.substring(0, row2.length() - 1));
        return response;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getCanonicalName() {
        return canonicalName;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
