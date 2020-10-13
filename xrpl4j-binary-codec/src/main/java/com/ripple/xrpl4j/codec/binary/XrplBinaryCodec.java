package com.ripple.xrpl4j.codec.binary;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.ripple.xrpl4j.codec.addresses.UnsignedByteArray;
import com.ripple.xrpl4j.codec.binary.definitions.DefinitionsService;
import com.ripple.xrpl4j.codec.binary.definitions.FieldInstance;
import com.ripple.xrpl4j.codec.binary.serdes.BinaryParser;
import com.ripple.xrpl4j.codec.binary.types.AccountIdType;
import com.ripple.xrpl4j.codec.binary.types.STObjectType;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class XrplBinaryCodec {

  public static final String TRX_SIGNATURE_PREFIX = "53545800";

  public static final String TRX_MULTI_SIGNATURE_PREFIX = "534D5400";

  private DefinitionsService definitionsService = DefinitionsService.getInstance();

  private ObjectMapper objectMapper = BinaryCodecObjectMapperFactory.getObjectMapper();

  /**
   * Encodes JSON to canonical XRPL binary as a hex string.
   *
   * @param json String containing JSON to be encoded.
   * @return hex encoded representations
   * @throws JsonProcessingException if JSON is not valid.
   */
  public String encode(String json) throws JsonProcessingException {
    JsonNode node = BinaryCodecObjectMapperFactory.getObjectMapper().readTree(json);
    return encode(node);
  }

  /**
   * Encodes JSON to canonical XRPL binary as a hex string.
   *
   * @param json String containing JSON to be encoded.
   * @return hex encoded representations
   * @throws JsonProcessingException if JSON is not valid.
   */
  public String encodeForSigning(String json) throws JsonProcessingException {
    JsonNode node = BinaryCodecObjectMapperFactory.getObjectMapper().readTree(json);
    return TRX_SIGNATURE_PREFIX + encode(removeNonSigningFields(node));
  }

  /**
   * Encodes JSON to canonical XRPL binary as a hex string for signing purposes.
   *
   * @param json String containing JSON to be encoded.
   * @return hex encoded representations
   * @throws JsonProcessingException if JSON is not valid.
   */
  public String encodeForMultiSigning(String json, String xrpAccountId) throws JsonProcessingException {
    JsonNode node = BinaryCodecObjectMapperFactory.getObjectMapper().readTree(json);
    if (!node.isObject()) {
      throw new IllegalArgumentException("JSON object required for signing");
    }
    // any existing signing keys should not also be signed
    ((ObjectNode) node).set("SigningPubKey", new TextNode(""));
    String suffix = new AccountIdType().fromJSON(new TextNode(xrpAccountId)).toHex();
    return TRX_MULTI_SIGNATURE_PREFIX + encode(removeNonSigningFields(node)) + suffix;
  }

  /**
   * Decodes canonical XRPL binary hex string to JSON.
   *
   * @param hex value to decode.
   * @return
   * @throws JsonProcessingException
   */
  public String decode(String hex) {
    return new BinaryParser(hex).readType(STObjectType.class)
        .toJSON()
        .toString();
  }

  private String encode(JsonNode node) {
    UnsignedByteArray byteList = UnsignedByteArray.empty();
    new STObjectType().fromJSON(node).toBytesSink(byteList);
    return byteList.hexValue();
  }

  /**
   * Creates a deep copy of the given node, removing any fields that are not flagged as signing fields
   * according to the defintion metadata.
   *
   * @param node
   * @return deep clone with non-signing fields omitted
   */
  private JsonNode removeNonSigningFields(JsonNode node) {
    if (!node.isObject()) {
      // signing field filtering is based on JSON field names
      // since JSON objects contain field names, no further processing has to be
      // done on non-objects like arrays or simple values.
      return node;
    }

    Map<String, JsonNode> signingFields = Lists.newArrayList(node.fieldNames())
        .stream()
        .filter(fieldName -> isSigningField(fieldName))
        .collect(Collectors.toMap(Function.identity(), node::get));

    return new ObjectNode(objectMapper.getNodeFactory(), signingFields);
  }

  private Boolean isSigningField(String fieldName) {
    return definitionsService.getFieldInstance(fieldName).map(FieldInstance::isSigningField).orElse(false);
  }


}