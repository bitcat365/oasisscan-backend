package romever.scan.oasisscan.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public abstract class Mappers {

    public static final ObjectMapper jsonMapper;
    public static final ObjectMapper prettyJsonMapper;
    private static final JsonFactory jsonFactory;
    private static final ObjectMapper cborMapper;
    private static final Base64.Decoder base64Decoder;

    static {
        jsonMapper = Jackson2ObjectMapperBuilder.json().simpleDateFormat("yyyy-MM-dd HH:mm:ss").build();
        prettyJsonMapper = Jackson2ObjectMapperBuilder.json().serializationInclusion(JsonInclude.Include.NON_DEFAULT)
                .indentOutput(true).build();
        jsonFactory = new JsonFactory();
        cborMapper = new ObjectMapper(new CBORFactory());
        base64Decoder = Base64.getDecoder();
    }

    public static <T> T parseCborFromBase64(String base64, TypeReference<T> type) throws IOException {
        return parseCbor(base64Decoder.decode(base64), type);
    }

    public static <T> T parseCbor(byte[] bytes, TypeReference<T> type) throws IOException {
        try {
            return cborMapper.readValue(bytes, type);
        } catch (IOException e) {
            log.warn("parse cbor failure", e);
            return null;
        }
//        return cborMapper.readValue(bytes, type);
    }

    /*
     * json格式化
     */

    public static String json(Object obj) {
        return json(obj, jsonMapper);
    }

    public static String prettyJson(Object obj) {
        return json(obj, prettyJsonMapper);
    }

    private static String json(Object obj, ObjectMapper mapper) {
        if (obj == null)
            return null;
        try {
            return mapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * json解析
     */

    public static <T> Optional<T> parseJson(String json, Class<T> clazz) {
        if (StringUtils.isBlank(json))
            return Optional.empty();
        try {
            return Optional.ofNullable(jsonMapper.readValue(json, clazz));
        } catch (IOException e) {
            log.warn("parse json failure: {}", json, e);
            return Optional.empty();
        }
    }

    public static <T> Optional<T> parseJson(String json, JavaType type) {
        if (StringUtils.isBlank(json))
            return Optional.empty();
        try {
            return Optional.ofNullable(jsonMapper.readValue(json, type));
        } catch (IOException e) {
            log.warn("parse json failure: {}", json, e);
            return Optional.empty();
        }
    }

    public static <T> T parseJson(String json, TypeReference<T> type) {
        if (StringUtils.isBlank(json))
            return null;
        try {
            return jsonMapper.readValue(json, type);
        } catch (IOException e) {
            log.warn("parse json failure: {}", json, e);
            return null;
        }
    }

    public static <T> Optional<T> parseJson(JsonNode json, Class<T> clazz) {
        if (json == null || json.isMissingNode() || json.isNull())
            return Optional.empty();
        try {
            return Optional.ofNullable(jsonMapper.treeToValue(json, clazz));
        } catch (JsonProcessingException e) {
            log.warn("parse json failure: {}", json.toString(), e);
            return Optional.empty();
        }
    }

    public static JsonNode parseJson(String json) {
        if (StringUtils.isBlank(json))
            return MissingNode.getInstance();
        try {
            return jsonMapper.readTree(json);
        } catch (IOException e) {
            log.warn("parse json failure: {}", json, e);
            return MissingNode.getInstance();
        }
    }

    public static JavaType construct(Function<TypeFactory, JavaType> mapper) {
        return mapper.apply(jsonMapper.getTypeFactory());
    }

    public static JsonParser jsonParser(String json) throws IOException {
        return jsonFactory.createParser(json);
    }

    /**
     * to map
     *
     * @param obj
     * @return
     */
    public static Map<String, Object> map(Object obj) {
        Map<String, Object> map = jsonMapper.convertValue(obj, Map.class);
        return map;
    }
}
