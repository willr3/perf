package perf.jfr.commands;

import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.completer.FileOptionCompleter;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import perf.jfr.JfrCollection;
import perf.util.AsciiArt;
import perf.util.StringUtil;

import java.io.File;
import java.util.List;

/**
 * Created by wreicher
 */
@CommandDefinition(name="load", description = "load jfr")
public class LoadJfr implements Command<CommandInvocation> {

    @Option(name="alias", shortName='a')
    String alias;

    @Arguments(completer = FileOptionCompleter.class)
    private List<File> files;

    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {


        if(files == null){
            commandInvocation.getShell().out().println("load requires a file name");
            return CommandResult.FAILURE;
        }
        if(files.size() > 1){
            commandInvocation.getShell().out().println("only one jfr can be loaded per call to load");
            return CommandResult.FAILURE;
        }
        File file = files.get(0);
        if(!file.exists()){
            commandInvocation.getShell().out().printf("cannot find %s %n",file.getAbsolutePath());
            return CommandResult.FAILURE;
        }

        if(alias == null){
            alias = file.getName();
        }

        commandInvocation.getShell().out().printf("loading %s %s %n",
            AsciiArt.printKMG(file.length()),
            alias);
        long startTime = System.currentTimeMillis();
        JfrCollection.COLLECTION.loadJfr(file,alias);
        long stopTime = System.currentTimeMillis();

        commandInvocation.getShell().out().printf("loaded %s in %s %n",
                alias,
                StringUtil.durationToString(stopTime-startTime));
        return CommandResult.SUCCESS;
    }
}
