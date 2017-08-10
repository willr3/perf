package perf.wildfly.client;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Created by wreicher
 */
public class NativeClient {

    ModelControllerClient client = null;

    Tree deployment = Tree.newRoot();
    Tree subsystems = Tree.newRoot().add("subsystem");

    public NativeClient(ModelControllerClient client){
        this.client = client;

        //TODO sub.add("ejb3","entity-bean");
        Tree d = deployment.add("result").as("name");

        Tree sub = new Tree("subsystem");
        d.add("subdeployment","*").add(sub);
        sub.add("ejb3","message-driven-bean","*","execution-time");
        sub.add("ejb3","message-driven-bean","*","invocations");
        sub.add("ejb3","message-driven-bean","*","peak-concurrent-invocations");
        sub.add("ejb3","message-driven-bean","*","pool-available-count");
        sub.add("ejb3","message-driven-bean","*","pool-create-count");
        sub.add("ejb3","message-driven-bean","*","pool-current-size");
        sub.add("ejb3","message-driven-bean","*","pool-max-size");
        sub.add("ejb3","message-driven-bean","*","pool-remove-count");
        sub.add("ejb3","message-driven-bean","*","wait-time");

        //TODO sub.add("ejb3","singleton-bean");
        sub.add("ejb3","stateful-session-bean","*","cache-size");
        sub.add("ejb3","stateful-session-bean","*","");
        sub.add("ejb3","stateful-session-bean","*","execution-time");
        sub.add("ejb3","stateful-session-bean","*","invocations");
        sub.add("ejb3","stateful-session-bean","*","passivated-count");
        sub.add("ejb3","stateful-session-bean","*","peak-concurrent-invocations");
        sub.add("ejb3","stateful-session-bean","*","total-size");
        sub.add("ejb3","stateful-session-bean","*","wait-time");

        sub.add("ejb3","stateless-session-bean","*","execution-time");
        sub.add("ejb3","stateless-session-bean","*","invocations");
        sub.add("ejb3","stateless-session-bean","*","peak-concurrent-invocations");
        sub.add("ejb3","stateless-session-bean","*","pool-available-count");
        sub.add("ejb3","stateless-session-bean","*","pool-create-count");
        sub.add("ejb3","stateless-session-bean","*","pool-current-size");
        sub.add("ejb3","stateless-session-bean","*","pool-max-size");
        sub.add("ejb3","stateless-session-bean","*","pool-remove-count");
        sub.add("ejb3","stateless-session-bean","*","wait-time");

        sub.add("undertow").run(tree(3));
        sub.add("undertow","active-sessions");
        sub.add("undertow","expired-sessions");
        sub.add("undertow","max-active-sessions");
        sub.add("undertow","rejected-sessions");
        sub.add("undertow","session-avg-alive-time");
        sub.add("undertow","session-max-alive-time");
        sub.add("undertow","sessions-created");
        sub.add("undertow","servlet","*","max-request-time");
        sub.add("undertow","servlet","*","min-request-time");
        sub.add("undertow","servlet","*","request-count");
        sub.add("undertow","servlet","*","total-request-time");


        sub.add("webservices","endpoint","*","average-processing-time");
        sub.add("webservices","endpoint","*","fault-count");
        sub.add("webservices","endpoint","*","max-processing-time");
        sub.add("webservices","endpoint","*","min-processing-time");
        sub.add("webservices","endpoint","*","request-count");
        sub.add("webservices","endpoint","*","response-count");
        sub.add("webservices","endpoint","*","total-processing-time");
        //TODO activemq.add("server","*","jms-topic");


        Tree activemq = subsystems.add("messaging-activemq");
        activemq.add("server","*","jms-queue","*","consumer-count");
        activemq.add("server","*","jms-queue","*","delivering-count");
        activemq.add("server","*","jms-queue","*","message-count");
        activemq.add("server","*","jms-queue","*","messages-added");
        activemq.add("server","*","jms-queue","*","scheduled-count");

        activemq.add("server","*","runtime-queue","*","consumer-count");
        activemq.add("server","*","runtime-queue","*","delivering-count");
        activemq.add("server","*","runtime-queue","*","message-count");
        activemq.add("server","*","runtime-queue","*","messages-added");
        activemq.add("server","*","runtime-queue","*","scheduled-count");

        Tree undertow = subsystems.add("undertow");
        undertow.add("statistics-enabled");
        undertow.add("server","*","http-listener","*","bytes-received");
        undertow.add("server","*","http-listener","*","bytes-sent");
        undertow.add("server","*","http-listener","*","error-count");
        undertow.add("server","*","http-listener","*","max-processing-time");
        undertow.add("server","*","http-listener","*","processing-time");
        undertow.add("server","*","http-listener","*","read-timeout");//TODO is this a stat?
        undertow.add("server","*","http-listener","*","request-count");

        undertow.add("server","*","https-listener","*","bytes-received");
        undertow.add("server","*","https-listener","*","bytes-sent");
        undertow.add("server","*","https-listener","*","error-count");
        undertow.add("server","*","https-listener","*","max-processing-time");
        undertow.add("server","*","https-listener","*","processing-time");
        undertow.add("server","*","https-listener","*","read-timeout");//TODO is this a stat?
        undertow.add("server","*","https-listener","*","request-count");

        Tree transactions = subsystems.add("transactions");
        transactions.add("enable-statistics").run((n)->{if(!n.asBoolean()){
            System.out.println("why no transaction statistics");
            }
        });
        //TODO transactions.add("statistics-enabled");

        transactions.add("number-of-aborted-transactions");
        transactions.add("number-of-application-rollbacks");
        transactions.add("number-of-committed-transactions");
        transactions.add("number-of-heuristics");
        transactions.add("number-of-inflight-transcations");
        transactions.add("number-of-nested-transactions");
        transactions.add("number-of-resource-rollbacks");
        transactions.add("number-of-timed-out-transactions");
        transactions.add("number-of-transactions");




    }


