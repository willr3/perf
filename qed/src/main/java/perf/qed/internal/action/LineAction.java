package perf.qed.internal.action;

import perf.qed.Line;

/**
 * Created by wreicher
 */
public class LineAction extends Action {


    private Line line;
    public LineAction(Line line, String name){
        super(name);
        this.line = line;
    }

    @Override
    public void apply(String input) {
        try{
            String output = line.apply(input);
            ok(output);
        } catch (Exception e){
            error(e.getMessage());
        }
    }

    @Override
    public String toString(){
        return line.toString();
    }
}
