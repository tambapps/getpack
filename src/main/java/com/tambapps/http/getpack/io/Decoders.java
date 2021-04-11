package com.tambapps.http.getpack.io;

import com.tambapps.http.getpack.ContentType;
import com.tambapps.http.getpack.ContentType;
import groovy.json.JsonSlurper;
import groovy.lang.Closure;
import groovy.util.XmlSlurper;
import okhttp3.ResponseBody;
import org.codehaus.groovy.runtime.MethodClosure;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Decoders {

  private Decoders() {}

  public static Map<ContentType, Closure<?>> getMap() {
    Map<ContentType, Closure<?>> map = new HashMap<>();
    map.put(ContentType.JSON, new MethodClosure(Decoders.class, "decodeJsonResponseBody"));
    map.put(ContentType.XML, new MethodClosure(Decoders.class, "decodeXmlResponseBody"));
    map.put(ContentType.TEXT, new MethodClosure(Decoders.class, "decodeStringResponseBody"));
    map.put(ContentType.HTML, new MethodClosure(Decoders.class, "decodeStringResponseBody"));
    map.put(ContentType.BINARY, new MethodClosure(Decoders.class, "decodeBytesResponseBody"));
    // default decoder (when no content type was found)
    map.put(null, new MethodClosure(Decoders.class, "decodeStringResponseBody"));
    return map;
  }

  public static Object decodeJsonResponseBody(ResponseBody body) throws IOException {
    return new JsonSlurper().parseText(body.string());
  }

  public static Object decodeXmlResponseBody(ResponseBody body) throws IOException {
    try {
      return new XmlSlurper().parseText(body.string());
    } catch (SAXException | ParserConfigurationException e) {
      throw new IOException("An error occureed whilte attempting to load XML resopnse body");
    }
  }

  public static String decodeStringResponseBody(ResponseBody body) throws IOException {
    return body.string();
  }

  public static byte[] decodeBytesResponseBody(ResponseBody body) throws IOException {
    return body.bytes();
  }
}
