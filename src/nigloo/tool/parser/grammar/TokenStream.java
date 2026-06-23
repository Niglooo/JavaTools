package nigloo.tool.parser.grammar;

import java.util.*;

class TokenStream implements Iterator<Token<?>> {

    private final Iterator<Token<?>> input;
    private Token<?> peeked;
    private boolean endOfInputConsumed = false;

    public <T extends Enum<T> & GrammarElement> TokenStream(Iterator<Token<T>> input) {
        //noinspection unchecked
        this.input = (Iterator<Token<?>>)(Iterator<?>) Objects.requireNonNull(input, "input");
        this.peeked = null;
    }

    @Override
    public boolean hasNext() {
        return !endOfInputConsumed;
    }

    @Override
    public Token<?> next() {
        if (!hasNext())
            throw new NoSuchElementException();

        if (peeked != null) {
            Token<?> element = peeked;
            endOfInputConsumed = (peeked == Token.END_OF_INPUT);
            peeked = null;
            return element;
        }

        if (input.hasNext()) {
            return input.next();
        }

        endOfInputConsumed = true;
        return Token.END_OF_INPUT;
    }

    public Token<?> peek() {
        if (!hasNext())
            return null;

        if (peeked != null) {
            return peeked;
        }

        peeked = input.hasNext() ? input.next() : Token.END_OF_INPUT;
        return peeked;
    }
}
