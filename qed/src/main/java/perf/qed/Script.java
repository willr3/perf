package perf.qed;

/**
 * Created by wreicher
 */
public interface Script {
    Script NOOP = (output,actor) -> {};
    public void run(String output, Actor actor);
}
