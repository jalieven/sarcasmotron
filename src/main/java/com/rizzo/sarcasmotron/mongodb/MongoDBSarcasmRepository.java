package com.rizzo.sarcasmotron.mongodb;

import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(exported = true)
public interface MongoDBSarcasmRepository extends MongoRepository<Sarcasm, String> {

    List<Sarcasm> findByCommentsUser(@Param("user") String user);

}