package eu.h2020.symbiote.pr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.ResourceSharingInformation;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.pr.dummyListeners.DummySubscriptionManagerListener;
import eu.h2020.symbiote.pr.model.PersistentVariable;
import eu.h2020.symbiote.pr.repositories.CloudResourceRepository;
import eu.h2020.symbiote.pr.repositories.PersistentVariableRepository;
import eu.h2020.symbiote.pr.repositories.ResourceRepository;
import eu.h2020.symbiote.pr.services.AuthorizationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.*;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public abstract class PlatformRegistryBaseTestClass {
    private static Log log = LogFactory.getLog(PlatformRegistryBaseTestClass.class);

    @Autowired
    protected WebApplicationContext wac;

    protected MockMvc mockMvc;

    @Autowired
    protected AuthorizationService authorizationService;

    @Autowired
    protected CloudResourceRepository cloudResourceRepository;

    @Autowired
    protected ResourceRepository resourceRepository;

    @Autowired
    protected PersistentVariableRepository persistentVariableRepository;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected PersistentVariable idSequence;

    @Autowired
    protected DummySubscriptionManagerListener dummySubscriptionManagerListener;

    @Value("${platform.id}")
    protected String platformId;

    @Value("${rabbit.exchange.platformRegistry.name}")
    protected String platformRegistryExchange;

    @Value("${rabbit.routingKey.platformRegistry.addOrUpdateRequest}")
    protected String addOrUpdateRequestKey;

    @Value("${rabbit.routingKey.platformRegistry.removalRequest}")
    protected String removalRequestKey;

    @Value("${rabbit.routingKey.platformRegistry.shareResources}")
    protected String shareResourcesKey;

    @Value("${rabbit.routingKey.platformRegistry.unshareResources}")
    protected String unshareResourcesKey;

    @Value("${rabbit.routingKey.platformRegistry.addOrUpdateFederatedResources}")
    protected String addOrUpdateFederatedResourcesKey;

    @Value("${rabbit.routingKey.platformRegistry.removeFederatedResources}")
    protected String removeFederatedResourcesKey;

    protected String serviceResponse = "testServiceResponse";

    // Used for SubscriptionManager tests
    protected String testPlatformId = "testPlatform";

    protected String federation1 = "fed1";
    protected String federation2 = "fed2";
    protected String stationarySensorInternalId = "stationarySensorInternalId";
    protected String actuatorInternalId = "actuatorInternalId";
    protected String serviceInternalId = "serviceInternalId";

    protected ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setup() {
        cloudResourceRepository.deleteAll();
        resourceRepository.deleteAll();
        persistentVariableRepository.deleteAll();
        dummySubscriptionManagerListener.clearLists();

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @After
    public void cleanup() {
        cloudResourceRepository.deleteAll();
        resourceRepository.deleteAll();
        persistentVariableRepository.deleteAll();
    }

    public String createNewResourceId(long id) {
        return String.format("%0" + Long.BYTES * 2 + "x@%s", id, platformId);
    }

    public String createNewResourceId(long id, String platformId) {
        return String.format("%0" + Long.BYTES * 2 + "x@%s", id, platformId);
    }

    public List<CloudResource> createTestCloudResources() {

        List<Resource> resources = createTestResources(platformId);

        // Create 1st cloudResource
        Map<String, ResourceSharingInformation> resourceSharingInformationMap1 = new HashMap<>();
        ResourceSharingInformation sharingInformation1 = new ResourceSharingInformation();
        sharingInformation1.setBartering(true);
        resourceSharingInformationMap1.put(federation1, sharingInformation1);
        ResourceSharingInformation sharingInformation2 = new ResourceSharingInformation();
        sharingInformation2.setBartering(false);
        resourceSharingInformationMap1.put(federation2, sharingInformation2);

        CloudResource cloudResource1 = new CloudResource();
        cloudResource1.setResource(resources.get(0));
        cloudResource1.setInternalId("stationarySensorInternalId");
        cloudResource1.setFederationInfo(resourceSharingInformationMap1);

        // Create 2nd cloudResource
        Map<String, ResourceSharingInformation> resourceSharingInformationMap2 = new HashMap<>();
        ResourceSharingInformation sharingInformation3 = new ResourceSharingInformation();
        sharingInformation3.setBartering(true);
        resourceSharingInformationMap2.put(federation1, sharingInformation3);

        CloudResource cloudResource2 = new CloudResource();
        cloudResource2.setResource(resources.get(1));
        cloudResource2.setInternalId("actuatorInternalId");
        cloudResource2.setFederationInfo(resourceSharingInformationMap2);

        // Create 3rd cloudResource
        Map<String, ResourceSharingInformation> resourceSharingInformationMap3 = new HashMap<>();
        ResourceSharingInformation sharingInformation4 = new ResourceSharingInformation();
        sharingInformation4.setBartering(true);
        resourceSharingInformationMap3.put(federation1, sharingInformation4);

        CloudResource cloudResource3 = new CloudResource();
        cloudResource3.setResource(resources.get(2));
        cloudResource3.setInternalId("serviceInternalId");
        cloudResource3.setFederationInfo(resourceSharingInformationMap3);

        // Create a registration request for a federatedCloudResource
        List<CloudResource> cloudResources = new ArrayList<>();
        cloudResources.add(cloudResource1);
        cloudResources.add(cloudResource2);
        cloudResources.add(cloudResource3);

        return cloudResources;
    }

    public List<FederatedResource> createTestFederatedResources(String platform) {
        List<Resource> resources = createTestResources(platform);
        List<FederatedResource> federatedResources = new ArrayList<>();
        federatedResources.add(new FederatedResource(
                resources.get(0), createNewResourceId(0, testPlatformId), federation1, true));
        federatedResources.add(new FederatedResource(
                resources.get(1), createNewResourceId(1, testPlatformId), federation2, false));
        federatedResources.add(new FederatedResource(
                resources.get(2), createNewResourceId(2, testPlatformId), federation1, true));
        return federatedResources;
    }

    public List<Resource> createTestResources(String platform) {
        List<Resource> resources = new ArrayList<>();

        // Create 1st resource
        StationarySensor stationarySensor = new StationarySensor();
        stationarySensor.setName("stationarySensor");
        stationarySensor.setDescription(Collections.singletonList("sensor1Description"));
        stationarySensor.setInterworkingServiceURL("https://stationarySensor.com");
        stationarySensor.setObservesProperty(Arrays.asList("property1", "property2"));
        resources.add(stationarySensor);

        // Create 2nd resource
        Actuator actuator = new Actuator();
        actuator.setName("actuator");
        actuator.setInterworkingServiceURL("https://actuator.com");
        resources.add(actuator);

        // Create 3rd resource
        Service service = new Service();
        service.setName("service");
        service.setInterworkingServiceURL("https://service.com");
        resources.add(service);

        return resources;
    }

    public List<CloudResource> addOrUpdateResources(List<CloudResource> cloudResources) {
        List<CloudResource> registrationResult = null;

        try {
            String jsonArray = mapper.writeValueAsString(rabbitTemplate
                    .convertSendAndReceive(platformRegistryExchange, addOrUpdateRequestKey, cloudResources));
            CollectionType javaType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, CloudResource.class);
            registrationResult = mapper.readValue(jsonArray, javaType);
        } catch (IOException e) {
            log.info("Problem deserializing addOrUpdate request", e);
        }

        return registrationResult;
    }

    public List<String> removeResources(List<String> internalIdsToBeRemoved) {
        List<String> removalResult = null;

        try {
            List<String> helper =  (List<String>) rabbitTemplate
                    .convertSendAndReceive(platformRegistryExchange, removalRequestKey, internalIdsToBeRemoved);
            String jsonArray = mapper.writeValueAsString(helper);
            CollectionType javaType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, String.class);
            removalResult = mapper.readValue(jsonArray, javaType);
        } catch (IOException e) {
            log.info("Problem deserializing removal request", e);
        }

        return removalResult;
    }

    public List<CloudResource> shareResources(Map<String, Map<String, Boolean>> resourcesToBeShared) {
        List<CloudResource> shareResourcesResult = null;

        try {
            String jsonArray = mapper.writeValueAsString(rabbitTemplate
                    .convertSendAndReceive(platformRegistryExchange, shareResourcesKey, resourcesToBeShared));
            CollectionType javaType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, CloudResource.class);
            shareResourcesResult = mapper.readValue(jsonArray, javaType);
        } catch (IOException e) {
            log.info("Problem deserializing sharing resources request", e);
        }

        return shareResourcesResult;
    }

    public List<CloudResource> unshareResources(Map<String, List<String>> resourcesToBeUnshared) {
        List<CloudResource> shareResourcesResult = null;

        try {
            String jsonArray = mapper.writeValueAsString(rabbitTemplate
                    .convertSendAndReceive(platformRegistryExchange, unshareResourcesKey, resourcesToBeUnshared));
            CollectionType javaType = mapper.getTypeFactory()
                    .constructCollectionType(List.class, CloudResource.class);
            shareResourcesResult = mapper.readValue(jsonArray, javaType);
        } catch (IOException e) {
            log.info("Problem deserializing unsharing resources request", e);
        }

        return shareResourcesResult;
    }
}
