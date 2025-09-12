package com.jbh.gateway.client;

public class JbhGatewayException extends Exception {

  private final String errorCode;
  private final Integer httpStatusCode;

  public JbhGatewayException(String message) {
    super(message);
    this.errorCode = null;
    this.httpStatusCode = null;
  }

  public JbhGatewayException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = null;
    this.httpStatusCode = null;
  }

  public JbhGatewayException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
    this.httpStatusCode = null;
  }

  public JbhGatewayException(String message, String errorCode, Integer httpStatusCode) {
    super(message);
    this.errorCode = errorCode;
    this.httpStatusCode = httpStatusCode;
  }

  public JbhGatewayException(String message, String errorCode, Integer httpStatusCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.httpStatusCode = httpStatusCode;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public Integer getHttpStatusCode() {
    return httpStatusCode;
  }
}
