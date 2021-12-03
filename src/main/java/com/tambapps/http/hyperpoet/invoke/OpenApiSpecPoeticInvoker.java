package com.tambapps.http.hyperpoet.invoke;

import static com.tambapps.http.hyperpoet.util.ParametersUtils.getOrDefault;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.tambapps.http.hyperpoet.HttpMethod;
import com.tambapps.http.hyperpoet.HttpPoet;
import com.tambapps.http.hyperpoet.url.UrlBuilder;
import groovy.lang.MissingMethodException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import okhttp3.HttpUrl;
import okio.BufferedSink;
import okio.Okio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class OpenApiSpecPoeticInvoker implements PoeticInvoker {

  private final Map<String, EndpointOperation> endpointOperationMap;
  private final OpenApiInteractionValidator validator;

  public static OpenApiSpecPoeticInvoker fromSpec(String spec) throws IOException {
    ParseOptions parseOptions = new ParseOptions();
    parseOptions.setResolve(true);
    parseOptions.setResolveFully(true);
    SwaggerParseResult result = new OpenAPIV3Parser().readContents(spec, null, parseOptions);

    if (result.getMessages() != null && !result.getMessages().isEmpty()) {
      throw new IOException("Error while parsing spec: " + String.join(", ", result.getMessages()));
    }
    return fromSpec(result.getOpenAPI(), OpenApiInteractionValidator.createFor(result.getOpenAPI()).build());
  }

  public static OpenApiSpecPoeticInvoker fromSpec(OpenAPI openAPI, OpenApiInteractionValidator validator) {
    Map<String, EndpointOperation> endpointOperationMap = new HashMap<>();
    for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
      String path = entry.getKey();
      PathItem item = entry.getValue();
      addOperation(endpointOperationMap, path, item.getGet(), HttpMethod.GET);
      addOperation(endpointOperationMap, path, item.getDelete(), HttpMethod.DELETE);
      addOperation(endpointOperationMap, path, item.getPost(), HttpMethod.POST);
      addOperation(endpointOperationMap, path, item.getPut(), HttpMethod.PUT);
      addOperation(endpointOperationMap, path, item.getPatch(), HttpMethod.PATCH);
    }

    return new OpenApiSpecPoeticInvoker(Collections.unmodifiableMap(endpointOperationMap), validator);
  }

  private static void addOperation(Map<String, EndpointOperation> endpointOperationMap, String path, Operation operation, HttpMethod method) {
    if (operation == null) return;
    endpointOperationMap.put(operation.getOperationId(), new EndpointOperation(path, method, operation));
  }

  @Override
  public Object invokeOrThrow(HttpPoet poet, String methodName, Object[] args,
      MissingMethodException e) throws IOException {
    EndpointOperation op = endpointOperationMap.get(methodName);
    if (op == null) {
      throw e;
    }
    Map<?, ?> additionalParams = getAdditionalParams(args);

    Request request = toRequest(poet, op, args, additionalParams);
    ValidationReport validationReport = validator.validateRequest(request);
    if (!validationReport.hasErrors()) {
      return poet.method(additionalParams, op.getPath(), op.getMethod());
    }

    StringBuilder messageBuilder = new StringBuilder();
    for (ValidationReport.Message message : validationReport.getMessages()) {
      messageBuilder.append(message.getMessage()).append("\n");
    }
    throw new IllegalArgumentException(messageBuilder.toString());
  }

  @SneakyThrows
  private Request toRequest(HttpPoet poet, EndpointOperation op, Object[] args,
      Map<?, ?> additionalParams) {
    String path = op.getPath(); // TODO resolve eventual path variables

    okhttp3.Request.Builder okBuilder = poet.request(new UrlBuilder(poet.getBaseUrl()).append(path).toString(), additionalParams);
    switch (op.getMethod()) {
      case GET:
        okBuilder.get();
        break;
      case DELETE:
        okBuilder.delete();
        break;
      case PUT:
        okBuilder.put(poet.requestBody(additionalParams));
        break;
      case POST:
        okBuilder.post(poet.requestBody(additionalParams));
        break;
      case PATCH:
        okBuilder.patch(poet.requestBody(additionalParams));
        break;
    }

    okhttp3.Request okHttpRequest = okBuilder.build();
    HttpUrl url = okHttpRequest.url();
    SimpleRequest.Builder builder = new SimpleRequest.Builder(Request.Method.valueOf(op.getMethod().toString()), op.getPath());
    for (int i = 0; i < url.querySize(); i++) {
      builder.withQueryParam(url.queryParameterName(i), url.queryParameterValue(i));
    }
    if (okHttpRequest.body() != null) {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      BufferedSink buffer = Okio.buffer(Okio.sink(bos));
      okHttpRequest.body().writeTo(buffer);
      buffer.flush();
      builder.withBody(bos.toByteArray());
    }

    return builder.build();
  }

  private Map<?, ?> getAdditionalParams(Object[] args) {
    return (Map<?, ?>) Arrays.stream(args).filter(a -> a instanceof Map).findFirst().orElse(Collections.emptyMap());
  }

  @Value
  private static class EndpointOperation {
    String path;
    HttpMethod method;
    Operation operation;
  }

}
