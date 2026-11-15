package com.jbh.gateway.internal.core.http.adapter;

import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.core.http.JbhHttpClientAdapter;
import com.jbh.gateway.internal.core.http.model.JbhHttpHeaders;
import com.jbh.gateway.internal.core.http.model.JbhHttpRequest;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Wraps another {@link JbhHttpClientAdapter} with one CLIENT span per call. The span covers all
 * retries and the circuit breaker below it. It puts the trace context in the request headers.
 *
 * <p>No header value, query string, body or raw path goes into the span. Exceptions are not
 * recorded as events, because their messages can hold a URL or user data.
 */
public class TracingHttpClientAdapter implements JbhHttpClientAdapter {

  private static final String TRACER_NAME = "com.jbh.gateway";
  private static final AttributeKey<String> HTTP_METHOD =
      AttributeKey.stringKey("http.request.method");
  private static final AttributeKey<String> URL_TEMPLATE = AttributeKey.stringKey("url.template");
  private static final AttributeKey<String> SERVER_ADDRESS =
      AttributeKey.stringKey("server.address");
  private static final AttributeKey<Long> STATUS_CODE =
      AttributeKey.longKey("http.response.status_code");
  private static final AttributeKey<String> ERROR_TYPE = AttributeKey.stringKey("error.type");

  private final JbhHttpClientAdapter delegate;
  private final OpenTelemetry openTelemetry;
  private final Tracer tracer;

  public TracingHttpClientAdapter(JbhHttpClientAdapter delegate, OpenTelemetry openTelemetry) {
    this.delegate = delegate;
    this.openTelemetry = openTelemetry;
    this.tracer = openTelemetry.getTracer(TRACER_NAME);
  }

  @Override
  public JbhHttpResponse execute(JbhHttpRequest request) throws IOException, InterruptedException {
    Span span = startSpan(request);
    try (Scope ignored = span.makeCurrent()) {
      JbhHttpResponse response = delegate.execute(injectContext(request, span));
      recordReply(span, response);
      return response;
    } catch (IOException | InterruptedException | RuntimeException | Error e) {
      recordFailure(span, e);
      throw e;
    } finally {
      span.end();
    }
  }

  private Span startSpan(JbhHttpRequest request) {
    String method = request.getMethod().name();
    String template = request.getUriTemplate();
    String name = template == null ? method : method + " " + template;
    Span span =
        tracer
            .spanBuilder(name)
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute(HTTP_METHOD, method)
            .startSpan();
    if (template != null) {
      span.setAttribute(URL_TEMPLATE, template);
    }
    String host = request.getUri().getHost();
    if (host != null) {
      span.setAttribute(SERVER_ADDRESS, host);
    }
    return span;
  }

  /**
   * Returns the same request when the propagator adds nothing (the no-op instance). Otherwise it
   * first drops every header this propagator owns. The W3C propagator writes no {@code tracestate}
   * when it is empty, so a caller's old {@code tracestate} would stay next to the new {@code
   * traceparent}.
   */
  private JbhHttpRequest injectContext(JbhHttpRequest request, Span span) {
    TextMapPropagator propagator = openTelemetry.getPropagators().getTextMapPropagator();
    Map<String, String> carrier = new HashMap<>();
    propagator.inject(
        Context.current().with(span), carrier, (map, key, value) -> map.put(key, value));
    if (carrier.isEmpty()) {
      return request;
    }
    JbhHttpHeaders headers = request.getHeaders();
    for (String field : propagator.fields()) {
      headers = headers.without(field);
    }
    for (Map.Entry<String, String> entry : carrier.entrySet()) {
      headers = headers.withReplaced(entry.getKey(), entry.getValue());
    }
    return request.withHeaders(headers);
  }

  private void recordReply(Span span, JbhHttpResponse response) {
    int status = response.getStatusCode();
    span.setAttribute(STATUS_CODE, (long) status);
    if (status >= 500) {
      span.setAttribute(ERROR_TYPE, String.valueOf(status));
      span.setStatus(StatusCode.ERROR);
    }
  }

  private void recordFailure(Span span, Throwable error) {
    span.setAttribute(ERROR_TYPE, error.getClass().getName());
    span.setStatus(StatusCode.ERROR);
  }
}
