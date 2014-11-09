package com.rizzo.sarcasmotron.mongodb;

import com.rizzo.sarcasmotron.domain.mongodb.Sarcasm;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(exported = false)
public interface MongoDBSarcasmRepository extends MongoRepository<Sarcasm, String> {

    List<Sarcasm> findByCommentsUser(@Param("user") String user);

    @Query("{votes.?0: null, user: {$ne: '?0'}}")
    List<Sarcasm> findToVote(String user);

    @Query("{favorites: '?0'}")
    List<Sarcasm> findFavorites(String user);

}