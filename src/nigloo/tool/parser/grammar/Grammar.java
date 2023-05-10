package nigloo.tool.parser.grammar;

import java.util.List;
import java.util.Objects;

public record Grammar<T extends Enum<T> & GrammarElement, Out>(Class<T> tokenType, List<GrammarRule<Out>> rules, GrammarRule<Out> start) {
    public Grammar {
        Objects.requireNonNull(tokenType, "tokenType");
        Objects.requireNonNull(rules, "rules");
        Objects.requireNonNull(start, "start");

        rules.stream()
                .filter(rule -> rule.possibleSequences().isEmpty())
                .findAny()
                .ifPresent(rule -> {throw new IllegalArgumentException("Rule "+rule+" have no possible sequences");});

        if (!rules.contains(start))
            throw new IllegalArgumentException("The start rule must be one of rules");
    }
}
