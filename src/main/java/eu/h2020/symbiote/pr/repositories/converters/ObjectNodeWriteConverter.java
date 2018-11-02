package eu.h2020.symbiote.pr.repositories.converters;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import org.springframework.core.convert.converter.Converter;

public class ObjectNodeWriteConverter implements Converter<ObjectNode, DBObject> {

    @Override
    public DBObject convert(ObjectNode source) {
        DBObject dbObject = (DBObject) JSON.parse(source.toString());
        dbObject.put("_class", ObjectNode.class.getCanonicalName());
        return dbObject;
    }
}