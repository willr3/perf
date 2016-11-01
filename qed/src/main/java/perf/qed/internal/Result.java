package perf.qed.internal;

/**
 * Created by wreicher
 */
public interface Result {


    public boolean isPending();
    public boolean isActive();
    public boolean isDone();
    public boolean worked();
    public boolean failed();
    public void ok(String message);

    public void error(String message);
    public void update(String message);

}
