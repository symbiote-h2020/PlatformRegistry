package eu.h2020.symbiote.pr;

import com.fasterxml.jackson.databind.type.CollectionType;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.ResourceSharingInformation;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.model.cim.StationarySensor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
public class RegistrationHandlerListenerTests extends PlatformRegistryBaseTestClass {

    private static Log log = LogFactory.getLog(RegistrationHandlerListenerTests.class);

    @Test
    public void registerResourcesTest() throws InterruptedException {

        // Register resources
        List<CloudResource> cloudResources = createTestCloudResources();
        long initialId = (Long) idSequence.getValue();

        List<CloudResource> result = null;
        try {
            String jsonArray = mapper.writeValueAsString(rabbitTemplate
                    .convertSendAndReceive(platformRegistryExchange, addOrUpdateRequestKey, cloudResources));
            CollectionType javaType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, CloudResource.class);
            result = mapper.readValue(jsonArray, javaType);
        } catch (IOException e) {
            log.info("Problem deserializing registration request", e);
        }

        String expectedResourceId1 = result.get(0).getFederationInfo().get(federation1).getSymbioteId();
        String expectedResourceId2 = result.get(0).getFederationInfo().get(federation2).getSymbioteId();
        String expectedResourceId3 = result.get(1).getFederationInfo().get(federation1).getSymbioteId();
        String expectedResourceId4 = result.get(2).getFederationInfo().get(federation1).getSymbioteId();

        // Testing the RegistrationHandlerListener response
        assertEquals(2, result.get(0).getFederationInfo().size());
        assertEquals(1, result.get(1).getFederationInfo().size());
        assertEquals(1, result.get(2).getFederationInfo().size());

        // Checking what is stored in the database
        List<CloudResource> storedCloudResources = cloudResourceRepository.findAll();
        assertEquals(3, storedCloudResources.size());

        List<FederatedResource> storedFederatedResources = resourceRepository.findAll();
        assertEquals(4, storedFederatedResources.size());

        FederatedResource resource1 = resourceRepository.findOne(expectedResourceId1);
        assertTrue(resource1.getResource() instanceof StationarySensor);

        FederatedResource resource2 = resourceRepository.findOne(expectedResourceId2);
        assertTrue(resource2.getResource() instanceof StationarySensor);
        assertTrue(resource1.getBartered() != resource2.getBartered());
        assertEquals("stationarySensor", resource1.getResource().getName());
        assertEquals(resource1.getResource().getName(), resource2.getResource().getName());

        FederatedResource resource3 = resourceRepository.findOne(expectedResourceId3);
        assertTrue(resource3.getResource() instanceof Actuator);
        assertTrue(resource3.getBartered());
        assertEquals("actuator", resource3.getResource().getName());

        FederatedResource resource4 = resourceRepository.findOne(expectedResourceId4);
        assertTrue(resource4.getResource() instanceof Service);
        assertTrue(resource4.getBartered());
        assertEquals("service", resource4.getResource().getName());

        // Check what dummySubscriptionManagerListener received
        while (dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size() == 0)
            TimeUnit.MILLISECONDS.sleep(100);

        assertEquals(1, dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size());
        assertEquals(0, dummySubscriptionManagerListener.getResourcesDeletedMessages().size());
        List<FederatedResource> message = dummySubscriptionManagerListener
                .getResourcesAddedOrUpdatedMessages().get(0).getNewFederatedResources();

