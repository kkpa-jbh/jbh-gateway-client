package com.jbh.api.client.vo;

public class JbhApiException extends Exception {

  private final String errorCode;
  private final Integer httpStatusCode;

  public JbhApiException(String message) {
    super(message);
    this.errorCode = null;
    this.httpStatusCode = null;
  }

  public JbhApiException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = null;
    this.httpStatusCode = null;
  }

  public JbhApiException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
    this.httpStatusCode = null;
  }

  public JbhApiException(String message, String errorCode, Integer httpStatusCode) {
    super(message);
    this.errorCode = errorCode;
    this.httpStatusCode = httpStatusCode;
  }

  public JbhApiException(String message, String errorCode, Integer httpStatusCode, Throwable cause) {
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
