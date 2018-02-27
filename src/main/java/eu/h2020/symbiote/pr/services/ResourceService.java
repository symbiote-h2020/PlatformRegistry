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
    private String smaddOrUpdateFederatedResourcesKey;
    private String smremoveFederatedResourcesKey;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository,
                           CloudResourceRepository cloudResourceRepository,
                           RabbitTemplate rabbitTemplate,
                           PersistentVariableRepository persistentVariableRepository,
                           PersistentVariable idSequence,
                           @Value("${platform.id}") String platformId,
                           @Value("${rabbit.exchange.subscriptionManager.name}") String subscriptionManagerExchange,
                           @Value("${rabbit.routingKey.subscriptionManager.addOrUpdateFederatedResources}") String smaddOrUpdateFederatedResourcesKey,
                           @Value("${rabbit.routingKey.subscriptionManager.removeFederatedResources}") String smremoveFederatedResourcesKey) {
        this.resourceRepository = resourceRepository;
        this.cloudResourceRepository = cloudResourceRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.persistentVariableRepository = persistentVariableRepository;
        this.idSequence = idSequence;

        Assert.notNull(platformId, "The platformId should not be null");
        this.platformId = platformId;

        Assert.notNull(subscriptionManagerExchange, "The subscriptionManagerExchange should not be null");
        this.subscriptionManagerExchange = subscriptionManagerExchange;

        Assert.notNull(smaddOrUpdateFederatedResourcesKey, "The smaddOrUpdateFederatedResourcesKey should not be null");
        this.smaddOrUpdateFederatedResourcesKey = smaddOrUpdateFederatedResourcesKey;

        Assert.notNull(smremoveFederatedResourcesKey, "The smremoveFederatedResourcesKey should not be null");
        this.smremoveFederatedResourcesKey = smremoveFederatedResourcesKey;
    }

    /**
     * Stores the federated resources offered by the platform
     *
     * @param cloudResources registration message sent by Registration Handler
     * @return a list of the new/updated CloudResources
     */
    public List<CloudResource> savePlatformResources(List<CloudResource> cloudResources) {
        log.trace("savePlatformResources: " + ReflectionToStringBuilder.toString(cloudResources));

        List<FederatedResource> resourcesToSave = new LinkedList<>();

        long nextId = (Long) idSequence.getValue();
        for (CloudResource cloudResource : cloudResources) {
            // Here, we do a trick. Instead of cloning the cloudResource, we just serialize it and deserialize it.
            // So, every time we deserialize a new object (cloned) is created. We did that to avoid the cumbersome
            // implementation of the clone() in every Resource and Resource field (e.g. StationarySensor, MobileSensor,
            // Location, ...)

            String serializedResource = serializeResource(cloudResource.getResource());

            for (Map.Entry<String,  ResourceSharingInformation> entry : cloudResource.getFederationInfo().entrySet()) {
                Resource newResource = deserializeResource(serializedResource);
                String federationId = entry.getKey();
                ResourceSharingInformation sharingInformation = entry.getValue();

                if (newResource != null) {
                    if (sharingInformation.getSymbioteId() == null) {
                        String newFederatedId = createNewResourceId(nextId);
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
            rabbitTemplate.convertAndSend(subscriptionManagerExchange, smaddOrUpdateFederatedResourcesKey,
                    new ResourcesAddedOrUpdatedMessage(resourcesToSave));

        // Find the resources that should be removed
        List<String> resourcesToBeRemoved = findResourcesToBeRemoved(cloudResources, resourcesToSave);

        if (resourcesToBeRemoved.size() > 0)
            rabbitTemplate.convertAndSend(subscriptionManagerExchange, smremoveFederatedResourcesKey,
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
        log.trace("removeFederatedResources: " + ReflectionToStringBuilder.toString(internalIds));

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
        rabbitTemplate.convertAndSend(subscriptionManagerExchange, smremoveFederatedResourcesKey,
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
        return new ArrayList<>();
    }


    /**
     * Unshares resources from federations
     *
     * @param resourcesToBeUnshared a map with key the federationId and value the list of internalIds to be unshared
     * @return a list of the updated CloudResources
     */
    public List<CloudResource>  unshareResources(Map<String, List<String>> resourcesToBeUnshared) {
        return new ArrayList<>();
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

    private String createNewResourceId(long id) {
        return String.format("%0" + Long.BYTES * 2 + "x@%s", id, platformId);
    }
}
