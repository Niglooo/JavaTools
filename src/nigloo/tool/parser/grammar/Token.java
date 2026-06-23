package nigloo.tool.parser.grammar;

import java.util.Objects;

public record Token<T extends Enum<T> & GrammarElement> (T type, CharSequence value, int offset, Integer line, Integer column) {
    public Token {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
    }

    private enum EndOfInputType implements GrammarElement {
        END_OF_INPUT
    }

    static final Token<?> END_OF_INPUT = new Token<>(
            EndOfInputType.END_OF_INPUT,
            EndOfInputType.END_OF_INPUT.toString(),
            -1,
            null, null);
}
