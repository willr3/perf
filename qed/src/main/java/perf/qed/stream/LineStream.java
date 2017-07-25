package perf.qed.stream;

import perf.qed.internal.Result;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by wreicher
 */
public class LineStream extends OutputStream {

    public static void printB(byte b[],int off,int len){
        String spaces = "         ";
        StringBuilder bytes = new StringBuilder();
        StringBuilder chars = new StringBuilder();
        bytes.append("[");
        chars.append("[");
        if(b!=null && b.length>0){
            int lim = off+len;
            for(int i=off; i<lim; i++){
                int v = b[i];
                String append = v+"";
                bytes.append(append);
                bytes.append(" ");
                if(v == 10){
                    chars.append(spaces.substring(0,append.length()-2));
                    chars.append("\\n");
                }else if (v == 13){
                    chars.append(spaces.substring(0,append.length()-2));
                    chars.append("\\r");
                }else {
                    chars.append(spaces.substring(0, append.length() - 1));
                    chars.append((char) v);
                }
                chars.append(" ");
            }
            bytes.append("]");
            chars.append("]");
        }
        System.out.println("bytes="+bytes.toString());
        System.out.println("chars="+chars.toString());
    }

    private static final byte LINE_SEPARATOR[] = System.lineSeparator().getBytes();

    private Result result;

    int index = 0;
    byte buffered[] = new byte[4*1024];

    public LineStream() {
    }

    public void setResult(Result result){
        this.result = result;
    }

    public void reset(){
        index = 0;
    }
    @Override
    public void write(int b) throws IOException {
        System.out.println(":::::::::::::::::::write("+b+")::::::::::::::::::::::");
    }
    @Override
    public void write(byte b[]) throws IOException {
        write(b,0,b.length);
    }
    @Override
    public void write(byte b[], int off, int len) {
        int lineBreak = -1;
        int next=-1;
        int lim = off+len;


        //printB(b,off,len);
        for(int i = off; i<lim; i++){
            if( b[i] == 10 || b[i]==13 ){ // if CR or LR
                lineBreak=i;

                if(index==0){


                    emit(b,off,lineBreak-off);//because we don't want the char @ lineBreak
                }else{
                    if(index+lineBreak-off >= buffered.length){
                        byte newBuf[] = new byte[buffered.length*2];
                        System.arraycopy(buffered,0,newBuf,0,index);
                        buffered = newBuf;
                    }

                    System.arraycopy(b,off,buffered,index,(lineBreak-off));
                    index+=(lineBreak-off);

                    emit(buffered,0,index);
                }
                if(b[i+1]==10 || b[i+1]==13){//skip the next CR or LR

                    lineBreak++;
                    i++;
                }
                len-=lineBreak-off+1;//because we don't want the char @ lineBreak
                off=lineBreak+1;//because we don't want the char @ lineBreak
                //printB(b,off,len);
            }
        }
        if(len>0){

            if(index+len>buffered.length){
                byte newBuffered[] = new byte[buffered.length*2];
                System.arraycopy(buffered,0,newBuffered,0,index);
                buffered = newBuffered;
            }

            System.arraycopy(b,off,buffered,index,len);
            index+=len;
        }
    }
    private void emit(byte content[], int start,int length){
        if(length==0) {
            result.update("");
            return;
        }

        //printB(content,start,length);

        int lim = start+length;

        String toEmit = new String(content,start,length).replaceAll("\u001B\\[[;\\d]*m", "");
        if(result!=null){
            result.update(toEmit);
        }
        index=0;
    }

    public int find(byte[] array,byte[] content,int start,int length){
        if(array == null || content == null || array.length < content.length){
            return -1;
        }
        if(start >=array.length || length == 0 || length+start > array.length){
            return -1;
        }
        int matched = 0;
        for(int a=start; a<=start+length-content.length; a++){
            matched = 0;
            for(int c=0; c<content.length; c++){
                if( array[a+c] != content[c] ){
                    break;
                } else {
                    matched ++;
                }

            }
            if( matched == content.length){
                return a;
            }

        }
        return -1;
    }
}
