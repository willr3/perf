package perf.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;

import java.nio.ByteBuffer;

/**
 * Created by wreicher
 */
public class Buffering {

    public static void main(String[] args) {
        int size = 1024*8;
        ByteBuf buffer = Unpooled.buffer(size);
        System.out.println(buffer.getClass());
        buffer = (new PooledByteBufAllocator()).directBuffer(size);
        System.out.println(buffer.refCnt());
        System.out.println(buffer.getClass());
        buffer.release();
        buffer.release();

    }
}
