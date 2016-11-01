package perf.qed.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

/**
 *
 */
public class MonitoredStream extends OutputStream {



    private Map<String,OutputStream> streams;
    private Semaphore lock;

    private byte prompt[];

    public MonitoredStream(Semaphore lock, byte bytes[]){
        this.streams = new HashMap<String,OutputStream>();
        this.lock = lock;
        this.prompt = bytes;
    }

    @Override
    public void close() throws IOException {
        super.close();
        for(OutputStream s : streams.values()){
            s.close();
        }
    }

    public void addStream(String key,OutputStream stream){
        streams.put(key,stream);
    }
    public void removeStream(String key){
        if(hasStream(key)){
            streams.remove(key);
        }
    }
    public boolean hasStream(String key){
        return streams.containsKey(key);
    }

    public boolean hasSuffix(byte sequence[],byte suffix[],int offset,int length){
        if(suffix == null) {
            return true;
        }
        if(sequence == null || length < suffix.length || sequence.length-offset < length){
            return false;
        }
        int diff = offset+length-suffix.length;
        byte seqSuffix[] = Arrays.copyOfRange(sequence,diff,diff+suffix.length);
        boolean rtrn =Arrays.equals(seqSuffix,suffix);
        return rtrn;
    }
    @Override
    public void write(int b) throws IOException {
        for(OutputStream s : streams.values()){
            //s.write(b);
        }
    }
    @Override
    public void write(byte b[]) throws IOException {

        write(b,0,b.length);
    }
    @Override
    public void write(byte b[], int off, int len) throws IOException {
        for(OutputStream s : streams.values()){
            try {
                s.write(b, off, len);
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        if(hasSuffix(b,prompt,off,len)){
            lock.release();
        }
    }

    @Override
    public void flush() throws IOException {
        for(OutputStream s : streams.values()){
            s.flush();
        }
    }
}
