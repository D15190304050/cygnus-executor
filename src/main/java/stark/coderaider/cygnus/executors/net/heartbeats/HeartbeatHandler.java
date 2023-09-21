package stark.coderaider.cygnus.executors.net.heartbeats;

import org.apache.dubbo.common.utils.NetUtils;
import org.apache.zookeeper.CreateMode;
import stark.coderaider.cygnus.commons.ZkConstants;
import stark.coderaider.cygnus.commons.jobs.CronSimpleJobBase;
import stark.coderaider.cygnus.executors.autoconfig.CygnusExecutorProperties;
import stark.coderaider.cygnus.executors.invocation.InvocationInfo;
import stark.dataworks.basic.data.json.JsonSerializer;
import stark.dataworks.basic.zk.ZkQuickOperation;

import java.util.*;

public class HeartbeatHandler
{
    private final int heartbeatMs;
    private final ZkQuickOperation zkQuickOperation;
    private final CygnusExecutorProperties cygnusExecutorProperties;

    private final List<String> heartbeatPaths;

    private final String localIpAddress;

    public HeartbeatHandler(ZkQuickOperation zkQuickOperation, CygnusExecutorProperties cygnusExecutorProperties)
    {
        this.heartbeatMs = cygnusExecutorProperties.getHeartbeatMs();
        this.zkQuickOperation = zkQuickOperation;
        this.cygnusExecutorProperties = cygnusExecutorProperties;

        this.heartbeatPaths = new ArrayList<>();
        this.localIpAddress = NetUtils.getLocalHost();
    }

    public void startHeartbeats(HashMap<String, InvocationInfo> invocationMap) throws Exception
    {
        // 1. Convert invocationMap to ZK node info => (/cygnus/[applicationName]/[jobName]/[IP:port, refreshTime])
        // 2. Start a new thread to send heartbeats periodically.

        for (String jobName : invocationMap.keySet())
        {
            String applicationId = cygnusExecutorProperties.getApplicationId();
            String zkPathOfJob = getZkPathOfJob(applicationId, jobName);

            InvocationInfo invocationInfo = invocationMap.get(jobName);
            String cron = invocationInfo.getCron();
            CronSimpleJobBase cronSimpleJobBase = new CronSimpleJobBase();
            cronSimpleJobBase.setCron(cron);
            cronSimpleJobBase.setEnabled(true);
            cronSimpleJobBase.setJobName(jobName);

            // jobType:jobInfo
            String jobData = cronSimpleJobBase.getJobTimerType().toString() + ZkConstants.DELIMITER + JsonSerializer.serialize(cronSimpleJobBase);
            zkQuickOperation.tryCreateNode(zkPathOfJob, jobData, CreateMode.EPHEMERAL);

            String zkPathOfExecutorInstance = getZkPathOfExecutorInstance(applicationId);
            heartbeatPaths.add(zkPathOfExecutorInstance);
        }

        startHeartbeatLoop();
    }

    private void startHeartbeatLoop()
    {
        TimerTask task = new TimerTask()
        {
            @Override
            public void run()
            {
                try
                {
                    sendHeartbeat();
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };

        Timer timer = new Timer();
        timer.scheduleAtFixedRate(task, 0, heartbeatMs);
    }

    private void sendHeartbeat() throws Exception
    {
        // 1. Check if there is a node corresponding to this executor.
        // 1.1 If there is, refresh its state & refresh time. (Should not happen)
        // 1.2 If there is not, add a new node with state & refresh time.

        Date refreshTime = new Date();
        String refreshTimestamp = String.valueOf(refreshTime.getTime());

        for (String heartbeatPath : heartbeatPaths)
        {
            if (zkQuickOperation.nodeExists(heartbeatPath))
                zkQuickOperation.setNodeData(heartbeatPath, refreshTimestamp);
            else
                zkQuickOperation.tryCreateNode(heartbeatPath, refreshTimestamp, CreateMode.EPHEMERAL);
        }
    }

    private static String getZkPathOfJob(String applicationId, String jobName)
    {
        // /cygnus/[applicationId]/jobs/jobName
        return "/" + applicationId + "/jobs/" + jobName;
    }

    private String getZkPathOfExecutorInstance(String applicationId)
    {
        // /cygnus/[applicationId]/executors/[IP:port, refreshTime]
        return "/" + applicationId + "/executors/" + localIpAddress + ":" + cygnusExecutorProperties.getPort();
    }
}
