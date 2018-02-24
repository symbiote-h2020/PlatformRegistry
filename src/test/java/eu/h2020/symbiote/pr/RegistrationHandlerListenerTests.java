package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederatedCloudResource;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.MobileSensor;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.pr.model.FederatedResource;
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
    public void registerResourcesTest() {

        List<FederatedCloudResource> federatedCloudResources = createFederatedCloudResourceMessage();
        Map<String, Map<String,String>> result = (Map<String, Map<String,String>>) rabbitTemplate
                .convertSendAndReceive(platformRegistryExchange, rhRegistrationRequestKey, federatedCloudResources);

        // Testing the RegistrationHandlerListener response
        assertEquals(2, result.get("sensor1InternalId").size());
        assertEquals(1, result.get("sensor2InternalId").size());

        // Checking what is stored in the database
        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(3, stored.size());

        // Get the current sequenceId and remove the number of the registered federatedCloudResources to get the first
        // id of the newly created resources. That is because we created new resources by calling createFederatedCloudResourceMessage.
        // We do that because idSequence is used by previous tests too, so its initial value might not be 0
        long initialId = (Long) idSequence.getValue() - stored.size();

        Resource resource1 = resourceRepository.findOne(createNewResourceId(initialId)).getResource();
        assertTrue(resource1 instanceof StationarySensor);

        Resource resource2 = resourceRepository.findOne(createNewResourceId(initialId + 1)).getResource();
        assertTrue(resource2 instanceof StationarySensor);
        assertTrue(resource1.getBartered() != resource2.getBartered());
        assertEquals("stationarySensor", resource1.getName());
        assertEquals(resource1.getName(), resource2.getName());

        Resource resource3 = resourceRepository.findOne(createNewResourceId(initialId + 2)).getResource();
        assertTrue(resource3 instanceof MobileSensor);
        assertTrue(resource3.getBartered());
        assertEquals("mobileSensor", resource3.getName());
    }

    @Test
    public void updateResourcesTest() {

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
                .convertSendAndReceive(platformRegistryExchange, rhUpdateRequestKey, cloudResources);

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
    }

    @Test
    public void removeResourcesTest() throws InterruptedException {

        List<FederatedResource> federatedResources = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResources);

        List<String> resourceIds = new ArrayList<>();
        resourceIds.add(federatedResources.get(0).getId());
        resourceIds.add(federatedResources.get(2).getId());

        List<String> removalResult = (List<String>) rabbitTemplate
                .convertSendAndReceive(platformRegistryExchange, rhRemovalRequestKey, resourceIds);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(1, stored.size());
        assertEquals(federatedResources.get(1).getId(), stored.get(0).getResource().getId());
        assertEquals(2, removalResult.size());
        assertTrue(removalResult.containsAll(resourceIds));
    }
}
