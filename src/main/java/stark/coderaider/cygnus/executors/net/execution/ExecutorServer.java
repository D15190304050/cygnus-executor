package stark.coderaider.cygnus.executors.net.execution;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import stark.coderaider.cygnus.executors.autoconfig.CygnusExecutorProperties;
import stark.coderaider.cygnus.executors.invocation.InvocationInfo;
import stark.dataworks.basic.io.net.netty.JsonDecoder;
import stark.dataworks.basic.io.net.netty.JsonEncoder;
import stark.dataworks.boot.ExceptionLogger;

import java.util.HashMap;

@Slf4j
public class ExecutorServer
{
    private final HashMap<String, InvocationInfo> invocationMap;
    private final EventLoopGroup parentGroup;
    private final EventLoopGroup childGroup;
    private final ServerBootstrap serverBootstrap;
    private final int port;

    public ExecutorServer(int port, HashMap<String, InvocationInfo> invocationMap)
    {
        this.port = port;
        this.invocationMap = invocationMap;

        parentGroup = new NioEventLoopGroup(1);
        childGroup = new NioEventLoopGroup();

        serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(parentGroup, childGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>()
            {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception
                {
                    ChannelPipeline pipeline = socketChannel.pipeline();

                    pipeline.addLast(new JsonDecoder());
                    pipeline.addLast(new JsonEncoder());
                }
            });
    }

    private void start()
    {
        try
        {
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();

            log.info("Cygnus executor server started...");

            channelFuture.channel().closeFuture().sync();
        }
        catch (InterruptedException e)
        {
            ExceptionLogger.logExceptionInfo(e);
        }
        finally
        {
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
        }
    }
}
