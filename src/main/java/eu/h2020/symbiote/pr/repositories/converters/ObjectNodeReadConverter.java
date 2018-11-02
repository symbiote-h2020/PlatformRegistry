package eu.h2020.symbiote.pr.repositories.converters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBObject;
import org.springframework.core.convert.converter.Converter;

import java.io.IOException;
import java.io.UncheckedIOException;

public class ObjectNodeReadConverter implements Converter<DBObject, ObjectNode> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public ObjectNode convert(DBObject source) {
        try {
            JsonNode jsonNode = objectMapper.readTree(source.toString());
            ((ObjectNode) jsonNode).remove("_class");
            return (ObjectNode) jsonNode;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}