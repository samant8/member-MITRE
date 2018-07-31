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

package org.mitre.tangerine.client;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.media.multipart.MultiPartFeature;

//Controls the server connection
public class ConnectionHandle {
    private Client client;
    private WebTarget service;

    public ConnectionHandle() {
        setClient(ClientBuilder.newBuilder().register(MultiPartFeature.class).build());
    }

    public WebTarget getService() {
        return service;
    }

    public void setService(WebTarget service) {
        this.service = service;
    }

    public Client getClient() {
        return client;
    }

    private void setClient(Client client) {
        this.client = client;
    }

    public void closeConnection() {
        client.close();
    }
}
