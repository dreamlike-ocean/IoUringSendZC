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
    public static final ByteBuf BIG_BUF;

    static {
        BIG_BUF = Unpooled.unreleasableBuffer(Unpooled.directBuffer(Integer.getInteger("body.size", 16) * 1024));
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

            // BIG_BUF is unreleasable; duplicate() is safe for reuse across requests.
            ByteBuf content = BIG_BUF.retainedSlice();

            FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), OK, content);
            response.headers()
                    .set(CONTENT_TYPE, BINARY)
                    .setInt(CONTENT_LENGTH, content.readableBytes());

            if (keepAlive) {
                if (!req.protocolVersion().isKeepAliveDefault()) {
                    response.headers().set(CONNECTION, KEEP_ALIVE);
                }
            } else {
                response.headers().set(CONNECTION, CLOSE);
            }

            ChannelFuture f = ctx.writeAndFlush(response);
            if (!keepAlive) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            cause.printStackTrace();
            ctx.close();
        }

        @Override
        public boolean isSharable() {
            return true;
        }
    }
}
