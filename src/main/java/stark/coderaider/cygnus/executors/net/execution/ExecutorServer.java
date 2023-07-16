package stark.coderaider.cygnus.executors.net.execution;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import stark.coderaider.cygnus.executors.autoconfig.CygnusExecutorProperties;
import stark.dataworks.basic.io.net.netty.JsonDecoder;
import stark.dataworks.basic.io.net.netty.JsonEncoder;
import stark.dataworks.boot.ExceptionLogger;

public class ExecutorServer
{
    public ExecutorServer(CygnusExecutorProperties executorProperties)
    {
        EventLoopGroup parentGroup = new NioEventLoopGroup(1);
        EventLoopGroup childGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
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

        try
        {
            ChannelFuture channelFuture = serverBootstrap.bind(executorProperties.getPort()).sync();
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
