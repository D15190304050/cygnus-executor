package stark.coderaider.cygnus.executors.autoconfig;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import stark.coderaider.cygnus.executors.net.heartbeats.ExecutorHeartbeatService;
import stark.dataworks.basic.zk.ZkQuickOperation;

@Configuration
public class CygnusExecutorConfiguration
{
    @Autowired
    private CygnusExecutorProperties cygnusExecutorProperties;

    @Bean
    public ZkQuickOperation zkQuickOperation()
    {
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(cygnusExecutorProperties.getBaseSleepTimeMs(), cygnusExecutorProperties.getMaxRetries());
        CuratorFramework zkClient = CuratorFrameworkFactory.builder()
            .connectString(cygnusExecutorProperties.getZkAddress())
            .sessionTimeoutMs(cygnusExecutorProperties.getSessionTimeoutMs())
            .connectionTimeoutMs(cygnusExecutorProperties.getConnectionTimeoutMs())
            .retryPolicy(retryPolicy)
            .namespace("cygnus")
            .build();

        return new ZkQuickOperation(zkClient);
    }

    @Bean
    public ExecutorHeartbeatService heartbeatHandler(ZkQuickOperation zkQuickOperation)
    {
        return new ExecutorHeartbeatService(zkQuickOperation, cygnusExecutorProperties);
    }
}
