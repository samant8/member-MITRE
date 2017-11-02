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
import java.util.Map;
import org.docopt.Docopt;
import org.mitre.tangerine.models.ResponseModel;

import com.google.gson.Gson;

public class TangerineTester {

    private static final String doc = "Tangerine (Analysis Exchange) Test Client\n" + "\njava -jar client.jar\n\n"
                                      + "Usage:\n"
                                      + "  client.jar (analytics | collections | outputs)  [--host <hostname> --port <port> --count]\n"
                                      + "  client.jar collection <id> [--host <hostname> --port <port> --count ]\n"
                                      + "  client.jar analytic <name> [--host <hostname> --port <port> --count ]\n"
                                      + "  client.jar insert <file> <analyticName> [--uuid <uniqueIdentifier> --entity <entityName>  --key <key>  --host <hostname> --port <port>]\n"
                                      + "  client.jar drop <collectionId> [--host <hostname> --port <port>]\n"
                                      + "  client.jar reason <configuration> <ontology> <queries> [--outputFormat <type> --host <hostname> --port <port>]\n"
                                      + "  client.jar (-h | --help)\n" + "  client.jar --version\n" + "\n" + "Arguments:\n"
                                      + "  analytics  List supported analytics on the Tangerine Server.\n"
                                      + "  analytic  Retrieve datamap for the analytic.\n"
                                      + "  collection  Get the elements in a specific collection.\n"
                                      + "  collections  List data collections on Tangerine server.\n"
                                      + "  outputs  List output formats supported by the Tangerine server for returning reasoner responses.\n"
                                      + "  reasoner  Leverage the reasoner to query the data using a specified ontology.\n"
                                      + "  insert  Insert data into a specific collection using a specific analytic; optionally, create connections.\n" + "Options:\n"
                                      + "  --key <key>, -f <key>  Key to use when resolving entity [default: ].\n"
                                      + "  --uuid <uniqueIdentifier>, -u <uniqueIdentifier>  Unique identifier of entity [default: ].\n"
                                      + "  --entity <entity>, -e <entity>  Entity to resolve [default: ].\n"
                                      + "  --outputFormat <type>, -o <type>  Output format of reasoner response [default: org.mitre.tangerine.adapter.outbound.JsonAdapter].\n"
                                      + "  --count  Only return the number of items.\n"
                                      + "  --host <hostname>  Tangerine host server name or ip. [default: localhost]\n"
                                      + "  --port <port>  Port number Tangerine Server is running on. [default: 8080]\n"
                                      + "  --version  Show version.\n" + "  -h, --help  Show this screen.\n" + "\n";

    public static void main(String[] args) {
        Map<String, Object> opts = new Docopt(doc).withVersion("Tangerine (Analysis Exchange) Client 0.1").parse(args);

        AETClient aetClient = new AETClient((String) opts.get("--host"),
                                            (new Integer((String) opts.get("--port"))).intValue());
        ResponseModel response = null;

        if ((Boolean) opts.get("insert")) {
            String file = (String) opts.get("<file>"), analytic = (String) opts.get("<analyticName>"),
                   uuid = (String) opts.get("--uuid"), entity = (String) opts.get("--entity"),
                   key = (String) opts.get("--key");
            try {
                response = aetClient.insertData(file, analytic, uuid, entity, key);
            } catch (IOException e) {
                System.out.println("The file to insert could not be opened");
            }
            if (!response.hasError()) {
                System.out.println(new Gson().toJson(response));
            } else {
                System.out.println("Error message: " + response.getErrorMessage());
            }
        } else if ((Boolean) opts.get("reason")) {
            response = aetClient.reasoner((String) opts.get("<configuration>"), (String) opts.get("<ontology>"),
                                          (String) opts.get("<queries>"), (String) opts.get("--outputFormat"));
            System.out.println(response.getMessage());
        } else if ((Boolean) opts.get("drop")) {
            response = aetClient.dropCollection((String) opts.get("<collectionId>"));
            if (response.hasError()) {
                System.out.println(response.getErrorMessage());
            } else {
                System.out.println(response.getCollection() + " was dropped.");
            }
        } else if ((Boolean) opts.get("collections")) {
            response = aetClient.getCollections();
            if (response.hasError()) {
                System.out.println(response.getErrorMessage());
            } else if (!(Boolean) opts.get("--count")) {
                response.getCollections().forEach(System.out::println);
            } else {
                System.out.println("Collections: " + response.getCollections().size());
            }
        } else if ((Boolean) opts.get("analytics")) {
            response = aetClient.getAnalytics();
            if (response.hasError()) {
                System.out.println(response.getErrorMessage());
            } else if (!(Boolean) opts.get("--count")) {
                response.getAnalytics().forEach(System.out::println);
                System.out.println("Analytics:\n" + String.join("\n", response.getAnalytics()) + "\n");
            } else {
                System.out.println("Analytics: " + response.getAnalytics().size());
            }
        } else if ((Boolean) opts.get("outputs")) {
            response = aetClient.getOutputs();
            if (response.hasError()) {
                System.out.println(response.getErrorMessage());
            } else if (!(Boolean) opts.get("--count")) {
                response.getOutputs().forEach(System.out::println);
            } else {
                System.out.println("Outputs: " + response.getOutputs().size());
            }
        } else if ((Boolean) opts.get("analytic")) {
            response = aetClient.getAnalytic((String) opts.get("<name>"));
            if (response.hasError()) {
                System.out.println(response.getErrorMessage());
                return;
            }
            System.out.println(response.getDatamap());
        } else if ((Boolean) opts.get("collection")) {
            response = aetClient.getCollection((String) opts.get("<id>"));
            if (response.hasError()) {
                System.out.println(response.getErrorMessage());
                return;
            }
            response.getElements().forEach(System.out::println);
        }

    }
}
