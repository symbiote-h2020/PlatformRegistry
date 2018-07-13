package eu.h2020.symbiote.pr.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.cloud.model.internal.*;
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
public class RegistrationHandlerService {
    private static Log log = LogFactory.getLog(RegistrationHandlerService.class);

    private ResourceRepository resourceRepository;
    private RabbitTemplate rabbitTemplate;
    private String platformId;
    private String subscriptionManagerExchange;
    private String smAddOrUpdateFederatedResourcesKey;
    private String smRemoveFederatedResourcesKey;
    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public RegistrationHandlerService(ResourceRepository resourceRepository,
                                      RabbitTemplate rabbitTemplate,
                                      @Value("${platform.id}") String platformId,
                                      @Value("${rabbit.exchange.subscriptionManager.name}")
                                                  String subscriptionManagerExchange,
                                      @Value("${rabbit.routingKey.subscriptionManager.addOrUpdateFederatedResources}")
                                                  String smAddOrUpdateFederatedResourcesKey,
                                      @Value("${rabbit.routingKey.subscriptionManager.removeFederatedResources}")
                                                  String smRemoveFederatedResourcesKey) {

        this.resourceRepository = resourceRepository;
        this.rabbitTemplate = rabbitTemplate;

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

        for (CloudResource cloudResource : cloudResources) {

            // if the federationInfo of the cloudResource is null, initialize it with a valid symbiote id
            if (cloudResource.getFederationInfo() == null ||
                    cloudResource.getFederationInfo().getAggregationId() == null) {

                // If federationInfo is null or aggregationId is null that means we have a register operation
                // So, we create the federationInfo
                FederationInfoBean federationInfo = new FederationInfoBean();
                federationInfo.setAggregationId(createNewResourceId());

                //We update the sharingInformation in federationInfo
                if (cloudResource.getFederationInfo() != null && cloudResource.getFederationInfo().getSharingInformation() != null)
                    federationInfo.setSharingInformation(cloudResource.getFederationInfo().getSharingInformation());

                //to not overwrite the resource Trust value while updating the federation info with symbioteId
                if (cloudResource.getFederationInfo() != null && cloudResource.getFederationInfo().getResourceTrust() != null)
                    federationInfo.setResourceTrust(cloudResource.getFederationInfo().getResourceTrust());

                cloudResource.setFederationInfo(federationInfo);
            }

            String aggregationId = cloudResource.getFederationInfo().getAggregationId();

            // If barteringInfo == null, default it to false
            for (Map.Entry<String, ResourceSharingInformation> entry :
                    cloudResource.getFederationInfo().getSharingInformation().entrySet()) {
                entry.getValue().setSymbioteId(aggregationId + '@' + entry.getKey());
                if (entry.getValue().getBartering() == null)
                    entry.getValue().setBartering(false);
                if (entry.getValue().getSharingDate() == null)
                    entry.getValue().setSharingDate(new Date());
            }

            //resourceTrust is not required to be updated here
           // cloudResource.getFederationInfo().setResourceTrust(resourceTrust);

            resourcesToSave.add(new FederatedResource(cloudResource));//adaptiveTrust is null
            log.debug("FederatedResource " + cloudResource.getInternalId() + " is exposed to "
                    + cloudResource.getFederationInfo().getSharingInformation().keySet());
        }

        // Find the federations where the resources are no longer exposed to
        ResourcesDeletedMessage resourcesToBeRemoved = findResourcesToBeRemoved(cloudResources);

        if (resourcesToBeRemoved.getDeletedFederatedResources().size() > 0)
            rabbitTemplate.convertAndSend(subscriptionManagerExchange, smRemoveFederatedResourcesKey,
                    resourcesToBeRemoved);

        // Inform Subscription Manager for the new resources
        if (resourcesToSave.size() > 0)
            rabbitTemplate.convertAndSend(subscriptionManagerExchange, smAddOrUpdateFederatedResourcesKey,
                    new ResourcesAddedOrUpdatedMessage(resourcesToSave));

        resourceRepository.save(resourcesToSave);

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

        Set<String> internalIdsSet = new HashSet<>(internalIds);

        Set<String> federatedResourcesRemoved = new HashSet<>();

        List<FederatedResource> federatedResources = resourceRepository.findAllByCloudResource_InternalIdIn(internalIdsSet);

        for (FederatedResource federatedResource : federatedResources) {
            if (federatedResource.getCloudResource() != null &&
                    federatedResource.getCloudResource().getFederationInfo() != null &&
                    federatedResource.getCloudResource().getFederationInfo().getAggregationId() != null) {

                for(String fedId: federatedResource.getCloudResource().getFederationInfo().getSharingInformation().keySet())
                federatedResourcesRemoved.add(
                        federatedResource.getFederatedResourceInfoMap().get(fedId).getSymbioteId());
            }
        }

        List<String> internalIdsRemoved = resourceRepository.deleteAllByCloudResource_InternalIdIn(internalIdsSet).stream()
                .map(federatedResource -> federatedResource.getCloudResource().getInternalId())
                .collect(Collectors.toList());

        // Inform Subscription Manager for the removed resources
        rabbitTemplate.convertAndSend(subscriptionManagerExchange, smRemoveFederatedResourcesKey,
                new ResourcesDeletedMessage(federatedResourcesRemoved));

        return internalIdsRemoved;
    }


