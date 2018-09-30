package com.project.bsds.bsdsassignment;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import javax.ws.rs.core.MediaType;

public class ClientTest {
    private String baseUri;
    private Client client;
    private final static int CONNECT_TIMEOUT = 5000;
    private final static int READ_TIMEOUT = 10000;
        
    public ClientTest(String baseUri) {
        this.baseUri = baseUri;
        this.client = Client.create();
        this.client.setConnectTimeout(CONNECT_TIMEOUT);
        this.client.setReadTimeout(READ_TIMEOUT);
    }
    
    public ClientTest(String hostIp, int port, String uri) {
        this(new StringBuilder()
                .append(port == 0 /* direct path */ ? "https://" : "http://")
                .append(hostIp)
                .append(port == 0 /* direct path */ ? "" : ":" + port)
                .append(uri)
                .toString());
    }

    public ClientTest() {
        this("localhost", 8080, "/bsdsassignment-webapp/rest/hello/rs");
    }
 
    public String getStatusCall(String input) {
    	try {
            WebResource webResource = client
               .resource(this.baseUri + "?input=" + input);
            ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN)
               .get(ClientResponse.class);

            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                     + response.getStatus());
            }
            return response.getEntity(String.class);
	  } catch (Exception e) {
            throw new RuntimeException(e);
	  }
    }
    
    public String postDataCall(String input) {
        try {
            WebResource webResource = client
               .resource(baseUri);
            ClientResponse response = webResource
                    .accept(MediaType.APPLICATION_JSON)
                    .type(MediaType.APPLICATION_JSON)
                    .post(ClientResponse.class, "{\"input\":\"" + input+ "\"}");

            if (response.getStatus() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                     + response.getStatus());
            }
            return response.getEntity(String.class);
	  } catch (Exception e) {
            throw new RuntimeException(e);
	  }
    }
    
      public static void main(String[] args) {
          ClientTest ct = new ClientTest();
          System.out.println("Testing localhost");
          System.out.println(ct.getStatusCall("abc"));
          System.out.println(ct.postDataCall("def"));
          
          System.out.println("Testing ec2 instance");
          ct = new ClientTest("ec2-34-220-61-97.us-west-2.compute.amazonaws.com", 8080, "/bsdsassignment-webapp/rest/hello/rs");
          System.out.println(ct.getStatusCall("abc"));
          System.out.println(ct.postDataCall("def"));
          
          System.out.println("Testing lambda");
          ct = new ClientTest("h9jb4om09f.execute-api.us-west-2.amazonaws.com", 0, "/prod/bsdsassignment1");
          System.out.println(ct.getStatusCall("abc"));
          System.out.println(ct.postDataCall("def"));
          
      }
}