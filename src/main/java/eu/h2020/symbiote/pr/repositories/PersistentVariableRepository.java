package eu.h2020.symbiote.pr.repositories;

import eu.h2020.symbiote.pr.model.PersistentVariable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@RepositoryRestResource(collectionResourceRel = "persistentVariables", path = "persistentVariables")
public interface PersistentVariableRepository extends MongoRepository<PersistentVariable, String> {
    public PersistentVariable findByVariableName(String variableName);
}