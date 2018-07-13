package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.ResourceSharingInformation;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.model.cim.StationarySensor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.util.*;
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

        // Register resources
        List<CloudResource> cloudResources = createTestCloudResources();
        List<CloudResource> result = addOrUpdateResources(cloudResources);
        assertNotNull(result);

        String stationarySensorId = result.get(0).getFederationInfo().getAggregationId();
        String actuatorId = result.get(1).getFederationInfo().getAggregationId();
        String serviceId = result.get(2).getFederationInfo().getAggregationId();

        // Testing the response
        assertEquals(2, result.get(0).getFederationInfo().getSharingInformation().size());
        assertEquals(1, result.get(1).getFederationInfo().getSharingInformation().size());
        assertEquals(1, result.get(2).getFederationInfo().getSharingInformation().size());

        // Checking what is stored in the database

        List<FederatedResource> storedFederatedResources = resourceRepository.findAll();
        assertEquals(3, storedFederatedResources.size());

        FederatedResource resource1 = resourceRepository.findOne(stationarySensorId);
        assertTrue(resource1.getCloudResource().getResource() instanceof StationarySensor);
        assertEquals(2, resource1.getFederatedResourceInfoMap().size());
        assertTrue(resource1.getFederatedResourceInfoMap().keySet().containsAll(Arrays.asList(federation1, federation2)));
        assertEquals(2, resource1.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertTrue(resource1.getCloudResource().getFederationInfo().getSharingInformation().get(federation1).getBartering());
        assertFalse(resource1.getCloudResource().getFederationInfo().getSharingInformation().get(federation2).getBartering());
        assertEquals("stationarySensor", resource1.getCloudResource().getResource().getName());
        String aggregationId = resource1.getCloudResource().getFederationInfo().getAggregationId();
        assertEquals(aggregationId + '@' + federation1,
                resource1.getCloudResource().getFederationInfo().getSharingInformation().get(federation1).getSymbioteId());
        assertEquals(aggregationId + '@' + federation2,
                resource1.getCloudResource().getFederationInfo().getSharingInformation().get(federation2).getSymbioteId());

        FederatedResource resource2 = resourceRepository.findOne(actuatorId);
        assertTrue(resource2.getCloudResource().getResource() instanceof Actuator);
        assertEquals(1, resource2.getFederatedResourceInfoMap().size());
        assertTrue(resource2.getFederatedResourceInfoMap().containsKey(federation1));
        assertEquals(1, resource2.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertTrue(resource2.getCloudResource().getFederationInfo().getSharingInformation().get(federation1).getBartering());
        assertEquals("actuator", resource2.getCloudResource().getResource().getName());
        aggregationId = resource2.getCloudResource().getFederationInfo().getAggregationId();
        assertEquals(aggregationId + '@' + federation1,
                resource2.getCloudResource().getFederationInfo().getSharingInformation().get(federation1).getSymbioteId());

        FederatedResource resource3 = resourceRepository.findOne(serviceId);
        assertTrue(resource3.getCloudResource().getResource() instanceof Service);
        assertEquals(1, resource3.getFederatedResourceInfoMap().size());
        assertTrue(resource3.getFederatedResourceInfoMap().containsKey(federation1));
        assertEquals(1, resource3.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertTrue(resource3.getCloudResource().getFederationInfo().getSharingInformation().get(federation1).getBartering());
        assertEquals("service", resource3.getCloudResource().getResource().getName());
        aggregationId = resource3.getCloudResource().getFederationInfo().getAggregationId();
        assertEquals(aggregationId + '@' + federation1,
                resource3.getCloudResource().getFederationInfo().getSharingInformation().get(federation1).getSymbioteId());

        // Check what dummySubscriptionManagerListener received
        while (dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size() == 0)
            TimeUnit.MILLISECONDS.sleep(100);

        assertEquals(1, dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size());
        assertEquals(0, dummySubscriptionManagerListener.getResourcesDeletedMessages().size());
        List<FederatedResource> message = dummySubscriptionManagerListener
                .getResourcesAddedOrUpdatedMessages().get(0).getNewFederatedResources();

        assertEquals(3, message.size());
        assertTrue(message.stream()
                .map(FederatedResource::getAggregationId).collect(Collectors.toList())
                .containsAll(Arrays.asList(stationarySensorId, actuatorId, serviceId)));
    }

    @Test
    public void updateResourcesTest() throws InterruptedException {

        // Register resources
        List<CloudResource> cloudResources = createTestCloudResources();
        List<CloudResource> registrationResult = addOrUpdateResources(cloudResources);
        assertNotNull(registrationResult);

        String stationarySensorId = registrationResult.get(0).getFederationInfo().getAggregationId();
        String actuatorId = registrationResult.get(1).getFederationInfo().getAggregationId();
        String serviceId = registrationResult.get(2).getFederationInfo().getAggregationId();

        // We change the name of the stationary sensor
        String newStationarySensorName = "newStationarySensorName";
        registrationResult.get(0).getResource().setName(newStationarySensorName);

        // We expose the service in the federation2 too
        // We do not fill the bartering info, so it will get the default value "false"
        registrationResult.get(2).getFederationInfo().getSharingInformation().put(federation2, new ResourceSharingInformation());

        // Remove the actuator from the registration result, which will be used for updating
        registrationResult.remove(1);

        // Send update message
        List<CloudResource> updateResult = addOrUpdateResources(registrationResult);
        assertNotNull(updateResult);

        // Testing the response
        assertEquals(2, updateResult.size());
        assertEquals(newStationarySensorName, updateResult.get(0).getResource().getName());
        assertEquals(2, updateResult.get(1).getFederationInfo().getSharingInformation().size());
        assertTrue(updateResult.get(1).getFederationInfo().getSharingInformation().get(federation1).getBartering());
        assertFalse(updateResult.get(1).getFederationInfo().getSharingInformation().get(federation2).getBartering());

        // Check what is store in the database
        FederatedResource stationarySensor = resourceRepository.findOne(stationarySensorId);
        FederatedResource actuator = resourceRepository.findOne(actuatorId);
        FederatedResource service = resourceRepository.findOne(serviceId);

        assertEquals(newStationarySensorName, stationarySensor.getCloudResource().getResource().getName());
        assertEquals("actuator", actuator.getCloudResource().getResource().getName());
        assertEquals("service", service.getCloudResource().getResource().getName());
        assertEquals(2, service.getFederatedResourceInfoMap().size());
        assertTrue(service.getFederatedResourceInfoMap().keySet().containsAll(Arrays.asList(federation1, federation2)));
        assertEquals(2, service.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertTrue(service.getCloudResource().getFederationInfo().getSharingInformation().get(federation1).getBartering());
        assertFalse(service.getCloudResource().getFederationInfo().getSharingInformation().get(federation2).getBartering());

        // Check what dummySubscriptionManagerListener received
        while (dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size() < 2)
            TimeUnit.MILLISECONDS.sleep(100);


        assertEquals(2, dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size());
        List<FederatedResource> message = dummySubscriptionManagerListener
                .getResourcesAddedOrUpdatedMessages().get(1).getNewFederatedResources();

        assertEquals(2, message.size());
        assertTrue(message.stream().map(FederatedResource::getAggregationId).collect(Collectors.toList()).containsAll(
                Arrays.asList(stationarySensorId, serviceId)
        ));
    }

    @Test
    public void removeFederatedResourcesTest() throws InterruptedException {

        // Register resources
        List<CloudResource> cloudResources = createTestCloudResources();
        List<CloudResource> registrationResult = addOrUpdateResources(cloudResources);
        assertNotNull(registrationResult);

        // We delete the 1st and 3rd resource
        List<String> internalIdsToBeRemoved = new ArrayList<>();
        internalIdsToBeRemoved.add(stationarySensorInternalId);
        internalIdsToBeRemoved.add(serviceInternalId);

        List<String> removalResult = removeResources(internalIdsToBeRemoved);
        assertNotNull(removalResult);

        assertEquals(2, removalResult.size());
        assertTrue(removalResult.containsAll(internalIdsToBeRemoved));

        // Check what is stored in the database
        List<FederatedResource> storedFederatedResources = resourceRepository.findAll();
        assertEquals(1, storedFederatedResources.size());
        assertEquals(registrationResult.get(1).getFederationInfo().getAggregationId(),
                storedFederatedResources.get(0).getAggregationId());

        // Check what dummySubscriptionManagerListener received
        while (dummySubscriptionManagerListener.getResourcesDeletedMessages().size() == 0)
            TimeUnit.MILLISECONDS.sleep(100);

        String stationarySensorId = registrationResult.get(0).getFederationInfo().getAggregationId();
        String serviceId = registrationResult.get(2).getFederationInfo().getAggregationId();

        assertEquals(1, dummySubscriptionManagerListener.getResourcesDeletedMessages().size());
        Set<String> message = dummySubscriptionManagerListener
                .getResourcesDeletedMessages().get(0).getDeletedFederatedResources();

        assertEquals(3, message.size());
        assertTrue(message.containsAll(Arrays.asList(stationarySensorId+"@"+federation1, stationarySensorId+"@"+federation2)));
        assertTrue(message.contains(serviceId+"@"+federation1));
    }


    @Test
    public void shareResourcesTest() throws InterruptedException {
        // Register resources
        List<CloudResource> cloudResources = createTestCloudResources();
        List<CloudResource> registrationResult = addOrUpdateResources(cloudResources);
        assertNotNull(registrationResult);

        // Construct share resources message
        Map<String, Map<String, Boolean>> resourcesToBeShared = new HashMap<>();
        Map<String, Boolean> federation1Map = new HashMap<>();
        Map<String, Boolean> federation2Map = new HashMap<>();

        // This resource is already shared in federation1, but we want to make sure that its bartering status will change
        federation1Map.put(actuatorInternalId, false);

        federation2Map.put(actuatorInternalId, false);
        federation2Map.put(serviceInternalId, true);

        resourcesToBeShared.put(federation1, federation1Map);
        resourcesToBeShared.put(federation2, federation2Map);

        List<CloudResource> sharingResourcesResult = shareResources(resourcesToBeShared);
        assertNotNull(sharingResourcesResult);

        // Check what it is received
        assertEquals(2, sharingResourcesResult.size());
        assertEquals(2, sharingResourcesResult.get(0).getFederationInfo().getSharingInformation().size());
        assertEquals(2, sharingResourcesResult.get(1).getFederationInfo().getSharingInformation().size());

        CloudResource actuator = sharingResourcesResult.get(0).getResource() instanceof Actuator ?
                sharingResourcesResult.get(0) : sharingResourcesResult.get(1);
        CloudResource service = sharingResourcesResult.get(0).getResource() instanceof Service ?
                sharingResourcesResult.get(0) : sharingResourcesResult.get(1);

        // Make sure that the above works correctly
        assertTrue(actuator.getResource() instanceof Actuator);
        assertTrue(service.getResource() instanceof Service);

        String stationarySensorIdFed = registrationResult.get(0).getFederationInfo().getAggregationId();
        String actuatorIdFed = registrationResult.get(1).getFederationInfo().getAggregationId();
        String serviceIdFed = registrationResult.get(2).getFederationInfo().getAggregationId();

        // Check what is stored in the databases
        List<FederatedResource> storedFederatedResources = resourceRepository.findAll();
        assertEquals(3, storedFederatedResources.size());

        // We did not change anything on this resource
        FederatedResource stationarySensorFed = resourceRepository.findOne(stationarySensorIdFed);
        assertEquals(2, stationarySensorFed.getFederatedResourceInfoMap().size());
        assertTrue(stationarySensorFed.getFederatedResourceInfoMap().keySet().containsAll(Arrays.asList(federation1, federation2)));
        assertEquals(2, stationarySensorFed.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertTrue(stationarySensorFed.getCloudResource().getFederationInfo().getSharingInformation().get(federation1).getBartering());
        assertFalse(stationarySensorFed.getCloudResource().getFederationInfo().getSharingInformation().get(federation2).getBartering());

        FederatedResource actuatorFed = resourceRepository.findOne(actuatorIdFed);
        assertEquals(2, actuatorFed.getFederatedResourceInfoMap().size());
        assertTrue(actuatorFed.getFederatedResourceInfoMap().keySet().containsAll(Arrays.asList(federation1, federation2)));
        assertEquals(2, actuatorFed.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertFalse(actuatorFed.getCloudResource().getFederationInfo().getSharingInformation().get(federation1).getBartering());
        assertFalse(actuatorFed.getCloudResource().getFederationInfo().getSharingInformation().get(federation2).getBartering());

        FederatedResource serviceFed = resourceRepository.findOne(serviceIdFed);
        assertEquals(2, serviceFed.getFederatedResourceInfoMap().size());
        assertTrue(serviceFed.getFederatedResourceInfoMap().keySet().containsAll(Arrays.asList(federation1, federation2)));
        assertEquals(2, serviceFed.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertTrue(serviceFed.getCloudResource().getFederationInfo().getSharingInformation().get(federation1).getBartering());
        assertTrue(serviceFed.getCloudResource().getFederationInfo().getSharingInformation().get(federation2).getBartering());

        // Check what dummySubscriptionManagerListener received
        while (dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size() < 2)
            TimeUnit.MILLISECONDS.sleep(100);


        assertEquals(2, dummySubscriptionManagerListener.getResourcesAddedOrUpdatedMessages().size());
        List<FederatedResource> message = dummySubscriptionManagerListener
                .getResourcesAddedOrUpdatedMessages().get(1).getNewFederatedResources();

        assertEquals(2, message.size());
        assertTrue(message.stream().map(FederatedResource::getAggregationId).collect(Collectors.toList()).containsAll(
                Arrays.asList(actuatorIdFed, serviceIdFed)
        ));
    }

    @Test
    public void unshareResourcesTest() throws InterruptedException {

        // Register resources
        List<CloudResource> cloudResources = createTestCloudResources();
        List<CloudResource> registrationResult = addOrUpdateResources(cloudResources);
        assertNotNull(registrationResult);

        String stationarySensorIdFed = registrationResult.get(0).getFederationInfo().getAggregationId();
        String actuatorIdFed = registrationResult.get(1).getFederationInfo().getAggregationId();
        String serviceIdFed = registrationResult.get(2).getFederationInfo().getAggregationId();

        // Construct the unshare resources message
        Map<String, List<String>> resourcesToBeUnshared = new HashMap<>();
        List<String> federation1List = new ArrayList<>();
        List<String> federation2List = new ArrayList<>();

        // These resources are shared to federation1 and we want to unshare them
        federation1List.add(stationarySensorInternalId);
        federation1List.add(actuatorInternalId);

        // This resource is not shared to federation2, but we include it to make sure that it does not break things
        federation2List.add(actuatorInternalId);

        resourcesToBeUnshared.put(federation1, federation1List);
        resourcesToBeUnshared.put(federation2, federation2List);

        List<CloudResource> unsharingResourcesResult = unshareResources(resourcesToBeUnshared);
        assertNotNull(unsharingResourcesResult);

        // Check what it is received
        assertEquals(2, unsharingResourcesResult.size());
        assertEquals(1, unsharingResourcesResult.get(0).getFederationInfo().getSharingInformation().size());
        assertEquals(0, unsharingResourcesResult.get(1).getFederationInfo().getSharingInformation().size());

        CloudResource stationarySensor = unsharingResourcesResult.get(0).getResource() instanceof StationarySensor ?
                unsharingResourcesResult.get(0) : unsharingResourcesResult.get(1);
        CloudResource actuator = unsharingResourcesResult.get(0).getResource() instanceof Actuator ?
                unsharingResourcesResult.get(0) : unsharingResourcesResult.get(1);


        // Make sure that the above works correctly
        assertTrue(stationarySensor.getResource() instanceof StationarySensor);
        assertTrue(actuator.getResource() instanceof Actuator);

        // Check what is stored in the databases
        List<FederatedResource> storedFederatedResources = resourceRepository.findAll();
        assertEquals(3, storedFederatedResources.size());

        FederatedResource stationarySensorFed = resourceRepository.findOne(stationarySensorIdFed);
        assertEquals(1, stationarySensorFed.getFederatedResourceInfoMap().size());
        assertTrue(stationarySensorFed.getFederatedResourceInfoMap().containsKey(federation2));
        assertEquals(1, stationarySensorFed.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertFalse(stationarySensorFed.getCloudResource().getFederationInfo().getSharingInformation().get(federation2).getBartering());

        FederatedResource actuatorFed = resourceRepository.findOne(actuatorIdFed);
        assertEquals(0, actuatorFed.getFederatedResourceInfoMap().size());
        assertEquals(0, actuatorFed.getCloudResource().getFederationInfo().getSharingInformation().size());

        // We did not change anything on this resource
        FederatedResource serviceFed = resourceRepository.findOne(serviceIdFed);
        assertEquals(1, serviceFed.getFederatedResourceInfoMap().size());
        assertTrue(serviceFed.getFederatedResourceInfoMap().containsKey(federation1));
        assertEquals(1, serviceFed.getCloudResource().getFederationInfo().getSharingInformation().size());
        assertTrue(serviceFed.getCloudResource().getFederationInfo().getSharingInformation().get(federation1).getBartering());

        // Check what dummySubscriptionManagerListener received
        while (dummySubscriptionManagerListener.getResourcesDeletedMessages().size() < 1)
            TimeUnit.MILLISECONDS.sleep(100);


        assertEquals(1, dummySubscriptionManagerListener.getResourcesDeletedMessages().size());
        Set<String> message = dummySubscriptionManagerListener
                .getResourcesDeletedMessages().get(0).getDeletedFederatedResources();

        assertEquals(2, message.size());

        assertTrue(message.contains(stationarySensorIdFed+"@"+federation1));
        assertTrue(message.contains(actuatorIdFed+"@"+federation1));
    }
}
