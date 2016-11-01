package perf.diff.internal;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.Stack;

/**
 * Created by wreicher on 8/14/15.
 */
public class SaxHandler extends DefaultHandler {

    private Stack<XmlTag> tags;
    private XmlTag root;

    public SaxHandler(){
        tags = new Stack<XmlTag>();
        root = null;
    }
    public XmlTag getRoot(){
        return root;
    }

    @Override public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        System.out.println("uri="+uri+" local="+localName+" q="+qName);
        XmlTag newTag = new XmlTag(qName);
        if(root == null){
            root = newTag;
        }
        for(int i=0; i<attributes.getLength();i++){
            newTag.addAttribute(attributes.getQName(i),attributes.getValue(i));
        }
        if(!tags.empty()){
            tags.peek().addChild(newTag);
        }
        tags.push(newTag);

    }
    @Override public void endElement(String uri, String localName,String qName) throws SAXException {
        tags.pop();
    }
    @Override public void characters(char[] ch, int start, int length) throws SAXException {
        tags.peek().addValue(new String(ch,start,length));
    }

    public static void main(String[] args) {
        String toRead = "/home/wreicher/specWork/specjms/qa/standalone-full-ha-nio-nio.xml";

        SAXParserFactory parserFactor = SAXParserFactory.newInstance();
        try {
            SAXParser parser = parserFactor.newSAXParser();
            SaxHandler handler = new SaxHandler();
            parser.parse(new File(toRead),handler);
            System.out.println(handler.getRoot().pint(0));
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
