package nigloo.tool.parser.grammar;

import java.util.List;
import java.util.Objects;

public record GrammarRule<Out>(String name, List<GrammarSequence<Out>> possibleSequences) implements GrammarElement {

    public GrammarRule {
        Objects.requireNonNull(possibleSequences, "possibleSequences");
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public String toString() {
        return name;
    }
}
