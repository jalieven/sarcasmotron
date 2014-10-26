package com.rizzo.sarcasmotron.aop;

import com.rizzo.sarcasmotron.domain.Sarcasm;
import com.rizzo.sarcasmotron.elasticsearch.ElasticsearchSarcasmRepository;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticSearchIndexInterceptor implements MethodInterceptor {

    @Autowired
    private ElasticsearchSarcasmRepository elasticsearchSarcasmRepository;

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {

        final Object proceed = methodInvocation.proceed();

        final String methodName = methodInvocation.getMethod().getName();
        if("deleteAll".equals(methodName)) {
            elasticsearchSarcasmRepository.deleteAll();
        } else if("save".equals(methodName) && (methodInvocation.getArguments() != null && methodInvocation.getArguments().length == 1)) {
            if (methodInvocation.getArguments()[0] instanceof Sarcasm) {
                Sarcasm sarcasm = (Sarcasm) methodInvocation.getArguments()[0];
                elasticsearchSarcasmRepository.save(sarcasm);
            } else if(methodInvocation.getArguments()[0] instanceof Iterable) {
                Iterable<Sarcasm> sarcasms = (Iterable<Sarcasm>) methodInvocation.getArguments()[0];
                elasticsearchSarcasmRepository.save(sarcasms);
            }
        } else if("delete".equals(methodName) && (methodInvocation.getArguments() != null && methodInvocation.getArguments().length == 1)) {
            if (methodInvocation.getArguments()[0] instanceof String) {
                String id = (String) methodInvocation.getArguments()[0];
                elasticsearchSarcasmRepository.delete(id);
            } else if(methodInvocation.getArguments()[0] instanceof Sarcasm) {
                Sarcasm sarcasm = (Sarcasm) methodInvocation.getArguments()[0];
                elasticsearchSarcasmRepository.delete(sarcasm);
            } else if(methodInvocation.getArguments()[0] instanceof Iterable) {
                Iterable<Sarcasm> sarcasms = (Iterable<Sarcasm>) methodInvocation.getArguments()[0];
                elasticsearchSarcasmRepository.delete(sarcasms);
            }
        }
        return proceed;
    }

}
