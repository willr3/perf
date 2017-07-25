package perf.analysis.specjms;


import perf.parse.Eat;
import perf.parse.Exp;
import perf.parse.JsonConsumer;
import perf.parse.Merge;
import perf.parse.Parser;
import perf.parse.reader.TextLineReader;
import perf.util.IdGenerator;
import perf.util.file.FileUtility;
import perf.util.hash.HashFactory;
import perf.util.json.Jsons;
import perf.util.xml.Xml;
import perf.util.xml.XmlLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by wreicher
 */
public class SpecjmsResult {




    static class FirstLast implements JsonConsumer {
        String first = null;
        String last = null;

        @Override
        public void consume(Jsons object) {
            if(object.has("timestamp")){
                if(first==null){
                    first = object.getString("timestamp");
                }
                last = object.getString("timestamp");
            }
        }
        public String getFirst(){return first;}
        public String getLast(){return last;}
    }
    static class KeyConsumer implements JsonConsumer {

        private String key;
        private String value;
        public KeyConsumer(String key){
            this.key = key;
        }
        public String getKey(){return key;}
        public String getValue(){return value;}

        @Override
        public void consume(Jsons json) {
            if(json.has(key) && value == null){
                value = json.getString(key);
            }
        }
    }
    static class SignalTimes implements JsonConsumer{
        private Map<String,String> map;
        private Matcher m = Pattern.compile(".*? reached (?<signalName>\\S+)").matcher("");
        public SignalTimes(){
            map = new LinkedHashMap<>();

        }

        @Override
        public void consume(Jsons json) {
            if(json.has("message") && m.reset(json.getString("message")).matches()){
                String signalName = m.group("signalName");
                String timestamp = json.getString("timestamp");
                map.put(signalName,timestamp);
            }
        }

        public Map<String,String> getMap(){return Collections.unmodifiableMap(map);}
    }

    static class StateConsumer implements JsonConsumer{

        Map<String,String> state;
        public StateConsumer(){
            state = new HashMap<>();
        }

        public Map<String,String> getState(){
            return Collections.unmodifiableMap(state);
        }

        @Override
        public void consume(Jsons object) {
            for(String key : object.keySet()){
                if(key.equals("timestamp") || key.equals("message")){

                }else{
                    state.put(key,object.getString(key));
                }
            }
        }
    }

    static final IdGenerator<String> generator = new IdGenerator<>();


    private File baseFile;
    private Properties configProperties;
    private Map<String,String> signalTimes;
    private Map<String,String> sshState;
    private String firstRunTimestamp;
    private String lastRunTimestamp;
    private InteractionFinal interactionFinal;

