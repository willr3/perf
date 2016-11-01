package perf.diff;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import perf.util.StringUtil;
import perf.util.xml.XmlLoader;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.*;

/**
 *
 */
public class XmlDiff{

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private String leftPath;
    private String rightPath;

    private XmlLoader loader = null;

    private Document left;
    private String leftShort = "left";

    private Document right;
    private String rightShort = "right";

    private Map<String,Integer> keyAttributes;
    private Map<String,Integer> versionedAttributes;

    public XmlDiff(){
        loader = new XmlLoader();
        keyAttributes = new HashMap<String,Integer>();
        versionedAttributes = new HashMap<String,Integer>();
    }


    public void addKeyAttribute(String attribute){
        addKeyAttribute(attribute,0);
    }
    public void addKeyAttribute(String attribute,int tolerance){
        keyAttributes.put(attribute,tolerance);
    }
    public void addVersionAttribute(String attribute,int versionLength){
        versionedAttributes.put(attribute,versionLength);
    }
    public void loadLeft(Path path){
        loadLeft("left",path);
    }
    public void loadLeft(String aKey, Path path){
        leftShort = aKey;
        left = loader.loadDocument(path);
    }
    public void loadLeft(String key,String xml){
        leftShort = key;
        left = loader.loadDocument(xml);
    }

    public void loadRight(Path path) {
        loadRight("right",path);
    }
    public void loadRight(String key, Path path){
        rightShort = key;
        right = loader.loadDocument(path);
    }
    public void loadRight(String key,String xml){
        rightShort = key;
        right = loader.loadDocument(xml);
    }

