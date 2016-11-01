package perf.qed;

import java.util.function.UnaryOperator;

/**
 * Created by wreicher
 */
public interface Line extends UnaryOperator<String> {
    Line EMPTY = (input) -> { return ""; };
    Line ECHO = (input) -> { return input;};
    Line NULL = (input) -> { return null;};
    Line OK = (input) -> { return "ok";};
    public String apply(String input);
}
