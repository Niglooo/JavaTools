package nigloo.tool.parser.grammar;

import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

public class CompiledGrammar<T extends Enum<T> & GrammarElement, Out> {

    private final Map<GrammarElement, Map<GrammarElement, GrammarSequence<Out>>> transition;
    private final GrammarElement start;

    CompiledGrammar(Map<GrammarElement, Map<GrammarElement, GrammarSequence<Out>>> transition, GrammarElement start) {
        this.transition = transition;
        this.start = start;
    }

    public Out compile(Iterator<Token<T>> input) throws ParseException {

        TokenStream<T> tokenStream = new TokenStream<>(input);

        ASTNode<T, Out> astRoot = new ASTNode<>(start, null);
        Deque<ASTNode<T, Out>> stack = new ArrayDeque<>();
        stack.push(new ASTNode<>(Token.Type.END_OF_INPUT, null));
        stack.push(astRoot);

        while (tokenStream.peek() != null) {
            ASTNode<T, Out> astNode = stack.pop();
            GrammarElement top = astNode.element;
            Token<T> token = tokenStream.peek();
            GrammarElement next = (token == Token.END_OF_INPUT()) ? Token.Type.END_OF_INPUT : token.type();

            if (!(top instanceof GrammarRule)) { // Terminal
                // If top of the stack matches next input token, consume the input
                if (next == top) {
                    tokenStream.next();
                    astNode.token = token;
                }
                else
                    throw parseException("Expected " + top + ", got " + next, token);
            }
            else if (transition.getOrDefault(top, Map.of()).containsKey(next)) {
                GrammarSequence<Out> rule = transition.get(top).get(next);
                if (rule == null)
                    throw parseException("No valid transition from a non-terminal \"" + top + "\" with look ahead symbol \"" + next + "\"", token);

                astNode.grammarSequence = rule;
                for (int i = rule.elements().size() - 1 ; i >= 0 ; i--)
                    stack.push(new ASTNode<>(rule.elements().get(i), astNode));
            }
            else
                throw parseException("Unexpected \"" + next + "\"", token);
        }

        return astRoot.evaluate();
    }

    private static ParseException parseException(String message, Token<?> token) {
        String pos;
        if (token.line() != null) {
            pos = "line " + token.line();
            if (token.column() != null) {
                pos += " column "+ token.column();
            }
        }
        else
            pos = "offset "+token.offset();

        return new ParseException("Syntax error at " + pos + ": "+message, token.offset());
    }

    private static class ASTNode<T extends Enum<T> & GrammarElement, Out> {

        final GrammarElement element;
        final List<ASTNode<T, Out>> children = new ArrayList<>();
        Token<T> token = null;
        GrammarSequence<Out> grammarSequence = null;


        ASTNode (GrammarElement element, ASTNode<T, Out> parent) {
            this.element = element;
            if (parent != null)
                parent.children.add(0, this);
        }

        Out evaluate() {
            List<Object> childrenResult = new ArrayList<>(children.size());
            for (ASTNode<T, Out> child : children) {
                if (child.token != null)
                    childrenResult.add(child.token);
                else
                    childrenResult.add(child.evaluate());
            }
            return grammarSequence.evaluateFunction().apply(childrenResult);
        }

        @Override public String toString() {
            return toString(0);
        }
        public String toString(int indent) {
            return "  ".repeat(indent)+ASTNode.this.element+(token != null ? " : "+token:"")+"\n"
                    +ASTNode.this.children.stream().map(n -> n.toString(indent+1)).collect(Collectors.joining());
        }
    }
}
