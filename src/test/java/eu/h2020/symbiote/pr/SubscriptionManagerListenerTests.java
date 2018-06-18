package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.ResourcesAddedOrUpdatedMessage;
import eu.h2020.symbiote.cloud.model.internal.ResourcesDeletedMessage;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Service;
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
public class SubscriptionManagerListenerTests extends PlatformRegistryBaseTestClass {

    private static Log log = LogFactory.getLog(SubscriptionManagerListenerTests.class);

    @Test
    public void newResourcesTest() throws InterruptedException {

        List<FederatedResource> testFederatedResources = createTestFederatedResources(testPlatformId);
        rabbitTemplate.convertAndSend(platformRegistryExchange, addOrUpdateFederatedResourcesKey,
                new ResourcesAddedOrUpdatedMessage(testFederatedResources));

        // Wait until the resources are stored in the database
        while (resourceRepository.findAll().size() != testFederatedResources.size()) {
            TimeUnit.MILLISECONDS.sleep(100);
        }

        // Checking what is stored in the database
        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(3, stored.size());


        FederatedResource resource1 = resourceRepository.findOne(testFederatedResources.get(0).getAggregationId());
        assertTrue(resource1.getCloudResource().getResource() instanceof StationarySensor);
        assertEquals(2, resource1.getFederatedResourceInfoMap().size());
        assertTrue(resource1.getFederatedResourceInfoMap().keySet().containsAll(Arrays.asList(federation1, federation2)));
        assertEquals(2, resource1.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertTrue(resource1.getCloudResource().getFederationInfo().getSharingInformation().containsKey(federation1));
        assertTrue(resource1.getCloudResource().getFederationInfo().getSharingInformation().containsKey(federation2));


        FederatedResource resource2 = resourceRepository.findOne(testFederatedResources.get(1).getAggregationId());
        assertTrue(resource2.getCloudResource().getResource() instanceof Actuator);
        assertEquals(1, resource2.getFederatedResourceInfoMap().size());
        assertTrue(resource2.getFederatedResourceInfoMap().containsKey(federation1));
        assertEquals(1, resource2.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertTrue(resource2.getCloudResource().getFederationInfo().getSharingInformation().containsKey(federation1));

        FederatedResource resource3 = resourceRepository.findOne(testFederatedResources.get(2).getAggregationId());
        assertTrue(resource3.getCloudResource().getResource() instanceof Service);
        assertEquals(1, resource3.getFederatedResourceInfoMap().size());
        assertTrue(resource3.getFederatedResourceInfoMap().containsKey(federation1));
        assertEquals(1, resource3.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertTrue(resource3.getCloudResource().getFederationInfo().getSharingInformation().containsKey(federation1));
    }

    @Test
    public void resourcesDeletedTest() throws InterruptedException {

        List<FederatedResource> testFederatedResources = createTestFederatedResources(testPlatformId);
        resourceRepository.save(testFederatedResources);

        // Wait until the resources are stored in the database
        while (resourceRepository.findAll().size() != testFederatedResources.size()) {
            TimeUnit.MILLISECONDS.sleep(100);
        }

        Set<String> deletedFederatedResources = new HashSet<>();
        // We remove resource1 from federation2
        deletedFederatedResources.add(testFederatedResources.get(0).getAggregationId()+"@"+federation2);
        // We remove resource2 from federation1
        deletedFederatedResources.add(testFederatedResources.get(1).getAggregationId()+"@"+federation1);
        // We remove resource3 from federation1
        deletedFederatedResources.add(testFederatedResources.get(2).getAggregationId()+"@"+federation1);

        ResourcesDeletedMessage deleteMessage = new ResourcesDeletedMessage(deletedFederatedResources);

        rabbitTemplate.convertAndSend(platformRegistryExchange, removeFederatedResourcesKey, deleteMessage);

        // Sleep to make sure that the repo has been updated before querying
        while (resourceRepository.findAll().size() != 1) {
            TimeUnit.MILLISECONDS.sleep(100);
        }

        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(testFederatedResources.get(0).getAggregationId(), stored.get(0).getAggregationId());
    }
}