    public List<Diff> getDiff(){
        LinkedList<Diff> rtrn = new LinkedList<Diff>();

        Node ra = left.getDocumentElement();
        Node rb = right.getDocumentElement();

        diffNodes(ra,rb,"",rtrn);

        return rtrn;
    }
    private void diffAttributes(Node a,Node b,String aPath,List<Diff> diffs) {
        NamedNodeMap aAttributes = a.getAttributes();
        Map<String, String> bAttr = getAttributes(b);

        if (aAttributes != null) {
            for (int i = 0; i < aAttributes.getLength(); i++) {
                Node attr = aAttributes.item(i);
                if (bAttr.containsKey(attr.getNodeName())) {
                    if (attr.getNodeValue().equals(bAttr.get(attr.getNodeName()))) {
                        //no change
                    } else {
                        diffs.add(new Diff(Diff.Operation.MODIFY,aPath + "/@" + attr.getNodeName(), attr.getNodeValue(), bAttr.get(attr.getNodeName())));
                    }
                    bAttr.remove(attr.getNodeName());
                } else {
                    diffs.add(new Diff(Diff.Operation.DELETE,aPath + "/@" + attr.getNodeName(), attr.getNodeValue(), ""));
                }
            }
        }
        for (String toAdd: bAttr.keySet()){
            diffs.add(new Diff(Diff.Operation.ADD,aPath+"/@"+toAdd,"",bAttr.get(toAdd)));
        }


    }
    private String getKeyAttributes(Node n){

        StringBuilder attrs= new StringBuilder("[");
        for(String keyAttr : keyAttributes.keySet()){
            int tolerance = keyAttributes.get(keyAttr);
            if(n.hasAttributes() && n.getAttributes().getNamedItem(keyAttr)!=null){
                attrs.append(" @");
                attrs.append(keyAttr);
                attrs.append("=");
                attrs.append(n.getAttributes().getNamedItem(keyAttr).getNodeValue());
            }
        }
        for(String keyAttr : versionedAttributes.keySet()){
            //int tolerance = versionedAttributes.get(keyAttr);
            if(n.hasAttributes() && n.getAttributes().getNamedItem(keyAttr)!=null){
                attrs.append(" @");
                attrs.append(keyAttr);
                attrs.append("=");
                attrs.append(n.getAttributes().getNamedItem(keyAttr).getNodeValue());
            }
        }
        if(attrs.length()>1){
            attrs.append(" ]");
        }else {
            //System.out.println("<> empty attr for "+simpleString(n)+" "+attrs.toString());
            return "";
        }
        //System.out.println("<> attr for "+simpleString(n)+" "+attrs.toString());
        return attrs.toString();
    }
    private void diffNodes(Node a,Node b,String aParentPath,List<Diff> diffs){
//        System.out.println("diffNodes");
//        System.out.println(" a "+simpleString(a));
//        System.out.println(" b "+simpleString(b));
        if(a==null){
            diffs.add(new Diff(Diff.Operation.ADD,aParentPath+"/"+b.getNodeName()+getKeyAttributes(b), "", xmlString(b,false)));
            return;
        }
        String aPath = aParentPath+"/"+a.getNodeName()+getKeyAttributes(a);
        if(b==null){
            diffs.add(new Diff(Diff.Operation.DELETE, aParentPath+"/"+a.getNodeName()+getKeyAttributes(a), xmlString(a, false), ""));
            return;
        }
        if(a.getNodeName().equals(b.getNodeName())){
            //diff attributes then diff children
            diffAttributes(a,b,aPath,diffs);

            if(a.getNodeValue()!=null || b.getNodeValue()!=null){
                if(a.getNodeValue()==null){
                    diffs.add(new Diff(Diff.Operation.ADD,aPath,"",xmlString(b,false)) );
                } else if (b.getNodeValue()==null){
                    diffs.add(new Diff(Diff.Operation.DELETE,aPath,xmlString(a,false),"") );
                } else if (!a.getNodeValue().trim().equals(b.getNodeValue().trim())) {
                    diffs.add(new Diff(Diff.Operation.MODIFY,aPath,a.getNodeValue().trim(),b.getNodeValue().trim()));
                }
            }

            NodeList aChildren = a.getChildNodes();
            NodeList bChildren = b.getChildNodes();
            Map<String,List<Node>> namedNodes = new HashMap<String, List<Node>>();
            //build right map of node name - > nodes for children of B
            for(int bc=0; bc<bChildren.getLength(); bc++){
                Node bChild = bChildren.item(bc);
                if(bChild.getNodeType()==Node.TEXT_NODE && bChild.getNodeValue().trim().isEmpty()){
                    continue;
                }
                String childKey = bChild.getNodeName();
                if(!namedNodes.containsKey( childKey )){
                    List<Node> toAdd = new ArrayList<Node>();
                    namedNodes.put(childKey,toAdd);
                }
                namedNodes.get(childKey).add(bChild);
            }
            for(int i=0; i<aChildren.getLength();i++){
                Node aChild = aChildren.item(i);

                if(aChild.getNodeType()==Node.TEXT_NODE && aChild.getNodeValue().trim().isEmpty()){
                    continue;
                }

                String childKey = aChild.getNodeName();
                String childPath = aPath+"/"+childKey+getKeyAttributes(aChild);
                if(!namedNodes.containsKey( childKey )){
                    //TODO any call to diffs.put will require a unique childPath, repeat childPahts will replace previous values :(
                   diffs.add(new Diff(Diff.Operation.DELETE,childPath,xmlString(aChild, false), ""));
                }else{
                    List<Node> potentialMatches = namedNodes.get( childKey );
                    int threshold = 30;//15
                    int bestScore=Integer.MAX_VALUE;
                    int bestIdx=-1;
                    for(int pi=0; pi<potentialMatches.size();pi++){
                        int score = compareNodes(aChild,potentialMatches.get(pi));

                        if(score<bestScore && score < threshold){
                            //System.out.println(" current best "+score+" = "+simpleString(potentialMatches.get(pi)));
                            bestIdx = pi;
                            bestScore = score;
                        }
                    }
                    if(bestIdx == -1 ) {//no match found
                        diffs.add(new Diff(Diff.Operation.DELETE,childPath, xmlString(aChild,false)/*simpleString(aChild)*/,""));
                    } else {
                        if(bestScore!=0) {
                            //System.out.println(" >> " + bestScore + " = " + simpleString(potentialMatches.get(bestIdx)));
                        }
                        //Map<String,Diff> newDiffs = new LinkedHashMap<String, Diff>();
                        diffNodes(aChild, potentialMatches.get(bestIdx), aPath, diffs);
                        potentialMatches.remove(bestIdx);
                        //printMap(newDiffs);
                    }
                }
            }

            //go through all the remainder in namedNodes and Add
            for(String nodeName : namedNodes.keySet()){
                List<Node> toAdd = namedNodes.get(nodeName);
                for(Node n : toAdd){
                    //System.out.println("ADD "+aPath+"/"+nodeName+getKeyAttributes(n)+" -> "+xmlString(n,false));
                    diffs.add(new Diff(Diff.Operation.ADD,aPath+"/"+nodeName+getKeyAttributes(n),"",xmlString(n, false)));
                }
            }

        } else { // they don't match, drop left and add right
            diffs.add(new Diff(Diff.Operation.DELETE,aPath, simpleString(a), ""));
            diffs.add(new Diff(Diff.Operation.ADD,aParentPath+"/"+b.getNodeName()+getKeyAttributes(b), "", simpleString(b)));
            return;
        }
    }

