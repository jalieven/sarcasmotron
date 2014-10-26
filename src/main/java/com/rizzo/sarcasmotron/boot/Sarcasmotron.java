package com.rizzo.sarcasmotron.boot;

import com.rizzo.sarcasmotron.aop.ElasticSearchIndexInterceptor;
import com.rizzo.sarcasmotron.trend.TrendCalculator;
import com.rizzo.sarcasmotron.web.SarcasmotronController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Advisor;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;
import org.springframework.aop.interceptor.CustomizableTraceInterceptor;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableAutoConfiguration
@ComponentScan
@EnableMongoRepositories(value= {"com.rizzo.sarcasmotron.mongodb"})
@EnableElasticsearchRepositories(value = {"com.rizzo.sarcasmotron.elasticsearch"})
public class Sarcasmotron extends SpringBootServletInitializer implements CommandLineRunner  {

    private static final Logger LOGGER = LoggerFactory.getLogger(Sarcasmotron.class);

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
        interceptor.setExitMessage("Leaving $[methodName](..) with return value $[returnValue], took $[invocationTime]ms.");
        return interceptor;
    }

    @Bean
    public Advisor traceAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(public * org.springframework.data.repository.Repository+.*(..))");
        return new DefaultPointcutAdvisor(pointcut, traceInterceptor());
    }

    @Bean
    public ElasticSearchIndexInterceptor elasticSearchIndexInterceptor() {
        return new ElasticSearchIndexInterceptor();
    }

    @Bean
    public TrendCalculator trendCalculator() {
        return new TrendCalculator();
    }

    @Bean
    public Advisor elasticSearchAdvisor() {
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression("execution(public * org.springframework.data.repository.Repository+.delete*(..)) || execution(public * org.springframework.data.repository.Repository+.save*(..))");
        return new DefaultPointcutAdvisor(pointcut, elasticSearchIndexInterceptor());
    }

    @Bean
    public SarcasmotronController sarcasmotronController() {
        return new SarcasmotronController();
    }
}
