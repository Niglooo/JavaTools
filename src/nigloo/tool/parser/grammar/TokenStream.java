package nigloo.tool.parser.grammar;

import java.util.*;

class TokenStream<T extends Enum<T> & GrammarElement> implements Iterator<Token<T>> {

    private final Iterator<Token<T>> input;
    private Token<T> peeked;
    private boolean endOfInputConsumed = false;

    public TokenStream(Iterator<Token<T>> input) {
        this.input = Objects.requireNonNull(input, "input");
        this.peeked = null;
    }

    @Override
    public boolean hasNext() {
        return !endOfInputConsumed;
    }

    @Override
    public Token<T> next() {
        if (!hasNext())
            throw new NoSuchElementException();

        if (peeked != null) {
            Token<T> element = peeked;
            endOfInputConsumed = (peeked == Token.END_OF_INPUT());
            peeked = null;
            return element;
        }

        if (input.hasNext()) {
            return input.next();
        }

        endOfInputConsumed = true;
        return Token.END_OF_INPUT();
    }

    public Token<T> peek() {
        if (!hasNext())
            return null;

        if (peeked != null) {
            return peeked;
        }

        peeked = input.hasNext() ? input.next() : Token.END_OF_INPUT();
        return peeked;
    }
}
