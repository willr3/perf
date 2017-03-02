package perf.jfr.commands;

import org.jboss.aesh.cl.completer.OptionCompleter;
import org.jboss.aesh.console.command.completer.CompleterInvocation;
import perf.jfr.EventFields;

import java.util.Set;

/**
 * Created by wreicher
 */
public class EventCompleter implements OptionCompleter<CompleterInvocation> {
    @Override
    public void complete(CompleterInvocation completerInvocation) {
        Set<String> knownEvents = EventFields.getEvents();
        knownEvents.forEach((eventName)->{
            if(eventName.startsWith(completerInvocation.getGivenCompleteValue())){
                completerInvocation.addCompleterValue(eventName);
            }
        });
    }
}
