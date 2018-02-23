package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.FederatedCloudResource;
import eu.h2020.symbiote.model.cim.MobileSensor;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.pr.model.NewResourcesMessage;
import eu.h2020.symbiote.pr.model.PersistentVariable;
import eu.h2020.symbiote.pr.repositories.PersistentVariableRepository;
import eu.h2020.symbiote.pr.repositories.ResourceRepository;
import eu.h2020.symbiote.pr.services.AuthorizationService;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.*;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
public abstract class PlatformRegistryBaseTestClass {

    @Autowired
    protected WebApplicationContext wac;

    protected MockMvc mockMvc;

    @Autowired
    protected AuthorizationService authorizationService;

    @Autowired
    protected ResourceRepository resourceRepository;

    @Autowired
    protected PersistentVariableRepository persistentVariableRepository;

    @Autowired
    protected RabbitTemplate rabbitTemplate;

    @Autowired
    public PersistentVariable idSequence;

    @Value("${platform.id}")
    private String platformId;

    @Value("${rabbit.exchange.platformRegistry.name}")
    protected String platformRegistryExchange;

    @Value("${rabbit.routingKey.platformRegistry.rhRegistrationRequest}")
    protected String rhRegistrationRequestKey;

    @Value("${rabbit.routingKey.platformRegistry.rhRemovalRequest}")
    protected String rhRemovalRequestKey;

    @Value("${rabbit.routingKey.platformRegistry.smStoreResources}")
    protected String smStoreResourcesKey;

    @Value("${rabbit.routingKey.platformRegistry.smRemoveResources}")
    protected String smRemoveResourcesKey;

    @Before
    public void setup() {
        resourceRepository.deleteAll();
        persistentVariableRepository.deleteAll();

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
    }

    @After
    public void cleanup() {
        resourceRepository.deleteAll();
        persistentVariableRepository.deleteAll();
    }

    public String createNewResourceId(long id) {
        return String.format("%0" + Long.BYTES * 2 + "x@%s", id, platformId);
    }

    public String createNewResourceId(long id, String platformId) {
        return String.format("%0" + Long.BYTES * 2 + "x@%s", id, platformId);
    }

    public List<FederatedCloudResource> createFederatedCloudResourceMessage() {

        // Create 1st resource
        StationarySensor stationarySensor = new StationarySensor();
        stationarySensor.setName("stationarySensor");
        stationarySensor.setDescription(Collections.singletonList("sensor1Description"));
        stationarySensor.setInterworkingServiceURL("https://sensor1.com");
        stationarySensor.setObservesProperty(Arrays.asList("property1", "property2"));

        Map<String, Boolean> federationBarteringMap1 = new HashMap<>();
        federationBarteringMap1.put("fed1", true);
        federationBarteringMap1.put("fed2", false);

        FederatedCloudResource federatedCloudResource1 = new FederatedCloudResource();
        federatedCloudResource1.setResource(stationarySensor);
        federatedCloudResource1.setInternalId("sensor1InternalId");
        federatedCloudResource1.setFederationBarteredResourceMap(federationBarteringMap1);

        // Create 2nd resource
        MobileSensor mobileSensor = new MobileSensor();
        mobileSensor.setName("mobileSensor");

        Map<String, Boolean> federationBarteringMap2 = new HashMap<>();
        federationBarteringMap2.put("fed1", true);

        FederatedCloudResource federatedCloudResource2 = new FederatedCloudResource();
        federatedCloudResource2.setResource(mobileSensor);
        federatedCloudResource2.setInternalId("sensor2InternalId");
        federatedCloudResource2.setFederationBarteredResourceMap(federationBarteringMap2);

        // Create a registration request for a federatedCloudResource
        List<FederatedCloudResource> federatedCloudResources = new ArrayList<>();
        federatedCloudResources.add(federatedCloudResource1);
        federatedCloudResources.add(federatedCloudResource2);

        return federatedCloudResources;
    }

    public NewResourcesMessage createSMNewResourcesMessage() {
        return new NewResourcesMessage(createTestResources());
    }

    public void saveResourceToRepo() {
        resourceRepository.save(createTestResources());
    }

    public List<Resource> createTestResources() {
        List<Resource> resources = new ArrayList<>();

        // Create 1st resource
        StationarySensor stationarySensor = new StationarySensor();
        stationarySensor.setId(createNewResourceId(0, "platform1"));
        stationarySensor.setName("stationarySensor");
        stationarySensor.setDescription(Collections.singletonList("sensor1Description"));
        stationarySensor.setInterworkingServiceURL("https://sensor1.com");
        stationarySensor.setObservesProperty(Arrays.asList("property1", "property2"));
        resources.add(stationarySensor);

        // Create 2nd resource
        MobileSensor mobileSensor = new MobileSensor();
        mobileSensor.setId(createNewResourceId(1, "platform1"));
        mobileSensor.setName("mobileSensor");
        resources.add(mobileSensor);

        // Create 3rd resource
        Service service = new Service();
        service.setId(createNewResourceId(2, "platform1"));
        service.setName("service");
        resources.add(service);

        return resources;
    }
}
