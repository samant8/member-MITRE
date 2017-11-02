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

package org.mitre.tangerine.apps;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * NetOwl client --
 * @author ubaldino
 *
 */
public class NetOwlApp {

    public static void main(String[] args) throws IOException {
        String metadata = "{ \"title\": \"NetOwl Application\"}";
        Options options = new Options();
        HelpFormatter formatter = new HelpFormatter();
        CommandLineParser parser = new DefaultParser();

        options.addOption("text", true, "text to parse");
        //options.addOption("document", true, "document to parse");
        options.addOption("server", true, "NetOwl Server Base URL, 'http://host:port'");

        try {
            CommandLine cmd = parser.parse(options, args);
            String argText = cmd.getOptionValue("text");
            //String argDocument = cmd.getOptionValue("document (not implemented)");
            String server = cmd.getOptionValue("server");
            if (argText == null || server == null) {
                System.out.println("-text and -server arguents are required.");
                System.exit(-1);
            }
            if (server.endsWith("/")) {
                server = server.replaceFirst("\\/+$", "");
            }

            URL url = new URL(server +"/api/v2/_process?language=english");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.addRequestProperty("Accept", "text/xml");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.addRequestProperty("Content-Type", "text/plain");
            conn.addRequestProperty("X-NetOwl-metadata-content-type", "netowl-metadata/json");

            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
                // Include metadata in HTTP body. We use plain String here,
                // but you can use JSON libraries like Jackson or GSON.
                writer.write(metadata);
                writer.write(0); // deliminate with null
                if (cmd.hasOption("text") && argText != null) {
                    // We suspect data on command line that contains "$" currency values
                    // may require escaping. Still needs to be vetted:
                    String modText = argText.replaceAll("\\$", "\\\\\\$");
                    // Write text to stream
                    writer.write(modText);
                }
                // Not implemented
                // else if (cmd.hasOption("document") && argDocument != null) {
                //
                else {
                    System.out.println("Make sure you are using the correct arguments\n");
                    formatter.printHelp("NetOwl Application", options);
                    System.exit(1);
                }
                writer.flush();
            }

            // Connect and check for return code
            conn.connect();
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                String message;
                try (Scanner scanner = new Scanner(conn.getErrorStream(), "UTF-8")) {
                    message = scanner.useDelimiter("\\A").next();
                }
                conn.disconnect();
                throw new RuntimeException("Extractor returned code: " + code + "\nMessage: " + message);
            }

            // Read result from stream
            try (Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8")) {
                String result = scanner.useDelimiter("\\A").next();
                System.out.println(result);
            }
            conn.disconnect();

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
