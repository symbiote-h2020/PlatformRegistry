package eu.h2020.symbiote.pr.services;

import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.ResourceSharingInformation;
import eu.h2020.symbiote.cloud.model.internal.ResourcesAddedOrUpdatedMessage;
import eu.h2020.symbiote.cloud.model.internal.ResourcesDeletedMessage;
import eu.h2020.symbiote.cloud.trust.model.TrustEntry;
import eu.h2020.symbiote.pr.repositories.ResourceRepository;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ilia Pietri (ICOM)
 * @since 31/05/2018.
 */
@Service
public class TrustManagerService {
    private static Log log = LogFactory.getLog(TrustManagerService.class);

    private ResourceRepository resourceRepository;

    @Autowired
    public TrustManagerService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    /**
     * Update adaptive trust of federated resources offered by the other platforms
     *
     * @param resourceTrustUpdated message received from Trust Manager notifying about new trust values of federated resources
     */
    public void updateFedResAdaptiveResourceTrust(TrustEntry resourceTrustUpdated) {
        log.trace("updateFedResAdaptiveResourceTrust: " + ReflectionToStringBuilder.toString(resourceTrustUpdated));

        Map<String, FederatedResource> resourcesToBeStored = new HashMap<>();

        // Get the federated resource id in order to fetch it from the database
        Set<String> newFederatedResourcesIds = new HashSet<>();
            String aggregationId = resourceTrustUpdated.getResourceId().split("@", 3)[0] +
                    "@" + resourceTrustUpdated.getResourceId().split("@", 3)[1];
        String fedId = resourceTrustUpdated.getResourceId().split("@", 3)[2];
        newFederatedResourcesIds.add(aggregationId);

        // Find the stored federated resources and convert them to a map, in which the key is the internalId
        Map<String, FederatedResource> storedFederatedResources = resourceRepository.findAllByAggregationIdIn(newFederatedResourcesIds)
                .stream().collect(Collectors.toMap(FederatedResource::getAggregationId, federatedResource -> federatedResource));

            if (storedFederatedResources.containsKey(aggregationId)) {
                FederatedResource federatedResource = storedFederatedResources.get(aggregationId);
                federatedResource.setAdaptiveTrust(resourceTrustUpdated.getValue());
                resourcesToBeStored.put(federatedResource.getAggregationId(), federatedResource);
            }

        resourceRepository.save(new ArrayList<>(resourcesToBeStored.values()));
    }

}
