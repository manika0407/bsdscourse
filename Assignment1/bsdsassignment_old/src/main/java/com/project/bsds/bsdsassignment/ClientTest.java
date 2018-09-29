package com.project.bsds.bsdsassignment;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MediaType;

public class ClientTest {
    private Client client;
    private String baseUri;
    
        
    public ClientTest(String baseUri) {
        this.client = Client.create();
        this.baseUri = baseUri;
    }
    
    public ClientTest(String hostIp, int port, String uri) {
        this(new StringBuilder()
                .append("http://")
                .append(hostIp)
                .append(port > 0 ? ":" + port : "")
                .append(uri)
                .toString());
    }

    public ClientTest() {
        this("localhost", 8080, "/bsdsassignment/rest/hello/rs");
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
            ClientResponse response = webResource.type(MediaType.APPLICATION_JSON)
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
          System.out.println(ct.getStatusCall("abc"));
          System.out.println(ct.postDataCall("def"));
          ct = new ClientTest("https://h9jb4om09f.execute-api.us-west-2.amazonaws.com/prod/bsdsassignment1");
          System.out.println(ct.getStatusCall("abc"));
          System.out.println(ct.postDataCall("def"));
      }
}