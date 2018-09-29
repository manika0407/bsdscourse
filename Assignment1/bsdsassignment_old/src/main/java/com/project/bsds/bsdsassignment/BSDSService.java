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
/**
 *
 * @author manika2211
 */
@Path("/hello")
public class BSDSService {
    
        @GET
	@Path("/{param}")
	public Response getMsg(@PathParam("param") String msg) {
		String output = "Jersey say : " + msg;
		return Response.status(200).entity(output).build();
 
	}
    
        @POST
        @Path("/postdata")
        @Consumes(MediaType.TEXT_PLAIN)
        public Response postText(String content){
            return Response.status(201).entity(String.valueOf(content.length())).build();
        }
       
        
}
