package eu.h2020.symbiote.pr.repositories;

import eu.h2020.symbiote.model.cim.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@RepositoryRestResource(collectionResourceRel = "resources", path = "resources")
public interface ResourceRepository extends MongoRepository<Resource, String> {
    public Resource findByFederationId(String federationId);
}