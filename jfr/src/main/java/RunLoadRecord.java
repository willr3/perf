import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.console.AeshConsole;
import org.jboss.aesh.console.AeshConsoleBuilder;
import org.jboss.aesh.console.Config;
import org.jboss.aesh.console.Prompt;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.jboss.aesh.console.command.registry.AeshCommandRegistryBuilder;
import org.jboss.aesh.console.command.registry.CommandRegistry;
import org.jboss.aesh.console.settings.Settings;
import org.jboss.aesh.console.settings.SettingsBuilder;
import org.jboss.aesh.io.FileResource;
import perf.jfr.commands.*;

/**
 * Created by wreicher
 */
public class RunLoadRecord {


    @CommandDefinition(name="exit", description = "exit the program")
    public static class ExitCommand implements Command {
        @Override
        public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {
            commandInvocation.stop();
            return CommandResult.SUCCESS;
        }
    }

    public static void main(String[] args) {

        SettingsBuilder builder = new SettingsBuilder()
            .logging(true)
            .enableMan(true)
            .enableAlias(true)
            .enableExport(false)
            .setExecuteFileAtStart(new FileResource(Config.getHomeDir()+Config.getPathSeparator()+".aeshrc"))
            .readInputrc(false);

        Settings settings = builder.create();

        CommandRegistry registry = new AeshCommandRegistryBuilder()
            .command(ExitCommand.class)
            .command(new LoadJfr())
            .command(new UnloadJfr())
            .command(new ListJfrEvents())
            .command(new ListJfrs())
            .command(new ExportEvents())
            .create();

        AeshConsole aeshConsole = new AeshConsoleBuilder()
            .commandRegistry(registry)
            .settings(settings)
            .prompt(new Prompt("#> "))
            .create();

        aeshConsole.start();

        if(args!=null && args.length>0){
            StringBuffer argBuilder = new StringBuffer();

            for(String arg : args){
                argBuilder.append(arg);
                argBuilder.append(" ");
            }
            String splitArgs[] = argBuilder.toString().split(";");
            for(String arg : splitArgs){
                aeshConsole.execute(arg);
            }
        }




//        String jfrPath = "/home/wreicher/perfWork/byteBuffer/322U/flight_record_20170209_161252.jfr"; //2.1G
//
//        long startTime = System.currentTimeMillis();
//
//        FlightRecording recording = FlightRecordingLoader.loadFile(new File(jfrPath));
//
//        long recordLoadedTime = System.currentTimeMillis();
//
//        System.out.println(" record loaded in "+StringUtil.durationToString((recordLoadedTime - startTime)));
//
//        IView view = recording.createView();
//
//        AtomicInteger count = new AtomicInteger(0);
//
//        //HashSet<String> typePaths = new HashSet<>();
//
//        final Counters<String> typePaths = new Counters<String>();
//
//        //view.getEventTypes().forEach(eventFields);
//
//        HashSet<String> seen = new HashSet<>();
//
//        view.forEach(eventFieldTypes);
//        view.forEach((event)->{
//            count.incrementAndGet();
//            String eventTypePath = event.getEventType().getPath();
//            typePaths.add(eventTypePath);
//        });
//
//        long stopTime = System.currentTimeMillis();
//
//        System.out.println("Fount " + count + " events in " + StringUtil.durationToString(stopTime - startTime));
//        System.out.println("event types: "+typePaths.size());
//        double max = typePaths.entries().stream().map(typePaths::count).max(Integer::compare).get();
//        System.out.println(max);
//        int digits = (int)Math.ceil(Math.log10(max));
//        List<String> entries = typePaths.entries();
//        entries.sort((pathA, pathB)-> -Integer.compare(typePaths.count(pathA),typePaths.count(pathB)));
//        entries.forEach(path ->{
//            System.out.printf("%"+digits+"d - %s%n",typePaths.count(path),path);
//        });

    }
}

