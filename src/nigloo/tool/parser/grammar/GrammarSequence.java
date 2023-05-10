package nigloo.tool.parser.grammar;

import java.util.List;
import java.util.function.Function;

public record GrammarSequence<Out>(List<GrammarElement> elements, Function<List<?>, Out> evaluateFunction) {
    @Override
    public String toString() {
        return elements.toString();
    }
}
