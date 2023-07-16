package stark.coderaider.cygnus.executors.autoconfig;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("dataworks.cygnus.executor")
@NoArgsConstructor
@AllArgsConstructor
@Data
public class CygnusExecutorProperties
{
    private String appName;
    private String groupName;
    private int port = 28994;
    private String registryCenterAddress;
}
