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

        BooleanBuilder predicate=new BooleanBuilder();//build final predicate as geospatial queries are not supported by querydsl to be directly included in predicate.

        if(p!=null)//include predicate specified for the non geospatial query part
            predicate.and(p);

        if(near!=null) {//build and include predicate for the specified geospatial query part

            List<String> aggregationIds = new ArrayList<>(resourceRepository.findAllByLocationCoordsIsWithin(near).stream()
                    .map(FederatedResource::getAggregationId).collect(Collectors.toSet()));//find aggregationIds (as they are unique id specifiers) of federatedResources within the circle specified
            predicate.and(QFederatedResource.federatedResource.aggregationId.in(aggregationIds));//find federatedResources with the specified aggregationIds
        }

        List<FederatedResource> resources;
        if(sort==null)
            resources = resourceRepository.findAll(predicate);//all the federatedResources for the combined predicate.
        else
            resources = resourceRepository.findAll(predicate, sort);//Results sorted when specified
        FederationSearchResult response = new FederationSearchResult(resources);

        return AuthorizationServiceHelper.addSecurityService(response, new HttpHeaders(),
                HttpStatus.OK, (String) securityChecks.getBody());
    }

}
