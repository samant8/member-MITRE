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
import java.io.FileReader;
import java.io.IOException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.mitre.tangerine.models.ResponseModel;

public class AETClient {
    private ConnectionHandle connectionHandle;

    // Constructor
    public AETClient() {
        connectionHandle = new ConnectionHandle();
    }

    // Create connection to the server
    public ConnectionHandle createConnection(String host, String portNumber) {
        connectionHandle.setService(connectionHandle.getClient()
                .target(UriBuilder.fromUri("http://" + host + ":" + portNumber).build()));
        return connectionHandle;
    }

    // Get list of collections
    public ResponseModel getCollections(ConnectionHandle connectionHandle) {
        ResponseModel response = connectionHandle.getService().path("tangerine").path("collections")
                .request().accept(MediaType.APPLICATION_XML).get(ResponseModel.class);
        return response;
    }

    // Count number of elements in a collection
    public ResponseModel getCount(ConnectionHandle connectionHandle, String collection) {
        ResponseModel response = connectionHandle.getService().path("tangerine").path("count")
                .path(collection).request().accept(MediaType.APPLICATION_XML).get(ResponseModel.class);
        return response;
    }

    // Get first X amount of elements from collection
    public ResponseModel getElements(ConnectionHandle connectionHandle, String collection, int elements) {
        ResponseModel message = new ResponseModel();
        message.setCollection(collection);
        message.setNumberOfElements(elements);
        ResponseModel response = connectionHandle.getService().path("tangerine").path("elements")
                .path(collection).request().header("elements", elements).accept(MediaType.APPLICATION_XML)
                .get(ResponseModel.class);
        return response;
    }

    // Drop collection
    public void dropCollection(ConnectionHandle connectionHandle, String collection) {
        Response delete = connectionHandle.getService().path("tangerine").path("drop").path(collection)
                .request().delete();
        System.out.println(delete.readEntity(String.class));
    }

    // Add file to MongoDB
    public ResponseModel insertData(ConnectionHandle connectionHandle, String file, String type, String uuid,
            String entity) throws IOException {
        ResponseModel response = null;
        FileReader fr = new FileReader(file);
        response = connectionHandle.getService().path("tangerine").path("insert")
                .request(MediaType.APPLICATION_XML).header("type", type).header("uuid", uuid)
                .header("entity", entity)
                .put(Entity.entity(fr, MediaType.APPLICATION_OCTET_STREAM), ResponseModel.class);
        return response;
    }

    // Query the knowledge-store with a reasoner
    public ResponseModel reasoner(ConnectionHandle connectionHandle, String confFile, String ontFile, String queFile,
            String type) {
        FileDataBodyPart configuration = new FileDataBodyPart("configuration", new File(confFile));
        FileDataBodyPart ontology = new FileDataBodyPart("ontology", new File(ontFile));
        FileDataBodyPart queries = new FileDataBodyPart("queries", new File(queFile));
        FormDataMultiPart multipartEntity = new FormDataMultiPart();
        ResponseModel response = new ResponseModel();

        multipartEntity.field("type", type).bodyPart(configuration).bodyPart(ontology).bodyPart(queries);
        response = connectionHandle.getService().path("tangerine").path("reasoner").request()
                .post(Entity.entity(multipartEntity, multipartEntity.getMediaType()), ResponseModel.class);
        return response;
    }

}
