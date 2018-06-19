package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.ResourcesAddedOrUpdatedMessage;
import eu.h2020.symbiote.cloud.trust.model.TrustEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;


/**
 * @author Ilia Pietri (ICOM)
 * @since 31/05/2018.
 */
public class TrustManagerListenerTests extends PlatformRegistryBaseTestClass {

    private static Log log = LogFactory.getLog(TrustManagerListenerTests.class);


    @Test
    public void updateAdaptiveResourceTrustTest() throws InterruptedException {

        List<FederatedResource> testFederatedResources = createTestFederatedResources(testPlatformId);
        rabbitTemplate.convertAndSend(platformRegistryExchange, addOrUpdateFederatedResourcesKey,
                new ResourcesAddedOrUpdatedMessage(testFederatedResources));

        // Wait until the resources are stored in the database
        while (resourceRepository.findAll().size() != testFederatedResources.size()) {
            TimeUnit.MILLISECONDS.sleep(100);
        }

        for(String fedId: testFederatedResources.get(0).getFederatedResourceInfoMap().keySet()) {
            testFederatedResources.get(0).setAdaptiveTrust(17.00);
            TrustEntry trustEntry = new TrustEntry(TrustEntry.Type.ADAPTIVE_RESOURCE_TRUST, testFederatedResources.get(0).getPlatformId(), testFederatedResources.get(0).getFederatedResourceInfoMap().get(fedId).getSymbioteId());
            trustEntry.updateEntry(17.00);
            rabbitTemplate.convertAndSend(trustExchange, updateAdaptiveResourceTrustKey,
                    trustEntry);
            TimeUnit.SECONDS.sleep(1);

        }

        for(String fedId: testFederatedResources.get(1).getFederatedResourceInfoMap().keySet()) {
            testFederatedResources.get(1).setAdaptiveTrust(15.00);
            TrustEntry trustEntry = new TrustEntry(TrustEntry.Type.ADAPTIVE_RESOURCE_TRUST, testFederatedResources.get(1).getPlatformId(), testFederatedResources.get(1).getFederatedResourceInfoMap().get(fedId).getSymbioteId());
            trustEntry.updateEntry(15.00);
            rabbitTemplate.convertAndSend(trustExchange, updateAdaptiveResourceTrustKey,
                    trustEntry);
            TimeUnit.SECONDS.sleep(1);
        }
        for(String fedId: testFederatedResources.get(2).getFederatedResourceInfoMap().keySet()) {
            testFederatedResources.get(2).setAdaptiveTrust(19.00);
            TrustEntry trustEntry = new TrustEntry(TrustEntry.Type.ADAPTIVE_RESOURCE_TRUST, testFederatedResources.get(2).getPlatformId(), testFederatedResources.get(2).getFederatedResourceInfoMap().get(fedId).getSymbioteId());
            trustEntry.updateEntry(19.00);
            rabbitTemplate.convertAndSend(trustExchange, updateAdaptiveResourceTrustKey,
                    trustEntry);
            TimeUnit.SECONDS.sleep(1);
        }

        assertEquals(3, resourceRepository.findAll().size());

        // Checking what is stored in the database
        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(3, stored.size());

        FederatedResource resource1 = resourceRepository.findOne(testFederatedResources.get(0).getAggregationId());
        for(String fedId: resource1.getFederatedResourceInfoMap().keySet())
            assertTrue(resource1.getAdaptiveTrust().equals(17.0));

        FederatedResource resource2 = resourceRepository.findOne(testFederatedResources.get(1).getAggregationId());
        for(String fedId: resource2.getFederatedResourceInfoMap().keySet())
            assertTrue(resource2.getAdaptiveTrust().equals(15.0));

        FederatedResource resource3 = resourceRepository.findOne(testFederatedResources.get(2).getAggregationId());
        for(String fedId: resource3.getFederatedResourceInfoMap().keySet())
            assertTrue(resource3.getAdaptiveTrust().equals(19.0));


    }


}
