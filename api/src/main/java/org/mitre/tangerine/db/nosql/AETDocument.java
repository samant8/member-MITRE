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


package org.mitre.tangerine.db.nosql;

import java.util.List;

import org.mitre.tangerine.db.AETDatabase;

public abstract class AETDocument<M> extends AETDatabase {

    public AETDocument(String host, int port, String db) {
        super(host, port, db);
    }

    public abstract void accessCollection(String collection);

    public abstract void dropCollection();

    public abstract void indexCollection();

    public abstract long countElements();

    public abstract List<String> getCollections();

    public abstract List<String> getElements();

}
