package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederatedCloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.model.cim.MobileSensor;
import eu.h2020.symbiote.model.cim.StationarySensor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
public class RegistrationHandlerListenerTests extends PlatformRegistryBaseTestClass {

    private static Log log = LogFactory.getLog(RegistrationHandlerListenerTests.class);

    @Test
    public void registerResourcesTest() throws InterruptedException {

        List<FederatedCloudResource> federatedCloudResources = createFederatedCloudResourceMessage();
        Map<String, Map<String,String>> result = (Map<String, Map<String,String>>) rabbitTemplate
                .convertSendAndReceive(platformRegistryExchange, registrationRequestKey, federatedCloudResources);

        // Testing the RegistrationHandlerListener response
        assertEquals(2, result.get("sensor1InternalId").size());
        assertEquals(1, result.get("sensor2InternalId").size());

        // Checking what is stored in the database
        String expectedResourceId1 = createNewResourceId(0);
        String expectedResourceId2 = createNewResourceId(1);
        String expectedResourceId3 = createNewResourceId(2);

        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(3, stored.size());

        FederatedResource resource1 = resourceRepository.findOne(expectedResourceId1);
        assertTrue(resource1.getResource() instanceof StationarySensor);

        FederatedResource resource2 = resourceRepository.findOne(expectedResourceId2);
        assertTrue(resource2.getResource() instanceof StationarySensor);
        assertTrue(resource1.getBartered() != resource2.getBartered());
        assertEquals("stationarySensor", resource1.getResource().getName());
        assertEquals(resource1.getResource().getName(), resource2.getResource().getName());

        FederatedResource resource3 = resourceRepository.findOne(expectedResourceId3);
        assertTrue(resource3.getResource() instanceof MobileSensor);
        assertTrue(resource3.getBartered());
        assertEquals("mobileSensor", resource3.getResource().getName());

        // Check what dummySubscriptionManagerListener received
        while (dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size() == 0)
            TimeUnit.MILLISECONDS.sleep(100);

        assertEquals(1, dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size());
        List<FederatedResource> message = dummySubscriptionManagerListener
                .getResourcesAddedOrUpdatedMessages().get(0).getNewFederatedResources();

        assertEquals(3, message.size());
        assertEquals(expectedResourceId1, message.get(0).getId());
        assertEquals(expectedResourceId2, message.get(1).getId());
        assertEquals(expectedResourceId3, message.get(2).getId());
    }

    // @Test
    public void updateResourcesTest() throws InterruptedException {

        List<FederatedResource> federatedResources = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResources);

        List<CloudResource> cloudResources = createTestCloudResources(platformId);
        // Clear the id of the 1st resource
        cloudResources.get(0).getResource().setId(null);
        // Put non-existent id in the 3rd resource
        cloudResources.get(2).getResource().setId(createNewResourceId(1000, platformId));

        // Change the name of all resources to "newName"
        String newName = "newName";
        cloudResources = cloudResources.stream()
                .map(cloudResource -> {
                    cloudResource.getResource().setName(newName);
                    return cloudResource;
                })
                .collect(Collectors.toList());

        List<String> updateResult = (List<String>) rabbitTemplate
                .convertSendAndReceive(platformRegistryExchange, updateRequestKey, cloudResources);

        // Only the 2nd resource should be updated
        assertEquals(1, updateResult.size());
        assertEquals(federatedResources.get(1).getId(), updateResult.get(0));

        // Check what is store in the database
        FederatedResource stationarySensor = resourceRepository.findOne(federatedResources.get(0).getId());
        FederatedResource mobileSensor = resourceRepository.findOne(federatedResources.get(1).getId());
        FederatedResource service = resourceRepository.findOne(federatedResources.get(2).getId());

        assertFalse(stationarySensor.getResource().getName().equals(newName));
        assertTrue(mobileSensor.getResource().getName().equals(newName));
        assertFalse(service.getResource().getName().equals(newName));

        // Check what dummySubscriptionManagerListener received
        while (dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size() == 0)
            TimeUnit.MILLISECONDS.sleep(100);

        String expectedResourceId = federatedResources.get(1).getResource().getId();

        assertEquals(1, dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size());
        List<FederatedResource> message = dummySubscriptionManagerListener
                .getResourcesAddedOrUpdatedMessages().get(0).getNewFederatedResources();

        assertEquals(1, message.size());
        assertEquals(expectedResourceId, message.get(0).getId());
    }

    @Test
    public void removeResourcesTest() throws InterruptedException {

        List<FederatedResource> federatedResources = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResources);

        // We delete the 1st and 3rd resource
        List<String> resourceIds = new ArrayList<>();
        resourceIds.add(federatedResources.get(0).getId());
        resourceIds.add(federatedResources.get(2).getId());

        List<String> removalResult = (List<String>) rabbitTemplate
                .convertSendAndReceive(platformRegistryExchange, removalRequestKey, resourceIds);

        // Check what is stored in the database
        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(1, stored.size());
        assertEquals(federatedResources.get(1).getId(), stored.get(0).getResource().getId());
        assertEquals(2, removalResult.size());
        assertTrue(removalResult.containsAll(resourceIds));

        // Check what dummySubscriptionManagerListener received
        while (dummySubscriptionManagerListener.getResourcesDeletedMessages().size() == 0)
            TimeUnit.MILLISECONDS.sleep(100);

        String expectedResourceId1 = federatedResources.get(0).getResource().getId();
        String expectedResourceId2 = federatedResources.get(2).getResource().getId();

        assertEquals(1, dummySubscriptionManagerListener.getResourcesDeletedMessages().size());
        List<String> message = dummySubscriptionManagerListener
                .getResourcesDeletedMessages().get(0).getDeletedIds();

        assertEquals(2, message.size());
        assertEquals(expectedResourceId1, message.get(0));
        assertEquals(expectedResourceId2, message.get(1));
    }
}
