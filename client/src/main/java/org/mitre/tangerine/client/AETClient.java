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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.mitre.tangerine.models.ResponseModel;

public class AETClient {
    private ConnectionHandle connectionHandle;
    private final String servlet = "tangerine";

    // Constructor
    public AETClient() {
        connectionHandle = new ConnectionHandle();
    }

    // Create connection to the server
    public AETClient(String host, int portNumber) {
        this();
        connectionHandle.setService(
            connectionHandle.getClient().target(UriBuilder.fromUri("http://" + host + ":" + portNumber).build()));
    }

    // Get list of collections
    public ResponseModel getCollections() {
        ResponseModel response = this.connectionHandle.getService().path(servlet).path("collections").request()
                                 .accept(MediaType.APPLICATION_XML).get(ResponseModel.class);
        return response;
    }

    // Count number of elements in a collection
    public ResponseModel getCount(String collection) {
        ResponseModel response = this.connectionHandle.getService().path(servlet).path("count").path(collection)
                                 .request().accept(MediaType.APPLICATION_XML).get(ResponseModel.class);
        return response;
    }

    // Get first X amount of elements from collection
    public ResponseModel getCollection(String collection) {
        ResponseModel response = this.connectionHandle.getService().path(servlet).path("collection").path(collection).request().accept(MediaType.APPLICATION_XML).get(ResponseModel.class);
        return response;
    }

    public ResponseModel getAnalytics() {
        ResponseModel response = this.connectionHandle.getService().path(servlet).path("analytics")
                                 .request().accept(MediaType.APPLICATION_XML).get(ResponseModel.class);
        return response;
    }

    public ResponseModel getOutputs() {
        ResponseModel response = this.connectionHandle.getService().path(servlet).path("outputs")
                                 .request().accept(MediaType.APPLICATION_XML).get(ResponseModel.class);
        return response;
    }
    public ResponseModel getAnalytic(String analytic) {
        ResponseModel response = this.connectionHandle.getService().path(servlet).path("analytic")
                                 .request().header("analytic", analytic).accept(MediaType.APPLICATION_XML).get(ResponseModel.class);
        return response;
    }

    // Drop collection
    public ResponseModel dropCollection(String collection) {
        return this.connectionHandle.getService().path(servlet).path("drop").path(collection).request().delete()
               .readEntity(ResponseModel.class);

    }


    // Add file to MongoDB
    public ResponseModel insertData(String file, String type, String uuid, String entity, String foreignKey)
    throws IOException {
        ResponseModel response = null;
        InputStream fr = new FileInputStream(file);
        response = this.connectionHandle.getService().path(servlet).path("insert")
                   .request(MediaType.APPLICATION_XML).header("type", type).header("uuid", uuid).header("entity", entity)
                   .header("foreignKey", foreignKey)
                   .put(Entity.entity(fr, MediaType.APPLICATION_OCTET_STREAM), ResponseModel.class);
        return response;
    }

    // Query the knowledge-store with a reasoner
    public ResponseModel reasoner(String confFile, String ontFile, String queFile, String type) {
        FileDataBodyPart configuration = new FileDataBodyPart("configuration", new File(confFile));
        FileDataBodyPart ontology = new FileDataBodyPart("ontology", new File(ontFile));
        FileDataBodyPart queries = new FileDataBodyPart("queries", new File(queFile));
        FormDataMultiPart multipartEntity = new FormDataMultiPart();
        ResponseModel response = new ResponseModel();

        multipartEntity.field("type", type).bodyPart(configuration).bodyPart(ontology).bodyPart(queries);
        response = this.connectionHandle.getService().path(servlet).path("reasoner").request()
                   .post(Entity.entity(multipartEntity, multipartEntity.getMediaType()), ResponseModel.class);
        return response;
    }

}
