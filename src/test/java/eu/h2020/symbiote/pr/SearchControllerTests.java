package eu.h2020.symbiote.pr;

import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.hamcrest.Matchers.contains;
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
    public void listResourcesInPredicateBySymbioteIdSuccessfulTest() throws Exception {
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

        String symbId = federatedResourceList.get(0).getSymbioteId();
        String fed1 = "fed1";
        String fed2 = "fed2";
       String predicate="?federatedResourceId="+symbId+"&federationId="+fed1+"&federationId="+fed2;
        //String predicate="?federationId="+fed;
        // String predicate="?symbioteId="+symbId;
        //String predicate="?oDataUrl="+stationarySensorODataUrl;
        // String predicate="?symbioteId="+symbId+"&oDataUrl="+stationarySensorODataUrl;

        mockMvc.perform(get("/pr/list_resources_in_predicate/" + predicate))
                .andExpect(status().isOk())
                .andExpect(header().string(SecurityConstants.SECURITY_RESPONSE_HEADER, serviceResponse))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[*].symbioteId",
                        contains(federatedResourceList.get(0).getSymbioteId()
                        )))
                .andExpect(jsonPath("$.resources[*].oDataUrl",
                        contains(
                                stationarySensorODataUrl
                        )))
                .andExpect(jsonPath("$.resources[*].restUrl",
                        contains(
                                stationarySensorRestUrl
                        )));
    }

}
