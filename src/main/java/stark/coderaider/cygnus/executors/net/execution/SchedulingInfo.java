package stark.coderaider.cygnus.executors.net.execution;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The {@link SchedulingInfo} class represents the scheduling information sent from cygnus scheduler.
 * Copy / paste this file to cygnus scheduler to keep them same.
 * It is unworthy to use another tiny module to keep them same.
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class SchedulingInfo
{
    /**
     * Name of the job to be executed.
     */
    private String jobName;

    /**
     * Argument of the job to be executed.
     */
    private String argument;
}
