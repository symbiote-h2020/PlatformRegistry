package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.FederatedCloudResource;
import eu.h2020.symbiote.model.cim.MobileSensor;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.StationarySensor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        List<Resource> stored = resourceRepository.findAll();
        assertEquals(3, stored.size());

        // Get the current sequenceId and remove the number of the registered federatedCloudResources to get the first
        // id of the newly created resources. That is because we created new resources by calling createFederatedCloudResourceMessage.
        // We do that because idSequence is used by previous tests too, so its initial value might not be 0
        long initialId = (Long) idSequence.getValue() - stored.size();

        Resource resource1 = resourceRepository.findOne(createNewResourceId(initialId));
        assertTrue(resource1 instanceof StationarySensor);

        Resource resource2 = resourceRepository.findOne(createNewResourceId(initialId + 1));
        assertTrue(resource2 instanceof StationarySensor);
        assertTrue(resource1.getBartered() != resource2.getBartered());
        assertEquals("stationarySensor", resource1.getName());
        assertEquals(resource1.getName(), resource2.getName());

        Resource resource3 = resourceRepository.findOne(createNewResourceId(initialId + 2));
        assertTrue(resource3 instanceof MobileSensor);
        assertTrue(resource3.getBartered());
        assertEquals("mobileSensor", resource3.getName());
    }

    @Test
    public void removeResourceTest() throws InterruptedException{

        List<FederatedCloudResource> federatedCloudResources = createFederatedCloudResourceMessage();
        Map<String, Map<String,String>> result = (Map<String, Map<String,String>>) rabbitTemplate
                .convertSendAndReceive(platformRegistryExchange, rhRegistrationRequestKey, federatedCloudResources);

        // Testing the RegistrationHandlerListener response
        assertEquals(2, result.get("sensor1InternalId").size());
        assertEquals(1, result.get("sensor2InternalId").size());

        // Checking what is stored in the database
        List<Resource> stored = resourceRepository.findAll();
        assertEquals(3, stored.size());

        // Get the current sequenceId and remove the number of the registered federatedCloudResources to get the first
        // id of the newly created resources. That is because we created new resources by calling createFederatedCloudResourceMessage.
        // We do that because idSequence is used by previous tests too, so its initial value might not be 0
        long initialId = (Long) idSequence.getValue() - stored.size();

        List<String> resourceIds = new ArrayList<>();
        resourceIds.add(createNewResourceId(initialId));
        resourceIds.add(createNewResourceId(initialId + 2));

        rabbitTemplate.convertAndSend(platformRegistryExchange, rhRemovalRequestKey, resourceIds);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        stored = resourceRepository.findAll();
        assertEquals(1, stored.size());
        assertEquals(createNewResourceId(initialId + 1), stored.get(0).getId());
    }
}