        assertEquals(4, message.size());
        assertTrue(message.stream().map(FederatedResource::getId).collect(Collectors.toList()).containsAll(
                Arrays.asList(expectedResourceId1, expectedResourceId2, expectedResourceId3, expectedResourceId4)
        ));
    }

    @Test
    public void updateResourcesTest() throws InterruptedException {

        // Register resources
        List<CloudResource> cloudResources = createTestCloudResources();

        List<CloudResource> registrationResult = null;
        try {
            String jsonArray = mapper.writeValueAsString(rabbitTemplate
                    .convertSendAndReceive(platformRegistryExchange, addOrUpdateRequestKey, cloudResources));
            CollectionType javaType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, CloudResource.class);
            registrationResult = mapper.readValue(jsonArray, javaType);
        } catch (IOException e) {
            log.info("Problem deserializing registration request", e);
        }


        String stationarySensorId1 = registrationResult.get(0).getFederationInfo().get(federation1).getSymbioteId();
        String stationarySensorId2 = registrationResult.get(0).getFederationInfo().get(federation2).getSymbioteId();
        String actuatorId1 = registrationResult.get(1).getFederationInfo().get(federation1).getSymbioteId();
        String serviceId = registrationResult.get(2).getFederationInfo().get(federation1).getSymbioteId();

        // We change the name of the stationary sensor
        String newStationarySensorName = "newStationarySensorName";
        registrationResult.get(0).getResource().setName(newStationarySensorName);

        // Expose the mobile sensor in the federation2 too
        // We do not fill the bartering info, so it will get the default value "false"
        registrationResult.get(1).getFederationInfo().put(federation2, new ResourceSharingInformation());

        // Send update message
        List<CloudResource> updateResult = null;
        try {
            String jsonArray = mapper.writeValueAsString(rabbitTemplate
                    .convertSendAndReceive(platformRegistryExchange, addOrUpdateRequestKey, registrationResult));
            CollectionType javaType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, CloudResource.class);
            updateResult = mapper.readValue(jsonArray, javaType);
        } catch (IOException e) {
            log.info("Problem deserializing update request", e);
        }


        String actuatorId2 = updateResult.get(1).getFederationInfo().get(federation2).getSymbioteId();

        // Check what is store in the database
        assertEquals(3, cloudResourceRepository.findAll().size());
        assertEquals(5, resourceRepository.findAll().size());

        FederatedResource stationarySensor1 = resourceRepository.findOne(stationarySensorId1);
        FederatedResource stationarySensor2 = resourceRepository.findOne(stationarySensorId2);
        FederatedResource actuator1 = resourceRepository.findOne(actuatorId1);
        FederatedResource actuator2 = resourceRepository.findOne(actuatorId2);
        FederatedResource service = resourceRepository.findOne(serviceId);

        assertEquals(newStationarySensorName, stationarySensor1.getResource().getName());
        assertEquals(newStationarySensorName, stationarySensor2.getResource().getName());
        assertEquals("actuator", actuator1.getResource().getName());
        assertEquals("actuator", actuator2.getResource().getName());
        assertEquals("service", service.getResource().getName());

        // Check what dummySubscriptionManagerListener received
        while (dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size() < 2)
            TimeUnit.MILLISECONDS.sleep(100);


        assertEquals(2, dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size());
        List<FederatedResource> message = dummySubscriptionManagerListener
                .getResourcesAddedOrUpdatedMessages().get(1).getNewFederatedResources();

        assertEquals(5, message.size());
        assertTrue(message.stream().map(FederatedResource::getId).collect(Collectors.toList()).containsAll(
                Arrays.asList(stationarySensorId1, stationarySensorId2, actuatorId1, actuatorId2, serviceId)
        ));
    }

    @Test
    public void removeFederatedResourcesTest() throws InterruptedException {

        // Register resources
        List<CloudResource> cloudResources = createTestCloudResources();
        List<CloudResource> registrationResult = null;
        try {
            String jsonArray = mapper.writeValueAsString(rabbitTemplate
                    .convertSendAndReceive(platformRegistryExchange, addOrUpdateRequestKey, cloudResources));
            CollectionType javaType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, CloudResource.class);
            registrationResult = mapper.readValue(jsonArray, javaType);
        } catch (IOException e) {
            log.info("Problem deserializing registration request", e);
        }

        // We delete the 1st and 3rd resource
        List<String> internalIdsToBeRemoved = new ArrayList<>();
        internalIdsToBeRemoved.add(cloudResources.get(0).getInternalId());
        internalIdsToBeRemoved.add(cloudResources.get(2).getInternalId());

        List<String> removalResult = (List<String>) rabbitTemplate
                .convertSendAndReceive(platformRegistryExchange, removalRequestKey, internalIdsToBeRemoved);

        assertEquals(2, removalResult.size());
        assertTrue(removalResult.containsAll(internalIdsToBeRemoved));

        // Check what is stored in the database
        List<CloudResource> storedCloudResources = cloudResourceRepository.findAll();
        assertEquals(1, storedCloudResources.size());
        assertEquals(cloudResources.get(1).getInternalId(), storedCloudResources.get(0).getInternalId());
        
        List<FederatedResource> storedFederatedResources = resourceRepository.findAll();
        assertEquals(1, storedFederatedResources.size());
        assertEquals(registrationResult.get(1).getFederationInfo().get(federation1).getSymbioteId(),
                storedFederatedResources.get(0).getId());

        // Check what dummySubscriptionManagerListener received
        while (dummySubscriptionManagerListener.getResourcesDeletedMessages().size() == 0)
            TimeUnit.MILLISECONDS.sleep(100);

        String expectedResourceId1 = registrationResult.get(0).getFederationInfo().get(federation1).getSymbioteId();
        String expectedResourceId2 = registrationResult.get(0).getFederationInfo().get(federation2).getSymbioteId();
        String expectedResourceId3 = registrationResult.get(0).getFederationInfo().get(federation1).getSymbioteId();

        assertEquals(1, dummySubscriptionManagerListener.getResourcesDeletedMessages().size());
        List<String> message = dummySubscriptionManagerListener
                .getResourcesDeletedMessages().get(0).getDeletedIds();

        assertEquals(3, message.size());
        assertTrue(message.containsAll(Arrays.asList(expectedResourceId1, expectedResourceId2, expectedResourceId3)));
    }
}
