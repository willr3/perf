package perf.byteman;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Uses a mustache template find and replace to make it easier to apply a rule to multiple scenarios
 * The keys {@code name} and {@code uid} are always available and represent the name of the Pattern and
 * a counter to ensure uniqueness. Other keys needs to be in the <code>Map</code> provided to <code>apply</code>
 *
 * Mustache Cheat Sheet:
 * {{name}} will first check for a field name then name() then getName()
 * {{{name}}} will include the un-escaped output of name. Use this to avoid html escape codes when necessary (e.g. for {@code<init>} instead of {@code &lt;init&gt;}
 * {{.}} refers to the current object (helpful if iterating over a list of primatives)
 * {{.foo}} will call foo() on the current object
 * {{#bar}} ... {{/bar}} will apply the template in ... for each entry in bar
 * {{^bar}} ... {{/bar}} will apply the template in ... if bar is "falsey"
 *
 */
public class RulePattern {

    private static MustacheFactory MF = new DefaultMustacheFactory();



    private static final String PREFIX = "\\{\\{";
    private static final String SUFFIX = "\\}\\}";
    private static final String MUSTACHE_CONTROLS = "[#/>]";

    private Matcher substitueMatcher = Pattern.compile(PREFIX+MUSTACHE_CONTROLS+"?(?<key>[^\\}]+)"+SUFFIX).matcher("");
    private Set<String> keys;
    private String pattern;
    private Mustache mustache;
    private String name;

    private AtomicInteger uid;

    public static RulePattern onStart(String name,String...pattern){
        String combined =
                "CLASS org.jboss.as.server.ApplicationServerService" + System.lineSeparator()+
                "METHOD start" + System.lineSeparator() +
                combinePattern(pattern) +
                "ENDRULE" + System.lineSeparator();
        return new RulePattern(name,combined);
    }
    public static RulePattern onStop(String name,String...pattern){
        String combined =
                "CLASS org.jboss.as.server.ApplicationServerService" + System.lineSeparator()+
                        "METHOD stop" + System.lineSeparator() +
                        combinePattern(pattern) +
                        "ENDRULE" + System.lineSeparator();
        return new RulePattern(name,combined);
    }

    private static String combinePattern(String...pattern){
        return Arrays.asList(pattern).stream().reduce((a,b)-> a + System.lineSeparator() + b).get()+System.lineSeparator();
    }

    public RulePattern(String name, String...pattern){
        this(name, combinePattern(pattern));
    }
    public RulePattern(String name, String pattern){
        this.uid = new AtomicInteger(0);
        this.name = name;
        this.keys = new LinkedHashSet<>();
        this.pattern = pattern;
        checkRule();
        this.pattern = this.pattern +System.lineSeparator();
        this.mustache = MF.compile(new StringReader(this.pattern),name);

        findKeys();
    }

    public int getUid(){return uid.get();}
    public String getName(){return name;}

    private void checkRule(){

        if(!this.pattern.startsWith("RULE")){
            this.pattern =
                "RULE {{name}}_{{uid}}"+System.lineSeparator()+
                this.pattern;
        }
        if(!this.pattern.endsWith(System.lineSeparator())){
            this.pattern = this.pattern+System.lineSeparator();
        }
        if(!this.pattern.endsWith("ENDRULE"+System.lineSeparator())){
            this.pattern = this.pattern+"ENDRULE"+System.lineSeparator();
        }
    }
    private void findKeys(){
        substitueMatcher.reset(pattern);
        while(substitueMatcher.find()){
            keys.add(substitueMatcher.group("key"));
        }
    }


    public Set<String> getKeys(){return Collections.unmodifiableSet(keys);}

    public String apply(Map<String,Object> values){
        StringWriter writer = new StringWriter();
        apply(writer,values);
        return writer.getBuffer().toString();
    }
    public void apply(Writer writer, Map<String,Object> values){
        values.put("uid",""+uid.incrementAndGet());
        values.put("name",name);
        try {
            mustache.execute(writer,values).flush();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        RulePattern testRule = new RulePattern("test",
                "RULE {{name}}_{{uid}}",
                "{{^foo}} no foo {{/foo}}",
                "CLASS ^{{className}}",
                "METHOD {{{method}}}",
                "HELPER perf.byteman.JsonHelper",
                "AT ENTRY",
                "IF FALSE {{#params}}|| ${{key}}.getClass().getName().equals(\"{{value}}\") {{/params}}",
                "DO",
                "setTriggering(false);",
                "openTrace(\"hc\",\"{{traceFile}}\");",
                "traceJsonStack(\"hc\", new String[]{" +
                        " {{#nonStatic}} \"className\", $this.getClass().getName(), {{/nonStatic}}" +
                        "\"hashCode\", \"\"+System.identityHashCode($this) " +
                        "{{#params}} , " +
                            "\"param.{{.}}.hash\", \"\"+System.identityHashCode(${{.}}), " +
                            "\"param.{{.}}.className\", \"\"+${{key}}.getClass().getName() " +
                        "{{/params}} });",
                "setTriggering(true);",
                "ENDRULE"
        );
        System.out.println(testRule.getKeys());
        Set<Integer> params = new LinkedHashSet<>();
        params.add(0);
        params.add(3);
        Map<String,Object> values = new HashMap<>();
        values.put("params",params);
        values.put("name","myName");
        values.put("className",RulePattern.class.getName());
        values.put("method","<init>");
        values.put("meToo",", \"hi\", \"mom\"");
        values.put("traceFile","/home/wreicher/runtime/test.log");
        values.put("nonStatic",true);
        System.out.println(testRule.apply(values));
    }
}
