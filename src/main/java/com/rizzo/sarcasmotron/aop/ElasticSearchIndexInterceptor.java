package com.rizzo.sarcasmotron.aop;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.rizzo.sarcasmotron.domain.elasticsearch.ESComment;
import com.rizzo.sarcasmotron.domain.elasticsearch.ESSarcasm;
import com.rizzo.sarcasmotron.domain.mongodb.Comment;
import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;
import com.rizzo.sarcasmotron.elasticsearch.ElasticsearchSarcasmRepository;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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
                elasticsearchSarcasmRepository.save(mapSarcasm(sarcasm));
            } else if(methodInvocation.getArguments()[0] instanceof Iterable) {
                Iterable sarcasms = (Iterable) methodInvocation.getArguments()[0];
                final Object first = Iterables.getFirst(sarcasms, null);
                if(first != null && first instanceof Sarcasm) {
                    elasticsearchSarcasmRepository.save(mapSarcasms((Iterable<Sarcasm>) sarcasms));
                }
            }
        } else if("delete".equals(methodName) && (methodInvocation.getArguments() != null && methodInvocation.getArguments().length == 1)) {
            if (methodInvocation.getArguments()[0] instanceof String) {
                String id = (String) methodInvocation.getArguments()[0];
                elasticsearchSarcasmRepository.delete(id);
            } else if(methodInvocation.getArguments()[0] instanceof Sarcasm) {
                Sarcasm sarcasm = (Sarcasm) methodInvocation.getArguments()[0];
                elasticsearchSarcasmRepository.delete(mapSarcasm(sarcasm));
            } else if(methodInvocation.getArguments()[0] instanceof Iterable) {
                Iterable sarcasms = (Iterable) methodInvocation.getArguments()[0];
                final Object first = Iterables.getFirst(sarcasms, null);
                if(first != null && first instanceof Sarcasm) {
                    elasticsearchSarcasmRepository.delete(mapSarcasms((Iterable<Sarcasm>) sarcasms));
                }
            }
        }
        return proceed;
    }

    private Iterable<ESSarcasm> mapSarcasms(Iterable<Sarcasm> mongoSarcasms) {
        List<ESSarcasm> ESSarcasms = Lists.newArrayList();
        for (Sarcasm mongoSarcasm : mongoSarcasms) {
            ESSarcasms.add(mapSarcasm(mongoSarcasm));
        }
        return ESSarcasms;
    }

    private ESSarcasm mapSarcasm(Sarcasm mongoSarcasm) {
        return new ESSarcasm()
                .setId(mongoSarcasm.getId()).setQuote(mongoSarcasm.getQuote())
                .setContext(mongoSarcasm.getContext()).setUser(mongoSarcasm.getUser())
                .setCreator(mongoSarcasm.getCreator())
                .setVotes(mongoSarcasm.getVotes())
                .setComments(mapComments(mongoSarcasm.getComments()))
                .setTimestamp(mongoSarcasm.getTimestamp());
    }

    private List<ESComment> mapComments(List<Comment> mongoSarcasmComments) {
        List<ESComment> elasticsearchESComments = Lists.newArrayList();
        for (Comment mongoSarcasmComment : mongoSarcasmComments) {
            ESComment ESComment = new ESComment().setComment(mongoSarcasmComment.getComment()).setUser(mongoSarcasmComment.getUser()).setTimestamp(mongoSarcasmComment.getTimestamp());
            elasticsearchESComments.add(ESComment);
        }
        return elasticsearchESComments;
    }

}
