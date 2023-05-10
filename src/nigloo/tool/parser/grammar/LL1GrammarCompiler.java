package nigloo.tool.parser.grammar;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static nigloo.tool.parser.grammar.Token.Type.END_OF_INPUT;

public class LL1GrammarCompiler {

    private static final Logger LOGGER = Logger.getLogger(LL1GrammarCompiler.class.getName());

    public static <T extends Enum<T> & GrammarElement, Out> CompiledGrammar<T, Out> compileGrammar(Grammar<T, Out> grammar) {

        Objects.requireNonNull(grammar, "grammar");

        @SuppressWarnings("unchecked")
        GrammarRule<Out> syntheticStart = new GrammarRule<>("SYNTHETIC_START", List.of(new GrammarSequence<>(
                List.of(grammar.start(), END_OF_INPUT), evaluatedTokens -> (Out) evaluatedTokens.get(0))));

        ArrayList<GrammarRule<Out>> rules = new ArrayList<>(grammar.rules().size() + 1);
        rules.add(syntheticStart);
        rules.addAll(grammar.rules());

        Set<GrammarElement> nullables = computeNullable(rules);
        Map<GrammarElement, Set<GrammarElement>> first = computeFirst(rules, nullables, terminals(grammar.tokenType()));
        Map<GrammarElement, Set<GrammarElement>> follow = computeFollow(rules, nullables, first);

        // Compute transition table
        Map<GrammarElement, Map<GrammarElement, GrammarSequence<Out>>> transition = new HashMap<>();
        for (GrammarRule<Out> rule : rules) {
            Map<GrammarElement, GrammarSequence<Out>> ruleTransition = new HashMap<>();
            transition.put(rule, ruleTransition);
            for (GrammarSequence<Out> seq : rule.possibleSequences()) {
                for (GrammarElement a : firstRhs(seq, nullables, first)) {
                    GrammarSequence<Out> old = ruleTransition.put(a, seq);
                    if (old != null) {
                        throw new IllegalStateException("Conflict : "+rule+" -> "+a+" =>  "+old+"\t"+seq);
                    }
                }
                if (nullables.containsAll(seq.elements())){
                    for (GrammarElement a : follow.get(rule)) {
                        GrammarSequence<Out> old = ruleTransition.put(a, seq);
                        if (old != null) {
                            throw new IllegalStateException("Conflict : "+rule+" -> "+a+" =>  "+old+"\t"+seq);
                        }
                    }
                }
            }
        }

        return new CompiledGrammar<>(transition, grammar.start());
    }

    private static <Out> Set<GrammarElement> computeNullable(List<GrammarRule<Out>> rules) {
        LOGGER.finer("Computing nullable...");
        Set<GrammarElement> nullable = new HashSet<>();
        boolean fixpoint = false;
        int iteration = 0;

        while (!fixpoint) {
            fixpoint = true;
            for (GrammarRule<Out> rule : rules) {
                // RHS is nullable iff every symbol in the RHS is nullable
                if (rule.possibleSequences().stream().anyMatch(seq -> nullable.containsAll(seq.elements()))) {
                    if (nullable.add(rule)) {
                        fixpoint = false;
                    }
                }
            }
            LOGGER.finer("  Nullable table @ iteration " + (++iteration));
            LOGGER.finer("    " + nullable);
        }

        LOGGER.finer("Done!");
        return nullable;
    }

    // non-terminals => GrammarRule
    // terminals => TokenType & END_OF_INPUT

    private static <Out> Map<GrammarElement, Set<GrammarElement>> computeFirst(List<GrammarRule<Out>> rules, Set<GrammarElement> nullables, Collection<GrammarElement> terminals) {
        LOGGER.finer("Computing first...");
        Map<GrammarElement, Set<GrammarElement>> first = new HashMap<>();

        // To simplify first/follow logic, define first(t) = {t} for any terminal t
        first.put(END_OF_INPUT, Set.of(END_OF_INPUT));
        for (GrammarElement terminal : terminals)
            first.put(terminal, Set.of(terminal));
        for (GrammarRule<Out> rule : rules)
            first.put(rule, new HashSet<>());

        boolean fixpoint = false;
        int iteration = 0;

        while (!fixpoint) {
            fixpoint = true;
            for (GrammarRule<Out> rule : rules) {
                for (GrammarSequence<?> seq : rule.possibleSequences()) {
                    Set<GrammarElement> firstRhs = firstRhs(seq, nullables, first);
                    fixpoint &= !first.get(rule).addAll(firstRhs);
                }
            }
            LOGGER.finer("  First table @ iteration " + (++iteration) +"\n" +
                    first.entrySet().stream().map(e -> "    " + e.getKey() + " -> " + e.getValue()).collect(Collectors.joining("\n")));
        }

        LOGGER.finer("Done!");
        return first;
    }

    private static <Out> Map<GrammarElement, Set<GrammarElement>> computeFollow(List<GrammarRule<Out>> rules, Set<GrammarElement> nullables, Map<GrammarElement, Set<GrammarElement>> first) {
        LOGGER.finer("Computing follow...");
        Map<GrammarElement, Set<GrammarElement>> follow = new HashMap<>();

        for (GrammarRule<Out> rule : rules)
            follow.put(rule, new HashSet<>());

        boolean fixpoint = false;
        var iteration = 0;

        while (!fixpoint) {
            fixpoint = true;
            for (GrammarRule<Out> rule : rules) {
                for (GrammarSequence<Out> seq : rule.possibleSequences()) {
                    // invariant: ruleFollow is the follow set for position i of rule.rhs
                    Set<GrammarElement> ruleFollow = follow.get(rule);
                    for (int i = seq.elements().size() - 1 ; i >= 0 ; i--) {
                        GrammarElement element = seq.elements().get(i);
                        if (element instanceof GrammarRule) {
                            fixpoint &= !follow.get(element).addAll(ruleFollow);
                        }
                        if (nullables.contains(element)) {
                            ruleFollow = new HashSet<>(ruleFollow);
                            ruleFollow.addAll(first.get(element));
                        } else {
                            ruleFollow = first.get(element);
                        }
                    }
                }
            }
            LOGGER.finer("  Follow table @ iteration " + (++iteration) +"\n" +
                    follow.entrySet().stream().map(e -> "    " + e.getKey() + " -> " + e.getValue()).collect(Collectors.joining("\n")));
        }

        LOGGER.finer("Done!");
        return follow;
    }

    // compute first of a right-hand-side of a production (i.e., a
    // sequence of terminals and non-terminals).
    private static Set<GrammarElement> firstRhs(GrammarSequence<?> seq, Set<GrammarElement> nullables, Map<GrammarElement, Set<GrammarElement>> first) {
        // find longest nullable prefix
        int end = seq.elements().size();
        for (int i = 0 ; i < seq.elements().size() ; i++) {
            GrammarElement element = seq.elements().get(i);
            if (!nullables.contains(element)) {
                end = i + 1;
                break;
            }
        }
        return seq.elements().stream().limit(end).map(first::get).flatMap(Set::stream).collect(Collectors.toSet());
    }

    private static <T extends Enum<T> & GrammarElement> Collection<GrammarElement> terminals(Class<T> tokenType) {
        GrammarElement[] values = tokenType.getEnumConstants();
        ArrayList<GrammarElement> terminals = new ArrayList<>(values.length + 1);
        terminals.addAll(Arrays.asList(values));
        terminals.add(END_OF_INPUT);
        return terminals;
    }
}
