package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.pr.model.FederatedResource;
import eu.h2020.symbiote.pr.model.NewResourcesMessage;
import eu.h2020.symbiote.pr.model.ResourcesDeletedMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
public class SubscriptionManagerListenerTests extends PlatformRegistryBaseTestClass {

    private static Log log = LogFactory.getLog(SubscriptionManagerListenerTests.class);

    @Test
    public void newResourcesTest() throws InterruptedException{

        rabbitTemplate.convertAndSend(platformRegistryExchange, smAddOrUpdateResourcesKey,
                new NewResourcesMessage(createTestFederatedResources(testPlatformId)));

        // Sleep to make sure that the platform has been updated in the repo before querying
        TimeUnit.MILLISECONDS.sleep(500);

        // Checking what is stored in the database
        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(3, stored.size());

        Resource resource1 = resourceRepository.findOne(createNewResourceId(0, testPlatformId)).getResource();
        assertTrue(resource1 instanceof StationarySensor);

        Resource resource2 = resourceRepository.findOne(createNewResourceId(1, testPlatformId)).getResource();
        assertTrue(resource2 instanceof Actuator);

        Resource resource3 = resourceRepository.findOne(createNewResourceId(2, testPlatformId)).getResource();
        assertTrue(resource3 instanceof Service);
    }

    @Test
    public void resourcesDeletedTest() throws InterruptedException{

        List<FederatedResource> federatedResources = createTestFederatedResources(testPlatformId);
        resourceRepository.save(federatedResources);

        ResourcesDeletedMessage deleteMessage = new ResourcesDeletedMessage(Arrays.asList(
                federatedResources.get(0).getId(),
                federatedResources.get(2).getId(),
                createNewResourceId(1000, testPlatformId) // Does not exist
        ));

        rabbitTemplate.convertAndSend(platformRegistryExchange, smRemoveResourcesKey, deleteMessage);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(1, stored.size());
        assertEquals(createNewResourceId(1, testPlatformId), stored.get(0).getResource().getId());
    }
}
