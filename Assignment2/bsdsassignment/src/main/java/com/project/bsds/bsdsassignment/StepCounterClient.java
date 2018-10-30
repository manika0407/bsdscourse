package com.project.bsds.bsdsassignment;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import javax.ws.rs.core.MediaType;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;

public class StepCounterClient implements Closeable {
  private String baseUri;
  private Client client;
  private static final int CONNECT_TIMEOUT = 1000;
  private static final int READ_TIMEOUT = 2000;

  public StepCounterClient(String baseUri) {
    this.baseUri = baseUri;
    this.client = Client.create();
    this.client.setConnectTimeout(CONNECT_TIMEOUT);
    this.client.setReadTimeout(READ_TIMEOUT);
  }

  public StepCounterClient(String hostIp, int port, String uri) {
    this(
        new StringBuilder()
            .append(port == 0 /* direct path */ ? "https://" : "http://")
            .append(hostIp)
            .append(port == 0 /* direct path */ ? "" : ":" + port)
            .append(uri)
            .toString());
  }

  public StepCounterClient() {
    this("localhost", 8080, "/bsdsassignment2-webapp/stepcounter");
  }

  @Override
  public void close() throws IOException {
    client.destroy();
  }

  public void uploadStepsData(int userId, int day, int timeInterval, int stepCount) {
    String apiEndPoint =
            baseUri + "/" + userId + "/" + day + "/" + timeInterval + "/" + stepCount;
    WebResource webResource = client.resource(apiEndPoint);
    ClientResponse response = null;
    try {
      response =
          webResource
              .accept(MediaType.TEXT_PLAIN)
              .type(MediaType.TEXT_PLAIN)
              .post(ClientResponse.class);
      if (response.getStatus() != ErrorCodes.OK.getErrorCode()) {
        String errMsg = "";
        if (response.getStatus() == ErrorCodes.SERVER_ERROR.getErrorCode()) {
          errMsg = response.getEntity(String.class);
        }
        throw new RuntimeException(
            "Failed : HTTP error code : " + response.getStatus() + ", errMsg: " + errMsg);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally{
      if (response != null) {
        response.close();
      }
    }
  }

  public int getCurrentStepsForUser(int userId) {
    String apiEndPoint = baseUri + "/current/" + userId;
    WebResource webResource = client.resource(apiEndPoint);
    ClientResponse response = null;
    try {
      response = webResource.accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);
      if (response.getStatus() == ErrorCodes.OK.getErrorCode()) {
        return Integer.parseInt(response.getEntity(String.class));
      } else {
        String errMsg = "";
        if (response.getStatus() == ErrorCodes.SERVER_ERROR.getErrorCode()) {
          errMsg = response.getEntity(String.class);
        }
        throw new RuntimeException(
            "Failed : HTTP error code : " + response.getStatus() + ", errMsg: " + errMsg);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally{
      if (response != null) {
        response.close();
      }
    }
  }

  public int getSingleDayStepsForUser(int userId, int day) {
    String apiEndPoint = baseUri + "/single/" + userId + "/" + day;
    WebResource webResource = client.resource(apiEndPoint);
    ClientResponse response = null;
    try {
      response = webResource.accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);
      if (response.getStatus() == ErrorCodes.OK.getErrorCode()) {
        return Integer.parseInt(response.getEntity(String.class));
      } else {
        String errMsg = "";
        if (response.getStatus() == ErrorCodes.SERVER_ERROR.getErrorCode()) {
          errMsg = response.getEntity(String.class);
        }
        throw new RuntimeException(
            "Failed : HTTP error code : " + response.getStatus() + ", errMsg: " + errMsg);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally{
      if (response != null) {
        response.close();
      }
    }
  }

  public int[] getStepsForUserPerDay(int userId, int startDay, int numDays) {
    String apiEndPoint = baseUri + "/range/" + userId + "/" + startDay + "/" + numDays;
    WebResource webResource = client.resource(apiEndPoint);
    ClientResponse response = null;
    try {
      response = webResource.accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);
      if (response.getStatus() == ErrorCodes.OK.getErrorCode()) {
        String resp = response.getEntity(String.class);
        String[] arr = resp.split(",");
        int[] result = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
          result[i] = Integer.parseInt(arr[i]);
        }
        return result;
      } else {
        String errMsg = "";
        if (response.getStatus() == ErrorCodes.SERVER_ERROR.getErrorCode()) {
          errMsg = response.getEntity(String.class);
        }
        throw new RuntimeException(
            "Failed : HTTP error code : " + response.getStatus() + ", errMsg: " + errMsg);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally{
      if (response != null) {
        response.close();
      }
    }
  }

  // for testing
  public void clearData() {
    String apiEndPoint = baseUri + "/clear";
    WebResource webResource = client.resource(apiEndPoint);
    ClientResponse response = null;
    try {
      response = webResource.accept(MediaType.TEXT_PLAIN).get(ClientResponse.class);
      if (response.getStatus() != ErrorCodes.OK.getErrorCode()) {
        String errMsg = "";
        if (response.getStatus() == ErrorCodes.SERVER_ERROR.getErrorCode()) {
          errMsg = response.getEntity(String.class);
        }
        throw new RuntimeException(
            "Failed : HTTP error code : " + response.getStatus() + ", errMsg: " + errMsg);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally{
      if (response != null) {
        response.close();
      }
    }
  }

  public static void main(String[] args) {
    StepCounterClient sc = new StepCounterClient();
    System.out.println("Testing localhost");
    sc.clearData();
    sc.uploadStepsData(1, 1, 1, 500);
    sc.uploadStepsData(1, 1, 2, 200);
    sc.uploadStepsData(1, 2, 3, 300);
    sc.uploadStepsData(2, 2, 4, 400);

    System.out.println(sc.getCurrentStepsForUser(2));
    System.out.println(sc.getSingleDayStepsForUser(1, 1));
    System.out.println(Arrays.toString(sc.getStepsForUserPerDay(1, 1, 2)));
  }
}