    /**
     * Shares resources in federations
     *
     * @param resourcesToBeShared a map with key the federationId and value another map, which has as key
     *                            the internalId of the resource and value the bartering status
     * @return a list of the updated CloudResources
     */
    public List<CloudResource> shareResources(Map<String, Map<String, Boolean>> resourcesToBeShared) {

        // A map of the federated resources to be saved. The key is the internal id
        Map<String, FederatedResource> resourcesToSave = new HashMap<>();

        // First find all the resources that are going to be shared
        Set<String> cloudResourcesIds = new HashSet<>();
        for (Map.Entry<String, Map<String, Boolean>> federationEntry : resourcesToBeShared.entrySet())
            cloudResourcesIds.addAll(federationEntry.getValue().keySet());

        // Fetch the federated resources from the database and convert them to a map. The key is the internalId
        Map<String, FederatedResource> storedFederatedResources = resourceRepository
                .findAllByCloudResource_InternalIdIn(cloudResourcesIds).stream()
                .collect(Collectors.toMap(resource -> resource.getCloudResource().getInternalId(), resource -> resource));

        // Iterate and update the federatedResources
        for (Map.Entry<String, Map<String, Boolean>> federationEntry : resourcesToBeShared.entrySet()) {
            String federationId = federationEntry.getKey();

            for (Map.Entry<String, Boolean> resourceEntry : federationEntry.getValue().entrySet()) {
                String internalId = resourceEntry.getKey();
                Boolean barteringStatus = resourceEntry.getValue();
                Date sharingDate = new Date();

                log.debug("Resource " + internalId + " will be shared to federation " + federationId);

                // This contains all the federations where the resource is shared to
                FederatedResource storedFederatedResource = storedFederatedResources.get(internalId);

                // This contains only the newly added federations
                FederatedResource federatedResource = getFederatedResource(storedFederatedResource, resourcesToSave);
                if (federatedResource == null)
                    continue;

                // Add the new federation where the resource is shared to
                federatedResource.shareToNewFederation(federationId, barteringStatus, sharingDate);
                log.debug("SharingDate = " + federatedResource.getCloudResource().getFederationInfo()
                        .getSharingInformation().get(federationId).getSharingDate());

                // Update also this info to the storedFederation
                storedFederatedResource.shareToNewFederation(federationId, barteringStatus, sharingDate);

                resourcesToSave.put(internalId, federatedResource);

            }
        }

        // Inform Subscription Manager for the new resources
        if (resourcesToSave.size() > 0)
            rabbitTemplate.convertAndSend(subscriptionManagerExchange, smAddOrUpdateFederatedResourcesKey,
                    new ResourcesAddedOrUpdatedMessage(new ArrayList<>(resourcesToSave.values())));

        resourceRepository.save(new ArrayList<>(storedFederatedResources.values()));

        // We return the list of CloudResources from the storedFederatedResources
        return storedFederatedResources.values().stream().map(FederatedResource::getCloudResource).collect(Collectors.toList());
    }


    /**
     * Unshares resources from federations
     *
     * @param resourcesToBeUnshared a map with key the federationId and value the list of internalIds to be unshared
     * @return a list of the updated CloudResources
     */
    public List<CloudResource> unshareResources(Map<String, List<String>> resourcesToBeUnshared) {

        // A set of the federated resources to be saved. The key is the symbioteId
        Set<String> resourcesToBeRemoved = new HashSet<>();

        // First find all the resources that are going to be unshared
        Set<String> cloudResourcesIds = new HashSet<>();
        for (Map.Entry<String, List<String>> federationEntry : resourcesToBeUnshared.entrySet())
            cloudResourcesIds.addAll(federationEntry.getValue());

        // Fetch the federated resources from the database and convert them to a map. The key is the internalId
        Map<String, FederatedResource> storedFederatedResources = resourceRepository
                .findAllByCloudResource_InternalIdIn(cloudResourcesIds).stream()
                .collect(Collectors.toMap(resource -> resource.getCloudResource().getInternalId(), resource -> resource));

        // Iterate and update the federatedResources
        for (Map.Entry<String, List<String>> federationEntry : resourcesToBeUnshared.entrySet()) {
            String federationId = federationEntry.getKey();

            for (String internalId : federationEntry.getValue()) {

                // This contains all the federations where the resource is shared to
                FederatedResource storedFederatedResource = storedFederatedResources.get(internalId);

                if (storedFederatedResource != null) {
                    if(storedFederatedResource.getFederatedResourceInfoMap().containsKey(federationId))
                   resourcesToBeRemoved.add(storedFederatedResource.getFederatedResourceInfoMap().get(federationId).getSymbioteId());
                    // Update also this info to the storedFederation
                    storedFederatedResource.unshareFromFederation(federationId);
                }
            }
        }

        // Inform Subscription Manager for the removed resources
        rabbitTemplate.convertAndSend(subscriptionManagerExchange, smRemoveFederatedResourcesKey,
                new ResourcesDeletedMessage(resourcesToBeRemoved));

        resourceRepository.save(new ArrayList<>(storedFederatedResources.values()));

        // We return the list of CloudResources from the storedFederatedResources
        return storedFederatedResources.values().stream().map(FederatedResource::getCloudResource).collect(Collectors.toList());
    }


