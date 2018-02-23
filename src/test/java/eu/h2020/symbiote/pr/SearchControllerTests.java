package eu.h2020.symbiote.pr;

import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasSize;

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
                                createNewResourceId(2, "platform1"))));
        ;
    }
}
