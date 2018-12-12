package com.project.bsds.bsdsassignment.serverless;

import java.util.HashMap;
import java.util.Map;

public class StepCounterLambdaResponse {
  private int statusCode;
  private Map<String, String> headers;
  private String body;
  private boolean isBase64Encoded;

  public StepCounterLambdaResponse() {
  }

  public int getStatusCode() {
    return statusCode;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public String getBody() {
    return body;
  }

  public boolean getIsBase64Encoded() {
    return isBase64Encoded;
  }

  private StepCounterLambdaResponse(int statusCode, Map<String, String> headers, String body, boolean isBase64Encoded) {
    this.statusCode = statusCode;
    this.headers = headers;
    this.body = body;
    this.isBase64Encoded = isBase64Encoded;
  }

  public StepCounterLambdaResponseBuilder builder() {
    return new StepCounterLambdaResponseBuilder()
        .withStatusCode(this.getStatusCode())
        .withHeaders(this.getHeaders())
        .withBody(this.getBody())
        .withBase64Encoded(this.getIsBase64Encoded());
  }

  public static class StepCounterLambdaResponseBuilder {
    private int statusCode = 0;
    private Map<String, String> headers = new HashMap<>();
    private String body = "";
    private boolean isBase64Encoded = false;

    public StepCounterLambdaResponseBuilder withStatusCode(int statusCode) {
      this.statusCode = statusCode;
      return this;
    }

    public StepCounterLambdaResponseBuilder withHeaders(Map<String, String> headers) {
      this.headers = headers;
      return this;
    }

    public StepCounterLambdaResponseBuilder withBody(String body) {
      this.body = body;
      return this;
    }

    public StepCounterLambdaResponseBuilder withBase64Encoded(boolean base64Encoded) {
      isBase64Encoded = base64Encoded;
      return this;
    }

    public StepCounterLambdaResponse build() {
      return new StepCounterLambdaResponse(statusCode, headers, body, isBase64Encoded);
    }
  }

  @Override
  public String toString() {
    return "StepCounterLambdaResponse{" +
        "statusCode=" + statusCode +
        ", headers=" + headers +
        ", body='" + body + '\'' +
        ", isBase64Encoded=" + isBase64Encoded +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof StepCounterLambdaResponse)) return false;

    StepCounterLambdaResponse response = (StepCounterLambdaResponse) o;

    if (statusCode != response.statusCode) return false;
    if (isBase64Encoded != response.isBase64Encoded) return false;
    if (headers != null ? !headers.equals(response.headers) : response.headers != null) return false;
    return body != null ? body.equals(response.body) : response.body == null;

  }

  @Override
  public int hashCode() {
    int result = statusCode;
    result = 31 * result + (headers != null ? headers.hashCode() : 0);
    result = 31 * result + (body != null ? body.hashCode() : 0);
    result = 31 * result + (isBase64Encoded ? 1 : 0);
    return result;
  }
}
