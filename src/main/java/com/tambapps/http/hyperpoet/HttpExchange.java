package com.tambapps.http.hyperpoet;

import com.tambapps.http.hyperpoet.util.CachedResponseBody;
import groovy.lang.Closure;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.util.List;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpExchange {

  public static HttpExchange newInstance(Response response, Object requestBody, Closure<?> parser) {
    if (response != null && response.body() != null && !(response.body() instanceof CachedResponseBody)) {
      throw new IllegalArgumentException("Response body should have been cached");
    }
    return new HttpExchange(response, requestBody, parser);
  }

  @Getter
  Response response;
  @Getter
  Object requestBody;
  Closure<?> parser;

  public Request getRequest() {
    return response.request();
  }

  public RequestBody getRawRequestBody() {
    return getRequest().body();
  }

  public int getResponseCode() {
    return response.code();
  }

  public Map<String, List<String>> getResponseHeaders() {
    return response.headers().toMultimap();
  }

  public Map<String, List<String>> getRequestHeaders() {
    return getRequest().headers().toMultimap();
  }

  public ResponseBody getRawResponseBody() {
    return response.body();
  }

  public Object getResponseBody() {
    if (getRawResponseBody() == null || parser == null) {
      return null;
    } else {
      return parser.call(getRawResponseBody());
    }
  }
}