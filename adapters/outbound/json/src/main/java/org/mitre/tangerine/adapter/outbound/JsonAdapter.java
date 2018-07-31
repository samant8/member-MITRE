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

import org.mitre.tangerine.adapter.OutboundAdapter;
import org.mitre.tangerine.models.ReasonerModel;
import org.mitre.tangerine.models.ResponseModel;
import org.slf4j.Logger;

import com.google.gson.Gson;

public class JsonAdapter extends OutboundAdapter {

    private final String name = "JsonAdapter", canonicalName = this.getClass().getName(),
                         description = "This adapter uses the GSON library to convert Reasoner data to JSON.";

    @Override
    public ResponseModel transform(ReasonerModel data) {
        Logger log = this.getLogger();
        log.info("Converting ReasonerModel to JSON using " + canonicalName);
        ResponseModel resp = new ResponseModel();
        String message = new Gson().toJson(data);
        log.info("Adding JSON to ResponseModel");
        resp.setMessage(message);
        return resp;
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
