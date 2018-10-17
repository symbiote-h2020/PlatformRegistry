package eu.h2020.symbiote.pr;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import eu.h2020.symbiote.cloud.model.internal.*;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.pr.dummyListeners.DummySubscriptionManagerListener;
import eu.h2020.symbiote.pr.repositories.ResourceRepository;
import eu.h2020.symbiote.pr.services.AuthorizationService;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
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
import static org.junit.Assert.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    protected ResourceRepository resourceRepository;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    protected DummySubscriptionManagerListener dummySubscriptionManagerListener;

    @Value("${platform.id}")
    protected String platformId;

    @Value("${rabbit.exchange.platformRegistry.name}")
    protected String platformRegistryExchange;

    @Value("${rabbit.exchange.trust}")
    protected String trustExchange;

    @Value("${rabbit.routingKey.platformRegistry.update}")
    protected String addOrUpdateRequestKey;

    @Value("${rabbit.routingKey.platformRegistry.delete}")
    protected String removalRequestKey;

    @Value("${rabbit.routingKey.platformRegistry.share}")
    protected String shareResourcesKey;

    @Value("${rabbit.routingKey.platformRegistry.unshare}")
    protected String unshareResourcesKey;

    @Value("${rabbit.routingKey.platformRegistry.addOrUpdateFederatedResources}")
    protected String addOrUpdateFederatedResourcesKey;

    @Value("${rabbit.routingKey.platformRegistry.removeFederatedResources}")
    protected String removeFederatedResourcesKey;

    @Value("${rabbit.routingKey.trust.updateAdaptiveResourceTrust}")
    protected String updateAdaptiveResourceTrustKey;

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
        resourceRepository.deleteAll();
        dummySubscriptionManagerListener.clearLists();

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @After
    public void cleanup() {
        resourceRepository.deleteAll();
    }

    public String createNewResourceId(long id) {
        return String.format("%0" + Long.BYTES * 2 + "x@%s", id, platformId);
    }

    public String createNewResourceId(long id, String platformId) {
        return String.format("%0" + Long.BYTES * 2 + "x@%s", id, platformId);
    }

    private String createNewResourceId(String platformId) {

        long id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
        assertTrue(id>0);
        Set<String> ids = new HashSet<>();
        ids.add(String.format("%0" + Long.BYTES * 2 +"x@%s", id, platformId));

        //check it does not exist in the database
        while(resourceRepository.findAllByAggregationIdIn(ids).size()>0) {
            id = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;//randLong.nextLong();
            ids.clear();
            ids.add(String.format("%0" + Long.BYTES * 2 +"x@%s", id, platformId));
        }
                return String.format("%0" + Long.BYTES * 2 +"x@%s", id, platformId);
    }

    public List<CloudResource> createTestCloudResources() {

        List<Resource> resources = createTestResources();

        // Create 1st cloudResource
        Map<String, ResourceSharingInformation> resourceSharingInformationMap1 = new HashMap<>();

        ResourceSharingInformation sharingInformation1 = new ResourceSharingInformation();
        sharingInformation1.setBartering(true);
        resourceSharingInformationMap1.put(federation1, sharingInformation1);

        ResourceSharingInformation sharingInformation2 = new ResourceSharingInformation();
        sharingInformation2.setBartering(false);
        resourceSharingInformationMap1.put(federation2, sharingInformation2);

        FederationInfoBean federationInfoBean1 = new FederationInfoBean();
        federationInfoBean1.setSharingInformation(resourceSharingInformationMap1);

        CloudResource cloudResource1 = new CloudResource();
        cloudResource1.setResource(resources.get(0));
        cloudResource1.setInternalId("stationarySensorInternalId");
        federationInfoBean1.setResourceTrust(1.0);
        cloudResource1.setFederationInfo(federationInfoBean1);

        // Create 2nd cloudResource
        Map<String, ResourceSharingInformation> resourceSharingInformationMap2 = new HashMap<>();

        ResourceSharingInformation sharingInformation3 = new ResourceSharingInformation();
        sharingInformation3.setBartering(true);
        resourceSharingInformationMap2.put(federation1, sharingInformation3);

        FederationInfoBean federationInfoBean2 = new FederationInfoBean();
        federationInfoBean2.setSharingInformation(resourceSharingInformationMap2);
        CloudResource cloudResource2 = new CloudResource();
        cloudResource2.setResource(resources.get(1));
        cloudResource2.setInternalId("actuatorInternalId");
        federationInfoBean2.setResourceTrust(2.0);
        cloudResource2.setFederationInfo(federationInfoBean2);

        // Create 3rd cloudResource
        Map<String, ResourceSharingInformation> resourceSharingInformationMap3 = new HashMap<>();

        ResourceSharingInformation sharingInformation4 = new ResourceSharingInformation();
        sharingInformation4.setBartering(true);
        resourceSharingInformationMap3.put(federation1, sharingInformation4);

        FederationInfoBean federationInfoBean3 = new FederationInfoBean();
        federationInfoBean3.setSharingInformation(resourceSharingInformationMap3);

        CloudResource cloudResource3 = new CloudResource();
        cloudResource3.setResource(resources.get(2));
        cloudResource3.setInternalId("serviceInternalId");
        federationInfoBean3.setResourceTrust(3.0);
        cloudResource3.setFederationInfo(federationInfoBean3);

        // Create a registration request for a federatedCloudResource
        List<CloudResource> cloudResources = new ArrayList<>();
        cloudResources.add(cloudResource1);
        cloudResources.add(cloudResource2);
        cloudResources.add(cloudResource3);

        return cloudResources;
    }

    public List<FederatedResource> createTestFederatedResources(String platformId) {

        List<CloudResource> cloudResources = createTestCloudResources();

        Set <String> existingIds=new HashSet<>();
        Set <String> newIds=new HashSet<>();
        String id;

        id=createNewResourceId(platformId);
        newIds.clear();
        newIds.add(id);
        assertTrue(resourceRepository.findAllByAggregationIdIn(newIds).size()==0);
        FederatedResource federatedResource1 = new FederatedResource(id, cloudResources.get(0), 10.00);
        assertTrue(!existingIds.contains(id));
        existingIds.add(id);

        id=createNewResourceId(platformId);
        newIds.clear();
        newIds.add(id);
        assertTrue(resourceRepository.findAllByAggregationIdIn(newIds).size()==0);
        FederatedResource federatedResource2 = new FederatedResource(id, cloudResources.get(1), 7.00);
        assertTrue(!existingIds.contains(id));
        existingIds.add(id);

        id=createNewResourceId(platformId);
        newIds.clear();
        newIds.add(id);
        assertTrue(resourceRepository.findAllByAggregationIdIn(newIds).size()==0);
        FederatedResource federatedResource3 = new FederatedResource(id, cloudResources.get(2), 5.00);

        assertTrue(!existingIds.contains(id));
        existingIds.add(id);

        return new ArrayList<>(Arrays.asList(federatedResource1, federatedResource2, federatedResource3));
    }

    public List<Resource> createTestResources() {
        List<Resource> resources = new ArrayList<>();

        // Create 1st resource
        StationarySensor stationarySensor = new StationarySensor();
        stationarySensor.setLocatedAt(new WGS84Location(1.0, 1.0, 1.0, "location1", Arrays.asList("locationDescription1")));
        stationarySensor.setName("stationarySensor");
        stationarySensor.setDescription(Collections.singletonList("sensor1Description"));
        stationarySensor.setInterworkingServiceURL("https://stationarySensor.com");
        stationarySensor.setObservesProperty(Arrays.asList("property1", "property2"));
        resources.add(stationarySensor);

        // Create 2nd resource
        Actuator actuator = new Actuator();
        actuator.setName("actuator");
        actuator.setInterworkingServiceURL("https://actuator.com");
        actuator.setLocatedAt(new WGS84Location(2.0, 2.0, 2.0, "location2", Arrays.asList("locationDescription2")));
        resources.add(actuator);

        // Create 3rd resource
        Service service = new Service();
        service.setName("service");
        service.setInterworkingServiceURL("https://service.com");
        List<String> descriptionList=Arrays.asList("@type=Beacon","@beacon.id=f7826da6-4fa2-4e98-8024-bc5b71e0893e","@beacon.major=44933","@beacon.minor=46799","@beacon.tx=0x50");
        service.setDescription(descriptionList);
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
