package com.rizzo.sarcasmotron.elasticsearch;

import com.rizzo.sarcasmotron.domain.Sarcasm;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ElasticsearchSarcasmRepository extends ElasticsearchRepository<Sarcasm, String> {
}
