package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.Matchers.*;
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
                + federatedResourceList.get(0).getAggregationId() + "')";
        String actuatorODataUrl = "https://actuator.com/rap/Actuators('"
                + federatedResourceList.get(1).getAggregationId() + "')";
        String serviceODataUrl = "https://service.com/rap/Services('"
                + federatedResourceList.get(2).getAggregationId() + "')";

        String stationarySensorRestUrl = "https://stationarySensor.com/rap/Sensor/"
                + federatedResourceList.get(0).getAggregationId();
        String actuatorRestUrl = "https://actuator.com/rap/Actuator/"
                + federatedResourceList.get(1).getAggregationId();
        String serviceRestUrl = "https://service.com/rap/Service/"
                + federatedResourceList.get(2).getAggregationId();

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
                .andExpect(jsonPath("$.resources[*].aggregationId",
                        contains(
                                federatedResourceList.get(0).getAggregationId(),
                                federatedResourceList.get(1).getAggregationId(),
                                federatedResourceList.get(2).getAggregationId()
//                        )))
//                .andExpect(jsonPath("$.resources[*].oDataUrl",
//                        contains(
//                                stationarySensorODataUrl,
//                                actuatorODataUrl,
//                                serviceODataUrl
//                        )))
//                .andExpect(jsonPath("$.resources[*].restUrl",
//                        contains(
//                                stationarySensorRestUrl,
//                                actuatorRestUrl,
//                                serviceRestUrl
                        )));
    }

    @Test
    public void listResourcesInFederationSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        String stationarySensorODataUrl = "https://stationarySensor.com/rap/Sensors('"
                + federatedResourceList.get(0).getAggregationId() + "')";

        String stationarySensorRestUrl = "https://stationarySensor.com/rap/Sensor/"
                + federatedResourceList.get(0).getAggregationId();

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
                .andExpect(jsonPath("$.resources[*].aggregationId",
                        contains(federatedResourceList.get(0).getAggregationId())));
                //.andExpect(jsonPath("$.resources[*].oDataUrl",
                 //       contains(stationarySensorODataUrl)))
               // .andExpect(jsonPath("$.resources[*].restUrl",
                //        contains(stationarySensorRestUrl)));
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

        String ids[]={federatedResourceList.get(0).getAggregationId(), federatedResourceList.get(1).getAggregationId(), federatedResourceList.get(2).getAggregationId()};
        String aggregationIds = String.join(",",ids);
        String predicate="?id="+aggregationIds;

        mockMvc.perform(get("/pr/search" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(3)))
                .andExpect(jsonPath("$.resources[*].aggregationId",
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

        mockMvc.perform(get("/pr/search" + predicate))
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

        mockMvc.perform(get("/pr/search" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[*].cloudResource.resource.description",
                        contains(descriptions
                        )));
    }

    @Test
    public void listResourcesInPredicateByDescriptionSpecialCharsSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        List<String> descriptions=federatedResourceList.get(2).getCloudResource().getResource().getDescription();//Collections.singletonList("sensor1Description");
        String description = URLEncoder.encode("@type=Beacon", "UTF-8");//String.join(",", descriptions);
        String predicate="?description="+description;

        mockMvc.perform(get("/pr/search" + predicate))
                 .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[*].cloudResource.resource.description",
                       contains(descriptions)));

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

        mockMvc.perform(get("/pr/search" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[0].federatedResourceInfoMap.*", hasSize(2
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

        mockMvc.perform(get("/pr/search" + predicate))
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

        String resourceType = federatedResourceList.get(0).getResourceType();//"StationarySensor";
        String predicate="?resource_type="+resourceType;

        mockMvc.perform(get("/pr/search" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[*].resourceType",
                        contains(federatedResourceList.get(0).getResourceType()
                        )));
    }

    @Test
    public void listResourcesInPredicateByLocationNameSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        String locations[] = {federatedResourceList.get(0).getLocatedAt().getName(),
                federatedResourceList.get(1).getLocatedAt().getName()};
        String locationName = String.join(",", locations);
        String predicate="?location_name="+locationName+"&sort=locatedAt.longitude desc;";

        mockMvc.perform(get("/pr/search" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(2)))
                .andExpect(jsonPath("$.resources[0].locatedAt.longitude",
                        equalTo(2.0)))//lessThan//greaterThan
                .andExpect(jsonPath("$.resources[1].locatedAt.longitude",
                        equalTo(1.0)))
                .andExpect(jsonPath("$.resources[*].locatedAt.name",
                        containsInAnyOrder(locations//locationName
                        )));
    }


    @Test
    public void listResourcesInPredicateByLocationCoordsSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        String locationName=federatedResourceList.get(0).getLocatedAt().getName();
        WGS84Location l =(WGS84Location) federatedResourceList.get(0).getLocatedAt();
        Double locationLong =l.getLongitude();
        Double locationLat =l.getLatitude();
        String predicate="?location_lat="+locationLat
                +"&location_long="+locationLong;

        mockMvc.perform(get("/pr/search" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[*].locatedAt.name",
                        containsInAnyOrder(locationName
                        )));
    }

    @Test
    public void listResourcesInPredicateByGeoLocationSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        String locations[] = {federatedResourceList.get(0).getLocatedAt().getName(),
                federatedResourceList.get(1).getLocatedAt().getName()};
        String locationName = String.join(",", locations);

        Double locationLong=0.0;
        Double locationLat = 0.0;
        Double radius = 3.0;
        String predicate="?location_long="+locationLong
                +"&location_lat="+locationLat
                +"&max_distance="+radius
                +"&location_name="+locationName
                +"&sort=locatedAt.longitude desc";

        mockMvc.perform(get("/pr/search" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(2)))
                .andExpect(jsonPath("$.resources[0].locatedAt.longitude",
                        equalTo(2.0)))
                .andExpect(jsonPath("$.resources[*].locatedAt.name",
                        containsInAnyOrder(locations
                        )));
    }


    @Test
    public void listResourcesInPredicateByTrust() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        String predicate="?resource_trust=1.0&adaptive_trust=6.0";

        mockMvc.perform(get("/pr/search" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(2
                        )));
      }


    @Test
    public void listResourcesAllSuccessfulTest() throws Exception {
        List<FederatedResource> federatedResourceList = createTestFederatedResources(platformId);
        resourceRepository.save(federatedResourceList);

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(serviceResponse, HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any(), any());

        String predicate="?sort=aggregationId desc";//"";//

        mockMvc.perform(get("/pr/search" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(3)))
                .andExpect(jsonPath("$.resources[0].aggregationId",
                        greaterThan("$.resources[1].aggregationId")))
                .andExpect(jsonPath("$.resources[1].aggregationId",
                        greaterThan("$.resources[2].aggregationId"
                        )));

    }
}

