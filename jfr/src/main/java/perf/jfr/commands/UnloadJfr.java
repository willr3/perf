package perf.jfr.commands;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import perf.jfr.JfrCollection;
import perf.jfr.JfrCollectionCompleter;

import java.util.List;

/**
 * Created by wreicher
 */
@CommandDefinition(name="unload", description = "unload jfr")
public class UnloadJfr implements Command<CommandInvocation> {

    @Arguments(completer = JfrCollectionCompleter.class)
    private List<String> jfrs;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {

        if(jfrs==null){
            commandInvocation.getShell().out().println("unload requires a jfr name");
            return CommandResult.FAILURE;
        }

        for(String jfr : jfrs){
            if(JfrCollection.COLLECTION.contains(jfr)){
                JfrCollection.COLLECTION.unload(jfr);
            }
        }

        return CommandResult.SUCCESS;
    }
}
