package io.github.dreamlike;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import static io.netty.handler.codec.http.HttpHeaderNames.*;
import static io.netty.handler.codec.http.HttpHeaderValues.BINARY;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

public class HttpServer {
    public static final int BODY_SIZE;
    public static final ByteBuf BIG_BUF;

    static {
        BODY_SIZE = Integer.getInteger("body.size.kb", 64) * 1024;
        BIG_BUF = Unpooled.unreleasableBuffer(Unpooled.directBuffer(BODY_SIZE));
        BIG_BUF.writerIndex(BIG_BUF.capacity());
    }

    public static ChannelInitializer<Channel> childHandler() {
        HttpHelloWorldServerHandler httpHelloWorldServerHandler = new HttpHelloWorldServerHandler();
        return new ChannelInitializer<>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline()
                        .addLast(new HttpServerCodec())
                        .addLast(new HttpObjectAggregator(4 * 1024))
                        .addLast(httpHelloWorldServerHandler);
            }
        };
    }

    private static class HttpHelloWorldServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) {
            boolean keepAlive = HttpUtil.isKeepAlive(req);
            DefaultHttpResponse response = new DefaultHttpResponse(req.protocolVersion(), OK);
            response.headers()
                    .set(CONTENT_TYPE, BINARY)
                    .setInt(CONTENT_LENGTH, BODY_SIZE);

            if (keepAlive) {
                if (!req.protocolVersion().isKeepAliveDefault()) {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }
            } else {
                response.headers().set(CONNECTION, CLOSE);
            }
            ctx.write(response);
            StreamableBody streamableBody = sliceBuffer(sliceCount(req));
            for (ByteBuf byteBuf : streamableBody.byteBufs()) {
                ctx.write(new DefaultHttpContent(byteBuf));
            }
            ChannelFuture f = ctx.writeAndFlush(new DefaultLastHttpContent(streamableBody.lastBuf()));
            if (!keepAlive) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

        private StreamableBody sliceBuffer(int count) {
            ByteBuf[] byteBufs = new ByteBuf[count - 1];
            int bufferSize = BIG_BUF.capacity() / count;
            for (int i = 0; i < byteBufs.length; i++) {
                byteBufs[i] = BIG_BUF.retainedSlice(i * bufferSize, bufferSize);
            }
            ByteBuf last = BIG_BUF.retainedSlice(bufferSize * (count - 1), BIG_BUF.capacity() - bufferSize * (count - 1));
            return new StreamableBody(byteBufs, last);
        }

        private record StreamableBody(ByteBuf[] byteBufs, ByteBuf lastBuf){}

        private int sliceCount(FullHttpRequest request) {
            // /10
            String uri = request.uri();
            return Integer.parseInt(uri.substring(uri.indexOf('/') + 1));
        }

        @Override
        public boolean isSharable() {
            return true;
        }
    }
}
