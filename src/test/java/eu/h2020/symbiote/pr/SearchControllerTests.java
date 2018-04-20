package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/22/2018.
 */
public class SearchControllerTests extends PlatformRegistryBaseTestClass {

    @Test
    public void serviceResponseGenerationFailed() throws Exception {
        doReturn(new ResponseEntity<>("Failed to generate a service response", HttpStatus.INTERNAL_SERVER_ERROR))
                .when(authorizationService).generateServiceResponse();

        mockMvc.perform(get("/pr/list_resources"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Failed to generate a service response"));
    }

    @Test
    public void securityRequestVerificationFailed() throws Exception {
        doReturn(new ResponseEntity<>("testServiceResponse", HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>("The stored resource access policy was not satisfied", HttpStatus.UNAUTHORIZED))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        mockMvc.perform(get("/pr/list_resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("The stored resource access policy was not satisfied"));
    }

    @Test
    public void listResourcesSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        String stationarySensorODataUrl = "https://stationarySensor.com/rap/Sensors('"
                + federatedResourceList.get(0).getSymbioteId() + "')";
        String actuatorODataUrl = "https://actuator.com/rap/Actuators('"
                + federatedResourceList.get(1).getSymbioteId() + "')";
        String serviceODataUrl = "https://service.com/rap/Services('"
                + federatedResourceList.get(2).getSymbioteId() + "')";

        String stationarySensorRestUrl = "https://stationarySensor.com/rap/Sensor/"
                + federatedResourceList.get(0).getSymbioteId();
        String actuatorRestUrl = "https://actuator.com/rap/Actuator/"
                + federatedResourceList.get(1).getSymbioteId();
        String serviceRestUrl = "https://service.com/rap/Service/"
                + federatedResourceList.get(2).getSymbioteId();

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        mockMvc.perform(get("/pr/list_resources"))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(3)))
                .andExpect(jsonPath("$.resources[*].symbioteId",
                        contains(
                                federatedResourceList.get(0).getSymbioteId(),
                                federatedResourceList.get(1).getSymbioteId(),
                                federatedResourceList.get(2).getSymbioteId()
                        )))
                .andExpect(jsonPath("$.resources[*].oDataUrl",
                        contains(
                                stationarySensorODataUrl,
                                actuatorODataUrl,
                                serviceODataUrl
                        )))
                .andExpect(jsonPath("$.resources[*].restUrl",
                        contains(
                                stationarySensorRestUrl,
                                actuatorRestUrl,
                                serviceRestUrl
                        )));
    }

    @Test
    public void listResourcesInFederationSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        String stationarySensorODataUrl = "https://stationarySensor.com/rap/Sensors('"
                + federatedResourceList.get(0).getSymbioteId() + "')";

        String stationarySensorRestUrl = "https://stationarySensor.com/rap/Sensor/"
                + federatedResourceList.get(0).getSymbioteId();

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        mockMvc.perform(get("/pr/list_federation_resources/" + federation2))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[*].symbioteId",
                        contains(federatedResourceList.get(0).getSymbioteId())))
                .andExpect(jsonPath("$.resources[*].oDataUrl",
                        contains(stationarySensorODataUrl)))
                .andExpect(jsonPath("$.resources[*].restUrl",
                        contains(stationarySensorRestUrl)));
    }

    @Test
    public void listResourcesInPredicateByIdSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        String ids[]={federatedResourceList.get(0).getSymbioteId(), federatedResourceList.get(1).getSymbioteId(), federatedResourceList.get(2).getSymbioteId()};
        String symbioteIds = String.join(",",ids);
        String predicate="?id="+symbioteIds;

        mockMvc.perform(get("/pr/list_resources_in_predicate/" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(3)))
                .andExpect(jsonPath("$.resources[*].symbioteId",
                        containsInAnyOrder(ids
                 )));
    }

    @Test
    public void listResourcesInPredicateByNameSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        String names[] = {federatedResourceList.get(0).getCloudResource().getResource().getName(),
                          federatedResourceList.get(1).getCloudResource().getResource().getName()};//{"stationarySensor", "actuator"};
        String name = String.join(",",names);
        String predicate="?name="+name;

        mockMvc.perform(get("/pr/list_resources_in_predicate/" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(2)))
                .andExpect(jsonPath("$.resources[*].cloudResource.resource.name",
                        containsInAnyOrder(names
                        )));
    }

    @Test
    public void listResourcesInPredicateByDescriptionSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        List<String> descriptions=federatedResourceList.get(0).getCloudResource().getResource().getDescription();//Collections.singletonList("sensor1Description");
        String description = String.join(",", descriptions);
        String predicate="?description="+description;

        mockMvc.perform(get("/pr/list_resources_in_predicate/" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[*].cloudResource.resource.description",
                        contains(descriptions
                        )));
    }


    @Test
    public void listResourcesInPredicateByFedIdsSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        String feds[] = {"fed2"};//List<String> names = Arrays.asList(federatedResourceList.get(0).getCloudResource().getResource().getName());
        String fedIds = String.join(",",feds);
        String predicate="?federationId="+fedIds;

        mockMvc.perform(get("/pr/list_resources_in_predicate/" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[0].federations", hasSize(2
                )));
    }

    @Test
    public void listResourcesInPredicateByObservesPropertySuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        String properties[] = {"property1", "property2"};//(String[]) ((StationarySensor)federatedResourceList.get(0).getCloudResource().getResource()).getObservesProperty().toArray();
        String observesProperty = String.join(",", properties);
        String predicate="?observes_property="+observesProperty;

        mockMvc.perform(get("/pr/list_resources_in_predicate/" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[0].cloudResource.resource.observesProperty",
                        containsInAnyOrder(properties
                        )));
    }

    @Test
    public void listResourcesInPredicateByResourceTypeSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        String resourceType = "stationarySensor";
        String predicate="?resource_type="+resourceType;

        mockMvc.perform(get("/pr/list_resources_in_predicate/" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[*].cloudResource.resource.name",
                contains(federatedResourceList.get(0).getCloudResource().getResource().getName()
                )));
    }

    //@Test
    public void listResourcesInPredicateByLocationSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        Device s2 = (Device) federatedResourceList.get(0).getCloudResource().getResource();
        String locationName=s2.getLocatedAt().getName();
//        WGS84Location l =(WGS84Location) s2.getLocatedAt();
//        Double locationLong =l.getLongitude();
//        Double locationLat =l.getLatitude();
        String predicate="?location_name="+locationName;//+"&location_lat="+locationLat+"&location_long="+locationLong;

        mockMvc.perform(get("/pr/list_resources_in_predicate/" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[0].cloudResource.resource.locatedAt.name",
                        contains(locationName
                        )));
    }


}

