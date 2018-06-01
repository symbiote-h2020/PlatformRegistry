package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.ResourcesAddedOrUpdatedMessage;
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


        testFederatedResources.get(0).setAdaptiveTrust(17.0);
        testFederatedResources.get(1).setAdaptiveTrust(15.0);
        testFederatedResources.get(2).setAdaptiveTrust(19.0);

        assertEquals(3, resourceRepository.findAll().size());

        rabbitTemplate.convertAndSend(trustExchange, updateAdaptiveResourceTrustKey,
                new ResourcesAddedOrUpdatedMessage(testFederatedResources));

        TimeUnit.SECONDS.sleep(2);

//        // Checking what is stored in the database
        List<FederatedResource> stored = resourceRepository.findAll();
        assertEquals(3, stored.size());

        FederatedResource resource1 = resourceRepository.findOne(testFederatedResources.get(0).getSymbioteId());
        assertTrue(resource1.getAdaptiveTrust().equals(17.0));
        FederatedResource resource2 = resourceRepository.findOne(testFederatedResources.get(1).getSymbioteId());
        assertTrue(resource2.getAdaptiveTrust().equals(15.0));
        FederatedResource resource3 = resourceRepository.findOne(testFederatedResources.get(2).getSymbioteId());
        assertTrue(resource3.getAdaptiveTrust().equals(19.0));


    }


}
