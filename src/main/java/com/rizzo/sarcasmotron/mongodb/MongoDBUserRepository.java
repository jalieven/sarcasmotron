package com.rizzo.sarcasmotron.mongodb;

import com.rizzo.sarcasmotron.domain.mongodb.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = true)
public interface MongoDBUserRepository extends MongoRepository<User, String> {

    User findOneByNickName(@Param("nickName") String user);

}
