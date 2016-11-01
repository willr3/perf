package perf.wildfly.client;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

import javax.security.auth.callback.*;
import javax.security.sasl.RealmCallback;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by wreicher
 * /management/management-interfaces
 *   <native-interface security-realm="ManagementRealm">
 *     <socket-binding native="management-native"/>
 *   </native-interface>
 * /socket-binding-group
 *   <socket-binding name="management-native" interface="management" port="${jboss.management.native.port:9999}"/>
 *
 *
 *
 */
public class ClientMonitor {

    public static void main(String[] args) {
        ModelControllerClient client = null;
        try {
            InetAddress inet = InetAddress.getByName("w520");
            int port = 9989;
            client = getClient( inet , port );
            if(needsToAuthenticate(client)){
                try {
                    client.close();
                } catch (IOException e) {}
                System.out.println("Authenticating!!!!");
                String username = "foo";
                String password = "foo";
                client = createClient(inet,port,username,password);//,"ManagementRealm");
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        ModelNode operation = new ModelNode();
        //operation.get("operation").set("read-resource");

        ModelNode address = operation.get("address");

        operation.get("include-runtime").set(true);
        operation.get("recursive").set(true);
        operation.get("operations").set(true);

        ModelNode op = new ModelNode();

        op.get("operation").set("read-resource");
        op.get("recursive").set("true");
        op.get("include-runtime").set("true");
        op.get("include-defaults").set("true");

        //op.get("operation").set("read-resource-description");
        //op.get("operations").set("true");

//        op.get("operation").set("read-children-names");
//        op.get("child-type").set("jms-queue");

        address = op.get("address");
        address.add("subsystem", "messaging-activemq");
        address.add("server","artemis");
        //address.add("jms-queue","*");
        address.add("runtime-queue","*");
        //address.add("connector", "http");

        op.get("recursive").set(true);
        op.get("operations").set(true);

        try {
//            ModelNode returnValue = client.execute(op, new OperationMessageHandler() {
//                public void handleReport(MessageSeverity severity, String message) {
//                    System.out.println(severity.toString()+" -> "+message);
//                }
//            });

//            System.out.println(returnValue.toString());

            NativeClient nc = new NativeClient(client);
            List<String> subsystems = nc.getDeploymentNames();
            System.out.println(subsystems);

            boolean standalone = nc.isStandalone();

            nc.readDeployments();

        } finally {
            try {
                if(client!=null) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    public static ModelControllerClient getClient(InetAddress serverHost, int port){
        ModelControllerClient unauthenticatedClient = ModelControllerClient.Factory.create(serverHost, port);
        return unauthenticatedClient;
    }
    static boolean needsToAuthenticate(ModelControllerClient unauthenticatedClient) {

        try {
            ModelNode testConnection = new ModelNode();
            testConnection.get("operation").set("read-resource");
            unauthenticatedClient.execute(testConnection);
            System.out.println("don't need to authenticate");
            return false;
        } catch(Exception e) {
            System.out.println("need to authenticate "+e.getClass().toString());
            return true;
        }

    }
    private static ModelControllerClient createClient(final InetAddress host, final int port, final String username, final String password) {

        final CallbackHandler callbackHandler = new CallbackHandler() {

            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback current : callbacks) {
                    if (current instanceof NameCallback) {
                        NameCallback ncb = (NameCallback) current;
                        ncb.setName(username);
                    } else if (current instanceof PasswordCallback) {
                        PasswordCallback pcb = (PasswordCallback) current;
                        pcb.setPassword(password.toCharArray());
                    } else if (current instanceof RealmCallback) {
                        RealmCallback rcb = (RealmCallback) current;
                        rcb.setText(rcb.getDefaultText());
                    } else {
                        throw new UnsupportedCallbackException(current);
                    }
                }
            }
        };

        return ModelControllerClient.Factory.create(host, port, callbackHandler);
    }
}

