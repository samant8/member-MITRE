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

package org.mitre.tangerine.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Enumeration;
import java.lang.Class;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.bson.Document;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.server.ResourceConfig;
import org.mitre.tangerine.adapter.Analytic;
import org.mitre.tangerine.adapter.OutboundAdapter;
import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.db.mongo.AETMongo;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ReasonerModel;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.reasoner.Reasoner;

@Path("/")
public class AETServer extends ResourceConfig {

    private AETDatabase connect() {
        // TODO collect host, port, db from client and pass into the
        // constructor below
        AETDatabase queries = new AETMongo();
        queries.open();
        return queries;
    }

    @GET
    @Path("collections")
    @Produces(MediaType.APPLICATION_XML)
    public Response getCollections() {
        ResponseModel responseModel = new ResponseModel();
        AETMongo query = (AETMongo) connect();
        responseModel.setCollections(query.getCollections());
        query.close();
        responseModel.setError(false);
        return Response.status(200).entity(responseModel).build();
    }

    @GET
    @Path("outputs")
    @Produces(MediaType.APPLICATION_XML)
    public Response getOutputs() {
        ResponseModel responseModel = new ResponseModel();
        List<String> ret = new ArrayList<String>();
        Map<String, OutboundAdapter> outputs = null;

        if (outputs == null || outputs.keySet().size() == 0) {
            outputs = this.loadAdapters();
        }

        if (outputs.keySet().size() > 0) {
            for (String key : outputs.keySet())
                ret.add(key + " | " + outputs.get(key).getCanonicalName() + " | " + outputs.get(key).getDescription());
        }
        responseModel.setOutputs(ret);
        responseModel.setError(false);
        return Response.status(200).entity(responseModel).build();
    }

    @GET
    @Path("analytics")
    @Produces(MediaType.APPLICATION_XML)
    public Response getAnalytics() {
        ResponseModel responseModel = new ResponseModel();
        List<String> ret = new ArrayList<String>();
        Map<String, Analytic> analytics = null;

        if (analytics == null || analytics.keySet().size() == 0) {
            analytics = this.loadAnalytics();
        }

        if (analytics.keySet().size() > 0) {
            for (String key : analytics.keySet())
                ret.add(key + " | " + analytics.get(key).getCanonicalName() + " | "
                        + analytics.get(key).getDescription());
        }
        responseModel.setAnalytics(ret);
        responseModel.setError(false);
        return Response.status(200).entity(responseModel).build();
    }

    @GET
    @Path("analytic")
    @Produces(MediaType.APPLICATION_XML)
    public Response getAnalytic(@Context HttpHeaders headers) {
        ResponseModel responseModel = new ResponseModel();
        String analytic = headers.getRequestHeaders().get("analytic").get(0);
        Map<String, Analytic> analytics = null;
        if (analytics == null || analytics.keySet().size() == 0) {
            analytics = this.loadAnalytics();
        }
        if (analytics.containsKey(analytic)) {
            try {
                responseModel.setDatamap(analytics.get(analytic).getDatamap());
            } catch (IOException e1) {
                responseModel.setError(true);
                responseModel.setMessage("Couldn't retrieve data map");
                return Response.status(500).entity(responseModel).build();
            }

        } else {
            // TODO throw 400
            responseModel.setError(true);
            responseModel.setMessage("Analytic " + analytic + " not supported.");
            return Response.status(400).entity(responseModel).build();

        }
        responseModel.setError(false);
        return Response.status(200).entity(responseModel).build();
    }

    @GET
    @Path("collection/{collection}")
    @Consumes(MediaType.APPLICATION_XML)
    @Produces(MediaType.APPLICATION_XML)
    public Response getCollection(@Context HttpHeaders headers, @PathParam("collection") String collection) {
        ResponseModel responseModel = new ResponseModel();
        AETMongo query = (AETMongo) connect();
        query.access(collection);
        responseModel.setElements(query.getElements());
        query.close();
        responseModel.setError(false);
        return Response.status(200).entity(responseModel).build();
    }

    @DELETE
    @Path("drop/{collection}")
    public Response dropCollection(@PathParam("collection") String collection) {
        AETDatabase queries = connect();
        queries.access(collection);
        queries.drop();
        queries.close();
        ResponseModel responseModel = new ResponseModel();
        responseModel.setError(false);
        return Response.status(202).entity(responseModel).build();
    }

