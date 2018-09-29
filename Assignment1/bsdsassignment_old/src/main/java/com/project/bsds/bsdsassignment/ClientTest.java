package com.project.bsds.bsdsassignment;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import javax.ws.rs.core.MediaType;

public class ClientTest {
    private Client client;
    private String baseUri;
    
    public ClientTest(String hostIp, int port) {
        this.client = Client.create();
        this.baseUri = new StringBuilder().append("http://").append(hostIp).append(":").append(port).append("/bsdsassignment/rest/hello").toString();
        //this.baseUri = "http://localhost:8080/bsdsassignment/rest/hello";
    }
    
    public ClientTest() {
        this("localhost", 8080);
    }
    
    public String getCall(String input) {
    	try {
		WebResource webResource = client
		   .resource(this.baseUri + "/" + input);
                ClientResponse response = webResource.accept(MediaType.TEXT_PLAIN)
                   .get(ClientResponse.class);

		if (response.getStatus() != 200) {
		   throw new RuntimeException("Failed : HTTP error code : "
			+ response.getStatus());
		}
               
		return response.getEntity(String.class);
	  } catch (Exception e) {
//		e.printStackTrace();
                throw new RuntimeException(e);
	  }
    }
    
    public String postCall(String input) {
        try {
		WebResource webResource = client
		   .resource(baseUri + "/postdata");
                ClientResponse response = webResource.type(MediaType.TEXT_PLAIN)
		   .post(ClientResponse.class, input);

		if (response.getStatus() != 201) {
			throw new RuntimeException("Failed : HTTP error code : "
			     + response.getStatus());
		}
                return response.getEntity(String.class);
	  } catch (Exception e) {
//		e.printStackTrace();
            throw new RuntimeException(e);

	  }
    }
    
      public static void main(String[] args) {
          ClientTest ct = new ClientTest();
          System.out.println(ct.getCall("manikacutehai"));
          System.out.println(ct.postCall("Abhinav"));
      }
}