package eu.h2020.symbiote.pr.helpers;

import eu.h2020.symbiote.pr.services.AuthorizationService;
import eu.h2020.symbiote.security.commons.SecurityConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/24/2018.
 */
public class AuthorizationServiceHelper {

    public static ResponseEntity checkSecurityRequestAndCreateServiceResponse(AuthorizationService authorizationService,
                                                                               HttpHeaders httpHeaders) {
        // Create the service response. If it fails, return appropriate error since there is no need to continue
        ResponseEntity serviceResponseResult = authorizationService.generateServiceResponse();
        if (serviceResponseResult.getStatusCode() != HttpStatus.valueOf(200))
            return serviceResponseResult;

        // Check the proper security headers. If the check fails, return appropriate error since there is no need to continue
        ResponseEntity checkListResourcesRequestValidity = authorizationService
                .checkListResourcesRequest(httpHeaders, (String) serviceResponseResult.getBody());

        return checkListResourcesRequestValidity.getStatusCode() != HttpStatus.OK ?
                checkListResourcesRequestValidity :
                serviceResponseResult;
    }

    public static ResponseEntity addSecurityService(Object response, HttpHeaders httpHeaders,
                                             HttpStatus httpStatus, String serviceResponse) {
        httpHeaders.put(SecurityConstants.SECURITY_RESPONSE_HEADER, Collections.singletonList(serviceResponse));
        return new ResponseEntity<>(response, httpHeaders, httpStatus);
    }
}