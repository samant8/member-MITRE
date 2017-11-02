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


package org.mitre.tangerine.db;

import java.util.List;

import org.mitre.tangerine.exception.AETException;

public abstract class AETDatabase {

    private String host, db;
    private int port;

    // TODO user, pass

    public AETDatabase(String host, int port, String db) {
        this.host = host;
        this.port = port;
        this.db   = db;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getDb() {
        return db;
    }

    public abstract void open();
    public abstract void access(String id);
    public abstract void update(Object data) throws AETException;
    public abstract void drop();
    public abstract void index();
    public abstract void close();
    public abstract List<String> getEntries();

}
