package perf.qed;

/**
 * Created by wreicher
 */
public class Specjms {

    public static void main(String[] args) {
        RunBuilder rb = new RunBuilder();
        rb.addScript("standalone", (output, a) -> {

            a.sh("cd ${{serverDir}}");
            a.sh("rm -r standalone/log/*");
            a.sh("rm -r standalone/data/activemq/*");
            a.addArtifact("${{serverDir}}/standalone/log/");
            a.addArtifact("${{serverDir}}/bin/standalone.conf");
            a.addArtifact("${{serverDir}}/bin/standalone.sh");
            a.addArtifact("/tmp/server.console.log");
            a.sh("./bin/standalone.sh -c ${{standaloneFile}} -P ${{standaloneProperties}} 2>&1 > /tmp/server.console.log &");
            a.sh("cat /tmp/server.console.log | grep JAVA_OPTS | cut -d \":\" -f 2-");
            //check the JAVA_OPTS for any artifacts that we want from the run
            a.thenAll(
                a.check(
                    a.patternMatch("-Xloggc:([^\\s]+)\\s"),
                    (fileName) -> {
                        a.addArtifact(fileName); return fileName;
                    }
                ),
                a.check(
                    a.patternMatch("-XX:StartFlightRecording.+?filename=([^,\\s]+"),
                    (fileName) -> {
                        a.addArtifact(fileName); return fileName;
                    }
                )
            );
            //check that started successfully
            a.doWhile(
                a.sh("egrep \"(WFLYSRV002|JBAS01587)[456]\" ${{serverDir}}/standalone/server.log"),
                a.delayCounter(100,4000)
            );
            a.check(
                a.patternMatch("errorString"),
                Line.EMPTY,
                (errorString) -> {
                    a.abort(("Standalone failed to start on "+a.getHostName()));
                    return "";
                }
            );
            a.signal("server_started");
        });

        rb.addScript("controller", (output,a) -> {
            a.sh("cd ${{installDir}}");
            a.sh("git fetch origin");
            a.sh("git checkout ${{runId}}");
            a.waitFor("server_started");
            a.sh("ant jms-setup");
            a.check(
                a.patternMatch("RuntimeException: (.*)"),
                (errorMessage) -> {
                    a.abort(errorMessage);
                    return errorMessage;
                }
            );
            a.signal("server_ready");
            ActionPromise controller = a.sh("ant startController");
            controller.watch((line)->{
                if(line.indexOf("RunLevel")>-1){
                    a.log(line);
                }
                return line;
            });
            controller.watch((line)->{
                if(line.indexOf("Heartbeat failed for an agent")>-1){
                    a.log(line);
                }
                return line;
            });
            controller.watch((line)->{
                if(line.indexOf("javax.jms.JMSException: Messages arrived in wrong order")>-1){
                    a.log(line);
                }
                return line;
            });

        });
        rb.addScript("satellite", (output,a) -> {
            a.sh("cd ${{installDir}}");
            a.sh("git fetch origin");
            a.sh("git checkout ${{runId}}");
            a.waitFor("server_ready");
            a.sh("ant startSatellite -Dcontroller.host=${{controllerHost}}");

        });
        rb.addScript("synctime", (output,a)->{
            a.exec("sudo ntpdate -u clock.redhat.com");
        });

        rb.addScript("dstat", (output,a) -> {
            a.addArtifact("/tmp/dstat.log");
            a.exec("dstat -Tcdngy 1> /tmp/dstat.log");
        });
        rb.addScript("threadCount",(output,a) -> {
           a.exec("ps huH p <PID_OF_U_PROCESS> | wc -l");
        });
        rb.addScript("agents",(output,a) -> {
           a.exec("jps | grep Agent");
        });


    }
}
