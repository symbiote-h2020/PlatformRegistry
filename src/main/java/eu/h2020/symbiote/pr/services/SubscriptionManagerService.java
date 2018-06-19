package eu.h2020.symbiote.pr.services;

import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.ResourceSharingInformation;
import eu.h2020.symbiote.cloud.model.internal.ResourcesAddedOrUpdatedMessage;
import eu.h2020.symbiote.cloud.model.internal.ResourcesDeletedMessage;
import eu.h2020.symbiote.pr.repositories.ResourceRepository;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@Service
public class SubscriptionManagerService {
    private static Log log = LogFactory.getLog(SubscriptionManagerService.class);

    private ResourceRepository resourceRepository;

    @Autowired
    public SubscriptionManagerService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }


    /**
     * Store the federated resources offered by the other platforms
     *
     * @param resourcesAddedOrUpdated message received from Subscription Manager notifying about new resources
     */
    public void addOrUpdateFederationResources(ResourcesAddedOrUpdatedMessage resourcesAddedOrUpdated) {
        log.trace("addOrUpdateFederationResources: " + ReflectionToStringBuilder.toString(resourcesAddedOrUpdated));

        // Todo: maybe remove the platform resources from the message.
        // Platform resources should not be present here. Only, federated resources offered by other platforms should be
        // in the NewResourceMessage

        Map<String, FederatedResource> resourcesToBeStored = new HashMap<>();

        // Get all the federated resources ids in order to fetch them from the database
        Set<String> newFederatedResourcesIds = resourcesAddedOrUpdated.getNewFederatedResources().stream()
                .map(FederatedResource::getAggregationId).collect(Collectors.toSet());

        // Find the stored federated resources and convert them to a map, in which the key is the internalId
        Map<String, FederatedResource> storedFederatedResources = resourceRepository.findAllByAggregationIdIn(newFederatedResourcesIds)
                .stream().collect(Collectors.toMap(FederatedResource::getAggregationId, federatedResource -> federatedResource));

        for (FederatedResource newFederatedResource : resourcesAddedOrUpdated.getNewFederatedResources()) {
            String symbioteId = newFederatedResource.getAggregationId();
            if (symbioteId == null)
                continue;

            for (Map.Entry<String, ResourceSharingInformation> entry :
                    newFederatedResource.getCloudResource().getFederationInfo().getSharingInformation().entrySet()) {
                String federationId = entry.getKey();
                Boolean barteringStatus = entry.getValue().getBartering();

                if (resourcesToBeStored.containsKey(symbioteId)) {

                    // We have already added this symbioteId to the resourcesToBeStored, so we only update the extra federation
                    FederatedResource federatedResource = resourcesToBeStored.get(symbioteId);
                    federatedResource.shareToNewFederation(federationId, barteringStatus);

                } else if (storedFederatedResources.containsKey(symbioteId)) {

                    // The resource exists in the repository so we read it from there and update it
                    //we have kept the adaptiveTrust field so we do not change it leave it as it is in the repository (set by Trust Manager).
                    FederatedResource federatedResource = storedFederatedResources.get(symbioteId);
                    federatedResource.shareToNewFederation(federationId, barteringStatus);

                    resourcesToBeStored.put(federatedResource.getAggregationId(), federatedResource);

                } else {//add it as it doesn't exist
                    resourcesToBeStored.put(symbioteId, newFederatedResource);
                }
            }
        }

        resourceRepository.save(new ArrayList<>(resourcesToBeStored.values()));
    }


    /**
     * Remove federated resources offered by other platforms
     *
     * @param resourcesDeleted message received from Subscription Manager notifying about removal of resources
     */
    public void removeFederationResources(ResourcesDeletedMessage resourcesDeleted) {
        log.trace("removeFederationResources: " + ReflectionToStringBuilder.toString(resourcesDeleted));

        // Todo: maybe remove the platform resources from the message.
        // Platform resources should not be present here. Only, federated resources offered by other platforms should be
        // in the ResourcesDeletedMessage

        Set<String> resourceIds = new HashSet<>();
        for (String symbioteId: resourcesDeleted.getDeletedFederatedResources()) {
            String aggregationId = symbioteId.split("@", 3)[0] +
                    "@"+symbioteId.split("@", 3)[1];
            resourceIds.add(aggregationId);

        }

        // Ids of the resources which are not shared in any federation any more
        Set<String> unsharedResourcesIds = new HashSet<>();

        // Fetch the stored federated resources and
        List<FederatedResource> storedFederatedResources = resourceRepository.findAllByAggregationIdIn(resourceIds);

        // Convert them to a map in which the key is the internalId
        Map<String, FederatedResource> storedFederatedResourcesMap = storedFederatedResources.stream()
                .collect(Collectors.toMap(FederatedResource::getAggregationId, federatedResource -> federatedResource));

        for (String entry : resourcesDeleted.getDeletedFederatedResources()) {

            String aggregationId = entry.split("@", 3)[0] +
                    "@"+entry.split("@", 3)[1];
            String federationId = entry.split("@", 3)[2];

                FederatedResource federatedResource = storedFederatedResourcesMap.get(aggregationId);

                if (federatedResource != null) {
                    federatedResource.unshareFromFederation(federationId);

                    // If the resource is not shared in any federation any more, we remove it from the repository
                    if (federatedResource.getFederatedResourceInfoMap().size() == 0) {
                        storedFederatedResourcesMap.remove(aggregationId);
                        unsharedResourcesIds.add(aggregationId);
                    }
                }

        }

        resourceRepository.save(new ArrayList<>(storedFederatedResourcesMap.values()));
        resourceRepository.deleteAllByAggregationIdIn(unsharedResourcesIds);

    }
}
