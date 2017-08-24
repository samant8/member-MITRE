package org.mitre.tangerine.netowl.parser;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * Sample Java program to invoke NetOwl Extractor over HTTP. 
 */
public class NetOwlClient {
	
  public static String host = "ats-89";
  public static int    port = 8080;
  public static String text = "Obama went to D.C.";
  public static String metadata = "{ \"title\": \"My Title\", \"author\": \"My Author\" }";
  
  public static void main(String[] args) throws IOException {
    // Setup HTTP POST.  Specifying the language parameter is the preferred usage
    URL url = new URL("http://" + host + ":" + port + "/api/v2/_process?language=english");
    HttpURLConnection conn = (HttpURLConnection)url.openConnection();
    conn.setDoInput(true);
    conn.setDoOutput(true);
    conn.setUseCaches(false);
    conn.setRequestMethod("POST");
    conn.setRequestProperty("Connection", "Keep-Alive");
    
    // Set the content type
    conn.addRequestProperty("Content-Type", "text/plain");
    
    // Accept specifies format of the output.  It's good to specify as some platforms may insert undesired default.
    conn.addRequestProperty("Accept", "text/xml");
    
    // If we have metadata to send, specify in header we're sending them
    conn.addRequestProperty("X-NetOwl-metadata-content-type", "netowl-metadata/json");
    
    try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
      // Include metadata in HTTP body.  We use plain String here, 
      // but you can use JSON libraries like Jackson or GSON.
      writer.write(metadata);
      writer.write(0);  // deliminate with null
      writer.write(text);
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
  }
}
