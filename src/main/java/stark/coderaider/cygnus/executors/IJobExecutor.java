package stark.coderaider.cygnus.executors;

public interface IJobExecutor<TArgument>
{
    void execute(TArgument argument);
}