    private ResourcesDeletedMessage findResourcesToBeRemoved(List<CloudResource> cloudResources) {

        // Create a list of the federated resource ids which are stored in the database
        Set<String> storedFederatedResourceIds = cloudResources.stream()
                .map(cloudResource -> cloudResource.getFederationInfo().getAggregationId()).collect(Collectors.toSet());

        // Fetch the stored federatedResources
        List<FederatedResource> storedFederatedResources = resourceRepository.findAllByAggregationIdIn(storedFederatedResourceIds);

        // Create a map in of the updated CloudResources
        Map<String, CloudResource> updatedCloudResourcesMap = cloudResources.stream()
                .collect(Collectors.toMap(CloudResource::getInternalId, cloudResource -> cloudResource));

        // Create the message to be sent to Subscription Manager
        Set<String> removedFederatedResources = new HashSet<>();

        for (FederatedResource federatedResource : storedFederatedResources) {

            if (federatedResource.getCloudResource().getFederationInfo() == null
                    || federatedResource.getCloudResource().getFederationInfo().getSharingInformation() == null)
                continue;

            // A list of federations where the resource was previously exposed to
            Set<String> oldFederations = federatedResource.getCloudResource().getFederationInfo().getSharingInformation().keySet();


            // A list of the federations where the resource is currently exposed to
            CloudResource updatedCloudResource = updatedCloudResourcesMap.get(federatedResource.getCloudResource().getInternalId());

            Set<String> newFederations = updatedCloudResource.getFederationInfo() == null
                    || updatedCloudResource.getFederationInfo().getSharingInformation() == null ?
                    new HashSet<>() :
                    updatedCloudResource.getFederationInfo().getSharingInformation().keySet();

            // Find which federations were removed
            for (String id : oldFederations) {
                if (!newFederations.contains(id))
                    removedFederatedResources.add(federatedResource.getFederatedResourceInfoMap().get(id).getSymbioteId());
            }

        }

        return new ResourcesDeletedMessage(removedFederatedResources);
    }

    private String createNewResourceId() {

        //create a randomly generated long number
        long id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;

        //cast it to check if it already exists in the database
        Set<String> ids = new HashSet<>();
        ids.add(String.format("%0" + Long.BYTES * 2 +"x@%s", id, platformId));

        //in case of collision, create a new id until it is unique
        while(resourceRepository.findAllByAggregationIdIn(ids).size()>0) {
            id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
            ids.clear();
            ids.add(String.format( "%0" + Long.BYTES * 2 +"x@%s", id, platformId));
        }

        return String.format( "%0" + Long.BYTES * 2 +"x@%s", id, platformId);
    }

    private FederatedResource getFederatedResource(FederatedResource storedFederatedResource,
                                                   Map<String, FederatedResource> cachedResources) {

        // If not found in the stored federated resources either, then the resource has not been registered
        // for being shared, so just continue
        if (storedFederatedResource == null)
            return null;

        String internalId = storedFederatedResource.getCloudResource().getInternalId();

        // This contains only the newly added federations
        FederatedResource federatedResource = cachedResources.get(internalId);

        // If not found, fetch it from the stored federated resources
        if (federatedResource == null) {
            // Here, we do a trick. Instead of cloning the federatedResource, we just serialize it and deserialize it.
            // So, every time we deserialize a new object is created. We did that to avoid the cumbersome
            // implementation of the clone() in every Resource and Resource field (e.g. StationarySensor, MobileSensor,
            // Location, ...)
            federatedResource = deserializeFederatedResource(serializeFederatedResource(storedFederatedResource));
            if (federatedResource == null)
                return null;
        }

        // If found, clear any sensitive platform data
        federatedResource.clearPrivateInfo();
        return federatedResource;
    }

    private String serializeFederatedResource(FederatedResource federatedResource) {
        String string;

        try {
            string = mapper.writeValueAsString(federatedResource);
        } catch (JsonProcessingException e) {
            log.info("Problem in serializing the federatedResource", e);
            return null;
        }
        return string;
    }

    private FederatedResource deserializeFederatedResource(String s) {

        FederatedResource federatedResource;

        try {
            federatedResource = mapper.readValue(s, FederatedResource.class);
        } catch (IOException e) {
            log.info("Problem in deserializing the federatedResource", e);
            return null;
        }
        return federatedResource;
    }
}
