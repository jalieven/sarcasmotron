package com.rizzo.sarcasmotron.mongodb;

import com.rizzo.sarcasmotron.domain.Sarcasm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MongoDBSarcasmRepository extends MongoRepository<Sarcasm, String> {

    List<Sarcasm> findByCommentsUser(@Param("user") String user);

}