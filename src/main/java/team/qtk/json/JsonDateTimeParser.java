package team.qtk.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class JsonDateTimeParser extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        long tryTransformToLong = jsonParser.getValueAsLong(-1);
        // 若是非数值，尝试文本转换
        if (tryTransformToLong == -1) {
            String tryTransformToString = jsonParser.getText();
            try {
                return LocalDateTime.parse(tryTransformToString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception error) {
                return LocalDate.parse(tryTransformToString, DateTimeFormatter.ofPattern("yyyy-MM-dd")).atStartOfDay();
            }
        } else {
            long timestamp = jsonParser.getLongValue();
            if (timestamp > 99999999999L) { //13位时间戳
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.ofHours(8));
            } else {
                return LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.ofHours(8));
            }
        }

    }
}
