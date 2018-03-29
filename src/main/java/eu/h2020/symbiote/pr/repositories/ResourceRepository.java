package eu.h2020.symbiote.pr.repositories;

import com.querydsl.core.types.Predicate;
import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Set;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@RepositoryRestResource(collectionResourceRel = "resources", path = "resources")
public interface ResourceRepository extends MongoRepository<FederatedResource, String>, QueryDslPredicateExecutor<FederatedResource> {
    // ToDo: consider some optimized query here
    List<FederatedResource> findAllBySymbioteIdIn(Set<String> ids);

    List<FederatedResource> findAllByCloudResource_InternalIdIn(Set<String> internalIds);

    // Todo: remove this and replace it with a custom query to get this info from the cloudResource
    List<FederatedResource> findAllByFederationsContaining(String federationId);

    List<FederatedResource> deleteAllBySymbioteIdIn(Set<String> ids);

    List<FederatedResource> findAll(Predicate predicate, Sort sort);

    List<FederatedResource> findAllByLocationCoordsIsWithin(Circle point);

    List<FederatedResource> deleteAllByCloudResource_InternalIdIn(Set<String> internalIds);
}