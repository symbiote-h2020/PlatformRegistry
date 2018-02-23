package eu.h2020.symbiote.pr;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MvcResult;

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
        ;
    }

    @Test
    public void securityRequestVerificationFailed() throws Exception {
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>("The stored resource access policy was not satisfied", HttpStatus.UNAUTHORIZED))
                .when(authorizationService).checkListResourcesRequest(any());

        mockMvc.perform(get("/pr/list_resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("The stored resource access policy was not satisfied"));
        ;
    }

    @Test
    public void listResourcesSuccessfulTest() throws Exception {
        saveResourceToRepo();

        String stationarySensorODataUrl = "https://stationarySensor.com/rap/Sensors('"
                + createNewResourceId(0, "platform1") + "')";
        String actuatorODataUrl = "https://actuator.com/rap/Actuators('"
                + createNewResourceId(1, "platform1") + "')";
        String serviceODataUrl = "https://service.com/rap/Services('"
                + createNewResourceId(2, "platform1") + "')";

        String stationarySensorRestUrl = "https://stationarySensor.com/rap/Sensor/"
                + createNewResourceId(0, "platform1");
        String actuatorRestUrl = "https://actuator.com/rap/Actuator/"
                + createNewResourceId(1, "platform1");
        String serviceRestUrl = "https://service.com/rap/Service/"
                + createNewResourceId(2, "platform1");

        // Sleep to make sure that the repo has been updated before querying
        TimeUnit.MILLISECONDS.sleep(500);

        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).generateServiceResponse();
        doReturn(new ResponseEntity<>(HttpStatus.OK))
                .when(authorizationService).checkListResourcesRequest(any());

        mockMvc.perform(get("/pr/list_resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resources", hasSize(3)))
                .andExpect(jsonPath("$.resources[*].resource.id",
                        contains(
                                createNewResourceId(0, "platform1"),
                                createNewResourceId(1, "platform1"),
                                createNewResourceId(2, "platform1"))))
                .andExpect(jsonPath("$.resources[*].oDataUrl",
                        contains(
                                stationarySensorODataUrl,
                                actuatorODataUrl,
                                serviceODataUrl)))
                .andExpect(jsonPath("$.resources[*].restUrl",
                        contains(
                                stationarySensorRestUrl,
                                actuatorRestUrl,
                                serviceRestUrl)));

    }
}
