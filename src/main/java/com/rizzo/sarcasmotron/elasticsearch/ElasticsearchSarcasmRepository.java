package com.rizzo.sarcasmotron.elasticsearch;

import com.rizzo.sarcasmotron.domain.elasticsearch.ESSarcasm;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ElasticsearchSarcasmRepository extends ElasticsearchRepository<ESSarcasm, String> {
}
