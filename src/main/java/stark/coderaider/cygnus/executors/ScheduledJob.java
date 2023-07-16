package stark.coderaider.cygnus.executors;

public @interface ScheduledJob
{
    String name();
    String cron();
}
