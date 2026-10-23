package com.jbh.gateway.internal.core.http.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.gateway.client.JbhHttpResponse;
import com.jbh.gateway.internal.config.JbhHttpClientConfig;
import com.jbh.gateway.internal.config.JbhRetryConfig;
import com.jbh.gateway.internal.core.http.model.JbhHttpRequest;
import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Retry rules of the native adapter, against a local MockWebServer. */
class NativeHttpClientAdapterTest {

  private static final Duration RETRY_DELAY = Duration.ofMillis(100);

  private MockWebServer server;
  private NativeHttpClientAdapter adapter;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();
    adapter = new NativeHttpClientAdapter(config(Duration.ofSeconds(5)));
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  private static JbhHttpClientConfig config(Duration requestTimeout) {
    return JbhHttpClientConfig.builder()
        .requestTimeout(requestTimeout)
        .retryConfig(JbhRetryConfig.builder().maxAttempts(3).initialDelay(RETRY_DELAY).build())
        .build();
  }

  private String url() {
    return server.url("/jbh-api/test").toString();
  }

  private static JbhHttpRequest post(String url, Duration timeout) {
    return JbhHttpRequest.post(url).jsonBody("{\"a\":1}").timeout(timeout).build();
  }

  @Test
  void getIsRetriedOn503() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(503));
    server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

    JbhHttpResponse response = adapter.execute(JbhHttpRequest.get(url()).build());

    assertEquals(200, response.getStatusCode());
    assertEquals(2, server.getRequestCount());
  }

  @Test
  void postIsNotRetriedOn504() throws Exception {
    server.enqueue(new MockResponse().setResponseCode(504));
    server.enqueue(new MockResponse().setResponseCode(200));

    JbhHttpResponse response = adapter.execute(post(url(), Duration.ofSeconds(5)));

    assertEquals(504, response.getStatusCode());
    assertEquals(1, server.getRequestCount());
  }

  @Test
  void postIsNotRetriedOnTimeout() {
    server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
    server.enqueue(new MockResponse().setResponseCode(200));
    NativeHttpClientAdapter shortTimeoutAdapter =
        new NativeHttpClientAdapter(config(Duration.ofMillis(300)));

    assertThrows(
        HttpTimeoutException.class,
        () -> shortTimeoutAdapter.execute(post(url(), Duration.ofMillis(300))));
    assertEquals(1, server.getRequestCount());
  }

  @Test
  void postIsRetriedWhenConnectionFails() throws IOException {
    String closedPortUrl = "http://127.0.0.1:" + freePort() + "/jbh-api/test";

    long start = System.nanoTime();
    assertThrows(
        ConnectException.class, () -> adapter.execute(post(closedPortUrl, Duration.ofSeconds(5))));
    Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

    // 3 attempts wait 100 ms, then 200 ms. A request with no retry fails at once.
    assertTrue(
        elapsed.compareTo(RETRY_DELAY.multipliedBy(3)) >= 0,
        "POST should be retried when the connection fails, elapsed " + elapsed.toMillis() + " ms");
  }

  /** A port with nothing listening on it: connecting to it is refused. */
  private static int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
