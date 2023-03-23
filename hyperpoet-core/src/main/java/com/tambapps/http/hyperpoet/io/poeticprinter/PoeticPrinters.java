package com.tambapps.http.hyperpoet.io.poeticprinter;

import com.tambapps.http.hyperpoet.ContentType;
import com.tambapps.http.hyperpoet.io.json.JsonGenerator;
import com.tambapps.http.hyperpoet.util.ContentTypeMapFunction;

public final class PoeticPrinters {

  private PoeticPrinters() {}

  public static ContentTypeMapFunction getMap(JsonGenerator generator) {
    ContentTypeMapFunction map = new ContentTypeMapFunction();
    map.put(ContentType.JSON, (rawData) -> {
      System.out.println(generator.composeRawDataToPrettyColoredJson(rawData));
      return null;
    });
    return map;
  }
}
