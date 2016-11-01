package perf.diff.internal;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class XmlTag {

    private String name;
    private XmlTag parent;
    private Map<String,String> attributes;
    private List<XmlTag> children;
    private Map<String,List<XmlTag>> namedChildren;
    private String value = "";
    private int parentIndex = -1;
    private int parentNamedIndex = -1;
    public XmlTag(String name){
        this.name = name;
        attributes = new LinkedHashMap<String, String>();
        namedChildren = new HashMap<String, List<XmlTag>>();
        children = new ArrayList<XmlTag>();

    }
    public XmlTag(Node node){
        //this.node = node;
        name = node.getNodeName();
        attributes = new LinkedHashMap<String, String>();
        namedChildren = new HashMap<String, List<XmlTag>>();
        children = new ArrayList<XmlTag>();

        //set value if have one
        if(node.getNodeValue()!=null && node.getNodeValue().trim().isEmpty()){
            value = node.getNodeValue();
        }

        Node parent = node.getParentNode();
        //find parent index
        if(parent!=null){
            NodeList children = parent.getChildNodes();
            for(int i=0; i<children.getLength(); i++){
                Node child = children.item(i);
                if(child == node){
                    parentIndex = i;
                    break;
                }
            }
        }
        //populate attributes
        if(node.hasAttributes()){
            NamedNodeMap attrs = node.getAttributes();
            for(int i=0; i<attrs.getLength(); i++){
                Node a = attrs.item(i);
                attributes.put(a.getNodeName(),a.getNodeValue());
            }
        }

        //add children
        if(node.hasChildNodes()){
            NodeList children = node.getChildNodes();
            for(int i=0; i<children.getLength(); i++){
                Node n = children.item(i);
                switch(n.getNodeType()){
                    case Node.TEXT_NODE:
                        if(n.getNodeValue()==null || n.getNodeValue().trim().isEmpty()){
                            //
                        }else{
                            addChild(new XmlTag(n));
                        }
                        break;
                    case Node.COMMENT_NODE:
                        break;
                    default:
                        addChild(new XmlTag(n));
                }
            }
        }

    }

    public XmlTag getParent(){ return parent; }
    public boolean hasParent(){ return parent!=null; }
    protected void setParent(XmlTag tag){ parent = tag; }
    public String getName(){return name;}
    public boolean hasValue(){return !value.isEmpty();}
    public String getValue(){return value;}
    protected void addValue(String v){
        value = value+v;
    }
    public int getParentIndex(){return parentIndex;}
    protected void setParentIndex(int index){ parentIndex = index;}
    public int getParentNamedIndex(){return parentNamedIndex;}
    protected void setParentNamedIndex(int index){ parentNamedIndex = index;}
    public Map<String,String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }
    public String getAttribute(String name){
        return attributes.get(name);
    }
    public boolean hasAttribute(String name){ return getAttribute(name) != null;}
    protected void addAttribute(String name,String value){attributes.put(name,value);}
    public void addChild(XmlTag tag){
        tag.setParent(this);
        tag.setParentIndex(getChildCount());
        children.add(tag);
        String name = tag.getName();
        if(!namedChildren.containsKey(name)){
            namedChildren.put(name,new ArrayList<XmlTag>());
        }
        tag.setParentNamedIndex(getNamedChildCount(name));
        namedChildren.get(name).add(tag);
    }
    public List<XmlTag> getChildren(){
        return Collections.unmodifiableList(children);
    }
    public boolean hasChildren(){return !children.isEmpty();}
    public List<XmlTag> getChilren(String name){
        if(namedChildren.containsKey(name))
            return namedChildren.get(name);
        return Collections.EMPTY_LIST;
    }
    public XmlTag getChild(int childIndex){
        return children.get(childIndex);
    }
    public int getChildCount(){return children.size();}
    public int getNamedChildCount(String name){
        if(namedChildren.containsKey(name)){
            return namedChildren.get(name).size();
        }
        return 0;
    }
    public void appendString(StringBuilder sb){
        CharSequence cs = null;
    }

    public String pint(int spaces){
        StringBuilder sb = new StringBuilder();
        StringBuilder sp = new StringBuilder();
        for(int i=0; i<spaces;i++){
            sp.append(" ");
        }
        sb.append(sp.toString());
        sb.append("<");
        sb.append(getParentIndex());
        sb.append(".");
        sb.append(getParentNamedIndex());
        sb.append(".");
        sb.append(getName());
        Map<String,String> attr = getAttributes();
        for(String aName : attr.keySet()){
            sb.append(" ");
            sb.append(aName);
            sb.append("=\"");
            sb.append(attr.get(aName));
            sb.append("\"");
        }
        if(hasChildren()){
            sb.append(" >");
            for(XmlTag child : getChildren()){
                sb.append("\n");
                sb.append(child.pint(spaces + 2 ));
            }
            sb.append("\n");
            sb.append(sp.toString());
            sb.append("</");
            sb.append(getName());
            sb.append(">");
        } else {
            sb.append(" />");
        }
        return sb.toString();
    }
}
