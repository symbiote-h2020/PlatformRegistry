package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.ResourcesAddedOrUpdatedMessage;
import eu.h2020.symbiote.cloud.model.internal.ResourcesDeletedMessage;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.model.cim.StationarySensor;
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

        List<FederatedResource> testFederatedResources = createTestFederatedResources(testPlatformId);
        rabbitTemplate.convertAndSend(platformRegistryExchange, addOrUpdateFederatedResourcesKey,
                new ResourcesAddedOrUpdatedMessage(testFederatedResources));

        // Sleep to make sure that the platform has been updated in the repo before querying
        TimeUnit.MILLISECONDS.sleep(500);

        // Checking what is stored in the database
        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(3, stored.size());



        Resource resource1 = resourceRepository.findOne(testFederatedResources.get(0).getId()).getResource();
        assertTrue(resource1 instanceof StationarySensor);

        Resource resource2 = resourceRepository.findOne(testFederatedResources.get(1).getId()).getResource();
        assertTrue(resource2 instanceof Actuator);

        Resource resource3 = resourceRepository.findOne(testFederatedResources.get(2).getId()).getResource();
        assertTrue(resource3 instanceof Service);
    }

    @Test
    public void resourcesDeletedTest() throws InterruptedException{

        List<FederatedResource> testFederatedResources = createTestFederatedResources(testPlatformId);
        resourceRepository.save(testFederatedResources);

        ResourcesDeletedMessage deleteMessage = new ResourcesDeletedMessage(Arrays.asList(
                testFederatedResources.get(0).getId(),
                testFederatedResources.get(2).getId(),
                createNewResourceId(1000, testPlatformId, federation1) // Does not exist
        ));

        rabbitTemplate.convertAndSend(platformRegistryExchange, removeFederatedResourcesKey, deleteMessage);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(1, stored.size());
        assertEquals(testFederatedResources.get(1).getId(), stored.get(0).getId());
    }
}
