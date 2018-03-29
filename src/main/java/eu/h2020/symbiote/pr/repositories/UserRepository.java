package eu.h2020.symbiote.pr.repositories;

import eu.h2020.symbiote.pr.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@RepositoryRestResource(collectionResourceRel = "resources", path = "resources")
public interface UserRepository extends MongoRepository<User, String>, QueryDslPredicateExecutor<User> {

}