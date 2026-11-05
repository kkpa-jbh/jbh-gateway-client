package com.jbh.gateway.internal.core.http.adapter;

import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.core.http.JbhHttpClientAdapter;
import com.jbh.gateway.internal.core.http.model.JbhHttpRequest;
import com.jbh.gateway.internal.core.resilience.CircuitOpenException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;

/**
 * Wraps another {@link JbhHttpClientAdapter} with a circuit breaker. The wrapped adapter keeps its
 * own retries, so one call here (with all its retries) counts once in the breaker.
 *
 * <p>While the breaker is open, {@link #execute} throws {@link CircuitOpenException} and sends
 * nothing.
 */
public class CircuitBreakerHttpClientAdapter implements JbhHttpClientAdapter {

  private final JbhHttpClientAdapter delegate;
  private final CircuitBreaker circuitBreaker;

  public CircuitBreakerHttpClientAdapter(
      JbhHttpClientAdapter delegate, CircuitBreaker circuitBreaker) {
    this.delegate = delegate;
    this.circuitBreaker = circuitBreaker;
  }

  @Override
  public JbhHttpResponse execute(JbhHttpRequest request) throws IOException, InterruptedException {
    try {
      return circuitBreaker.executeCheckedSupplier(() -> delegate.execute(request));
    } catch (CallNotPermittedException e) {
      throw new CircuitOpenException(circuitBreaker.getName(), e);
    } catch (IOException | InterruptedException | RuntimeException | Error e) {
      throw e;
    } catch (Throwable e) {
      // The delegate declares only IOException and InterruptedException. This never happens.
      throw new IOException("Unexpected error in circuit breaker " + circuitBreaker.getName(), e);
    }
  }
}
