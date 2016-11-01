package perf.qed;

/**
 * Performs the actions for the current script in the correct context (e.g. on the configured Host)
 *
 */
public interface Actor {

    public ActionPromise sh(String command);
    public ActionPromise exec(String command);

    public ActionPromise then(Line action);
    public ActionPromise then(ActionPromise action);

    //public DelayedAction then(Action pass,Action fail);

    public ActionPromise thenAll(Line... action);
    public ActionPromise thenAll(ActionPromise... action);

    public ActionPromise check(Line condition, Line pass);
    public ActionPromise check(Line condition, ActionPromise pass);
    public ActionPromise check(Line condition, Line pass, Line fail);
    public ActionPromise check(Line condition, ActionPromise pass, ActionPromise fail);

    public ActionPromise check(ActionPromise condition, Line pass);
    public ActionPromise check(ActionPromise condition, Line pass, Line fail);
    public ActionPromise check(ActionPromise condition, ActionPromise pass, ActionPromise fail);

    //public PendingAction doWhile(Action action,Action check);
    public ActionPromise doWhile(ActionPromise action, Line check);

    public void signal(String name);
    public void waitFor(String name);

    public void addArtifact(String name);

    public void abort(String message);

    public void log(String message);
    public void log(String message, Exception e);

    public String getHostName();
    public String getRunId();

    public String getRunEnv(String name);
    public void setRunEnv(String name, String value);

    public String getHostEnv(String name);
    public void setHostEnv(String name, String value);

    // Action factories

    public Line patternMatch(String regex);
    public Line delayCounter(int limit, long delay);
}
