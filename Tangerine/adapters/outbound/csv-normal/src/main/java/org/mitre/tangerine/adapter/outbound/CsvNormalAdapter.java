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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.mitre.tangerine.adapter.OutboundAdapter;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ReasonerModel;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.models.ReasonerModel.Data;
import org.mitre.tangerine.models.ReasonerModel.Data.Results;
import org.mitre.tangerine.models.ReasonerModel.Data.Results.Result;
import org.slf4j.Logger;

public class CsvNormalAdapter extends OutboundAdapter {

    private final String name = "CsvNormalAdapter", canonicalName = this.getClass().getName(),
                         description = "This adapter converts Reasoner Data to normal CSV.";

    @Override
    public ResponseModel transform(ReasonerModel data) throws AETException {
        Logger log = this.getLogger();
        log.info("Converting ReasonerModel to CSV using " + canonicalName);
        ArrayList<HashMap<String, String>> csv = new ArrayList<HashMap<String, String>>();
        ArrayList<String> keys = new ArrayList<String>();
        List<Data> dataList = data.getDataList();
        ResponseModel response = new ResponseModel();

        for (Data element : dataList) {
            for (Results results : element.getResults()) {
                csv.add(new HashMap<String, String>());
                for (Result result : results.getResult()) {
                    String key = result.getKey().substring(1);
                    String value = result.getValue();
                    if (value.matches("\\w{2}_\\w{8}_\\w{4}_\\w{4}_\\w{4}_\\w{12}_\\w{13}_.*")
                            || value.matches("\\w+#.+")) {
                        continue;
                    }
                    if(value.contains("\"")) {
                        value = value.replaceAll("\"", "");
                    }
                    if (value.contains(",")) {
                        value = "\"" + value + "\"";
                    }
                    log.info("Adding key: " + key + " value: " + value);
                    csv.get(csv.size() - 1).put(key, value);
                    if (!keys.contains(key)) {
                        keys.add(key);
                    }
                }
            }
        }

        log.info("Sorting Keys");
        String header = "";
        Collections.sort(keys);
        for (String key : keys) {
            header += key + ",";
        }
        header = header.substring(0, header.length() - 1) + "\n";

        log.info("Adding CSV data");
        String csvData = "";
        for (HashMap<String, String> map : csv) {
            for (String key : keys) {
                if (map.containsKey(key)) {
                    csvData += map.get(key) + ",";
                } else {
                    csvData += ",";
                }
            }
            csvData = csvData.substring(0, csvData.length() - 1) + "\n";
        }

        log.info("Adding CSV to ResponseModel");
        response.setMessage(header + csvData);
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
