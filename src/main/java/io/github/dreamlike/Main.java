package io.github.dreamlike;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.IoHandlerFactory;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.uring.*;

public class Main {
    static void main() throws InterruptedException {
        IoUringIoHandlerConfig ioUringIoHandlerConfig = new IoUringIoHandlerConfig();
        ioUringIoHandlerConfig.setBufferRingConfig(
                IoUringBufferRingConfig.builder()
                        .batchSize(16)
                        .bufferGroupId((short) 1)
                        .bufferRingSize((short) 1024)
                        .allocator(new IoUringFixedBufferRingAllocator(4 * 1024))
                        .build()
        );
        IoHandlerFactory ioHandlerFactory = IoUringIoHandler.newFactory(ioUringIoHandlerConfig);
        MultiThreadIoEventLoopGroup acceptor = new MultiThreadIoEventLoopGroup(1, ioHandlerFactory);
        MultiThreadIoEventLoopGroup worker = new MultiThreadIoEventLoopGroup(Runtime.getRuntime().availableProcessors(), ioHandlerFactory);
        ChannelFuture channelFuture = new ServerBootstrap()
                .group(acceptor, worker)
                .channel(IoUringServerSocketChannel.class)
                .childOption(IoUringChannelOption.IO_URING_WRITE_ZERO_COPY_THRESHOLD, HttpServer.BIG_BUF.capacity())
                .childOption(IoUringChannelOption.IO_URING_BUFFER_GROUP_ID, (short) 1)
                .childHandler(HttpServer.childHandler())
                .bind(80)
                .sync();
        if (!channelFuture.isSuccess()) {
            channelFuture.cause().printStackTrace();
        }
    }
}
