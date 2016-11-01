package perf.qed.internal.action;

import perf.qed.Line;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by wreicher
 */
public class PatternLine implements Line {

    private Matcher m;
    public PatternLine(String pattern){
        m = Pattern.compile(pattern).matcher("");
    }

    @Override
    public String apply(String input) {
        m.reset(input);
        if(m.find()){
            if(m.groupCount()==1) {
                return m.group(1);
            }else{
                return input;
            }
        }
        return null;
    }

    @Override
    public String toString(){
        return "s/"+m.pattern().pattern()+"/g";
    }
}
