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

import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.mitre.tangerine.models.ResponseModel;

import com.google.gson.Gson;

public class TangerineTester {

	public static void main(String[] args) {
		AETClient aetClient = new AETClient();
		ResponseModel response = null;
		Options options = new Options();
		options.addOption("hostName", true, "Server host name or IP.");
		options.addOption("port", true, "Port number of the service");
		options.addOption("insert", true,
				"Insert data. If the collection already exist it will append to it. (Use it with collection and type)");
		options.addOption("type", true,
				"Type of data that is going to be use (netowl, voucher, pii, esri, general[default])");
		options.addOption("collection", true, "Collection to use");
		options.addOption("count", false, "Count documents in collection. (Use with collection)");
		options.addOption("elements", true, "Show X amount of documents. (Use with collection)");
		options.addOption("collections", false, "Show all collections names");
		options.addOption("drop", false, "Drop collection");
		options.addOption("reasoner", false, "Send reasoner query. (Use with configuration, ontologies and queries)");
		options.addOption("configuration", true, "Reasoner configuration file. Required for reasoner.");
		options.addOption("ontologies", true, "Ontology file. Required for reasoner.");
		options.addOption("queries", true, "Reasoner queries file. Required for reasoner.");
		options.addOption("outputType", true, "reasoner output (csv, json[default])");
		options.addOption("uuid", true, "uuid to use when inserting data.(Optional. Use with insert and entity)");
		options.addOption("entity", true, "entity to use.(Optional. Use with insert and uuid)");
		options.addOption("help", false, "Show this message");

		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new DefaultParser();
		try {
			CommandLine cmd = parser.parse(options, args);
			String argInsert = cmd.getOptionValue("insert");
			String argCollection = cmd.getOptionValue("collection");
			String argElements = cmd.getOptionValue("elements");
			String argType = cmd.getOptionValue("type");
			String argHostName = cmd.getOptionValue("hostName");
			String argPort = cmd.getOptionValue("port");
			String argConfiguration = cmd.getOptionValue("configuration");
			String argOntologies = cmd.getOptionValue("ontologies");
			String argQueries = cmd.getOptionValue("queries");
			String argOutputType = cmd.getOptionValue("outputType");
			String argUuid = cmd.getOptionValue("uuid");
			String argEntity = cmd.getOptionValue("entity");

			if (!cmd.hasOption("type")) {
				argType = "general";
			}
			if (!cmd.hasOption("hostName")) {
				argHostName = "localhost";
			}
			if (!cmd.hasOption("port")) {
				argPort = "8080";
			}

			ConnectionHandle connectionHandle = aetClient.createConnection(argHostName, argPort);

			if (cmd.hasOption("collections")) {
				response = aetClient.getCollections(connectionHandle);
				if (!response.isError()) {
					List<String> collections = response.getCollections();
					for (int i = 0; i < collections.size(); i++) {
						System.out.println(collections.get(i));
					}
				} else {
					System.out.println("Error message: " + response.getErrorMessage());
				}
			} else if (cmd.hasOption("reasoner") && argConfiguration != null && argOntologies != null
					&& argQueries != null) {
				if (!cmd.hasOption("outputType")) {
					argOutputType = "json";
				}
				response = aetClient.reasoner(connectionHandle, argConfiguration, argOntologies, argQueries,
						argOutputType);
				if (!response.isError()) {
					System.out.println(response.getMessage());
				} else {
					System.out.println("Error message: " + response.getErrorMessage());
				}
			} else if (argCollection != null) {
				if (cmd.hasOption("count")) {
					response = aetClient.getCount(connectionHandle, argCollection);
					if (!response.isError()) {
						System.out.println("Number of elements in collection: " + response.getNumberOfElements());
					} else {
						System.out.println("Error message: " + response.getErrorMessage());
					}
				} else if (argElements != null) {
					response = aetClient.getElements(connectionHandle, argCollection, Integer.valueOf(argElements));
					;
					if (!response.isError()) {
						List<String> elements = response.getElements();
						for (int i = 0; i < elements.size(); i++) {
							System.out.println(elements.get(i));
						}
					} else {
						System.out.println("Error message: " + response.getErrorMessage());
					}
				} else if (cmd.hasOption("drop")) {
					aetClient.dropCollection(connectionHandle, argCollection);
				} else {
					System.out.println("Make sure that your arguments are correct.");
					formatter.printHelp("Analysis Exchange Tool", options);
				}
			} else if (argInsert != null) {
				if (argUuid == null) {
					argUuid = "";
				}
				if (argEntity == null) {
					argEntity = "";
				}
				response = aetClient.insertData(connectionHandle, argInsert, argType, argUuid, argEntity);
				if (!response.isError()) {
					System.out.println(new Gson().toJson(response));
				} else {
					System.out.println("Error message: " + response.getErrorMessage());
				}
			} else if (args.length == 0 || cmd.hasOption("help")) {
			    /*
			     * No arguments or Help.
			     */
				formatter.printHelp("Analysis Exchange Tool", options);
			} else {
			    /*
			     * Incorrect arguments.
			     */
				System.out.println("Make sure that your arguments are correct.");
				formatter.printHelp("Analysis Exchange Tool", options);
			}
		} catch (ParseException | IOException e) {
			System.err.println("Parsing failed.  Reason: " + e.getMessage());
		}
	}
}
