package eu.h2020.symbiote.pr.services;

import eu.h2020.symbiote.pr.model.FederatedResource;
import eu.h2020.symbiote.pr.model.FederationSearchResult;
import eu.h2020.symbiote.pr.repositories.ResourceRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/22/2018.
 */
@Service
public class SearchService {
    private static Log log = LogFactory.getLog(SearchService.class);

    private ResourceRepository resourceRepository;
    private AuthorizationService authorizationService;

    @Autowired
    public SearchService(ResourceRepository resourceRepository, AuthorizationService authorizationService) {
        this.resourceRepository = resourceRepository;
        this.authorizationService = authorizationService;
    }

    public ResponseEntity listResources(HttpHeaders httpHeaders) {
        log.trace("listResources request");

        // Create the service response. If it fails, return appropriate error since there is no need to continue
        ResponseEntity serviceResponseResult = authorizationService.generateServiceResponse();
        if (serviceResponseResult.getStatusCode() != HttpStatus.valueOf(200))
            return serviceResponseResult;

        // Check the proper security headers. If the check fails, return appropriate error since there is no need to continue
        ResponseEntity checkListResourcesRequestValidity = authorizationService.checkListResourcesRequest(httpHeaders);
        if (checkListResourcesRequestValidity.getStatusCode() != HttpStatus.OK)
            return checkListResourcesRequestValidity;

        List<FederatedResource> resources = resourceRepository.findAll();
        FederationSearchResult response = new FederationSearchResult();

        for (FederatedResource federatedResource : resources) {
            response.addFederationResourceResult(federatedResource);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