    public SpecjmsResult(File file){
        baseFile = file;
        if(file.exists()){
            configProperties = agentProperties(file);
            File runLogFile = new File(file,"run.log");
            if(runLogFile.exists()){
                TextLineReader reader = new TextLineReader();
                SignalTimes signalTimesConsumer = new SignalTimes();
                FirstLast firstLast = new FirstLast();
                StateConsumer stateConsumer = new StateConsumer();
                Parser parser = runLogParser();
                parser.add(signalTimesConsumer);
                parser.add(firstLast);
                parser.add(stateConsumer);
                reader.addParser(parser);
                reader.read(runLogFile.getPath());
                signalTimes = signalTimesConsumer.getMap();
                sshState = stateConsumer.getState();
                firstRunTimestamp = firstLast.getFirst();
                lastRunTimestamp = firstLast.getLast();
            }
            List<String> interactionFinals = FileUtility.getFiles(baseFile.getPath(),"interaction.final.xml",true);
            if(!interactionFinals.isEmpty() && interactionFinals.size()==1){
                this.interactionFinal = new InteractionFinal(new XmlLoader().loadXml(new File(interactionFinals.get(0)).toPath()));
            }else{
                this.interactionFinal = InteractionFinal.EMPTY;
            }
        }
    }
    public InteractionFinal getInteractionFinal(){return interactionFinal;}
    public String getFirstRunTimestamp(){return firstRunTimestamp;}
    public String getLastRunTimestamp(){return lastRunTimestamp;}
    public String getState(String key){
        if(sshState==null || !sshState.containsKey(key)){
            return "";
        }
        return sshState.get(key);
    }
    public List<String> getSignalNames(){
        if(signalTimes==null){
            return Collections.EMPTY_LIST;
        }
        return Collections.unmodifiableList(Arrays.asList(signalTimes.keySet().toArray(new String[0])));
    }
    public String getSignalTime(String signalName){
        if(signalTimes==null){
            return "";
        }
        return signalTimes.get(signalName);
    }
    public String getProperty(String name){
        if(configProperties==null || !configProperties.containsKey(name)){
            return "";
        }
        return configProperties.getProperty(name);
    }
    public boolean hasFile(String pattern){
        return !FileUtility.getFiles(baseFile.getPath(),pattern,true).isEmpty();
    }
    public String getHorizontalRow(){
        String home_key = getState("EAP_HOME").isEmpty() ? "AMQ6_HOME" : "EAP_HOME";
        String configName = home_key.equals("EAP_HOME")? getState("STANDALONE_XML") : "activemq.xml";
        String home = getState(home_key);

        String home_str = home.substring(home.lastIndexOf(File.separator)+1);

        HashFactory hf = new HashFactory();
        List<String> configFiles = FileUtility.getFiles(baseFile.getPath(),configName,true);
        String hash = "";
        if(!configFiles.isEmpty() && configFiles.size()==1){
            String configPath = configFiles.get(0);
            Xml configXml = (new XmlLoader()).loadXml( (new File(configPath)).toPath());
            configXml.trimEmptyText();
            hash = hf.getStringHash(configXml.documentString(2));
            hash = ""+generator.getId(hash);
        }
        List<String> sateliteFiles = FileUtility.getFiles(baseFile.getPath(),"satellite-",true);
        int pacingCount =
                sateliteFiles.stream()
                        .mapToInt(satPath->FileUtility.lineCount("Cannot maintain pacing distribution",satPath)).sum();


        String base_key = getState("TOPOLOGY").indexOf("horizontal")>-1 ? "org.spec.jms.horizontal.BASE" : "org.spec.jms.vertical.BASE";
        String rtrn = String.format("%22s, %4s, %40s, %10d, %9.2f, %8.0f, %1s, %8s, %5s, %12s, %12s, %12s, %10d",
            baseFile.getName(),
            getProperty(base_key),
            home_str,
            //hash,
            getInteractionFinal().getMessageCount(),
            getInteractionFinal().getAvgDeliveryTime(),
            getInteractionFinal().getMaxDeliveryTime(),
            hasFile(".jfr") ? "Y":"N",
            getInteractionFinal().isEmpty()?"failed":"finished",
            get(configName+FileUtility.SEARCH_KEY+"/server/profile/subsystem/server[@name=\"default\"]/address-setting[@name=\"#\"]/@address-full-policy"),
            get(configName+FileUtility.SEARCH_KEY+"/server/profile/subsystem/server[@name=\"default\"]/address-setting[@name=\"#\"]/@max-size-bytes"),
            get(configName+FileUtility.SEARCH_KEY+"/server/profile/subsystem/server[ @name=\"default\" ]/connection-factory[ @name=\"RemoteConnectionFactory\" ]/@min-large-message-size"),
            get(configName+FileUtility.SEARCH_KEY+"/server/profile/subsystem/server[ @name=\"default\" ]/connection-factory[ @name=\"RemoteConnectionFactory\" ]/@producer-window-size"),
            pacingCount
        );

        return rtrn;
    }

