package eu.h2020.symbiote.pr.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.cloud.model.internal.*;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.pr.model.PersistentVariable;
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
    private RabbitTemplate rabbitTemplate;
    private PersistentVariableRepository persistentVariableRepository;
    private PersistentVariable idSequence;
    private ObjectMapper mapper = new ObjectMapper();
    private String platformId;
    private String subscriptionManagerExchange;
    private String smAddOrUpdateResourcesKey;
    private String smRemoveResourcesKey;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository,
                           RabbitTemplate rabbitTemplate,
                           PersistentVariableRepository persistentVariableRepository,
                           PersistentVariable idSequence,
                           @Value("${platform.id}") String platformId,
                           @Value("${rabbit.exchange.subscriptionManager.name}") String subscriptionManagerExchange,
                           @Value("${rabbit.routingKey.subscriptionManager.addOrUpdateResources}") String smAddOrUpdateResourcesKey,
                           @Value("${rabbit.routingKey.subscriptionManager.removeResources}") String smRemoveResourcesKey) {
        this.resourceRepository = resourceRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.persistentVariableRepository = persistentVariableRepository;
        this.idSequence = idSequence;

        Assert.notNull(platformId, "The platformId should not be null");
        this.platformId = platformId;

        Assert.notNull(subscriptionManagerExchange, "The subscriptionManagerExchange should not be null");
        this.subscriptionManagerExchange = subscriptionManagerExchange;

        Assert.notNull(smAddOrUpdateResourcesKey, "The smAddOrUpdateResourcesKey should not be null");
        this.smAddOrUpdateResourcesKey = smAddOrUpdateResourcesKey;

        Assert.notNull(smRemoveResourcesKey, "The smRemoveResourcesKey should not be null");
        this.smRemoveResourcesKey = smRemoveResourcesKey;
    }

    /**
     * Stores the federated resources offered by the platform
     * @param federatedCloudResources registration message sent by Registration Handler
     * @return response to Registration Handler containing the federationIds of the resources
     */
    public Map<String, Map<String, String>> savePlatformResources(List<FederatedCloudResource> federatedCloudResources) {
        log.trace("savePlatformResources: " + ReflectionToStringBuilder.toString(federatedCloudResources));

        List<FederatedResource> resourcesToSave = new LinkedList<>();
        Map<String, Map<String, String>> internalIdResourceIdMap = new HashMap<>();

        long id = (Long) idSequence.getValue();
        for (FederatedCloudResource federatedCloudResource : federatedCloudResources) {
            // Here, we do a trick. Instead of cloning the federatedCloudResource, we just serialize it and deserialize it.
            // So, every time we deserialize a new object (cloned) is created. We did that to avoid the cumbersome
            // implementation of the clone() in every Resource and Resource field (e.g. StationarySensor, MobileSensor,
            // Location, ...)

            String serializedResource = serializeResource(federatedCloudResource.getResource());
            Map<String, String> federationResourceIdMap = new HashMap<>();

            for (Map.Entry<String,  Boolean> entry : federatedCloudResource.getFederationBarteredResourceMap().entrySet()) {
                Resource newResource = deserializeResource(serializedResource);
                String federationId = entry.getKey();
                Boolean isBartered = entry.getValue();

                if (newResource != null) {
                    newResource.setId(createNewResourceId(id));
                    newResource.setFederationId(federationId);
                    newResource.setBartered(isBartered);
                    resourcesToSave.add(new FederatedResource(newResource));
                    federationResourceIdMap.put(federationId, newResource.getId());
                    id++;
                }
            }

            if (federationResourceIdMap.size() > 0)
                internalIdResourceIdMap.put(federatedCloudResource.getInternalId(), federationResourceIdMap);
        }

        resourceRepository.save(resourcesToSave);
        idSequence.setValue(id);
        persistentVariableRepository.save(idSequence);

        // Inform Subscription Manager for the new resources
        rabbitTemplate.convertAndSend(subscriptionManagerExchange, smAddOrUpdateResourcesKey,
                new ResourcesAddedOrUpdatedMessage(resourcesToSave));

        return internalIdResourceIdMap;
    }

    /**
     * Update the federated resources offered by the platform
     * @param cloudResources update message sent by Registration Handler
     * @return response to Registration Handler containing the federatedIds of the updated resources
     */
    public List<String> updatePlatformResources(List<CloudResource> cloudResources) {
        log.trace("updatePlatformResources: " + ReflectionToStringBuilder.toString(cloudResources));

        // We filter out any resources with id == null
        List<String> validResourceIds = new LinkedList<>();
        for (CloudResource cloudResource : cloudResources) {
            if (cloudResource.getResource().getId() != null)
                validResourceIds.add(cloudResource.getResource().getId());
        }

        // Then, we find out which of these resources actually exist
        List<String> existingResourceIds = resourceRepository.findAllByIdIn(validResourceIds).stream()
                .map(FederatedResource::getId)
                .collect(Collectors.toList());

        List<FederatedResource> bill = resourceRepository.findAll();
        List<FederatedResource> bill2 = resourceRepository.findAllByIdIn(validResourceIds);

        // We keep only the existing resources. Only, these will be updated
        List<CloudResource> existingResources = cloudResources.stream()
                .filter(resource -> existingResourceIds.contains(resource.getResource().getId()))
                .collect(Collectors.toList());

        // Create the list of federatedResources in order to update the database
        List<FederatedResource> resourcesToUpdate = existingResources.stream()
                .map(resource -> new FederatedResource(resource.getResource()))
                .collect(Collectors.toList());

        resourceRepository.save(resourcesToUpdate);

        // Inform Subscription Manager for the updated resources
        rabbitTemplate.convertAndSend(subscriptionManagerExchange, smAddOrUpdateResourcesKey,
                new ResourcesAddedOrUpdatedMessage(resourcesToUpdate));

        // We return only the resources which were updated
        return existingResourceIds;
    }

    /**
     * Remove federated resources offered by the platform by using their federationIds
     * @param resourceIds the ids of the federated platform resources to be removed
     * @return the list of the removed resources
     */
    public List<String> removePlatformResources(List<String> resourceIds) {
        log.trace("removeResources: " + ReflectionToStringBuilder.toString(resourceIds));

        // Todo: maybe check if these are platform resources
        List<String> resourcesRemoved = resourceIds != null ?
                resourceRepository.deleteAllByIdIn(resourceIds)
                        .stream().map(resource -> resource.getResource().getId()).collect(Collectors.toList()) :
                new ArrayList<>();

        // Inform Subscription Manager for the removed resources
        rabbitTemplate.convertAndSend(subscriptionManagerExchange, smRemoveResourcesKey,
                new ResourcesDeletedMessage(resourcesRemoved));

        return resourcesRemoved;
    }

    public void addOrUpdateFederationResources(ResourcesAddedOrUpdatedMessage resourcesAddedOrUpdated) {
        log.trace("addOrUpdateFederationResources: " + ReflectionToStringBuilder.toString(resourcesAddedOrUpdated));

        // Todo: maybe remove the platform resources from the message.
        // Platform resources should not be present here. Only, federated resources offered by other platforms should be
        // in the NewResourceMessage

        if (resourcesAddedOrUpdated.getNewFederatedResources() != null)
            resourceRepository.save(resourcesAddedOrUpdated.getNewFederatedResources());
    }

    public void removeFederationResources(ResourcesDeletedMessage resourcesDeleted) {
        log.trace("removeFederationResources: " + ReflectionToStringBuilder.toString(resourcesDeleted));

        // Todo: maybe remove the platform resources from the message.
        // Platform resources should not be present here. Only, federated resources offered by other platforms should be
        // in the ResourcesDeletedMessage

        if (resourcesDeleted.getDeletedIds() != null)
            resourceRepository.deleteAllByIdIn(resourcesDeleted.getDeletedIds());
    }

    private String serializeResource(Resource resource) {
        String string = null;

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

    private String createNewResourceId(long id) {
        return String.format("%0" + Long.BYTES * 2 + "x@%s", id, platformId);
    }
}
