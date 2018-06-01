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
     * Store the federated resources offered by the other platforms
     *
     * @param resourcesUpdated message received from Trust Manager notifying about new resources
     */
    public void updateFedResAdaptiveResourceTrust(ResourcesAddedOrUpdatedMessage resourcesUpdated) {
        log.trace("updateFedResAdaptiveResourceTrust: " + ReflectionToStringBuilder.toString(resourcesUpdated));

        // Todo: maybe remove the platform resources from the message.
        // Platform resources should not be present here. Only, federated resources offered by other platforms should be
        // in the NewResourceMessage

        Map<String, FederatedResource> resourcesToBeStored = new HashMap<>();

        // Get all the federated resources ids in order to fetch them from the database
        Set<String> newFederatedResourcesIds = resourcesUpdated.getNewFederatedResources().stream()
                .map(FederatedResource::getSymbioteId).collect(Collectors.toSet());

        // Find the stored federated resources and convert them to a map, in which the key is the internalId
        Map<String, FederatedResource> storedFederatedResources = resourceRepository.findAllBySymbioteIdIn(newFederatedResourcesIds)
                .stream().collect(Collectors.toMap(FederatedResource::getSymbioteId, federatedResource -> federatedResource));

        for (FederatedResource newFederatedResource : resourcesUpdated.getNewFederatedResources()) {
            String symbioteId = newFederatedResource.getSymbioteId();
            if (symbioteId == null)
                continue;

            //check that it has not been provided twice. Not required
            if (resourcesToBeStored.containsKey(symbioteId))
                    continue;
                 else//we retrieve the federatedResource (with symbioteId) from the repository and update its adaptive trust value
                if (storedFederatedResources.containsKey(symbioteId)) {
                    FederatedResource federatedResource = storedFederatedResources.get(symbioteId);
                    federatedResource.setAdaptiveTrust(newFederatedResource.getAdaptiveTrust());
                    resourcesToBeStored.put(federatedResource.getSymbioteId(), federatedResource);
                } else {//adding new federatedResources. it shouldnt be the case
                    continue;//resourcesToBeStored.put(symbioteId, newFederatedResource);
                }
        }

        resourceRepository.save(new ArrayList<>(resourcesToBeStored.values()));
    }


}
