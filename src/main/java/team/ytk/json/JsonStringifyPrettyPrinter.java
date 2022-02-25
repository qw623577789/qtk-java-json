package team.ytk.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.PrettyPrinter;
import java.io.IOException;
import java.util.Arrays;

public class JsonStringifyPrettyPrinter implements PrettyPrinter {

    private static final String LINE_SEPARATOR = System.lineSeparator();
    private static final char[] SPACES = new char[128];

    static {
        Arrays.fill(SPACES, ' ');
    }

    private final int numSpacesPerIndent;
    private int indentLevel;

    public JsonStringifyPrettyPrinter(int numSpacesPerIndent) {
        this.numSpacesPerIndent = numSpacesPerIndent;
    }

    private void indent(JsonGenerator jg) throws IOException {
        jg.writeRaw(SPACES, 0, indentLevel * numSpacesPerIndent);
    }

    @Override
    public void writeRootValueSeparator(JsonGenerator jg) throws IOException {}

    /**
     * 准备开始输出对象时触发
     */
    @Override
    public void writeStartObject(JsonGenerator jg) throws IOException {
        jg.writeRaw("{");
        jg.writeRaw(LINE_SEPARATOR);
        indentLevel++;
        indent(jg);
    }

    /**
     * 输出对象第一个key前触发
     */
    @Override
    public void beforeObjectEntries(JsonGenerator jg) throws IOException {}

    /**
     * 对象key-value之间的分隔符
     */
    @Override
    public void writeObjectFieldValueSeparator(JsonGenerator jg) throws IOException {
        jg.writeRaw(": ");
    }

    /**
     * 对象每个值结束后的分割
     */
    @Override
    public void writeObjectEntrySeparator(JsonGenerator jg) throws IOException {
        jg.writeRaw(",");
        jg.writeRaw(LINE_SEPARATOR);
        indent(jg);
    }

    /**
     * 输出对象最后一个value前触发
     */
    @Override
    public void writeEndObject(JsonGenerator jg, int nrOfEntries) throws IOException {
        jg.writeRaw(LINE_SEPARATOR);
        indentLevel--;
        indent(jg);
        jg.writeRaw("}");
    }

    /**
     * 准备开始输出数组时触发
     */
    @Override
    public void writeStartArray(JsonGenerator jg) throws IOException {
        jg.writeRaw("[");
        jg.writeRaw(LINE_SEPARATOR);
        indentLevel++;
        indent(jg);
    }

    /**
     * 输出数组第一个item前触发
     */
    @Override
    public void beforeArrayValues(JsonGenerator jg) throws IOException {}

    /**
     * 数组item之间的分隔符
     */
    @Override
    public void writeArrayValueSeparator(JsonGenerator jg) throws IOException {
        jg.writeRaw(",");
        jg.writeRaw(LINE_SEPARATOR);
        indent(jg);
    }

    /**
     * 输出数组最后一个item前触发
     */
    @Override
    public void writeEndArray(JsonGenerator jg, int nrOfValues) throws IOException {
        jg.writeRaw(LINE_SEPARATOR);
        indentLevel--;
        indent(jg);
        jg.writeRaw("]");
    }
}
