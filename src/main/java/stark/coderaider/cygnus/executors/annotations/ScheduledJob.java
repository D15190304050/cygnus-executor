package stark.coderaider.cygnus.executors.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.TYPE})
public @interface ScheduledJob
{
    String name() default "";
    String cron() default "";
    String method();
}
