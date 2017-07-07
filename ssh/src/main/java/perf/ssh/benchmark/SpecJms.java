package perf.ssh.benchmark;

import perf.ssh.*;
import perf.ssh.cmd.*;
import perf.util.AsciiArt;
import perf.util.StringUtil;


import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by wreicher
 */
public class SpecJms {

    private static void populateRepo(ScriptRepo repo){

        repo.script("dstat")
            .then(Cmd.sh("rm -f /tmp/dstat.log"))//-f because @#%$^! root and the rm -i alias
            .then(Cmd.sh("dstat -Tcdngy 1 > /tmp/dstat.log 2>/dev/null & export DSTAT_PID=\"$!\""))
            .then(Cmd.queueDownload("/tmp/dstat.log"))
            .then(Cmd.waitFor("RUN_STOPPED"))
            .then(Cmd.sh("kill ${DSTAT_PID}"));


        repo.script("kill-agents")
            .then(Cmd.sh(" jps | grep Agent | cut -d \" \" -f 1 | xargs -I {} kill -9 {}"));

        repo.script("sync-time")
            .then(Cmd.sh("sudo ntpdate -u clock.redhat.com"));

        repo.script("satellite")
            .then(Cmd.script("kill-agents"))
            .then(Cmd.sh("rm /tmp/specjms.verbose-gc-*"))
            .then(Cmd.waitFor("CONFIG_READY"))
            .then(Cmd.sh("rsync -avz --exclude '.git' --exclude 'output' ${{CONTROLLER_HOST}}:/${{SPECJMS_HOME}}/ ${{SPECJMS_HOME}}"))
            .then(Cmd.sh("cd ${{SPECJMS_HOME}}"))
            .then(Cmd.waitFor("CONTROLLER_STARTED"))
            .then(Cmd.queueDownload("${{SPECJMS_HOME}}/output/${{RUNId}}/*",""))
            .then(Cmd.queueDownload("/tmp/specjms.verbose-gc-*",""))
            .then(Cmd.sh("ant startSatellite")
                .watch(Cmd.regex("SatelliteDriver : RunLevel[Starting Agents] has been signalled")
                    .then(Cmd.signal("SATELLITE_STARTED")))
                .watch(Cmd.regex("(?<interaction>[^:]+): Cannot maintain pacing distribution.*")
                    .then(Cmd.echo())
                )
            )
            .then(Cmd.signal("SATELLITE_STOPPED"));

        repo.script("controller")
            .then(Cmd.sh("cd ${{SPECJMS_HOME}}"))
            .then(Cmd.sh("sed -i '/org.spec.jms.files.topology/c\\org.spec.jms.files.topology = ${{TOPOLOGY}}' ${{SPECJMS_HOME}}/config/run.properties"))
            //only one of these 2 will match but run both to cover both topologies
            .then(Cmd.sh("sed -i '/org.spec.jms.horizontal.BASE/c\\org.spec.jms.horizontal.BASE = ${{BASE}}' ${{SPECJMS_HOME}}/config/${{TOPOLOGY}}"))
            .then(Cmd.sh("sed -i '/org.spec.jms.vertical.BASE/c\\org.spec.jms.vertical.BASE = ${{BASE}}' ${{SPECJMS_HOME}}/config/${{TOPOLOGY}}"))

            .then(Cmd.sh("sed -i '/org.spec.jms.dc.nodes/c\\org.spec.jms.dc.nodes = ${{SATELLITES}}' ${{SPECJMS_HOME}}/config/${{TOPOLOGY}}"))
            .then(Cmd.sh("sed -i '/org.spec.jms.sm.nodes/c\\org.spec.jms.sm.nodes = ${{SATELLITES}}' ${{SPECJMS_HOME}}/config/${{TOPOLOGY}}"))
            .then(Cmd.sh("sed -i '/org.spec.jms.sp.nodes/c\\org.spec.jms.sp.nodes = ${{SATELLITES}}' ${{SPECJMS_HOME}}/config/${{TOPOLOGY}}"))
            .then(Cmd.sh("sed -i '/org.spec.jms.hq.nodes/c\\org.spec.jms.hq.nodes = ${{SATELLITES}}' ${{SPECJMS_HOME}}/config/${{TOPOLOGY}}"))
            .then(Cmd.signal("CONFIG_READY"))
            .then(Cmd.waitFor("SERVER_STARTED"))
            .then(Cmd.sh("ant startController")
                .watch(Cmd.echo())
                .watch(Cmd.regex(".*?BUILD FAILED.*")
                    .then(Cmd.log("failed to start controller"))
                    .then(Cmd.abort())
                )
                .watch(Cmd.regex(".*?Heartbeat failed for an agent.*")
                    .then(Cmd.echo())
                )
                .watch(Cmd.regex(".*?Opened log output file: .*?/(?<RUNId>\\d+)/controller.txt")
                    .then(Cmd.log("runId ${{RUNId}}"))
                    .then(Cmd.queueDownload("${{SPECJMS_HOME}}/output/${{RUNId}}/*",""))
                )
                .watch(Cmd.regex(".*?Waiting for the SPECjmsSatelliteDrivers.*")
                    .then(Cmd.signal("CONTROLLER_STARTED"))
                )
                .watch(Cmd.regex(".*?RunLevel\\[Warmup period\\] started.*")
                    .then(Cmd.signal("WARMUP_STARTED"))
                )
                .watch(Cmd.regex(".*?RunLevel\\[Warmup period\\] stopped.*")
                    .then(Cmd.signal("WARMUP_STOPPED"))
                )
                .watch(Cmd.regex(".*?RunLevel\\[Measurement period\\] started.*")
                    .then(Cmd.signal("STEADYSTATE_STARTED"))
                )
                .watch(Cmd.regex(".*?RunLevel\\[Measurement period\\] stopped.*")
                    .then(Cmd.signal("STEADYSTATE_STOPPED"))
                )
                .watch(Cmd.regex(".*?RunLevel\\[Drain period\\] started.*")
                    .then(Cmd.signal("COOLDOWN_STARTED"))
                )
                .watch(Cmd.regex(".*?RunLevel\\[Drain period\\] stopped.*")
                    .then(Cmd.signal("COOLDOWN_STOPPED"))
                )
            )
            .then(Cmd.signal("CONTROLLER_STOPPED"))
            .then(Cmd.signal("RUN_STOPPED"));


        repo.script("docker-oracle")
            .then(Cmd.log("starting docker"))
            .then(Cmd.signal("DATABASE_STARTING"))
            .then(Cmd.sh("docker run -d -p 10080:8080 -p 1521:1521 sath89/oracle-12c"))
            .then(Cmd.code((input,state)->{
                state.set("ORACLE_CONTAINER_ID",input.trim());
                return Result.next(input.trim());
            }))
            .then(Cmd.log("container id = ${{ORACLE_CONTAINER_ID}}"))
            .then(Cmd.sh("docker logs -f ${{ORACLE_CONTAINER_ID}}")
                .watch(Cmd.echo())
                .watch(Cmd.regex(".*?Database ready to use\\. Enjoy! ;\\).*")
                    .then(Cmd.ctrlC()) // done tailing
                    .then(Cmd.signal("DATABASE_STARTED"))
                    .then(Cmd.log("DATABASE_STARTED"))
                )
            )
            .then(Cmd.waitFor("SERVER_STOPPED"))
            .then(Cmd.log("server stopped, stopping database"))
            .then(Cmd.sh("docker stop ${{ORACLE_CONTAINER_ID}}"))
            //.then(Cmd.sh("docker logs ${{ORACLE_CONTAINER_ID}}"))
            .then(Cmd.sh("docker rm ${{ORACLE_CONTAINER_ID}}"))
            //.then(Cmd.sh("docker ps -a"))
            .then(Cmd.signal("DATABASE_STOPPED"));

        repo.script("amq6")
            .then(Cmd.sh("cd ${{AMQ6_HOME}}"))
            .then(Cmd.sh("rm /perf1/amq6/log/*"))
            .then(Cmd.sh("rm /tmp/amq6.console.log"))
            .then(Cmd.queueDownload("/perf1/amq6/log/*"))
            .then(Cmd.queueDownload("${{AMQ6_HOME}/etc/activemq.xml"))
            .then(Cmd.queueDownload("${{AMQ6_HOME}/bin/karaf"))
            .then(Cmd.waitFor("DATABASE_STARTED"))
            .then(Cmd.signal("SERVER_STARTING"))
            .then(Cmd.sh("./bin/start"))
            .then(Cmd.sh("export SERVER_PID=$(jps -v | grep \"Dkaraf.home\" | cut -d \" \" -f1)"))
            .then(Cmd.sleep(1_000))//because otherwise we try to tail before it exists
            .then(Cmd.sh("tail -f /perf1/amq6/log/amq.log")
                .watch(Cmd.regex(".*?Broker amq has started.*")
                    .then(Cmd.ctrlC())
                    .then(Cmd.signal("SERVER_STARTED"))
                )
                .watch(Cmd.regex(".*?ERROR.*")//expect to see some about container-history and
                    .then(Cmd.echo())
                )
            )
            .then(Cmd.sh("jps -lvm | grep \"Dkaraf.home\" | cut -d \" \" -f2")
                .then(Cmd.regex(".*? -Xloggc:(?<gcFile>\\S+).*")
                    .then(Cmd.code((input,state)->{
                            String gcFile = state.get("gcFile");
                            if(gcFile!=null && gcFile.indexOf("%")>-1) {
                                state.setScript("gcFile",gcFile.substring(0,gcFile.indexOf("%")));
                                return Result.next(input);
                            }else{
                                return Result.skip(input);
                            }
                        })
                        .then(Cmd.sh("lsof -p ${SERVER_PID} | grep --color=none \"${{gcFile}}\"")
                                //added [\r\n]+ because otherwise rsync appends \#015 to the file name
                                .then(Cmd.regex(".*? (?<gcFile>/.+?)[\r\n]+")
                                        .then(Cmd.log(" updated gcFile=${{gcFile}}"))
                                )
                        )
                    )
                    .then(Cmd.log("gcFile=${{gcFile}}"))
                    .then(Cmd.queueDownload("${{gcFile}}"))
                )
                .then(Cmd.regex(".*? -XX:StartFlightRecording.*?filename=(?<jfrFile>\\S+).*")
                    .then(Cmd.queueDownload("${{jfrFile}}"))
                )
            )
            .then(Cmd.waitFor("CONTROLLER_STOPPED"))
            .then(Cmd.sh("./bin/stop"))
            .then(Cmd.sleep(1_000))
            .then(Cmd.signal("SERVER_STOPPED"));

        repo.script("eap")
            .then(Cmd.sh("cd ${{EAP_HOME}}"))
            .then(Cmd.sh("rm /tmp/eap7.standalone.console.log"))
            .then(Cmd.sh("rm ./standalone/log/*"))

            .then(Cmd.queueDownload("/tmp/eap7.standalone.console.log"))
            .then(Cmd.queueDownload("${{EAP_HOME}}/standalone/log/*"))
            .then(Cmd.queueDownload("${{EAP_HOME}}/bin/standalone.sh"))
            .then(Cmd.queueDownload("${{EAP_HOME}}/bin/standalone.conf"))
            .then(Cmd.queueDownload("${{EAP_HOME}}/standalone/configuration/${{STANDALONE_XML}}"))

            .then(Cmd.waitFor("DATABASE_STARTED"))
            .then(Cmd.signal("SERVER_STARTING"))
            .then(Cmd.sh("./bin/standalone.sh -c ${{STANDALONE_XML}} ${{STANDALONE_SH_ARGS}} > /tmp/eap7.standalone.console.log 2>/dev/null & ")
                    .then(Cmd.echo())
            )
            .then(Cmd.sh("export SERVER_PID=$(jps | grep \"jboss-modules.jar\" | cut -d \" \" -f1)"))
            .then(Cmd.sleep(1_000))//because otherwise we try to tail standalone/log/server.log before it exists
            .then(Cmd.sh("tail -f ./standalone/log/server.log")
                .watch(Cmd.regex(".*? WFLYSRV0025: (?<eapVersion>.*?) started in (?<eapStartTime>\\d+)ms.*")
                    .then(Cmd.ctrlC())//end the tail
                    .then(Cmd.log("eap ${{eapVersion}} started in "+AsciiArt.ANSI_GREEN+"${{eapStartTime}}"+AsciiArt.ANSI_RESET))
                    .then(Cmd.signal("SERVER_STARTED"))
                )
                .watch(Cmd.regex(".*? WFLYSRV0026: .*")
                    .then(Cmd.ctrlC())
                    .then(Cmd.log("eap started with errors"))
                    .then(Cmd.abort())
                )
                .watch(Cmd.regex(".*?FATAL.*")
                    .then(Cmd.log(AsciiArt.ANSI_RED+"FATAL"+AsciiArt.ANSI_RESET))
                    .then(Cmd.echo())
                    .then(Cmd.ctrlC())
                )
            )
            .then(Cmd.sh("grep --color=none \"javaOpts\\|JAVA_OPTS\" /tmp/eap7.standalone.console.log")
                .then(Cmd.regex(".*? -Xloggc:(?<gcFile>\\S+).*")
                    .then(Cmd.code((input,state)->{
                        String gcFile = state.get("gcFile");
                        if(gcFile!=null && gcFile.indexOf("%")>-1) {
                            state.setScript("gcFile",gcFile.substring(0,gcFile.indexOf("%")));
                            return Result.next(input);
                        }else{
                            return Result.skip(input);
                        }
                    })
                        .then(Cmd.sh("lsof -p ${SERVER_PID} | grep --color=none \"${{gcFile}}\"")
                                //added [\r\n]+ because otherwise rsync appends \#015 to the file name
                                .then(Cmd.regex(".*? (?<gcFile>/.+?)[\r\n]+")
                                    .then(Cmd.log(" updated gcFile=${{gcFile}}"))
                                )
                        )
                    )
                    .then(Cmd.log("gcFile=${{gcFile}}"))
                    .then(Cmd.queueDownload("${{gcFile}}"))
                )
                .then(Cmd.regex(".*? -XX:StartFlightRecording.*?filename=(?<jfrFile>\\S+).*")
                    .then(Cmd.queueDownload("${{jfrFile}}"))
                )
            )
            .then(Cmd.waitFor("CONTROLLER_STOPPED"))
            .then(Cmd.sh("kill ${SERVER_PID}"))
            .then(Cmd.sleep(1_000))
//            .then(Cmd.sh("tail -f ./standalone/log/server.log")
//                .watch(Cmd.regex(".*? WFLYSRV0050 .*")//wait for server stopped
//                    .then(Cmd.ctrlC())
//                )
//            )
            .then(Cmd.signal("SERVER_STOPPED"));
    }

