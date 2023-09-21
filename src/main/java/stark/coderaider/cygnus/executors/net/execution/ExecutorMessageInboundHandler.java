package stark.coderaider.cygnus.executors.net.execution;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import stark.coderaider.cygnus.commons.ScheduleInfo;
import stark.coderaider.cygnus.executors.invocation.InvocationInfo;
import stark.dataworks.basic.data.json.JsonSerializer;
import stark.dataworks.basic.io.net.netty.JsonMessage;
import stark.dataworks.boot.ExceptionLogger;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ExecutorMessageInboundHandler extends SimpleChannelInboundHandler<JsonMessage>
{
    private final HashMap<String, InvocationInfo> invocationMap;
    private final ThreadPoolExecutor threadPoolExecutor;

    public ExecutorMessageInboundHandler(HashMap<String, InvocationInfo> invocationMap)
    {
        this.invocationMap = invocationMap;

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
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        log.info("Connection established with " + ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, JsonMessage msg) throws Exception
    {
        threadPoolExecutor.submit(() ->
        {
            // Because there is no module (jar) for unifying the message data structure, the message body is deserialized directly as promised.
            ScheduleInfo scheduleInfo = JsonSerializer.deserialize(msg.getBody(), ScheduleInfo.class);

            // For current version, the "argument" field is ignored.
            // It will be used in the future version.

            InvocationInfo invocationInfo = invocationMap.get(scheduleInfo.getJobName());

            try
            {
                // Execute the job.
                invocationInfo.invoke();

                ctx.writeAndFlush(JsonMessage.toJsonMessage(Boolean.TRUE));
            }
            catch (InvocationTargetException | IllegalAccessException e)
            {
                ExceptionLogger.logExceptionInfo(e);

                ctx.writeAndFlush(JsonMessage.toJsonMessage(Boolean.FALSE));
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        ExceptionLogger.logExceptionInfo(cause);
        ctx.close();
    }
}
