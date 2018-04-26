package eu.h2020.symbiote.pr.services;

import com.querydsl.core.types.Predicate;
import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.FederationSearchResult;
import eu.h2020.symbiote.pr.helpers.AuthorizationServiceHelper;
import eu.h2020.symbiote.pr.repositories.ResourceRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * This service handles the search HTTP requests.
 *
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


    public ResponseEntity listPredicate(HttpHeaders httpHeaders, Predicate p, Sort sort) {
        log.trace("listFederationResources request");

        ResponseEntity securityChecks = AuthorizationServiceHelper.checkSecurityRequestAndCreateServiceResponse(
                authorizationService, httpHeaders);
        if (securityChecks.getStatusCode() != HttpStatus.OK)
            return securityChecks;
       //locations.sort((loc1, loc2) -> haversineCalculation(loc1.latitude, loc1.longitude, latitude, longitude).compareTo(haversineCalculation(loc1.latitude, loc1.longitude, latitude, longitude)));
        List<FederatedResource> resources;
        if(sort==null)
            resources=resourceRepository.findAll(p);
        else
            resources=resourceRepository.findAll(p, sort);

        FederationSearchResult response = new FederationSearchResult(resources);
        return AuthorizationServiceHelper.addSecurityService(response, new HttpHeaders(),
                HttpStatus.OK, (String) securityChecks.getBody());
    }

    public ResponseEntity listResources(HttpHeaders httpHeaders) {
        log.trace("listResources request");

        ResponseEntity securityChecks = AuthorizationServiceHelper.checkSecurityRequestAndCreateServiceResponse(
                authorizationService, httpHeaders);
        if (securityChecks.getStatusCode() != HttpStatus.OK)
            return securityChecks;

        List<FederatedResource> resources = resourceRepository.findAll();
        FederationSearchResult response = new FederationSearchResult(resources);

        return AuthorizationServiceHelper.addSecurityService(response, new HttpHeaders(),
                HttpStatus.OK, (String) securityChecks.getBody());
    }

    public ResponseEntity listFederationResources(HttpHeaders httpHeaders, String federationId) {
        log.trace("listFederationResources request");

        ResponseEntity securityChecks = AuthorizationServiceHelper.checkSecurityRequestAndCreateServiceResponse(
                authorizationService, httpHeaders);
        if (securityChecks.getStatusCode() != HttpStatus.OK)
            return securityChecks;

        List<FederatedResource> resources = resourceRepository.findAllByFederationsContaining(federationId);
        FederationSearchResult response = new FederationSearchResult(resources);

        return AuthorizationServiceHelper.addSecurityService(response, new HttpHeaders(),
                HttpStatus.OK, (String) securityChecks.getBody());
    }

}
