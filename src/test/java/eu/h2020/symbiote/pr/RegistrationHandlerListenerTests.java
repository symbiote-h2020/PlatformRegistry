package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.model.cim.MobileSensor;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.StationarySensor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.util.*;

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

        Map<String, Map<String,String>> result = (Map<String, Map<String,String>>) rabbitTemplate
                .convertSendAndReceive(platformRegistryExchange, rhRegistrationRequestKey, createFederatedCloudResourceMessage());

        // Testing the RegistrationHandlerListener response
        assertEquals(2, result.get("sensor1InternalId").size());
        assertEquals(1, result.get("sensor2InternalId").size());

        // Checking what is stored in the database
        List<Resource> stored = resourceRepository.findAll();
        assertEquals(3, stored.size());

        Resource resource1 = resourceRepository.findOne(createNewResourceId(0));
        assertTrue(resource1 instanceof StationarySensor);

        Resource resource2 = resourceRepository.findOne(createNewResourceId(1));
        assertTrue(resource2 instanceof StationarySensor);
        assertTrue(resource1.getBartered() != resource2.getBartered());
        assertEquals("stationarySensor", resource1.getName());
        assertEquals(resource1.getName(), resource2.getName());

        Resource resource3 = resourceRepository.findOne(createNewResourceId(2));
        assertTrue(resource3 instanceof MobileSensor);
        assertTrue(resource3.getBartered());
        assertEquals("mobileSensor", resource3.getName());
    }
}
