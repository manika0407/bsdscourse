/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.project.bsds.bsdsassignment;


import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.QueryParam;

/**
 *
 * @author manika2211
 */
@Path("/hello")
public class BSDSService {
        @GET
        @Path("/rs")
        public Response getStatus(@QueryParam("input") String input) {
            String output = "alive -> input: " + input;
            return Response.status(200).entity(output).build(); 
	}
    
        @POST
        @Path("/rs")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response postText(TestRequestObject content) {
            String output = "alive -> input: " + content.getInput();
            return Response.status(200).entity(output).build();   
        }
       
        
}
