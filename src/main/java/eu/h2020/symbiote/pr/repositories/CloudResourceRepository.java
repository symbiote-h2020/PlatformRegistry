package eu.h2020.symbiote.pr.repositories;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@RepositoryRestResource(collectionResourceRel = "cloudResources", path = "cloudResources")
public interface CloudResourceRepository extends MongoRepository<CloudResource, String> {
    List<CloudResource> findAllByInternalIdIn(List<String> internalIds);

    List<CloudResource> deleteAllByInternalIdIn(List<String> internalIds);
}