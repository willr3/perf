package perf.jfr;

import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.completer.CompleterInvocation;

/**
 * Created by wreicher
 */
public class JfrCollectionCompleter implements OptionCompleter<CompleterInvocation> {

    @Override
    public void complete(CompleterInvocation completerInvocation) {
        JfrCollection.COLLECTION.getNames().forEach((name) -> {
            if (name.startsWith(completerInvocation.getGivenCompleteValue())) {
                completerInvocation.addCompleterValue(name);
            }
        });
    }
}
