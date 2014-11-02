package com.rizzo.sarcasmotron.boot;

import com.rizzo.sarcasmotron.aop.ElasticSearchIndexInterceptor;
import com.rizzo.sarcasmotron.calc.VoteCalculator;
import com.rizzo.sarcasmotron.security.LoginEventHandler;
import com.rizzo.sarcasmotron.task.ScheduledTasks;
import com.rizzo.sarcasmotron.web.SarcasmotronController;
import com.rizzo.sarcasmotron.web.SarcasmotronRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.interceptor.CustomizableTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import javax.mail.MessagingException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableScheduling
@ImportResource("classpath:openam/security-ctx.xml")
@EnableMongoRepositories(value= {"com.rizzo.sarcasmotron.mongodb"})
@EnableElasticsearchRepositories(value = {"com.rizzo.sarcasmotron.elasticsearch"})
public class Sarcasmotron extends SpringBootServletInitializer implements CommandLineRunner, SchedulingConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sarcasmotron.class);

    @Value(value = "${scheduling.winnerCalculation.cron}")
    private String winnerCalculationCron;

    @Value(value = "${scheduling.winnerCalculation.period}")
    private String winnerCalculationPeriod;

    @Value(value = "${mail.host}")
    private String mailHost;

    @Value(value = "${mail.port}")
    private Integer mailPort;

    @Value(value = "${mail.protocol}")
    private String mailProtocol;

    @Override
    public void run(String... args) throws Exception {

    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(Sarcasmotron.class);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Sarcasmotron.class, args);
    }

    @Bean
    public CustomizableTraceInterceptor traceInterceptor() {
        CustomizableTraceInterceptor interceptor = new CustomizableTraceInterceptor();
        interceptor.setEnterMessage("Entering $[methodName]($[arguments]).");
        interceptor.setExitMessage("PERFORMANCE:{$[methodName]} [$[invocationTime]]ms.");
        return interceptor;
    }

    @Bean
    public Advisor traceAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(public * org.springframework.data.repository.Repository+.*(..)) || execution(public * com.rizzo.sarcasmotron.task.ScheduledTasks+.*(..))");
        return new DefaultPointcutAdvisor(pointcut, traceInterceptor());
    }

    @Bean
    public ElasticSearchIndexInterceptor elasticSearchIndexInterceptor() {
        return new ElasticSearchIndexInterceptor();
    }

    @Bean
    public VoteCalculator trendCalculator() {
        return new VoteCalculator();
    }

    @Bean
    public Advisor elasticSearchAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(public * org.springframework.data.repository.Repository+.delete*(..)) || execution(public * org.springframework.data.repository.Repository+.save*(..))");
        return new DefaultPointcutAdvisor(pointcut, elasticSearchIndexInterceptor());
    }

    @Bean
    public SarcasmotronRestController sarcasmotronRestController() {
        return new SarcasmotronRestController();
    }

    @Bean
    public SarcasmotronController sarcasmotronController() {
        return new SarcasmotronController();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());
        taskRegistrar.addTriggerTask(
                new Runnable() {
                    public void run() {
                        try {
                            scheduledTasks().calculateWinner();
                        } catch (MessagingException e) {
                            LOGGER.error("Fault while calculating winner!", e);
                        }
                    }
                },
                new CronTrigger(winnerCalculationCron)
        );
    }

    @Bean(destroyMethod = "shutdown")
    public Executor taskScheduler() {
        return Executors.newScheduledThreadPool(42);
    }

    @Bean
    public ScheduledTasks scheduledTasks() {
        return new ScheduledTasks(winnerCalculationPeriod);
    }

    @Bean
    public JavaMailSenderImpl javaMailSender() {
        final JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setProtocol(mailProtocol);
        return mailSender;
    }

    @Bean
    public LoginEventHandler loginEventHandler() {
        return new LoginEventHandler();
    }

}
