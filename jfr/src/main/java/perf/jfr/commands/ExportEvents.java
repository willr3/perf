package perf.jfr.commands;

import com.jrockit.mc.common.IMCFrame;
import com.jrockit.mc.common.IMCMethod;
import com.jrockit.mc.common.IMCStackTrace;
import com.jrockit.mc.flightrecorder.FlightRecording;
import com.jrockit.mc.flightrecorder.internal.model.FLRFrame;
import com.jrockit.mc.flightrecorder.internal.model.FLRMethod;
import com.jrockit.mc.flightrecorder.internal.model.FLRStackTrace;
import com.jrockit.mc.flightrecorder.internal.model.FLRThread;
import com.jrockit.mc.flightrecorder.internal.model.FLRType;
import com.jrockit.mc.flightrecorder.spi.IView;
import org.jboss.aesh.cl.Arguments;
import org.jboss.aesh.cl.CommandDefinition;
import org.jboss.aesh.cl.Option;
import org.jboss.aesh.cl.OptionList;
import org.jboss.aesh.console.command.Command;
import org.jboss.aesh.console.command.CommandException;
import org.jboss.aesh.console.command.CommandResult;
import org.jboss.aesh.console.command.invocation.CommandInvocation;
import org.json.JSONObject;
import perf.jfr.EventFields;
import perf.jfr.JfrCollection;
import perf.jfr.JfrCollectionCompleter;
import perf.jfr.NestedMap;
import perf.util.StringUtil;
import perf.util.json.Json;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Created by wreicher
 */
@CommandDefinition(name="export-events", description = "export events")
public class ExportEvents implements Command<CommandInvocation> {


    private BiFunction<NestedMap<String,String>,Integer,String> csvFunction = (map, offset)->{
        StringBuffer buffer = new StringBuffer();

        Set<String> keys = map.getKeys();
        for(Iterator<String> keyIter = keys.iterator(); keyIter.hasNext();){
            String key = keyIter.next();
            NestedMap<String,String> value = map.get(key);
            if(!value.hasChildren()){
                buffer.append(value.get());
            } else {
                String childString = this.csvFunction.apply(value,0);
                buffer.append(childString.replaceAll(",",":"));
            }
            if(keyIter.hasNext()){
                buffer.append(",");
            }
        }
        buffer.append(System.lineSeparator());
        return buffer.toString();
    };

    private static void pad(StringBuffer buffer,int amount){
        for(int i=0; i<amount; i++){
            buffer.append(" ");
        }
    }

    private BiFunction<NestedMap<String,String>,Integer,String> jsonFunction = (map, offset)->{
        StringBuffer buffer = new StringBuffer();
        Set<String> keys = map.getKeys();

        if(map.hasChildren()){
            if(map.isList()){
                buffer.append("[");
            }else{
                buffer.append("{");
                buffer.append(System.lineSeparator());
                pad(buffer,offset);
            }
            for(Iterator<String> keyIter = keys.iterator(); keyIter.hasNext();){
                String key = keyIter.next();
                NestedMap<String,String> value = map.get(key);

                if(!map.isList()){
                    buffer.append(key);
                    buffer.append(" : ");
                }
                String valueString = this.jsonFunction.apply(value,offset+2);
                buffer.append(valueString);

                if(keyIter.hasNext()){
                    buffer.append(",");
                    if(map.hasChildren() && !map.isList()){
                        buffer.append(System.lineSeparator());
                        pad(buffer,offset);
                    }
                }else{
                    buffer.append(System.lineSeparator());
                    pad(buffer,offset-2);
                }


            }
            if(map.isList()){
                buffer.append("]");
            }else{
                buffer.append("}");
            }
        }else{
            String value = map.get();
            if("false".equals(value) || "true".equals(value)){
              buffer.append(value);
            } else if(!Pattern.matches("\\d+\\.?\\d*",value)){
                buffer.append("\"");
                buffer.append(value);
                buffer.append("\"");
            } else {
                buffer.append(value);
            }
        }

        return buffer.toString();
    };


    @Option(name="event",shortName = 'e',completer = EventCompleter.class, required = true, description = "which event to export from the jfr")
    String eventTargetPath;

    @Option(name="output",shortName = 'o', required = false, description = "the output file for exporting the events, defaults to terminal")
    String outputPath;

    @Option(name="type",shortName = 't', description = "output format [csv,json]",defaultValue = {"csv"})
    String outputType;

