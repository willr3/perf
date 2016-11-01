package perf.qed;

/**
 * Created by wreicher
 */
public interface ActionPromise {

    public void run();

    public boolean isPending();
    public boolean isActive();
    public boolean isDone();
    public boolean failed();
    public boolean worked();
    
    public ActionPromise then(Line action);
    public ActionPromise then(ActionPromise action);

    public ActionPromise watch(Line action);

    public ActionPromise done(Line action);
    public ActionPromise done(ActionPromise action);

    public ActionPromise fail(Line action);
    public ActionPromise fail(ActionPromise action);
}
