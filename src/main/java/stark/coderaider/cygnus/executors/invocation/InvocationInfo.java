package stark.coderaider.cygnus.executors.invocation;

import lombok.Data;
import org.springframework.util.StringUtils;
import stark.dataworks.basic.datetime.CronExpressionValidator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Data
public class InvocationInfo
{
    private Object invocationTarget;
    private Method method;
    private String cron;
    private String name;

    public InvocationInfo(Object invocationTarget, Method method, String cron, String name)
    {
        this.invocationTarget = invocationTarget;
        this.method = method;
        this.cron = cron;
        this.name = name;

        validateCron();
    }

    private void validateCron()
    {
        if (StringUtils.hasText(cron) && (!CronExpressionValidator.isValid(cron)))
            throw new IllegalArgumentException(String.format("Illegal cron expression \"%s\" for job \"%s\"", cron, name));
    }

    public Object invoke() throws InvocationTargetException, IllegalAccessException
    {
        return method.invoke(invocationTarget);
    }

    public Object invoke(Object arg) throws InvocationTargetException, IllegalAccessException
    {
        return method.invoke(invocationTarget, arg);
    }
}
