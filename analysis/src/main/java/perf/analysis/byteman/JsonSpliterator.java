package perf.analysis.byteman;

import org.json.JSONObject;
import perf.parse.Parser;
import perf.util.file.FileUtility;
import perf.util.json.Jsons;

import java.io.*;
import java.util.Spliterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Created by wreicher
 */
public class JsonSpliterator implements Spliterator<Jsons>{


    private class CountingInputStream extends BufferedInputStream{


        private AtomicInteger count = new AtomicInteger(0);
        public CountingInputStream(InputStream in,int size) {
            super(in,size);
        }
        public CountingInputStream(InputStream in) {
            super(in);
        }


        @Override
        public int read() throws IOException {
            System.out.println("read()");
            count.incrementAndGet();
            return super.read();
        }
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            System.out.println("read(b,"+off+","+len+")");
            int rtrn = super.read(b,off,len);
            count.addAndGet(off);
            count.addAndGet(rtrn);
            return rtrn;
        }
    }


    private CountingInputStream is;
    private BufferedReader reader;
    private Parser parser;
    private long totalBytes = 0;
    private int bytesRead =0;

    public JsonSpliterator(String filePath,Parser parser){
        this.is = new CountingInputStream(FileUtility.getInputStream(filePath));
        this.reader = new BufferedReader(new InputStreamReader(is));
        this.parser = parser;
        this.totalBytes = FileUtility.getInputSize(filePath);
        System.out.println(filePath+" -> "+totalBytes);

    }

    @Override
    public boolean tryAdvance(Consumer<? super Jsons> consumer) {
        String line = null;
        JSONObject toEmit = null;

        try{
            do {
                line=reader.readLine();
                bytesRead +=line.getBytes().length;
                System.out.println("bytesRead = "+ bytesRead);
                if(line!=null){
                    toEmit = parser.onLine(line);
                }
            }while(line!=null && toEmit==null && bytesRead < totalBytes);

            if(toEmit!=null){
                System.out.println("is.count="+is.count.get());
                consumer.accept(new Jsons(toEmit));
                return true;
            }else{
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public Spliterator<Jsons> trySplit() {
        long newStart = bytesRead + (totalBytes-bytesRead)/2;


        return null;
    }

    @Override
    public long estimateSize() {
        try {
            return is.available();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public int characteristics() {
        return IMMUTABLE;
    }
}
