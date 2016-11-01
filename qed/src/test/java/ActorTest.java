import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import perf.qed.Run;
import perf.qed.Script;
import perf.qed.internal.ActorImpl;
import perf.qed.internal.Coordinator;
import perf.qed.internal.Host;
import perf.qed.internal.Shell;

import java.util.Properties;
import java.util.concurrent.Semaphore;

/**
 * Created by wreicher
 */
public class ActorTest {


    public static void printB(byte b[],int off,int len){
        String spaces = "         ";
        StringBuilder bytes = new StringBuilder();
        StringBuilder chars = new StringBuilder();
        bytes.append("[");
        chars.append("[");
        if(b!=null && b.length>0){
            int lim = off+len;
            for(int i=off; i<lim; i++){
                int v = b[i];
                String append = v+"";
                bytes.append(append);
                bytes.append(" ");
                if(v == 10){
                    chars.append(spaces.substring(0,append.length()-2));
                    chars.append("\\n");
                }else if (v == 13){
                    chars.append(spaces.substring(0,append.length()-2));
                    chars.append("\\r");
                }else {
                    chars.append(spaces.substring(0, append.length() - 1));
                    chars.append((char) v);
                }
                chars.append(" ");
            }
            bytes.append("]");
            chars.append("]");
        }
        System.out.println("bytes="+bytes.toString());
        System.out.println("chars="+chars.toString());
    }

    public static void main(String[] args) {
        System.out.println("main-"+Thread.currentThread().getName());
        JSch jsch = new JSch();
        Properties cfg = new Properties();
        cfg.put("StrictHostKeyChecking", "no");


        byte b[] = new byte[1024];
        byte tst[] = "test \r\nstring \rline".getBytes();
        System.arraycopy(tst,0,b,0,tst.length);

        printB(b,0,tst.length);

        for(int i=0; i<tst.length; i++){
            if(tst[i] == 10){//newline
                if(tst[i+1] == 13){
                    System.out.println(i+"..."+(i+1));
                }else{
                    System.out.println(i);
                }
            }
        }

        System.out.println(0x1B);
        //System.exit(0);

        try {
            jsch.setKnownHosts("~/.ssh/known_hosts");
            jsch.addIdentity("~/.ssh/id_rsa");
            Session session = jsch.getSession("wreicher","127.0.0.1",22);
            session.setConfig(cfg);
            session.connect(10*000);
            if(!session.isConnected()){
                System.out.println("session not connected");
            }else{
                System.out.println("session connected");
            }
            ChannelShell csh = (ChannelShell)session.openChannel("shell");

            Host h = new Host("localhost",22);
            Coordinator coordinator = new Coordinator();
            Semaphore sem = new Semaphore(1);
            Shell s = new Shell(csh,sem);

            Script script = (output,a)->{

                a.sh("pwd");
                a.check(a.patternMatch("wreicher"),
                        (input)->{
                            Thread.dumpStack();
                            System.out.println("hi will");
                            return input;
                        },
                        (input)->{
                            System.out.println("where did you go?");
                            return input;
                        });

//                a.sh("/home/wreicher/code/github/wildfly/dist/target/wildfly-10.0.0.CR5-SNAPSHOT/bin/standalone.sh")
//                    .watch((input)->{
//                        System.out.println("{"+input+"}");
//                        if(input.contains("WFLYSRV0025")){
//                           System.out.println(":) :) :) :) :) :) :) :) :) :) :) :) :) :) :) :)");
//                            System.exit(-1);
//                        }
//
//                        return input;
//
//                    });

            };
            Run r = new Run("test-run");
            ActorImpl ai = new ActorImpl(s,session,coordinator,r,"test",h);

            //ai.start("hi mom");

        } catch (JSchException e) {
            e.printStackTrace();
        }

    }





}
