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

package org.mitre.tangerine.adapter;

import java.io.IOException;

import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;

public interface InboundAdapter {

    // TODO
    //public void setDatamap(InputStream is) throws IOException;
    public String getDatamap() throws IOException;
    public void updateDB(ResponseModel data, AETDatabase db) throws AETException;

}
