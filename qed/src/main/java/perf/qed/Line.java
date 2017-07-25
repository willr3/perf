package perf.qed;

import java.util.function.UnaryOperator;

/**
 * Created by wreicher
 * This is just a UnaryOperator, why did I extend it?
 */
public interface Line extends UnaryOperator<String> {
    Line EMPTY = (input) -> { return ""; };
    Line ECHO = (input) -> { return input;};
    Line NULL = (input) -> { return null;};
    Line OK = (input) -> { return "ok";};
    public String apply(String input);
}
