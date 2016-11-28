package io.vertx.core.http;

import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.streams.ReadStream;

/**
 * A builder for configuring client-side HTTP requests.
 * <p>
 * Instances are created by an {@link HttpClient} instance, via one of the methods {@code createXXX} corresponding to the
 * specific HTTP methods.
 * <p>
 * The request builder shall be configured prior making a request, the builder is immutable and when a configuration method
 * is called, a new builder is returned allowing to reuse builder as templates and apply further customization.
 * <p>
 * After the request builder has been configured, the methods
 * <ul>
 *   <li>{@link #send(Handler)}</li>
 *   <li>{@link #send(ReadStream, Handler)}</li>
 *   <li>{@link #bufferBody()}</li>
 * </ul>
 * can be called.
 * <p>
 * The {@code #bufferBody} configures the builder to buffer the entire HTTP response body and returns a
 * {@link HttpClientResponseBuilder} for configuring the response body.
 * <p>
 * The {@code send} methods perform the actual request, they can be used multiple times to perform HTTP requests.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
@VertxGen
public interface HttpClientRequestBuilder {

  /**
   * Configure the builder to use a new method {@code value}.
   *
   * @return a new {@code HttpClientRequestBuilder} instance with the specified method {@code value}
   */
  HttpClientRequestBuilder method(HttpMethod value);

  /**
   * Configure the builder to use a new port {@code value}.
   *
   * @return a new {@code HttpClientRequestBuilder} instance with the specified port {@code value}
   */
  HttpClientRequestBuilder port(int value);

  /**
   * Configure the builder to use a new host {@code value}.
   *
   * @return a new {@code HttpClientRequestBuilder} instance with the specified host {@code value}
   */
  HttpClientRequestBuilder host(String value);

  /**
   * Configure the builder to use a new request URI {@code value}.
   *
   * @return a new {@code HttpClientRequestBuilder} instance with the specified request URI {@code value}
   */
  HttpClientRequestBuilder requestURI(String value);

  /**
   * Configure the builder to add a new HTTP header.
   *
   * @param name the header name
   * @param value the header value
   * @return a new {@code HttpClientRequestBuilder} instance with the specified header
   */
  HttpClientRequestBuilder putHeader(String name, String value);

  // Could be called end() instead ?
  // For RxJava we could generate with send(Observable<Buffer> stream
  // would it work well ? i.e could the Observable<Buffer> could be subscribed easily ?
  // in the case of FileSystem it *should* work (need to check):

  // Observable<Buffer> fileObs = fileSystem.open("file.txt", new OpenOptions());
  // Observable<HttpClientResponse> respObs = httpClient.createPost().send(fileObs);
  // respObs.subscribe(resp -> {});

  // the idea is that when calling a method with an Observable, the original method should be called
  // with a subscription
  // as the Handler<AsyncResult> delays the call (on subscribe) it should work

  /**
   * Like {@link #send(Handler)} but with an HTTP request {@code body} stream.
   *
   * @param body the body
   */
  void send(ReadStream<Buffer> body, Handler<AsyncResult<HttpClientResponse>> handler);

  /**
   * Send a request, the {@code handler} will receive the response as an {@link HttpClientResponse}.
   */
  void send(Handler<AsyncResult<HttpClientResponse>> handler);

  /**
   * Configure to buffer the body and returns a {@link HttpClientResponseBuilder<Buffer>} for further configuration of
   * the response or {@link HttpClientResponseBuilder#send(Handler) sending} the request.
   */
  HttpClientResponseBuilder<Buffer> bufferBody();

}