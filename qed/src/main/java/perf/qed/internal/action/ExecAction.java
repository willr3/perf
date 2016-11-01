package perf.qed.internal.action;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;

/**
 * Created by wreicher
 */
public class ExecAction extends Action {

    private ChannelExec exec;
    private String command;
    public ExecAction(ChannelExec channel, String command){
        super(command);
        this.exec = channel;
        this.command = command;
    }
    @Override
    public void apply(String input) {
        //execs.put(name,exec);
        try {
            exec.setCommand(command);
            exec.setPty(true);
            exec.connect();
            ok(input);
        } catch (JSchException e) {
            e.printStackTrace();
            error(e.getMessage());
        }
    }

    @Override
    public String toString(){
        return command;
    }
}
