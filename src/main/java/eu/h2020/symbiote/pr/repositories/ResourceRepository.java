package eu.h2020.symbiote.pr.repositories;

import eu.h2020.symbiote.model.cim.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@RepositoryRestResource(collectionResourceRel = "resources", path = "resources")
public interface ResourceRepository extends MongoRepository<Resource, String> {
    Resource findByFederationId(String federationId);
    List<Resource> deleteAllByIdIn(List<String> ids);
}