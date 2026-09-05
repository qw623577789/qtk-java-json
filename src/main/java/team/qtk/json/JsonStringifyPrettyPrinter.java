package team.qtk.json;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;

public class JsonStringifyPrettyPrinter extends DefaultPrettyPrinter {

    private static final String LINE_SEPARATOR = System.lineSeparator();

    private final int numSpacesPerIndent;

    public JsonStringifyPrettyPrinter(int numSpacesPerIndent) {
        // withSeparators只存引用不重算缓存字段，需经构造器传入才能生效
        super(Separators.createDefaultInstance()
            .withObjectFieldValueSeparator(':')
            .withObjectFieldValueSpacing(Separators.Spacing.AFTER)); //保持原 ": " 风格，默认是 " : "
        this.numSpacesPerIndent = numSpacesPerIndent;
        DefaultIndenter indenter = new DefaultIndenter(
            " ".repeat(Math.max(0, numSpacesPerIndent)),
            LINE_SEPARATOR
        );
        indentObjectsWith(indenter);
        indentArraysWith(indenter);
    }

    @Override
    public JsonStringifyPrettyPrinter createInstance() {
        return new JsonStringifyPrettyPrinter(numSpacesPerIndent);
    }
}