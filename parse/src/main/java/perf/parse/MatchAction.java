package perf.parse;

import org.json.JSONObject;
/**
 *
 */
public interface MatchAction {

    public void onMatch(JSONObject match, Exp pattern, Parser parser);
}
