package perf.jfr.commands;

import com.jrockit.mc.flightrecorder.FlightRecording;
import com.jrockit.mc.flightrecorder.spi.ITimeRange;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import perf.jfr.JfrCollection;
import perf.util.StringUtil;

import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Created by wreicher
 */
@CommandDefinition(name="list-jfrs", description = "list loaded jfrs")
public class ListJfrs implements Command<CommandInvocation> {


    static final String DATEFORMAT =  "yyyy-MM-dd HH:mm:ss.SSS";

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        if ( JfrCollection.COLLECTION.size()==0 ) {
            commandInvocation.getShell().out().println("0 loaded jfrs");
        } else {
            int nameWidth = JfrCollection.COLLECTION.getNames().stream().map((name -> name.length())).max(Integer::compare).get();
            final SimpleDateFormat sdf = new SimpleDateFormat(DATEFORMAT);
            final String printFormat = "%-"+nameWidth+"s %-"+DATEFORMAT.length()+"s %s %n";

            commandInvocation.getShell().out().printf(printFormat,"name","timestamp","duration");

            JfrCollection.COLLECTION.getNames().forEach((name) -> {
                FlightRecording record = JfrCollection.COLLECTION.get(name);
                ITimeRange range = record.getTimeRange();
                Date startDate = new Date(range.getStartTimestamp()/1_000_000);
                String startDateString = sdf.format(startDate);
                commandInvocation.getShell().out().printf(printFormat,
                    name,
                    startDateString,
                    StringUtil.durationToString( (range.getEndTimestamp() - range.getStartTimestamp()) / 1_000_000 ) );
            });
        }
        return CommandResult.SUCCESS;
    }
}
