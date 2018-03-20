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
                .map(FederatedResource::getSymbioteId).collect(Collectors.toSet());

        // Find the stored federated resources and convert them to a map, in which the key is the internalId
        Map<String, FederatedResource> storedFederatedResources = resourceRepository.findAllBySymbioteIdIn(newFederatedResourcesIds)
                .stream().collect(Collectors.toMap(FederatedResource::getSymbioteId, federatedResource -> federatedResource));

        for (FederatedResource newFederatedResource : resourcesAddedOrUpdated.getNewFederatedResources()) {
            String symbioteId = newFederatedResource.getSymbioteId();
            if (symbioteId == null)
                continue;

            for (Map.Entry<String, ResourceSharingInformation> entry :
                    newFederatedResource.getCloudResource().getFederationInfo().getSharingInformation().entrySet()) {
                String federationId = entry.getKey();
                Boolean barteringStatus = entry.getValue().getBartering();

                if (resourcesToBeStored.containsKey(symbioteId)) {

                    // We have already added this symbioteId to the resourcesToBeStored, so we will update it there
                    FederatedResource federatedResource = resourcesToBeStored.get(symbioteId);
                    federatedResource.shareToNewFederation(federationId, barteringStatus);

                } else if (storedFederatedResources.containsKey(symbioteId)) {

                    // We have already added this symbioteId to the resourcesToBeStored, so we will update it there
                    FederatedResource federatedResource = storedFederatedResources.get(symbioteId);
                    federatedResource.shareToNewFederation(federationId, barteringStatus);
                    resourcesToBeStored.put(federatedResource.getSymbioteId(), federatedResource);

                } else {
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

        Set<String> resourceIds = resourcesDeleted.getDeletedFederatedResourcesMap().keySet();

        // Ids of the resources which are not shared in any federation any more
        Set<String> unsharedResourcesIds = new HashSet<>();

        // Fetch the stored federated resources and
        List<FederatedResource> storedFederatedResources = resourceRepository.findAllBySymbioteIdIn(resourceIds);

        // Convert them to a map in which the key is the internalId
        Map<String, FederatedResource> storedFederatedResourcesMap = storedFederatedResources.stream()
                .collect(Collectors.toMap(FederatedResource::getSymbioteId, federatedResource -> federatedResource));

        for (Map.Entry<String, Set<String>> entry : resourcesDeleted.getDeletedFederatedResourcesMap().entrySet()) {

            String symbioteId = entry.getKey();

            for (String federationId : entry.getValue()) {

                FederatedResource federatedResource = storedFederatedResourcesMap.get(symbioteId);

                if (federatedResource != null) {
                    federatedResource.unshareFromFederation(federationId);

                    // If the resource is not shared in any federation any more, we remove it from the repository
                    if (federatedResource.getFederations().size() == 0) {
                        storedFederatedResourcesMap.remove(symbioteId);
                        unsharedResourcesIds.add(symbioteId);
                    }
                }
            }
        }


        resourceRepository.save(new ArrayList<>(storedFederatedResourcesMap.values()));
        resourceRepository.deleteAllBySymbioteIdIn(unsharedResourcesIds);

    }
}
