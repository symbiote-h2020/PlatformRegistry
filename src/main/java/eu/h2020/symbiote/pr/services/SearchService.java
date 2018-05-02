package eu.h2020.symbiote.pr.services;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.FederationSearchResult;
import eu.h2020.symbiote.cloud.model.internal.QFederatedResource;
import eu.h2020.symbiote.pr.helpers.AuthorizationServiceHelper;
import eu.h2020.symbiote.pr.repositories.ResourceRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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

    public ResponseEntity listByPredicate(HttpHeaders httpHeaders, Predicate p, Sort sort, Circle near) {
        log.trace("listByPredicate request");

        ResponseEntity securityChecks = AuthorizationServiceHelper.checkSecurityRequestAndCreateServiceResponse(
                authorizationService, httpHeaders);
        if (securityChecks.getStatusCode() != HttpStatus.OK)
            return securityChecks;

        BooleanBuilder builder=new BooleanBuilder();

        if(p!=null)
            builder.and(p);

        if(near!=null) {
            List<String> symbioteIds = new ArrayList<>(resourceRepository.findAllByLocationCoordsIsWithin(near).stream()
                    .map(FederatedResource::getSymbioteId).collect(Collectors.toSet()));
            builder.and(QFederatedResource.federatedResource.symbioteId.in(symbioteIds));
        }

        List<FederatedResource> resources=resourceRepository.findAll(builder, sort);
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