    public Consumer<ModelNode> tree(final int depth){
        return (n)->{
            System.out.println("TREE::");
            n.asPropertyList().forEach((p)->{keyTree(p,"","  ",depth);});
        };
    }

    public boolean isDomain(){
        return false;
    }
    public boolean isStandalone(){
        boolean rtrn = false;

        ModelNode op = new ModelNode();

        op.get("operation").set("read-attribute");
        op.get("name").set("process-type");

        ModelNode response = null;
        try {
            response = client.execute(op);
            rtrn = response.get("result").toString().equalsIgnoreCase("Server");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
    public void readDeployments(){
        this.readDeployments(null);
    }
    private void processDeployment(ModelNode node){

        System.out.println("WALK");
        System.out.println(node.toJSONString(false));
        Map<String,Double> matches = new HashMap<>();

        deployment.walk(node,"",matches);

        for(String k : matches.keySet()){
            System.out.println("  "+k+" : "+matches.get(k));
        }

//        node.get("subdeployment").asPropertyList().forEach((p)->{
//            System.out.println(p.getName()+"::");
//            p.getValue().get("subsystem").asPropertyList().forEach((k)->{
//                System.out.println("  "+k.getName());
//                k.getValue().asPropertyList().forEach((v)->{
//                    System.out.println("    "+v.getName()+" :: "+v.getValue().getType());
//                });
//            });
//        });

        //node.asPropertyList().forEach((k)->{keyTree(k,"","  ",2);});


//        if(node.get("subdeployment").isDefined()) {
//            node.get("subdeployment").asPropertyList().forEach((p) -> {
//                p.getName();
//                processDeployment(p.getValue());
//            });
//        }
//        node.get("subsystem").asPropertyList().forEach((p)->{
//            switch(p.getName().toLowerCase()){
//                case "undertow":
//                    keyTree(p,"","  ", 3);
//                    break;
//                case "ejb3":
//
//                    break;
//                default:
//                    System.out.println(p.getName());
//            }
//        });
    }
    private void keyTree(Property node, String prefix, String step,int depth){
        System.out.println(prefix+node.getName()+" :: "+node.getValue().getType());
        switch(node.getValue().getType()){
            case OBJECT:
                node.getValue().asPropertyList().forEach((v)->{
                    if(depth>0){
                        keyTree(v,prefix+step,step,depth-1);
                    }
                });
                break;
            case LIST:
                node.getValue().asList().forEach((v)->{
                    if(depth>0){
                        keyTree(v.asProperty(),prefix+step,step,depth-1);
                    }
                });
        }
    }
    public void readDeployments(String name){
        ModelNode op = new ModelNode();
        ModelNode address = op.get("address");
        op.get("operation").set("read-resource");
        op.get("include-runtime").set(true);
        op.get("recursive").set(true);
        op.get("include-defaults").set(true);

        if(name!=null){
            address.get("deployment").set(name);
        }else{
            address.get("deployment").set("*");
        }


        try {
            ModelNode response = client.execute(op);
            //System.out.println(response.toJSONString(false));

            //System.out.println(response.get("result").toJSONString(false));

            System.out.println(response.toJSONString(false));

            if(name==null){
                System.out.println("name==null");

                response.get("result").asList().forEach( dep -> processDeployment(dep) );
            }else{
                System.out.println("name!=null");
                processDeployment(response.get("result"));
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public List<String> getDeploymentNames(){
        List<String> rtrn = new ArrayList<String>();
        ModelNode op = new ModelNode();
        ModelNode address = op.get("address");
        op.get("operation").set("read-children-names");
        op.get("child-type").set("deployment");
        try {
            ModelNode response = client.execute(op);
            response.get("result").asList().forEach( (node) -> rtrn.add(node.toString()) );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.unmodifiableList(rtrn);
    }
    public List<String> getSubsystemNames(){
        List<String> rtrn = new ArrayList<String>();
        ModelNode op = new ModelNode();
        ModelNode address = op.get("address");
        op.get("operation").set("read-children-names");
        op.get("child-type").set("subsystem");
        try {
            ModelNode response = client.execute(op);
            response.get("result").asList().forEach( (node) -> rtrn.add(node.toString()) );
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.unmodifiableList(rtrn);
    }
}
