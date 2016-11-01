package perf.qed.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by wreicher
 */
public class ScriptParser {

    private LinkedList<Command> commands;
    public ScriptParser(){
        this.commands = new LinkedList<>();
    }

    public void run(String filePath){
        File file = new File(filePath);
        if(!file.exists()){
            //TODO log error
            return;
        }

        try {
            FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr);
            String line = null;
            while( (line=br.readLine())!=null ){

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
