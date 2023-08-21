package stark.coderaider.cygnus.executors.net.execution;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import stark.dataworks.basic.io.net.netty.JsonMessage;

import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorMessageInboundHandler extends SimpleChannelInboundHandler<JsonMessage>
{
    private final ThreadPoolExecutor threadPoolExecutor;

    public ExecutorMessageInboundHandler()
    {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        threadPoolExecutor = new ThreadPoolExecutor(availableProcessors,
            availableProcessors,
            30,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());
    }

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
