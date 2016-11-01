package perf.qed.internal;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Created by wreicher
 */
public class Host {

    private String hostName;
    private String address;
    private int port;
    private byte password[];
    private String identity;
    private String knownHosts;
    private String userName;

    private HashMap<String,String> env;

    public Host(String hostName){
        this(hostName,null,22);
    }
    public Host(String hostName, int port){
        this(hostName,null,port);
    }
    public Host(String hostName, String userName, int port){
        this.hostName = hostName;
        this.userName = userName;
        this.port = port;
        try {
            this.address = InetAddress.getByName(hostName).getHostAddress();
        } catch (UnknownHostException e) {
            System.out.println("failed to resolve "+hostName);
            e.printStackTrace();
        }
        env = new HashMap<>();
    }

    public String getEnv(String name){
        return env.get(name);
    }
    public void setEnv(String name,String value){
        env.put(name,value);
    }

    public String getAddress() {
        return address;
    }
    public int getPort() {
        return port;
    }
    public String getKnownHosts() {
        return knownHosts;
    }
    public String getUserName() {
        return userName;
    }
    public byte[] getPassword() {
        return password;
    }
    public String getIdentity() {
        return identity;
    }
    public String getHostName() {
        return hostName;
    }

    public Host setUser(String userName){
        this.userName = userName;
        return this;
    }
    public Host setIdentity(String identity){
        this.identity = identity;
        return this;
    }
    public Host setPassword(byte password[]){
        this.password = password;
        return this;
    }
    public Host setKnownHosts(String knownHosts){
        this.knownHosts = knownHosts;
        return this;
    }

}