    public static void main(String[] args) {


        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();

        final AtomicInteger factoryCounter = new AtomicInteger(0);
        ThreadFactory factory = r -> new Thread(r,"PT-"+factoryCounter.getAndIncrement());
        ThreadPoolExecutor executor = new ThreadPoolExecutor(8,24,30, TimeUnit.MINUTES,workQueue,factory);

        CommandDispatcher dispatcher = new CommandDispatcher(executor);
        dispatcher.addObserver(new CommandDispatcher.Observer() {
            @Override
            public void onStart(Cmd command) {}

            @Override
            public void onNext(Cmd command,String output){}


            @Override
            public void onStop(Cmd command) {}

            @Override
            public void onStart() {
                System.out.println(AsciiArt.ANSI_GREEN+"starting "+AsciiArt.ANSI_RESET);
            }

            @Override
            public void onStop() {
                System.out.println(AsciiArt.ANSI_GREEN+"stopping "+AsciiArt.ANSI_RESET);
            }
        });



        Host server3 = new Host("root","benchserver3");
        Host server4 = new Host("benchuser","benchserver4");

        Host client1 = new Host("benchuser","benchclient1");
        Host client2 = new Host("benchuser","benchclient2");
        Host client3 = new Host("benchuser","benchclient3");
        Host client4 = new Host("benchuser","benchclient4");
        Host w520 = new Host("wreicher","w520");

        for(String base : Arrays.asList("30")){
            System.out.println(AsciiArt.ANSI_CYAN+ "BASE "+base+AsciiArt.ANSI_RESET);
            Run run = new Run("specjms2007","/home/wreicher/perfWork/amq/jdbc/run-"+base+"-"+System.currentTimeMillis(),dispatcher);
            populateRepo(run.getRepo());

            State state = run.getState();
            state.setRun("SPECJMS_HOME","/home/benchuser/code/specjms2007");

            //state.setRun("EAP_HOME","/home/benchuser/runtime/jboss-eap-7.1.0.ER1-jdbc");
            state.setRun("EAP_HOME","/home/benchuser/runtime/jboss-eap-7.x.patched");
            state.setRun("STANDALONE_XML","standalone-full-ha-jdbc-store.xml");
            state.setRun("STANDALONE_SH_ARGS","-b 0.0.0.0");
            state.setRun("TOPOLOGY","horizontal.properties");//horizontal.properties or vertical.properties
            state.setRun("BASE",base);
            state.setRun("CONTROLLER_HOST","benchclient1");
            state.setRun("SATELLITES","benchclient1");

            ScriptRepo repo = run.getRepo();

            run.getRole("server").add(server4);
            run.getRole("server").addRunScript(repo.script("eap"));

            run.getRole("database").add(server3);
            run.getRole("database").addRunScript(repo.script("docker-oracle"));

            run.getRole("controller").add(client1);
            run.getRole("controller").addRunScript(repo.script("controller"));

            run.getRole("satellite").add(client1);
            run.getRole("satellite").addRunScript(repo.script("satellite"));

            run.allHosts().addSetupScript(repo.script("sync-time"));
            run.allHosts().addRunScript(repo.script("dstat"));

            System.out.println("Starting");
            long start = System.currentTimeMillis();
            run.run();
            long stop = System.currentTimeMillis();
            System.out.println(AsciiArt.ANSI_GREEN+"Finished in "+ StringUtil.durationToString(stop-start)+AsciiArt.ANSI_RESET);
            System.out.println("ActiveCount = "+dispatcher.getActiveCount());

            dispatcher.closeSessions();

        }


        List<Runnable> runnables = executor.shutdownNow();
        System.out.println("Runnables?");

        for(Runnable runnable : runnables){
            System.out.println(runnable.getClass());
        }

        //System.exit(0);
//
//        Cmd startAmq7 = (input, api)->{
//            api.sh("cd "+api.get("ARTEMIS_HOME"));
//            api.sh("rm -r ./data/*");
//            api.sh("./bin/artemis run 2>&1 > /tmp/amq7.console.log &")
//                .onWorked((pid,a)->{
//                    Matcher integerMatcher = Pattern.compile("\\d+").matcher("");
//                    String split = pid.substring(pid.indexOf(" "));
//                    if(split == null || !integerMatcher.reset(split).matches()){
//                        a.abort("startAmq7 Cmd expected [1] {pid} as output but got: "+pid);
//                    }
//                    a.set(a.getHostname()+"_amq7",pid.split("]")[1].trimEmptyText());
//                });
//        };
//        Cmd stopAmq7 = (input, api)->{
//            String pid = api.get(api.getHostname()+"_dstatPid");
//            if(pid==null || pid.isEmpty()){
//                api.abort("killDstat Cmd expected env "+ api.getHostname()+"_dstatPid but got: "+pid);
//            }else{
//                api.sh("kill "+pid);
//            }
//        };
    }
}