    @PUT
    @Path("insert")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_XML)
    public Response insertData(@Context HttpHeaders headers, String data) {
        // TODO change the call below to use AETMongo(host, port, db)
        ResponseModel responseModel = new ResponseModel();
        AETMongo query = new AETMongo();
        String entity = headers.getRequestHeader("entity").get(0), type = headers.getRequestHeader("type").get(0),
               uuid = headers.getRequestHeader("uuid").get(0),
               foreignKey = headers.getRequestHeader("foreignKey").get(0);
        Map<String, String> params = new HashMap<String, String>();
        if (uuid != null && uuid.length() != 0)
            params.put("uuid", uuid);
        if (entity != null && entity.length() != 0)
            params.put("entity", entity);
        if (foreignKey != null && foreignKey.length() != 0)
            params.put("key", foreignKey);
        Map<String, Analytic> analytics = null;

        InputStream stream = new ByteArrayInputStream(data.getBytes());
        String epoc = String.valueOf(Instant.now().toEpochMilli());
        String collectionUuid = UUID.randomUUID().toString().replaceAll("-", "_");
        String collection = "";
        Analytic temp = null;

        responseModel.setDataType(type);

        if (analytics == null || !analytics.keySet().contains(type)) {
            analytics = this.loadAnalytics();
        }
        if (!analytics.keySet().contains(type)) {
            responseModel.setError(true);
            responseModel.setErrorMessage("analytic not on server");
            return Response.status(400).entity(responseModel).build();

        } else {
            temp = analytics.get(type);
            collection = temp.getName() + "_" + collectionUuid + "_" + epoc;
            responseModel.setCollection(collection);
            try {
                responseModel = temp.adapt(collection, stream, params);
                temp.updateDB(responseModel, query);
            } catch (IOException | AETException e) {
                responseModel.setError(true);
                responseModel.setErrorMessage(e.getMessage());
                return Response.status(500).entity(responseModel).build();
            }
        }
        responseModel.setError(false);
        return Response.status(200).entity(responseModel).build();
    }

    @POST
    @Path("reasoner")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response reasoner(FormDataMultiPart formParams) {
        Reasoner reasoner = new Reasoner();
        String configuration = formParams.getField("configuration").getValueAs(String.class);
        String ontologies = formParams.getField("ontology").getValueAs(String.class);
        String queries = formParams.getField("queries").getValueAs(String.class);
        String type = formParams.getField("type").getValueAs(String.class);
        Map<String, OutboundAdapter> adapters = null;

        OutboundAdapter temp = null;
        ReasonerModel model = reasoner.Reason(configuration, ontologies, queries);
        adapters = this.loadAdapters();
        System.out.println("keySet contains " + type);

        if (adapters.keySet().contains(type)) {
            System.out.println("keySet contains " + type);
            temp = adapters.get(type);
            try {
                return Response.status(200).entity(temp.transform(model)).build();
            } catch (AETException e) {
                ResponseModel mod = new ResponseModel();
                mod.setError(true);
                mod.setMessage(e.getMessage());
                return Response.status(500).entity(mod).build();
            }
        } else {
            ResponseModel rep = new ResponseModel();
            rep.setError(true);
            rep.setMessage("Adapter not found");
            return Response.status(400).entity(rep).build();
        }
        // return Response.status(500).build();
    }

    private Map<String, OutboundAdapter> loadAdapters() {
        Map<String, OutboundAdapter> adapters = new HashMap<String, OutboundAdapter>();

        ClassLoader er = getClass().getClassLoader();

        URL[] classPath = ((URLClassLoader) er).getURLs();
        try {
            for (int i = 0; i < classPath.length; i++) {
                if (classPath[i].getFile().contains("outbound")) {
                    String filename = classPath[i].getFile();
                    filename = filename.substring(filename.lastIndexOf('/') + 1);
                    Class<?> classToLoad = er.loadClass(
                                               "org.mitre.tangerine.adapter.outbound." + filename.substring(0, filename.lastIndexOf(".")));
                    OutboundAdapter t = (OutboundAdapter) classToLoad.newInstance();
                    adapters.put(t.getCanonicalName(), t);
                }

            }
        } catch (Exception e) {
        }

        return adapters;

    }

    private Map<String, Analytic> loadAnalytics() {
        Map<String, Analytic> analytics = new HashMap<String, Analytic>();

        ClassLoader er = getClass().getClassLoader();

        URL[] classPath = ((URLClassLoader) er).getURLs();
        try {
            for (int i = 0; i < classPath.length; i++) {
                if (classPath[i].getFile().contains("inbound")) {
                    String filename = classPath[i].getFile();
                    filename = filename.substring(filename.lastIndexOf('/') + 1);
                    Class<?> classToLoad = er.loadClass(
                                               "org.mitre.tangerine.analytic." + filename.substring(0, filename.lastIndexOf(".")));
                    Analytic t = (Analytic) classToLoad.newInstance();
                    analytics.put(t.getCanonicalName(), t);
                }

            }
        } catch (Exception e) {
        }

        return analytics;

    }
}