    public void printList(List<Diff> diffs){
        for(Diff d : diffs){

            System.out.println(ANSI_BLUE+d.getLocation()+ANSI_RESET);
            System.out.printf("%-14s\n",d.getOperation().toString());
            System.out.printf("  %-16s  %s\n", leftShort,d.getLeft());
            System.out.printf("  %-16s  %s\n", rightShort,d.getRight());
        }
    }
    private String xmlString(Node n,boolean indent){
        try {
            DOMSource domSource = new DOMSource(n);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer xformer = tf.newTransformer();
            if(indent){
                xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            }else{
                xformer.setOutputProperty(OutputKeys.INDENT, "no");
            }
            xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            xformer.setOutputProperty(OutputKeys.METHOD, "xml");

            xformer.transform(domSource,result);

            return writer.toString();

        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return "";
    }
    private String simpleString(Node n){
        StringBuilder sb = new StringBuilder("<"+n.getNodeName());
        if(n.hasAttributes()){
            sb.append(" ");
            NamedNodeMap nnm = n.getAttributes();
            for(int i=0; i<nnm.getLength(); i++){
                Node attr = nnm.item(i);
                sb.append(attr.getNodeName());
                sb.append("=");
                sb.append(attr.getNodeValue());
                if(i<nnm.getLength()-1){
                    sb.append(" ");
                }
            }
        }
        sb.append(">");
        if(n.getNodeValue()!=null && !n.getNodeValue().trim().isEmpty()){
            sb.append(n.getNodeValue().trim());
            sb.append("</");
            sb.append(n.getNodeName());
            sb.append(">");
        }
        return sb.toString();
    }
    private int compareNodes(Node a,Node b){
        int rtrn = 0;
        if(!a.getNodeName().equals( b.getNodeName() )){
            return Integer.MAX_VALUE;
            //rtrn+=StringUtil.editDistance(left.getNodeName(),right.getNodeName());
        }
        for(String keyAttribute : keyAttributes.keySet()){
            int tolerance = keyAttributes.get(keyAttribute);
            Node aAttr;
            if( a.hasAttributes() && b.hasAttributes() && (aAttr=a.getAttributes().getNamedItem(keyAttribute)) != null ) {
                Node bAttr = b.getAttributes().getNamedItem(keyAttribute);
                if(bAttr != null && StringUtil.editDistance(aAttr.getNodeValue(),bAttr.getNodeValue()) > tolerance ){
                    return Integer.MAX_VALUE;
                }
            }
        }
        for(String keyAttribute : versionedAttributes.keySet()){
            int tolerance = versionedAttributes.get(keyAttribute);
            Node aAttr;
            if( a.hasAttributes() && b.hasAttributes() && (aAttr=a.getAttributes().getNamedItem(keyAttribute)) != null ) {
                Node bAttr = b.getAttributes().getNamedItem(keyAttribute);
                if(bAttr == null) {
                    return Integer.MAX_VALUE;
                }
                String aValue = aAttr.getNodeValue();
                String bValue = bAttr.getNodeValue();

                //System.out.println("versionedKeys "+keyAttribute+" "+aValue+" : "+bValue);
                aValue = aValue.substring(0, aValue.length()-tolerance);
                bValue = bValue.substring(0, bValue.length()-tolerance);
                //System.out.println("   "+aValue+" : "+bValue);
                if(!aValue.equals(bValue)){
                    return Integer.MAX_VALUE;
                }
            }
        }
        rtrn+=attributeDistance(a,b);

        //todo deep search for node comparison

        return rtrn;
    }
    private Set<String> getAttributeNames(Node node){
        Set<String> rtrn = new LinkedHashSet<String>();
        NamedNodeMap nnm = node.getAttributes();
        for(int i=0; i<nnm.getLength(); i++){
            rtrn.add(nnm.item(i).getNodeName());
        }
        return rtrn;
    }
    private Map<String,String> getAttributes(Node node){

        Map<String,String> rtrn = new LinkedHashMap<String, String>();
        NamedNodeMap nnm = node.getAttributes();
        if(!node.hasAttributes()){
            return rtrn;
        }
        for(int i=0; i<nnm.getLength(); i++){
            rtrn.put(nnm.item(i).getNodeName(), nnm.item(i).getNodeValue());
        }
        return rtrn;
    }
    private int attributeDistance(Node a, Node b){
        int rtrn=0;
        Map<String,String> aMap = getAttributes(a);
        Map<String,String> bMap = getAttributes(b);

        Set<String> names = new HashSet<String>();
        names.addAll(aMap.keySet());
        names.addAll(bMap.keySet());
        for(String name : names){
            if(aMap.containsKey(name)){
               if(bMap.containsKey(name)){
                   rtrn+=StringUtil.editDistance(aMap.get(name),bMap.get(name));
               } else {
                   rtrn+=aMap.get(name).length()+name.length();
               }
            } else {
                rtrn+=bMap.get(name).length()+name.length();
            }
        }
        return rtrn;
    }

    public static void main(String[] args) {

        XmlDiff diff = new XmlDiff();
        diff.addKeyAttribute("name");
        diff.addVersionAttribute("xmlns", 3);
        diff.addKeyAttribute("module");
        diff.addKeyAttribute("category");
        //diff.loadLeft( "CR4", new File("/home/wreicher/specWork/wildfly-10.0.0.CR4_specjms.standalone-full-ha-netty-nio-aio.xml").toPath());
        //diff.loadRight("FNL", new File("/home/wreicher/specWork/wildfly-10.0.0.Final.standalone-specjms.xml").toPath());
        diff.loadLeft  ("Svr",new File("/home/wreicher/runtime/wildfly-10.0.0.Final-server-domain/domain/configuration/domain.xml").toPath());
        diff.loadRight ("Fnl",new File("/home/wreicher/runtime/wildfly-10.0.0.Final/domain/configuration/domain.xml").toPath());

        List<Diff> diffs = diff.getDiff();
        System.out.println("diffs:");
        diff.printList(diffs);

    }
}
