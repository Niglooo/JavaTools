package nigloo.tool.parser.grammar;

import java.util.Objects;

public record Token<T extends Enum<T> & GrammarElement> (T type, CharSequence value, int offset, Integer line, Integer column) {
    public Token {
        if (END_OF_INPUT != null)
            Objects.requireNonNull(type, "type");
        Objects.requireNonNull(value, "value");
    }

    enum Type implements GrammarElement {
        END_OF_INPUT
    }

    private static final Token<?> END_OF_INPUT = new Token<>(
            null,
            Type.END_OF_INPUT.toString(),
            -1,
            null, null);

    @SuppressWarnings("unchecked")
    public static <T extends Enum<T> & GrammarElement> Token<T> END_OF_INPUT() {
        return (Token<T>) END_OF_INPUT;
    }
}
