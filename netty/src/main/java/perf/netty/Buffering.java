package perf.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.sctp.nio.NioSctpChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.LinkedList;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wreicher
 */
public class Buffering {

    public static void main(String[] args) {
//        int port = 2311;
//        EventLoopGroup eventLoop = new NioEventLoopGroup();
//
//        new ServerBootstrap().channel(NioServerSocketChannel.class).group(eventLoop).childHandler(new Init()).bind(port);
//        new Bootstrap().channel(NioSocketChannel.class).group(eventLoop).handler(new Init()).connect("localhost", port);


        PooledByteBufAllocator alloc = new PooledByteBufAllocator();

        ByteBuf buffer = alloc.buffer(1024);

        System.out.println(buffer.refCnt());

        ByteBuf dup = buffer.duplicate();

        System.out.println(buffer.refCnt());

    }

    private static class Init extends ChannelInboundHandlerAdapter {

        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            System.out.println("Writing to " + ctx.channel());
            final ByteBuf buf = ctx.alloc().buffer();
            buf.writeInt(13123);
            System.out.println("channelActive buf.refCnt="+buf.refCnt());
            ctx.channel().writeAndFlush(buf).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    System.out.println("operation Complete buf.refCnt="+buf.refCnt());
                    if (buf.refCnt() != 0) {
                        System.err.println("ctx: " + ctx.channel() + " buf: " + buf + ": FAILED");
                    }
                }
            });
        }

        public void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
            // Discard
        }
    }
}
