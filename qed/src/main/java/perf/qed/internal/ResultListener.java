package perf.qed.internal;

/**
 * Created by wreicher
 */
public interface ResultListener {

    public void onOk(String message);

    public void onError(String message);

    public void onUpdate(String message);
}
