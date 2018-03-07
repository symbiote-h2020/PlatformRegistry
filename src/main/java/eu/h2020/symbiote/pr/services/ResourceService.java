package eu.h2020.symbiote.pr.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.cloud.model.internal.*;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.pr.model.PersistentVariable;
import eu.h2020.symbiote.pr.repositories.CloudResourceRepository;
import eu.h2020.symbiote.pr.repositories.PersistentVariableRepository;
import eu.h2020.symbiote.pr.repositories.ResourceRepository;
import io.jsonwebtoken.lang.Assert;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@Service
public class ResourceService {
    private static Log log = LogFactory.getLog(ResourceService.class);

    private ResourceRepository resourceRepository;
    private CloudResourceRepository cloudResourceRepository;
    private RabbitTemplate rabbitTemplate;
    private PersistentVariableRepository persistentVariableRepository;
    private PersistentVariable idSequence;
    private ObjectMapper mapper = new ObjectMapper();
    private String platformId;
    private String subscriptionManagerExchange;
    private String smAddOrUpdateFederatedResourcesKey;
    private String smRemoveFederatedResourcesKey;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository,
                           CloudResourceRepository cloudResourceRepository,
                           RabbitTemplate rabbitTemplate,
                           PersistentVariableRepository persistentVariableRepository,
                           PersistentVariable idSequence,
                           @Value("${platform.id}") String platformId,
                           @Value("${rabbit.exchange.subscriptionManager.name}") String subscriptionManagerExchange,
                           @Value("${rabbit.routingKey.subscriptionManager.addOrUpdateFederatedResources}") String smAddOrUpdateFederatedResourcesKey,
                           @Value("${rabbit.routingKey.subscriptionManager.removeFederatedResources}") String smRemoveFederatedResourcesKey) {
        this.resourceRepository = resourceRepository;
        this.cloudResourceRepository = cloudResourceRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.persistentVariableRepository = persistentVariableRepository;
        this.idSequence = idSequence;

        Assert.notNull(platformId, "The platformId should not be null");
        this.platformId = platformId;

        Assert.notNull(subscriptionManagerExchange, "The subscriptionManagerExchange should not be null");
        this.subscriptionManagerExchange = subscriptionManagerExchange;

        Assert.notNull(smAddOrUpdateFederatedResourcesKey, "The smAddOrUpdateFederatedResourcesKey should not be null");
        this.smAddOrUpdateFederatedResourcesKey = smAddOrUpdateFederatedResourcesKey;

        Assert.notNull(smRemoveFederatedResourcesKey, "The smRemoveFederatedResourcesKey should not be null");
        this.smRemoveFederatedResourcesKey = smRemoveFederatedResourcesKey;
    }

    /**
     * Stores the federated resources offered by the platform
     *
     * @param cloudResources registration message sent by Registration Handler
     * @return a list of the new/updated CloudResources
     */
    public List<CloudResource> addOrUpdatePlatformResources(List<CloudResource> cloudResources) {
        log.trace("addOrUpdatePlatformResources: " + ReflectionToStringBuilder.toString(cloudResources));

        List<FederatedResource> resourcesToSave = new LinkedList<>();

        long nextId = (Long) idSequence.getValue();
        for (CloudResource cloudResource : cloudResources) {
            // Here, we do a trick. Instead of cloning the cloudResource, we just serialize it and deserialize it.
            // So, every time we deserialize a new object (cloned) is created. We did that to avoid the cumbersome
            // implementation of the clone() in every Resource and Resource field (e.g. StationarySensor, MobileSensor,
            // Location, ...)

            String serializedResource = serializeResource(cloudResource.getResource());

            // if the federationInfo of the cloudResource is null, initialize it
            if (cloudResource.getFederationInfo() == null)
                cloudResource.setFederationInfo(new HashMap<>());

            for (Map.Entry<String,  ResourceSharingInformation> entry : cloudResource.getFederationInfo().entrySet()) {
                Resource newResource = deserializeResource(serializedResource);

                if (newResource != null) {
                    String federationId = entry.getKey();
                    ResourceSharingInformation sharingInformation = entry.getValue();

                    if (sharingInformation.getSymbioteId() == null) {
                        String newFederatedId = createNewResourceId(nextId, federationId);
                        sharingInformation.setSymbioteId(newFederatedId);
                        nextId++;
                    }

                    resourcesToSave.add(new FederatedResource(newResource, sharingInformation.getSymbioteId(),federationId,
                            sharingInformation.getBartering() != null ? sharingInformation.getBartering() : false));
                }
            }

        }

        resourceRepository.save(resourcesToSave);
        cloudResourceRepository.save(cloudResources);
        idSequence.setValue(nextId);
        persistentVariableRepository.save(idSequence);

        // Inform Subscription Manager for the new resources
        if (resourcesToSave.size() > 0)
            rabbitTemplate.convertAndSend(subscriptionManagerExchange, smAddOrUpdateFederatedResourcesKey,
                    new ResourcesAddedOrUpdatedMessage(resourcesToSave));

        // Find the resources that should be removed
        List<String> resourcesToBeRemoved = findResourcesToBeRemoved(cloudResources, resourcesToSave);

        if (resourcesToBeRemoved.size() > 0)
            rabbitTemplate.convertAndSend(subscriptionManagerExchange, smRemoveFederatedResourcesKey,
                    new ResourcesDeletedMessage(resourcesToBeRemoved));

        return cloudResources;
    }


    /**
     * Remove federated resources offered by the platform by using their internalIds
     *
     * @param internalIds the internal ids of the platform resources to be removed
     * @return the list of the removed resources
     */
    public List<String> removePlatformResources(List<String> internalIds) {
        log.trace("removePlatformResources: " + ReflectionToStringBuilder.toString(internalIds));

        if (internalIds == null)
            return new ArrayList<>();

        List<String> federatedIdsToRemove = new ArrayList<>();

        List<CloudResource> cloudResources = cloudResourceRepository.findAllByInternalIdIn(internalIds);

        for (CloudResource cloudResource : cloudResources) {
            for (Map.Entry<String, ResourceSharingInformation> entry : cloudResource.getFederationInfo().entrySet())
                federatedIdsToRemove.add(entry.getValue().getSymbioteId());
        }

        List<String> federatedResourcesRemoved = resourceRepository.deleteAllByIdIn(federatedIdsToRemove).stream()
                .map(FederatedResource::getId).collect(Collectors.toList());

        List<String> cloudResourcesRemoved = cloudResourceRepository.deleteAllByInternalIdIn(internalIds).stream()
                .map(CloudResource::getInternalId).collect(Collectors.toList());

        // Inform Subscription Manager for the removed resources
        rabbitTemplate.convertAndSend(subscriptionManagerExchange, smRemoveFederatedResourcesKey,
                new ResourcesDeletedMessage(federatedResourcesRemoved));

        return cloudResourcesRemoved;
    }


    /**
     * Shares resources in federations
     *
     * @param resourcesToBeShared a map with key the federationId and value another map, which has as key
     *                            the internalId of the resource and value the bartering status
     * @return a list of the updated CloudResources
     */
    public List<CloudResource> shareResources(Map<String, Map<String, Boolean>> resourcesToBeShared) {

        long nextId = (Long) idSequence.getValue();
        List<FederatedResource> resourcesToSave = new LinkedList<>();

        // First find all the resources that are going to be shared
        Set<String> cloudResourcesIds = new HashSet<>();
        for (Map.Entry<String, Map<String, Boolean>> federationEntry : resourcesToBeShared.entrySet())
            for (Map.Entry<String, Boolean> resourceEntry : federationEntry.getValue().entrySet())
                cloudResourcesIds.add(resourceEntry.getKey());

        // Fetch the cloudResources from the database and convert it to a map
        Map<String, CloudResource> cloudResourcesMap = cloudResourceRepository.findAllByInternalIdIn(cloudResourcesIds).stream()
                .collect(Collectors.toMap(CloudResource::getInternalId, cloudResource -> cloudResource));

        // Iterate again and create the extra FederatedResources
        for (Map.Entry<String, Map<String, Boolean>> federationEntry : resourcesToBeShared.entrySet()) {
            String federationId = federationEntry.getKey();

            for (Map.Entry<String, Boolean> resourceEntry : federationEntry.getValue().entrySet()) {
                String internalId = resourceEntry.getKey();
                Boolean barteringStatus = resourceEntry.getValue();
                CloudResource cloudResource = cloudResourcesMap.get(internalId);

                // Continue if the cloudResource exists and if it is not already shared in the federation
                if (cloudResource != null) {
                    // Here, we do a trick. Instead of cloning the cloudResource, we just serialize it and deserialize it.
                    // So, every time we deserialize a new object (cloned) is created. We did that to avoid the cumbersome
                    // implementation of the clone() in every Resource and Resource field (e.g. StationarySensor, MobileSensor,
                    // Location, ...)

                    String serializedResource = serializeResource(cloudResource.getResource());
                    Resource newResource = deserializeResource(serializedResource);

                    if (newResource != null) {
                        ResourceSharingInformation sharingInformation;

                        if (!cloudResource.getFederationInfo().containsKey(federationId)) {
                            sharingInformation= new ResourceSharingInformation();
                            sharingInformation.setBartering(barteringStatus);
                            sharingInformation.setSymbioteId(createNewResourceId(nextId, federationId));
                            nextId++;
                        } else
                            sharingInformation = cloudResource.getFederationInfo().get(federationId);


                        // Create the new FederatedResource
                        resourcesToSave.add(new FederatedResource(
                                newResource,
                                sharingInformation.getSymbioteId(),
                                federationId,
                                sharingInformation.getBartering() != null ? sharingInformation.getBartering() : false));

                        // Update the cloudResource
                        cloudResource.getFederationInfo().put(federationId, sharingInformation);
                    }


                }
            }
        }

        // Create CloudResource list
        List<CloudResource> cloudResources = new ArrayList<>(cloudResourcesMap.values());

        resourceRepository.save(resourcesToSave);
        cloudResourceRepository.save(cloudResources);
        idSequence.setValue(nextId);
        persistentVariableRepository.save(idSequence);

        // Inform Subscription Manager for the new resources
        if (resourcesToSave.size() > 0)
            rabbitTemplate.convertAndSend(subscriptionManagerExchange, smAddOrUpdateFederatedResourcesKey,
                    new ResourcesAddedOrUpdatedMessage(resourcesToSave));

        return cloudResources;
    }


    /**
     * Unshares resources from federations
     *
     * @param resourcesToBeUnshared a map with key the federationId and value the list of internalIds to be unshared
     * @return a list of the updated CloudResources
     */
    public List<CloudResource>  unshareResources(Map<String, List<String>> resourcesToBeUnshared) {

        List<String> federatedIdsToRemove = new ArrayList<>();

        // First find all the resources that are going to be unshared
        Set<String> cloudResourcesIds = new HashSet<>();
        for (Map.Entry<String, List<String>> federationEntry : resourcesToBeUnshared.entrySet())
            cloudResourcesIds.addAll(federationEntry.getValue());

        // Fetch the cloudResources from the database and convert it to a map
        Map<String, CloudResource> cloudResourcesMap = cloudResourceRepository.findAllByInternalIdIn(cloudResourcesIds).stream()
                .collect(Collectors.toMap(CloudResource::getInternalId, cloudResource -> cloudResource));

        // Iterate again and remove the federatedResources
        for (Map.Entry<String, List<String>> federationEntry : resourcesToBeUnshared.entrySet()) {
            String federationId = federationEntry.getKey();

            for (String internalId : federationEntry.getValue()) {
                CloudResource cloudResource = cloudResourcesMap.get(internalId);

                if (cloudResource != null) {
                    // Remove it from the cloudResource
                    ResourceSharingInformation sharingInfo = cloudResource.getFederationInfo().remove(federationId);

                    // Add it to the remove list
                    if (sharingInfo != null)
                        federatedIdsToRemove.add(sharingInfo.getSymbioteId());
                }

            }
        }

        // Create CloudResource list
        List<CloudResource> cloudResources = new ArrayList<>(cloudResourcesMap.values());

        cloudResourceRepository.save(cloudResources);
        resourceRepository.deleteAllByIdIn(federatedIdsToRemove);

        // Inform Subscription Manager for the removed resources
        rabbitTemplate.convertAndSend(subscriptionManagerExchange, smRemoveFederatedResourcesKey,
                new ResourcesDeletedMessage(federatedIdsToRemove));

        return cloudResources;
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

        if (resourcesAddedOrUpdated.getNewFederatedResources() != null)
            resourceRepository.save(resourcesAddedOrUpdated.getNewFederatedResources());
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

        if (resourcesDeleted.getDeletedIds() != null)
            resourceRepository.deleteAllByIdIn(resourcesDeleted.getDeletedIds());
    }

    private String serializeResource(Resource resource) {
        String string;

        try {
            string = mapper.writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            log.info("Problem in serializing the resource", e);
            return null;
        }
        return string;
    }

    private Resource deserializeResource(String s) {

        Resource resource;

        try {
            resource = mapper.readValue(s, Resource.class);
        } catch (IOException e) {
            log.info("Problem in deserializing the resource", e);
            return null;
        }
        return resource;
    }


    private List<String> findResourcesToBeRemoved(List<CloudResource> cloudResources,
                                                  List<FederatedResource> resourcesToBeStored) {

        List<CloudResource> storedResources = cloudResourceRepository.findAllByInternalIdIn(
                cloudResources.stream().map(CloudResource::getInternalId).collect(Collectors.toList())
        );

        List<String> storedResourceIds = new ArrayList<>();

        for (CloudResource cloudResource : storedResources) {
            for (Map.Entry<String, ResourceSharingInformation> entry : cloudResource.getFederationInfo().entrySet())
                storedResourceIds.add(entry.getValue().getSymbioteId());
        }

        return resourcesToBeStored.stream()
                .filter(federatedResource -> !storedResourceIds.contains(federatedResource.getId()))
                .map(FederatedResource::getId)
                .collect(Collectors.toList());
    }

    private String createNewResourceId(long id, String federationId) {
        return String.format("%0" + Long.BYTES * 2 + "x@%s@%s", id, platformId, federationId);
    }
}
