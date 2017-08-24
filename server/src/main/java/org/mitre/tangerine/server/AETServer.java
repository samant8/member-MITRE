package org.mitre.tangerine.server;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

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
import org.mitre.tangerine.db.AETDatabase;
import org.mitre.tangerine.db.mongo.AETMongo;
import org.mitre.tangerine.esri.adapter.EsriAdapter;
import org.mitre.tangerine.esri.parser.EsriParser;
import org.mitre.tangerine.exception.AETException;
import org.mitre.tangerine.models.ResponseModel;
import org.mitre.tangerine.netowl.adapter.NetOwlAdapter;
import org.mitre.tangerine.netowl.parser.NetOwlParser;
import org.mitre.tangerine.pii.adapter.PIIAdapter;
import org.mitre.tangerine.pii.parser.PIIParser;
import org.mitre.tangerine.reasoner.Reasoner;
import org.mitre.tangerine.voucher.adapter.VoucherAdapter;
import org.mitre.tangerine.voucher.parser.VoucherParser;

import com.google.gson.Gson;

@Path("/")
public class AETServer extends ResourceConfig {

	private AETDatabase connect() {
		// TODO collect host, port, db from client and pass into the
		// constructor below
		AETDatabase queries = new AETMongo();
		queries.openConnection();
		queries.accessDatabase();
		return queries;
	}

	@GET
	@Path("collections")
	@Produces(MediaType.APPLICATION_XML)
	public ResponseModel getCollections() {
		ResponseModel responseModel = new ResponseModel();
		AETMongo query = (AETMongo) connect();
		responseModel.setCollections(query.getCollections());
		if (responseModel.getCollections().size() < 1) {
			responseModel.setError(true);
			responseModel.setErrorMessage("There are no collections available");
		}
		query.closeConnection();
		return responseModel;
	}

	@GET
	@Path("count/{collection}")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public ResponseModel getCount(@PathParam("collection") String collection) {
		ResponseModel responseModel = new ResponseModel();
		AETMongo query = (AETMongo) connect();
		query.accessSelection(collection);
		responseModel.setNumberOfElements((int) query.countElements());
		query.closeConnection();
		return responseModel;
	}

	@GET
	@Path("elements/{collection}")
	@Consumes(MediaType.APPLICATION_XML)
	@Produces(MediaType.APPLICATION_XML)
	public ResponseModel getElements(@Context HttpHeaders headers, @PathParam("collection") String collection) {
		ResponseModel responseModel = new ResponseModel();
		int elements = Integer.parseInt(headers.getRequestHeader("elements").get(0));
		AETMongo query = (AETMongo) connect();
		query.accessSelection(collection);
		responseModel.setElements(query.getElements(elements));
		query.closeConnection();
		return responseModel;
	}

	@DELETE
	@Path("drop/{collection}")
	public Response dropCollection(@PathParam("collection") String collection) {
		AETDatabase queries = connect();
		queries.accessSelection(collection);
		queries.dropSelection();
		queries.closeConnection();
		return Response.status(202).entity(collection + " has been dropped").build();
	}

	@PUT
	@Path("insert")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	@Produces(MediaType.APPLICATION_XML)
	public ResponseModel insertData(@Context HttpHeaders headers, String data) {
		// TODO change the call below to use AETMongo(host, port, db)
		ResponseModel responseModel = new ResponseModel();
		AETMongo query = new AETMongo();
		String entity = headers.getRequestHeader("entity").get(0);
		String type = headers.getRequestHeader("type").get(0);
		String uuid = headers.getRequestHeader("uuid").get(0);
		responseModel.setDataType(type);

		InputStream stream = new ByteArrayInputStream(data.getBytes());
		String epoc = String.valueOf(Instant.now().toEpochMilli());
		String collectionUuid = UUID.randomUUID().toString().replaceAll("-", "_");
		String collection = "";

		if (type.equals("general")) {
			collection = "GE_" + collectionUuid + "_" + epoc;
			responseModel.setCollection(collection);
			query.openConnection();
			query.accessSelection(collection);
			query.accessDatabase();
			Document doc;
			String[] input = data.split("\n");
			int count = 0;
			for (String line : input) {
				if (!line.trim().equals("")) {
					doc = Document.parse(line);
					try {
						query.updateSelection(doc);
					} catch (AETException e) {
						responseModel.setError(true);
						responseModel.setErrorMessage(e.getMessage());
					}
					count++;
				}
			}
			responseModel.setNumberOfElements(count);
			query.closeConnection();

		} else if (type.equals("netowl")) {
			NetOwlParser nop = new NetOwlParser();
			NetOwlAdapter adapter = new NetOwlAdapter();
			collection = adapter.getName() + "_" + collectionUuid + "_" + epoc;
			try {
				responseModel = adapter.adapt(collection, nop.parse(stream), uuid, entity);
				responseModel = adapter.updateDB(responseModel, query);
			} catch (AETException e) {
				responseModel.setError(true);
				responseModel.setErrorMessage(e.getMessage());
			}

		} else if (type.equals("voucher")) {
			VoucherParser vp = new VoucherParser();
			VoucherAdapter va = new VoucherAdapter();
			collection = va.getName() + "_" + collectionUuid + "_" + epoc;
			Map<String, String> voucherForms = vp.parse(stream);
			try {
				responseModel = va.adapt(collection, voucherForms);
				responseModel = va.updateDB(responseModel, query);
			} catch (AETException e) {
				responseModel.setError(true);
				responseModel.setMessage(e.getMessage());
			}

		} else if (type.equals("pii")) {
			PIIParser pii = new PIIParser();
			PIIAdapter piiAdapter = new PIIAdapter();
			collection = piiAdapter.getName() + "_" + collectionUuid + "_" + epoc;
			Map<String, String> piiData = pii.parse(stream);
			try {
				responseModel = piiAdapter.adapt(collection, piiData, uuid);
				responseModel = piiAdapter.updateDB(responseModel, query);
			} catch (AETException e) {
				responseModel.setError(true);
				responseModel.setMessage(e.getMessage());
			}

		} else if (type.equals("esri")) {
			EsriParser esri = new EsriParser();
			EsriAdapter esriAdapter = new EsriAdapter();
			collection = esriAdapter.getName() + "_" + collectionUuid + "_" + epoc;
			Map<String, String> esriData = esri.parse(stream);
			try {
				responseModel = esriAdapter.adapt(collection, esriData, uuid, entity);
				responseModel = esriAdapter.updateDB(responseModel, query);
			} catch (AETException e) {
				responseModel.setError(true);
				responseModel.setMessage(e.getMessage());
			}
		} else {
			responseModel.setError(true);
			responseModel.setErrorMessage("Data type not available");
		}
		System.out.println(new Gson().toJson(responseModel));
		return responseModel;
	}

	@POST
	@Path("reasoner")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public ResponseModel reasoner(FormDataMultiPart formParams) {
		ResponseModel responseModel = new ResponseModel();
		String configuration = formParams.getField("configuration").getValueAs(String.class);
		String ontology = formParams.getField("ontology").getValueAs(String.class);
		String queries = formParams.getField("queries").getValueAs(String.class);
		String type = formParams.getField("type").getValueAs(String.class);
		Reasoner reasoner = new Reasoner();

		responseModel.setMessage(reasoner.Reason(configuration, ontology, queries, type));
		return responseModel;
	}
}
