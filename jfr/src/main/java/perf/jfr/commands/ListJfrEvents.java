package perf.jfr.commands;

import com.jrockit.mc.flightrecorder.FlightRecording;
import com.jrockit.mc.flightrecorder.spi.IEventType;
import com.jrockit.mc.flightrecorder.spi.IView;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import perf.jfr.JfrCollection;
import perf.jfr.JfrCollectionCompleter;
import perf.util.Counters;
import perf.util.StringUtil;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wreicher
 */
@CommandDefinition(name="list-events", description = "list event types")
public class ListJfrEvents implements Command<CommandInvocation> {


    @Option(name="fields", shortName='f', hasValue = false, defaultValue = {"false"})
    boolean showFields;

    @Option(name="types", shortName='t', hasValue = false, defaultValue = {"false"})
    boolean showTypes;

    @Option(name="counts", shortName='c', hasValue = false, defaultValue = {"false"})
    boolean showCounts;

    @Option(name="sort", shortName='s', hasValue = false, defaultValue = {"false"})
    boolean sort;

    @Option(name="size-sort", shortName='S', hasValue = false, defaultValue = {"false"})
    boolean sizeSort;

    @Option(name="reverse", shortName='r', hasValue = false, defaultValue = {"false"})
    boolean reverse;

    @Arguments(completer = JfrCollectionCompleter.class)
    List<String> jfrNames;

//    not supported becuase would require iterating over events to get field values rather than eventTypes
//    @Option(name="types", shortName='t', hasValue = false, defaultValue = {"false"})
//    boolean showTypes;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        if(jfrNames == null){
            jfrNames = new ArrayList<>();
        }
        if(jfrNames.isEmpty() && JfrCollection.COLLECTION.onlyOneLoaded()){
            jfrNames.add(JfrCollection.COLLECTION.getDefaultName());
        }

        if(jfrNames.isEmpty()){
            return CommandResult.FAILURE;
        }

        jfrNames.forEach((jfrName)->{
            FlightRecording record = JfrCollection.COLLECTION.get(jfrName);

            if(record==null){
                File newRecordFile = new File(jfrName);
                if(newRecordFile.exists()){
                    long startLoad = System.currentTimeMillis();
                    commandInvocation.getShell().out().println("loading "+jfrName);
                    JfrCollection.COLLECTION.loadJfr(newRecordFile);
                    commandInvocation.getShell().out().println("loaded in "+StringUtil.durationToString((System.currentTimeMillis()-startLoad)));
                    record = JfrCollection.COLLECTION.get(jfrName);
                }
            }

            IView view = record.createView();

            final int pathWidth = view.getEventTypes().stream().map((type)->type.getPath().length()).max(Integer::compare).get();
            final AtomicInteger fieldIdentifierWidth = new AtomicInteger(0);
            final AtomicInteger fieldNameWidth = new AtomicInteger(0);


            AtomicInteger eventCount = new AtomicInteger(0);

            final Counters<String> typePaths = new Counters<>();

            if(showCounts || sizeSort) {
                commandInvocation.getShell().out().printf("counting events %n");
                long startCounting = System.currentTimeMillis();
                view.forEach((event) -> {
                    eventCount.incrementAndGet();
                    String eventTypePath = event.getEventType().getPath();
                    typePaths.add(eventTypePath);
                });
                commandInvocation.getShell().out().printf("finsihed counting in %s %n", StringUtil.durationToString(System.currentTimeMillis()-startCounting));
            }
            if(showFields || showTypes){
                view.getEventTypes().forEach((eventType)->{
                    eventType.getFields().forEach((field)->{
                        fieldIdentifierWidth.set(Math.max(fieldIdentifierWidth.get(),field.getIdentifier().length()));
                        fieldNameWidth.set(Math.max(fieldNameWidth.get(),field.getName().length()));
                    });


                });
            }
            ArrayList<IEventType> eventTypes = new ArrayList<IEventType>(view.getEventTypes());
            if(sort || sizeSort){
                //sort if size sort too so that events with same count are alphabetical
                Collections.sort(eventTypes , (a,b) -> a.getPath().compareTo(b.getPath()) );
            }
            if(sizeSort){
                Collections.sort(eventTypes , (a,b) -> Integer.compare(typePaths.count(a.getPath()),typePaths.count(b.getPath())) );
            }
            if(reverse){
                Collections.reverse(eventTypes);
            }


            HashMap<String,String> seen = new HashMap<>();

            view.forEach((event) -> {
                eventCount.incrementAndGet();
                String eventTypePath = event.getEventType().getPath();
                if(eventTypePath==null){
                    commandInvocation.getShell().out().println("null path for eventType="+event.getEventType().toString());
                }
                if(!seen.containsKey(eventTypePath)){
                    StringBuffer sb = new StringBuffer("");
                    if(showFields || showTypes) {
                        event.getEventType().getFields().forEach((field) -> {
                            String fieldIdentifier = field.getIdentifier();
                            String fieldName = field.getName();
                            if(showTypes){
                                Object value = event.getValue(fieldIdentifier);
                                String valueType = value == null ? "null" : value.getClass().getName();
                                sb.append(String.format("  %-"+fieldIdentifierWidth.get()+"s %-"+fieldNameWidth.get()+"s %s%n",fieldIdentifier,fieldName,valueType));
                            } else {
                                sb.append(String.format("  %-" + fieldIdentifierWidth.get() + "s %-" + fieldNameWidth.get() + "s %n", fieldIdentifier, fieldName));
                            }
                        });
                    }
                    seen.put(eventTypePath,sb.toString());
                }
            });

            eventTypes.forEach((eventType)->{
                String eventTypePath = eventType.getPath();
                String eventTypeValue = seen.get(eventTypePath);
                if (showCounts || sizeSort) {
                    commandInvocation.getShell().out().printf("%-"+pathWidth+"s %d %n",eventTypePath,typePaths.count(eventTypePath));
                    //commandInvocation.getShell().out().printf();
                } else {
                    commandInvocation.getShell().out().printf("%-"+pathWidth+"s %n",eventTypePath);
                    //commandInvocation.getShell().out().printf("%-"+pathWidth+"s %n",eventTypePath);
                }
                if (eventTypeValue==null) {
                    //commandInvocation.getShell().out().println(eventTypePath+" -> null");
                } else {
                    commandInvocation.getShell().out().print(seen.get(eventTypePath));
                }
            });

        });

        return CommandResult.SUCCESS;
    }
}
