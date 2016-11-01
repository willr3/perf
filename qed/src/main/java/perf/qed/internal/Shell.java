package perf.qed.internal;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import perf.qed.stream.LineStream;
import perf.qed.stream.MonitoredStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.concurrent.Semaphore;

public class Shell {

    private static final String prompt = "#%@!*> "; // a string unlikely to appear in the output of any command

    private ChannelShell shell;
    private PrintStream commandStream;
    private Semaphore shellLock;
    private MonitoredStream outputStream;
    private ByteArrayOutputStream shStream = new ByteArrayOutputStream();

    private LineStream lineStream = new LineStream();

    private Result result = null;
    private boolean sendUpdates = false;

    private String sentCommand = "";

    public Shell(ChannelShell newShell,Semaphore lock){
        shell = newShell;
        shellLock = lock;
        try {

            shell.setPty(true);
            shell.setPtySize(1024,80,1024,80);
            PipedInputStream pipeIn = new PipedInputStream();
            PipedOutputStream pipeOut = new PipedOutputStream(pipeIn);
            commandStream = new PrintStream(pipeOut);

            shell.setInputStream(pipeIn);

            outputStream = new MonitoredStream(shellLock,prompt.getBytes());

            outputStream.addStream("out",System.out);

            shell.connect();
            shell.setOutputStream(outputStream,true);

            sh("export PS1='" + prompt + "'");

            outputStream.addStream("lineStream",lineStream);

            outputStream.addStream("sh",shStream);

        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void close(){
        try {
            shell.getInputStream().close();
            shell.setInputStream(null);
            outputStream.removeStream("out");
            outputStream.close();
            shell.setOutputStream(null);

            shell.disconnect();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    protected String getOutput(){
        String rtrn = shStream.toString();
        byte b[] = System.lineSeparator().getBytes();
        for(int i=0; i<b.length;i++){
            System.out.println(b[i]+" ");
        }

        //System.out.println("..start "+sentCommand+" ->"+rtrn);
        if(rtrn.startsWith(sentCommand)){
            rtrn = rtrn.substring(sentCommand.length());
            //do not trim here in case prompt has spaces
        }
        if(rtrn.endsWith(prompt)) {
            rtrn = rtrn.substring(0,rtrn.length()-prompt.length()).trim();
        }

        return rtrn;
    }
    public void sh(String command){
        sh(command,true);
    }

    protected void sh(String command,boolean acquireLock){
        if(!shell.isConnected()){
            System.err.println("shell not connected for "+command);
            System.exit(-1);
        } else {

            if(acquireLock){
                try{
                    shellLock.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Thread.interrupted();
                }
            }
            shStream.reset();
            lineStream.reset();
            lineStream.setResult(result);
            commandStream.println(command);
            sentCommand = command;
            commandStream.flush();

        }
    }

    public static void main(String[] args) {

        JSch jsch = new JSch();
        Properties cfg = new Properties();
        cfg.put("StrictHostKeyChecking", "no");
        try {
            jsch.setKnownHosts("~/.ssh/known_hosts");
            jsch.addIdentity("~/.ssh/id_rsa");
            Session session = jsch.getSession("wreicher","127.0.0.1",22);
            session.setConfig(cfg);
            session.connect(10*000);

            ChannelShell csh = (ChannelShell)session.openChannel("shell");

            Host h = new Host("localhost",22);
            Coordinator coordinator = new Coordinator();

            Semaphore semaphore = new Semaphore(1);

            Shell shell = new Shell(csh,semaphore);


            shell.sh("pwd");
            shell.sh("dstat 1 10");
            Thread.sleep(100);
            for(int i=0; i<10; i++){
                System.out.println("--------------------");
                Thread.sleep(500);
            }
            shell.sh("pwd");

            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("calling close");
            shell.close();
            session.disconnect();
            System.out.println("called close");

        } catch (JSchException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }
}
