package com.project.bsds.bsdsassignment;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

/** @author manika2211 */
@Path("/stepcounter")
public class StepCounterService {

  private static StepCounterDB db;

  public StepCounterService() {
    db = StepCounterDB.getInstance();
  }

  @POST
  @Path(
      "/{userId : [1-9][0-9]*}/{day : [1-9][0-9]*}/{timeInterval : [0-9]|1[0-9]|2[0-3]}/{stepCount : \\d+}")
  public Response uploadStepsData(
      @PathParam("userId") int userId,
      @PathParam("day") int day,
      @PathParam("timeInterval") int timeInterval,
      @PathParam("stepCount") int stepCount) {
    try {
      db.insert(userId, day, timeInterval, stepCount);
      return Response.status(ErrorCodes.OK.getErrorCode()).entity("Successfully uploaded").build();
    } catch (IOException e) {
      String errMsg =
          "Failed to upload stepCount for input -> "
              + "userId: "
              + userId
              + ", "
              + "day: "
              + userId
              + ", "
              + "timeInterval: "
              + userId
              + ", "
              + "stepCount: "
              + stepCount;
      return Response.status(ErrorCodes.SERVER_ERROR.getErrorCode()).entity(errMsg).build();
    }
  }

  @GET
  @Path("/current/{userId : [1-9][0-9]*}")
  public Response getCurrentStepsForUser(@PathParam("userId") int userId) {
    try {
      int steps = db.getCurrentDayStepCount(userId);
      return Response.status(ErrorCodes.OK.getErrorCode()).entity(String.valueOf(steps)).build();
    } catch (IOException e) {
      String errMsg = "Failed to get current day stepCount for input -> " + "userId: " + userId;
      return Response.status(ErrorCodes.SERVER_ERROR.getErrorCode()).entity(errMsg).build();
    }
  }

  @GET
  @Path("/single/{userId : [1-9][0-9]*}/{day : [1-9][0-9]*}")
  public Response getSingleDayStepsForUser(
      @PathParam("userId") int userId, @PathParam("day") int day) {
    try {
      int steps = db.getStepCount(userId, day);
      return Response.status(ErrorCodes.OK.getErrorCode()).entity(String.valueOf(steps)).build();
    } catch (IOException e) {
      String errMsg =
          "Failed to get stepCount for input -> " + "userId: " + userId + ", " + "day: " + day;
      return Response.status(ErrorCodes.SERVER_ERROR.getErrorCode()).entity(errMsg).build();
    }
  }

  @GET
  @Path("/range/{userId : [1-9][0-9]*}/{startDay : [1-9][0-9]*}/{numDays : \\d+}")
  public Response getStepsForUserPerDay(
      @PathParam("userId") int userId,
      @PathParam("startDay") int startDay,
      @PathParam("numDays") int numDays) {
    try {
      List<Integer> stepsPerDay = db.getStepCountsInDays(userId, startDay, numDays);
      return Response.status(ErrorCodes.OK.getErrorCode())
          .entity(StringUtils.join(stepsPerDay, ","))
          .build();
    } catch (IOException e) {
      String errMsg =
          "Failed to get range stepsCount for input -> "
              + "userId: "
              + userId
              + ", "
              + "startDay: "
              + startDay
              + ", numDays: "
              + numDays;
      return Response.status(ErrorCodes.SERVER_ERROR.getErrorCode()).entity(errMsg).build();
    }
  }

  // health api called by loadbalancer to verify if node is healthy or not
  @GET
  @Path("/health")
  public Response checkHealth() {
    try {
      // checks db health by getting random day steps
      db.getRandomUserDayStepCount();
      return Response.status(ErrorCodes.OK.getErrorCode()).entity("success").build();
    } catch (IOException e) {
      return Response.status(ErrorCodes.SERVER_ERROR.getErrorCode()).entity("failure").build();
    }
  }

  // only for testing
  @GET
  @Path("/clear")
  public Response clearData() {
    try {
      db.checkOrCreateTable(true /* reset */);
      return Response.status(ErrorCodes.OK.getErrorCode())
          .entity("successfully truncated table")
          .build();
    } catch (IOException e) {
      String errMsg = "Failed to get clear table data";
      return Response.status(ErrorCodes.SERVER_ERROR.getErrorCode()).entity(errMsg).build();
    }
  }
}
