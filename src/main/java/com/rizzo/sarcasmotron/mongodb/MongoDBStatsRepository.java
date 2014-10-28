package com.rizzo.sarcasmotron.mongodb;

import com.rizzo.sarcasmotron.domain.web.Stats;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface MongoDBStatsRepository extends MongoRepository<Stats, String> {


}
