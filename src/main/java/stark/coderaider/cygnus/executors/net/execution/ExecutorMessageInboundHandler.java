package stark.coderaider.cygnus.executors.net.execution;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import stark.dataworks.basic.io.net.netty.JsonMessage;

public class ExecutorMessageInboundHandler extends SimpleChannelInboundHandler<JsonMessage>
{
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JsonMessage msg) throws Exception
    {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        cause.printStackTrace();
        ctx.close();
    }
}
