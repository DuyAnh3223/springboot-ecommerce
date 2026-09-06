package spring.abtechzone.modules.payment.gateway;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;

public final class GatewayCrypto {
    public static final ObjectMapper JSON = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);

    private GatewayCrypto() {}

    public static String hmac(String algorithm, String secret, String input) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm));
            return HexFormat.of().formatHex(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC unavailable");
        }
    }

    public static void verify(String expected, String actual) {
        if (actual == null
                || !actual.matches("[a-fA-F0-9]{" + expected.length() + "}")
                || !MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.US_ASCII),
                        actual.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII)))
            throw new GatewayException(GatewayException.Kind.SIGNATURE);
    }

    public static String text(JsonNode node, String field) {
        if (node == null || !node.isObject()) throw new GatewayException(GatewayException.Kind.DATA);
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isValueNode())
            throw new GatewayException(GatewayException.Kind.DATA);
        return value.asText();
    }

    public static String optional(JsonNode node, String field) {
        return node.path(field).asText("");
    }

    public static String fields(JsonNode node, String delimiter, String... names) {
        return Arrays.stream(names).map(name -> optional(node, name)).collect(Collectors.joining(delimiter));
    }

    public static String pairs(Map<String, String> fields, boolean encode) {
        return new TreeMap<>(fields)
                .entrySet().stream()
                        .map(e -> (encode ? encode(e.getKey()) : e.getKey()) + "="
                                + (encode ? encode(e.getValue()) : e.getValue()))
                        .collect(Collectors.joining("&"));
    }

    public static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    public static Map<String, String> strings(JsonNode node) {
        Map<String, String> result = new TreeMap<>();
        node.properties()
                .forEach(e -> result.put(
                        e.getKey(), e.getValue().isNull() ? "" : e.getValue().asText()));
        return result;
    }

    public static String payosCanonical(JsonNode data) {
        Map<String, String> values = new TreeMap<>();
        data.properties().forEach(e -> {
            JsonNode value = e.getValue();
            String s = value.isContainerNode() ? sorted(value).toString() : value.isNull() ? "" : value.asText();
            values.put(e.getKey(), Set.of("null", "undefined").contains(s) ? "" : s);
        });
        return pairs(values, false);
    }

    private static JsonNode sorted(JsonNode node) {
        if (node.isArray()) {
            var array = JSON.createArrayNode();
            node.forEach(n -> array.add(sorted(n)));
            return array;
        }
        if (node.isObject()) {
            var object = JSON.createObjectNode();
            new TreeMap<>(JSON.convertValue(node, Map.class))
                    .forEach((key, value) -> object.set(key.toString(), sorted(JSON.valueToTree(value))));
            return object;
        }
        return node;
    }
}