    public String get(String reference){
        int index;
        StringBuilder sb = new StringBuilder();
        if( (index=reference.indexOf(FileUtility.SEARCH_KEY)) > -1 ){
            String fileRef = reference.substring(0,index);
            String search = reference.substring(index+1);
            if(fileRef.endsWith("run.log")){

            }else if (fileRef.endsWith(".xml")){
                XmlLoader loader = new XmlLoader();
                List<String> foundFiles = FileUtility.getFiles(baseFile.toString(),fileRef,true);
                if(!foundFiles.isEmpty()){
                    String basePath = foundFiles.get(0);
                    String loaderPath = basePath+FileUtility.SEARCH_KEY+search;
                    String value =loader.getValue(loaderPath);
                    sb.append(value);
                }
            }else if (fileRef.endsWith(".properties")){
                Properties prop = new Properties();
                try {
                    prop.load(new FileInputStream( new File(fileRef) ) );
                    sb.append(prop.getProperty(search));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }


        return sb.toString();
    }

    public static void main(String[] args) {

        File baseDirectory = new File("/home/wreicher/perfWork/amq/jdbc/save");

        List<String> results = Arrays.asList(
                //old
                //amq6
                "run-10-1499877726210",
                "run-20-1499876141066",
                "run-30-1499873845390",
                "run-40-1499872249226",
                "run-50-1499870542858",
                "run-75-1499879894443",
                "run-100-1499881621499",
                "run-125-1499883408400",

                //eap block 307200 1000000000
                "run-10-1499973232358",
                "run-20-1499971693685",
                "run-30-1499970123079",
                "run-40-1499965607977",
                "run-50-1499967422185",



                //eap page  307200 1000000000
                "run-10-1499974908529",
                "run-20-1499980606396",
                "run-30-1499982124762",
                "run-40-1499984119354",
                "run-50-1499991194534",

                //eap       307200 1000000000
                "run-10-1500084349538",
                "run-20-1500000311585",
                "run-30-1499998463213",
                "run-40-1499996617595",
                "run-50-1499994727096",

                //eap       307200 10000000000
                "run-50-1500294860536",

                //eap BLOCK 307200 10000000000
                "run-50-1500296554641",

                //patch       307200 1000000000
                "run-10-1500060382786",
                "run-20-1500062289583",
                "run-30-1500005271092",
                "run-40-1500003680283",
                "run-50-1500001946411",

                //patch BLOCK 307200 1000000000
                "run-10-1500052219431",
                "run-20-1500053722890",//failed
                "run-20-1500054989870",
                "run-30-1500010502518",
                "run-40-1500008544641",
                "run-50-1500006834059",


                //patch PAGE 307200 1000000000
                "run-10-1500058786374",
                "run-20-1500057080894",
                "run-30-1500012204584",
                "run-40-1500013737842",
                "run-50-1500015367758",

                //eap7.1_kaban
                "run-10-1500259802371",
                "run-50-1500261299624",
                "run-50-1500264350758",
                "run-50-1500265972289",
                "run-50-1500267671073",

                //"run-50-1500384571281"//gzipped because of 1.5G server.log
                //"run-50-1500477631362",
                "run-50-1500478787162",

                "run-30-1500515685850",
                "run-40-1500524669666",
                "run-50-1500559163452",

                "run-50-1500652935888",
                "run-50-1500920984283"

        );

        for(String resultName : results){
            File resultFile = new File(baseDirectory,resultName);
            SpecjmsResult result = new SpecjmsResult(resultFile);
            System.out.println(result.getHorizontalRow());
        }

        System.out.println("");



        System.exit(0);


    }

    public static Properties agentProperties(File baseFile){
        Properties rtrn = new Properties();
        File agentFile = new File(baseFile,"benchclient1"+File.separator+"config"+File.separator+"agent.properties");
        if(agentFile.exists()){
            try {
                rtrn.load(new FileInputStream(agentFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return rtrn;
    }
    public static Parser runLogParser(){
        Parser rtrn = new Parser();
        rtrn.add(new Exp("runEntry","(?<timestamp>\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3}) (?<message>.*\n?)").eat(Eat.Line).set(Merge.NewStart));
        rtrn.add(new Exp("kv","^(?<key>\\S+) = (?<value>.+)").eat(Eat.Line).set("key","value"));
        return rtrn;
    }
}