    @OptionList(name="fields",shortName = 'f',required = false, description = "the fields to export from the specified events (if they exist)")
    List<String> fields;

    @Arguments(completer = JfrCollectionCompleter.class)
    List<String> jfrNames;


    @Override
    public CommandResult execute(CommandInvocation commandInvocation) throws CommandException, InterruptedException {


        if(jfrNames == null){
            jfrNames = new ArrayList<>();
        }
        if(jfrNames.isEmpty() && JfrCollection.COLLECTION.onlyOneLoaded()){
            jfrNames.add(JfrCollection.COLLECTION.getDefaultName());
        }

        if(jfrNames.isEmpty()){
            commandInvocation.getShell().out().println("export-events requires a loaded jfr");
            return CommandResult.FAILURE;
        }
        if(jfrNames.size() > 1 ){
            commandInvocation.getShell().out().println("export-events requires one jfr");
            return CommandResult.FAILURE;
        }
        if(fields == null || fields.isEmpty()){
            fields = EventFields.get(eventTargetPath);
        }

        if(fields.isEmpty()){
            commandInvocation.getShell().out().println("no fields specified for "+eventTargetPath);
            return CommandResult.FAILURE;
        }

        PrintStream outStream = null;//commandInvocation.getShell().out();

        if(outputPath != null){
            try {
                outStream = new PrintStream(new FileOutputStream(outputPath));
            } catch (FileNotFoundException e) {
                commandInvocation.getShell().out().println("could not create output file: "+outputPath);
                commandInvocation.getShell().out().println("  ["+e.getMessage()+"]");
                return CommandResult.FAILURE;
            }
        }

        final PrintStream out = outStream == null ? commandInvocation.getShell().out() : outStream;

        String jfrName = jfrNames.get(0);

        FlightRecording record = JfrCollection.COLLECTION.get(jfrName);

        if(record==null){
            File newRecordFile = new File(jfrName);
            if(newRecordFile.exists()){
                long startLoad = System.currentTimeMillis();
                commandInvocation.getShell().out().println("loading "+jfrName);
                JfrCollection.COLLECTION.loadJfr(newRecordFile);
                commandInvocation.getShell().out().println("loaded in "+ StringUtil.durationToString((System.currentTimeMillis()-startLoad)));
                record = JfrCollection.COLLECTION.get(jfrName);
            }
        }

        IView view = record.createView();

        StringBuffer header = new StringBuffer();
        for(int i=0; i<fields.size(); i++){
            if(i>0){
                header.append(",");
            }
            header.append(fields.get(i));
        }
        out.println(header.toString());
        header = null;

        view.forEach((event)->{
            String eventTypePath = event.getEventType().getPath();
            if(eventTargetPath.equals(eventTypePath)){
                final NestedMap<String,String> dataMap = new NestedMap<>();
                final Queue<QueueEntry> objectQueue = new LinkedList<>();

                fields.forEach((fieldKey)-> objectQueue.add(new QueueEntry(fieldKey,event.getValue(fieldKey),dataMap)));

                while(!objectQueue.isEmpty()){
                    QueueEntry current = objectQueue.poll();
                    Object value = current.data();
                    String valueString = value == null ? "null" : value.getClass().getName();

                    NestedMap<String,String> subMap = null;
                    switch (valueString){
                        case "null":
                            current.map().put(current.key(),"null");
                            break;
                        case "java.lang.Boolean":
                        case "java.lang.Long":
                        case "java.lang.Integer":
                        case "java.lang.String":
                            current.map().put(current.key(),value.toString());
                            break;
                        case "java.lang.Float":
                            current.map().put(current.key(),String.format("%.3f", (Float)value));
                            break;
                        case "java.lang.Double":
                            current.map().put(current.key(),String.format("%.3f", (Double)value));
                            break;
                        case "com.jrockit.mc.common.IMCFrame":
                            IMCFrame frame = (com.jrockit.mc.common.IMCFrame)value;
                            subMap = current.map().put(current.key(),null);

                            objectQueue.add(new QueueEntry("method",frame.getMethod(),subMap));
                            objectQueue.add(new QueueEntry("line",frame.getFrameLineNumber(),subMap));
                            objectQueue.add(new QueueEntry("bci",frame.getBCI(),subMap));
                            objectQueue.add(new QueueEntry("type",frame.getType().toString(),subMap));
                            break;
                        case "com.jrockit.mc.common.IMCFrame$Type":
                            IMCFrame.Type imcFrameType = (com.jrockit.mc.common.IMCFrame.Type)value;
                            subMap = current.map().put(current.key(),null);
                            objectQueue.add(new QueueEntry("name",imcFrameType.getName(),subMap));
                            break;
                        case "com.jrockit.mc.common.IMCMethod":
                            IMCMethod imcMethod = (com.jrockit.mc.common.IMCMethod)value;
                            subMap = current.map().put(current.key(),null);

                            objectQueue.add(new QueueEntry("name",imcMethod.getMethodName(),subMap));
                            objectQueue.add(new QueueEntry("class",imcMethod.getClassName(),subMap));
                            objectQueue.add(new QueueEntry("classloaderId",imcMethod.getClassLoaderId(),subMap));
                            objectQueue.add(new QueueEntry("file",imcMethod.getFileName(),subMap));
                            objectQueue.add(new QueueEntry("line",imcMethod.getLineNumber(),subMap));
                            objectQueue.add(new QueueEntry("package",imcMethod.getPackageName(),subMap));
                            objectQueue.add(new QueueEntry("isNative",imcMethod.getIsNative(),subMap));
                            break;
                        case "com.jrockit.mc.common.IMCStackTrace.TruncationState": // shouldn't occur
                            IMCStackTrace.TruncationState truncationState = (com.jrockit.mc.common.IMCStackTrace.TruncationState)value;

                            break;
                        case "com.jrockit.mc.flightrecorder.internal.model.FLRThread":
                            FLRThread thread = (com.jrockit.mc.flightrecorder.internal.model.FLRThread)value;
                            subMap = current.map().put(current.key(),null);
                            objectQueue.add(new QueueEntry("name",thread.getName(),subMap));
                            objectQueue.add(new QueueEntry("javaId",thread.getJavaId(),subMap));
                            objectQueue.add(new QueueEntry("group",thread.getThreadGroup(),subMap));
                            objectQueue.add(new QueueEntry("state",thread.getThreadState(),subMap));
                            objectQueue.add(new QueueEntry("isDeadlocked",thread.isDeadlocked(),subMap));
                            objectQueue.add(new QueueEntry("duration",thread.getDuration(),subMap));
                            objectQueue.add(new QueueEntry("endTimestamp",thread.getEndTimestamp(),subMap));
                            objectQueue.add(new QueueEntry("platformId",thread.getPlatformId(),subMap));
                            objectQueue.add(new QueueEntry("startTimestamp",thread.getStartTimestamp(),subMap));
                            objectQueue.add(new QueueEntry("threadId",thread.getThreadId(),subMap));
                            break;
                        case "com.jrockit.mc.flightrecorder.internal.model.FLRStackTrace":
                            FLRStackTrace stackTrace = (com.jrockit.mc.flightrecorder.internal.model.FLRStackTrace)value;
                            subMap = current.map().put(current.key(),null);
                            NestedMap<String,String> frameMap = subMap.put("frames",null);
                            List<? extends IMCFrame> frames  = stackTrace.getFrames();
                            for(int i=0; i<frames.size(); i++){
                                objectQueue.add(new QueueEntry(Integer.toString(i),frames.get(i),frameMap));
                            }
                            objectQueue.add(new QueueEntry("almostThreadRootFrame",stackTrace.getAlmostThreadRootFrame(),subMap));
                            objectQueue.add(new QueueEntry("truncationState",stackTrace.getTruncationState().isTruncated(),subMap));
                            objectQueue.add(new QueueEntry("validTopFrame",stackTrace.getValidTopFrame(),subMap));

                            break;
                        case "com.jrockit.mc.flightrecorder.internal.model.FLRFrame":
                            FLRFrame flrFrame = (com.jrockit.mc.flightrecorder.internal.model.FLRFrame)value;
                            subMap = current.map().put(current.key(),null);
                            objectQueue.add(new QueueEntry("bci",flrFrame.getBCI(),subMap));
                            objectQueue.add(new QueueEntry("line",flrFrame.getFrameLineNumber(),subMap));
                            objectQueue.add(new QueueEntry("method",flrFrame.getMethod(),subMap));
                            objectQueue.add(new QueueEntry("type",flrFrame.getType(),subMap));
                            break;
                        case "com.jrockit.mc.flightrecorder.internal.model.FLRType":
                            FLRType flrType = (com.jrockit.mc.flightrecorder.internal.model.FLRType)value;
                            subMap = current.map().put(current.key(),null);
                            objectQueue.add(new QueueEntry("package",flrType.getPackageName(),subMap));
                            objectQueue.add(new QueueEntry("descriptor",flrType.getDescriptor(),subMap));
                            objectQueue.add(new QueueEntry("name",flrType.getTypeName(),subMap));
                            objectQueue.add(new QueueEntry("isArray",flrType.getIsArray(),subMap));
                            objectQueue.add(new QueueEntry("isPrimitive",flrType.getIsPrimitive(),subMap));
                            break;
                        case "com.jrockit.mc.flightrecorder.internal.model.FLRMethod":
                            FLRMethod flrMethod = (com.jrockit.mc.flightrecorder.internal.model.FLRMethod)value;
                            subMap = current.map().put(current.key(),null);
                            objectQueue.add(new QueueEntry("package",flrMethod.getPackageName(),subMap));
                            objectQueue.add(new QueueEntry("file",flrMethod.getFileName(),subMap));
                            objectQueue.add(new QueueEntry("type",flrMethod.getType(),subMap));
                            objectQueue.add(new QueueEntry("classloaderId",flrMethod.getClassLoaderId(),subMap));
                            objectQueue.add(new QueueEntry("class",flrMethod.getClassName(),subMap));
                            objectQueue.add(new QueueEntry("isNative",flrMethod.getIsNative(),subMap));
                            objectQueue.add(new QueueEntry("line",flrMethod.getLineNumber(),subMap));
                            objectQueue.add(new QueueEntry("name",flrMethod.getMethodName(),subMap));

                            break;
                        default:
                            System.err.println("missing type for "+valueString);
                    }
                }
                //commandInvocation.getShell().out().println("{\n"+dataMap.toString()+"}");
                commandInvocation.getShell().out().println(jsonFunction.apply(dataMap,2));
            }



        });

//        view.forEach((event)->{
//            String eventTypePath = event.getEventType().getPath();
//            if(eventTargetPath.equals(eventTypePath)){
//                StringBuilder sb = new StringBuilder();
//                for(int i=0; i<fields.size(); i++){
//                    if(i>0){
//                        sb.append(",");
//                    }
//                    Object value = event.getValue(fields.get(i));
//                    String valueType = value == null ? "null" : value.getClass().getName();
//                    switch(valueType){
//                        case "null":
//                            break;
//                        case "java.lang.Boolean":
//                        case "java.lang.Long":
//                        case "java.lang.Integer":
//                        case "java.lang.String":
//                            sb.append(value.toString());
//                            break;
//                        case "java.lang.Float":
//                            sb.append(String.format("%.3f", (Float)value));
//                            break;
//                        case "java.lang.Double":
//                            sb.append(String.format("%.3f", (Double)value));
//                            break;
//                        case "com.jrockit.mc.flightrecorder.internal.model.FLRThread":
//                            FLRThread thread = (com.jrockit.mc.flightrecorder.internal.model.FLRThread)value;
//                            String threadName = thread.getName();
//                            String threadGroup = thread.getThreadGroup();
//                            long javaId = thread.getJavaId();
//                            long platformId = thread.getPlatformId();
//                            long threadId = thread.getThreadId();
//                            String threadState = thread.getThreadState();
//                            sb.append("javaId=["+javaId+"], platformId=["+platformId+"]: threadId=["+threadId+"]: state="+threadState+"]: name="+threadName+"]: group=["+threadGroup+"]:");
//                            break;
//                        case "com.jrockit.mc.flightrecorder.internal.model.FLRStackTrace":
//                            FLRStackTrace stackTrace = (com.jrockit.mc.flightrecorder.internal.model.FLRStackTrace)value;
//                            FLRFrame almostThreadRootFrame = stackTrace.getAlmostThreadRootFrame();
//                            List<? extends IMCFrame> frames = stackTrace.getFrames();
//
//                        deault:
//                            System.err.println("missing type for "+valueType);
//                    }
//                }
//                out.println(sb.toString());
//            }
//        });

        return CommandResult.SUCCESS;
    }

    private class QueueEntry {
        private Object data;
        private NestedMap<String,String> map;
        private String key;

        public QueueEntry(String key,Object data,NestedMap<String,String> map){
            this.key = key;
            this.data = data;
            this.map = map;
        }
        public String key(){return key;}
        public Object data(){return data;}
        public NestedMap<String,String> map(){return map;}
    }
}
