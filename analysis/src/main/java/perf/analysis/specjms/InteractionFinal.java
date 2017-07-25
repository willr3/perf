package perf.analysis.specjms;

import perf.util.file.FileUtility;
import perf.util.xml.Xml;
import perf.util.xml.XmlLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Created by wreicher
 */
public class InteractionFinal {


    public static final InteractionFinal EMPTY = new InteractionFinal();

    long sentMessages;
    long receivedMessages;
    long messageCount;
    double maxDeliveryTime;
    double avgDeliveryTime;
    double distribution[];

    private InteractionFinal(){
        sentMessages = 0;
        receivedMessages = 0;
        messageCount = 0;
        maxDeliveryTime = 0.0;
        avgDeliveryTime = 0.0;
        distribution = new double[100];

    }

    public boolean isEmpty(){return EMPTY.equals(this);}
    public InteractionFinal(Xml xml){
        messageStats(xml);
        distribution = messagePercentileDistribution(xml);
    }
    long getSentMessages(){return sentMessages;}
    long getReceivedMessages(){return receivedMessages;}
    long getMessageCount(){return messageCount;}
    double getMaxDeliveryTime(){return maxDeliveryTime;}
    double getAvgDeliveryTime(){return avgDeliveryTime;}
    double[] getDistribution(){return distribution;}

    private void messageStats(Xml xml){
        AtomicInteger interactionCount = new AtomicInteger();
        LongAdder sentMessagesAdder = new LongAdder();
        LongAdder receivedMessagesAdder = new LongAdder();
        LongAdder countMessagesAdder = new LongAdder();
        DoubleAdder maxDeliveryTimeAdder = new DoubleAdder();
        DoubleAdder avgDeliveryTimeAdder = new DoubleAdder();
        AtomicLong distributionCount = new AtomicLong(0);
        xml.getAll("./interaction").forEach(interaction -> {
            interactionCount.incrementAndGet();
            Long received = Long.parseLong(interaction.get("@messagesReceived").toString());
            Long sent = Long.parseLong(interaction.get("@messagesSent").toString());
            sentMessagesAdder.add(sent);
            receivedMessagesAdder.add(received);

            LongAdder countSum = new LongAdder();

            interaction.getAll("//messagesIn").forEach(messagesIn ->{
                distributionCount.incrementAndGet();
                Long count = Long.parseLong(messagesIn.get("@count").toString());

                countSum.add(count);
                countMessagesAdder.add(count);

                Double avg = Double.parseDouble(messagesIn.get("deliveryTime/@avg").toString());
                Double max = Double.parseDouble(messagesIn.get("deliveryTime/@max").toString());

                if(max > maxDeliveryTimeAdder.doubleValue()){
                    maxDeliveryTimeAdder.reset();
                    maxDeliveryTimeAdder.add(max);
                }

                avgDeliveryTimeAdder.add(avg*count);

            });

        });
        this.sentMessages = sentMessagesAdder.longValue();
        this.receivedMessages = receivedMessagesAdder.longValue();
        this.messageCount = countMessagesAdder.longValue();
        this.avgDeliveryTime = (avgDeliveryTimeAdder.doubleValue()/countMessagesAdder.longValue());
        this.maxDeliveryTime = maxDeliveryTimeAdder.doubleValue();
    }
    private double[] messagePercentileDistribution(Xml xml){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        AtomicLong totalReceived = new AtomicLong(0);
        AtomicLong totalSent = new AtomicLong(0);
        AtomicLong distributionCount = new AtomicLong(0);
        double distribution[] = new double[100];

        xml.getAll("./interaction").forEach(interaction ->{
            Long received = Long.parseLong(interaction.get("@messagesReceived").toString());
            Long sent = Long.parseLong(interaction.get("@messagesSent").toString());
            interaction.getAll("./step/messagesIn").forEach(messagesIn->{
                Long count = Long.parseLong(messagesIn.get("@count").toString());

                distributionCount.incrementAndGet();
                totalReceived.addAndGet(count);

                String deliveryTime = messagesIn.get("deliveryTime/text()").toString();
                String deliverSplit[] = deliveryTime.split(", ");

                for(int i=0; i<100; i++){
                    distribution[i]+=Double.parseDouble(deliverSplit[i])/count;
                }
            });
        });
        for(int i=0; i<100; i++){
            distribution[i]=distribution[i]*100/distributionCount.get();
        }
        return distribution;
    }

