package stark.coderaider.cygnus.executors.autoconfig;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import stark.coderaider.cygnus.executors.annotations.ScheduledJob;
import stark.coderaider.cygnus.executors.invocation.InvocationInfo;
import stark.coderaider.cygnus.executors.net.execution.ExecutorServer;
import stark.coderaider.cygnus.executors.net.heartbeats.HeartbeatHandler;
import stark.dataworks.basic.data.json.JsonSerializer;
import stark.dataworks.boot.beans.BasePackageScanner;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ExecutorInitializer implements ApplicationContextAware, ApplicationListener<WebServerInitializedEvent>
{
    private static final String RESOURCE_PATTERN = "/**/*.class";
    public static final String CHECK_SUFFIX = ", please check your code & configuration.";

    @Autowired
    private HeartbeatHandler heartbeatHandler;

    @Autowired
    private CygnusExecutorProperties cygnusExecutorProperties;

    private ApplicationContext applicationContext;
    private final HashMap<String, InvocationInfo> invocationMap;

    public ExecutorInitializer()
    {
        invocationMap = new HashMap<>();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event)
    {
        scanJobs();
        startExecutorServer();

        try
        {
            startHeartbeats();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    // Throw the exception and stop the execution of the program if there is something wrong.
    private void scanJobs()
    {
        HashSet<String> basePackages = BasePackageScanner.getBasePackages(applicationContext);

        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

        log.info("basePackages = " + JsonSerializer.serialize(basePackages));

        try
        {
            for (String basePackage : basePackages)
            {
                String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + ClassUtils.convertClassNameToResourcePath(basePackage) + RESOURCE_PATTERN;
                Resource[] resources = resourcePatternResolver.getResources(pattern);

                log.info("resources = " + JsonSerializer.serialize(resources));

                for (Resource resource : resources)
                {
                    MetadataReader reader = readerFactory.getMetadataReader(resource);
                    String className = reader.getClassMetadata().getClassName();
                    Class<?> clazz = Class.forName(className);

                    log.info("className = " + className);

                    ScheduledJob scheduledJob = clazz.getAnnotation(ScheduledJob.class);

                    if (scheduledJob != null)
                    {
                        String jobName = getJobName(scheduledJob, clazz, invocationMap);

                        if (jobName != null)
                        {
                            String methodName = scheduledJob.method();
                            String cron = scheduledJob.cron();

                            Method targetMethod = getTargetMethod(clazz, methodName);
                            Object invocationTarget;

                            try
                            {
                                invocationTarget = getInvocationTarget(clazz);
                            }
                            catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e)
                            {
                                throw new RuntimeException(String.format("Error when trying to create a new instance of class \"%s\".", className), e);
                            }

                            InvocationInfo invocationInfo = new InvocationInfo(invocationTarget, targetMethod, cron, jobName);
                            invocationMap.put(jobName, invocationInfo);
                        }
                    }
                }
            }
        }
        catch (IOException | ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String getJobName(ScheduledJob scheduledJob, Class<?> clazz, HashMap<String, InvocationInfo> invocationMap)
    {
        String methodName = scheduledJob.method();
        String name = scheduledJob.name();

        String jobName = StringUtils.hasText(name) ? name : clazz.getSimpleName() + "#" + methodName;

        // In the same application, each scheduled job must have different name.
        if (invocationMap.containsKey(jobName))
        {
//            throw new IllegalArgumentException(String.format("Duplicate job name \"%s\"" + CHECK_SUFFIX, jobName));
            return null;
        }

        return jobName;
    }

    private static Method getTargetMethod(Class<?> clazz, String methodName)
    {
        List<Method> candidateMethods = new ArrayList<>();

        // Only public method is needed.
        Method[] declaredMethods = clazz.getMethods();
        for (Method method : declaredMethods)
        {
            // Get the method(s) with specified name, and it is not a static method.
            if (method.getName().equals(methodName) && (!Modifier.isStatic(method.getModifiers())))
                candidateMethods.add(method);
        }

        if (candidateMethods.isEmpty())
            throw new IllegalArgumentException(String.format("No match method found for class \"%s\", method name: %s" + CHECK_SUFFIX, clazz.getName(), methodName));

        if (candidateMethods.size() > 1)
            throw new IllegalArgumentException(String.format("More than 1 method with name \"%s\" of class \"%s\"" + CHECK_SUFFIX, methodName, clazz.getName()));

        return candidateMethods.get(0);
    }

    private Object getInvocationTarget(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        Object invocationTarget = null;
        try
        {
            invocationTarget = applicationContext.getBean(clazz);
        }
        catch (NoSuchBeanDefinitionException e)
        {
            log.warn(String.format("No bean of type \"%s\" is found in the application context, will create it by no-arg constructor.", clazz.getName()));
        }

        // If "invocationTarget" is null, then it is not in the Spring IoC, thus, we need to create an instance of the class by the public no-arg constructor.
        if (invocationTarget == null)
        {
            Constructor<?> noArgConstructor = clazz.getConstructor();
            invocationTarget = noArgConstructor.newInstance();
        }

        return invocationTarget;
    }

    private void startHeartbeats() throws Exception
    {
        heartbeatHandler.startHeartbeats(invocationMap);
    }

    /**
     * Starts the executor server.
     */
    private void startExecutorServer()
    {
        ExecutorServer executorServer = new ExecutorServer(cygnusExecutorProperties.getPort(), invocationMap);
        executorServer.start();
    }
}
