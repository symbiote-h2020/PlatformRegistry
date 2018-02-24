package eu.h2020.symbiote.pr.repositories;

import eu.h2020.symbiote.pr.model.FederatedResource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@RepositoryRestResource(collectionResourceRel = "resources", path = "resources")
public interface ResourceRepository extends MongoRepository<FederatedResource, String> {
    // ToDo: consider some optimized query here
    List<FederatedResource> findAllByIdIn(List<String> ids);

    List<FederatedResource> findByResourceFederationId(String federationId);

    List<FederatedResource> deleteAllByIdIn(List<String> ids);
}