    public static void main(String[] args) {

        String fileName = "interaction.final.xml";
        XmlLoader xmlLoader = new XmlLoader();


        //Horizontal

        Xml amq6     = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/00236/"+fileName).toPath());
        Xml amq6Huge = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/00237/"+fileName).toPath());
        Xml amq7     = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/00239/"+fileName).toPath());
        Xml amq7Huge = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/00240/"+fileName).toPath());

        Xml amq610   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00247/"+fileName).toPath());
        Xml amq615   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00248/"+fileName).toPath());
        Xml amq620   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00250/"+fileName).toPath());
        Xml amq630   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00251/"+fileName).toPath());
        Xml amq640   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00252/"+fileName).toPath());
        Xml amq650   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00253/"+fileName).toPath());
        Xml amq675   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00255/"+fileName).toPath());

        Xml eap730   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00259/client1/"+fileName).toPath());
        Xml eap7502   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00262/client1/"+fileName).toPath());
        Xml eap7402   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00263/client1/"+fileName).toPath());
        Xml eap740   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00265/client1/"+fileName).toPath());
        //Xml eap750   = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00266/client1/"+fileName).toPath());

        Xml eap7_block_10 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-10-1499257325414/benchclient1/"+fileName).toPath());
        Xml eap7_block_20 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-20-1499262841904/benchclient1/"+fileName).toPath());
        Xml eap7_block_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499110490412/benchclient1/"+fileName).toPath());
        Xml eap7_block_40 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-40-1499113953226/benchclient1/"+fileName).toPath());
        Xml eap7_block_50 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-50-1499133802075/benchclient1/"+fileName).toPath());
        Xml eap7_block_75 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-75-1499224646443/benchclient1/"+fileName).toPath());


        Xml eap7_patch_block_10 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-10-1499278384835/benchclient1/"+fileName).toPath());
        Xml eap7_patch_block_20 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-20-1499276095803/benchclient1/"+fileName).toPath());
        Xml eap7_patch_block_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499279888285/benchclient1/"+fileName).toPath());
        Xml eap7_patch_block_40 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-40-1499281380586/benchclient1/"+fileName).toPath());
        Xml eap7_patch_block_50 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-50-1499282878629/benchclient1/"+fileName).toPath());
        Xml eap7_patch_block_75 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-75-1499284381275/benchclient1/"+fileName).toPath());

        Xml eap7_patch_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499290002275/benchclient1/"+fileName).toPath());
        Xml eap7_patch_nada_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499389430117/benchclient1/"+fileName).toPath());

        Xml eap7_patch_page_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499305715630/benchclient1/"+fileName).toPath());

        Xml eap7_teset2_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499391175757/benchclient1/"+fileName).toPath());

        Xml eap7_teset3_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499404924104/benchclient1/"+fileName).toPath());
        Xml eap7_patch_teset3_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499406726806/benchclient1/"+fileName).toPath());
        Xml eap7_patch_test4_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499431942584/benchclient1/"+fileName).toPath());
        Xml eap7_patch_test5_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499434095520/benchclient1/"+fileName).toPath());
        Xml eap7_patch_test6_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499442429258/benchclient1/"+fileName).toPath());
        Xml eap7_test6_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/run-30-1499453397545/benchclient1/"+fileName).toPath());
        //Vertical
        //time synced
        Xml eap7_vertical_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00267/client1/"+fileName).toPath());
        Xml eap7_vertical_40 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00268/client1/"+fileName).toPath());
        Xml eap7_vertical_50 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00269/client1/"+fileName).toPath());
        Xml eap7_vertical_60 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00270/client1/"+fileName).toPath());
        Xml eap7_vertical_75 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00271/client1/"+fileName).toPath());
        Xml eap7_vertical_100 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00272/client1/"+fileName).toPath());
        Xml eap7_vertical_125 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00273/client1/"+fileName).toPath());
        Xml eap7_vertical_125_2EH = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00274/client1/"+fileName).toPath());
        Xml eap7_vertical_125_4EH = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00275/client1/"+fileName).toPath());


        //Xml amq6_vertical_100 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00276/client1/"+fileName).toPath());
        Xml amq6_vertical_75 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00278/client1/"+fileName).toPath());
        Xml amq6_vertical_60 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00279/client1/"+fileName).toPath());
        Xml amq6_vertical_50 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00280/client1/"+fileName).toPath());
        Xml amq6_vertical_40 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00281/client1/"+fileName).toPath());
        Xml amq6_vertical_30 = xmlLoader.loadXml(new File("/home/wreicher/perfWork/amq/jdbc/00282/client1/"+fileName).toPath());
    }

}
