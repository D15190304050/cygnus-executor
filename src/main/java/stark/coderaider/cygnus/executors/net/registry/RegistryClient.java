package stark.coderaider.cygnus.executors.net.registry;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import stark.dataworks.basic.io.net.netty.JsonDecoder;
import stark.dataworks.basic.io.net.netty.JsonEncoder;

public class RegistryClient
{
    public RegistryClient()
    {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(new ChannelInitializer<SocketChannel>()
            {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception
                {
                    ChannelPipeline pipeline = socketChannel.pipeline();

                    pipeline.addLast(new JsonEncoder());
                    pipeline.addLast(new JsonDecoder());
                    pipeline.addLast(null);
                }
            });


    }
}
