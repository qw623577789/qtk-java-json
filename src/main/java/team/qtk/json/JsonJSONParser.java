package team.qtk.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public class JsonJSONParser extends JsonSerializer<JSON> {

    @Override
    public void serialize(JSON json, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObject(json.getJacksonNode());
    }
}
