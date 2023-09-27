package stark.coderaider.cygnus.executors.autoconfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("coderaider.cygnus.executor")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CygnusExecutorProperties
{
    private String applicationId;
    private int port = 28994;

    private String zkAddress;
    private int sessionTimeoutMs;
    private int connectionTimeoutMs;
    private int baseSleepTimeMs;
    private int maxRetries;

    private int heartbeatMs = 30000;
}
