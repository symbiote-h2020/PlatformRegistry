package eu.h2020.symbiote.pr.communication.rest.controllers;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.QFederatedResource;
import eu.h2020.symbiote.pr.services.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controller for handling HTTP requests
 *
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/22/2018.
 */
@Controller
@RequestMapping("/pr")
@CrossOrigin
public class SearchController {
    private static Log log = LogFactory.getLog(SearchController.class);

    private SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/list_resources")
    public ResponseEntity listAllResources(@RequestHeader HttpHeaders httpHeaders) {
        log.trace("Request to /list_resources");
        return searchService.listResources(httpHeaders);
    }

    @GetMapping("/list_federation_resources/{federationId}")
    public ResponseEntity listFederationResources(@RequestHeader HttpHeaders httpHeaders,
                                                  @PathVariable String federationId) {
        log.trace("Request to /list_federation_resources");
        return searchService.listFederationResources(httpHeaders, federationId);
    }

    @GetMapping("/list_resources_in_predicate/")
    public ResponseEntity listResourcesInPredicate(@RequestHeader HttpHeaders httpHeaders,
                                                   @QuerydslPredicate(root = FederatedResource.class) Predicate p,
                                                   @RequestParam MultiValueMap<String, String> requestParams) {
        log.trace("Request to /list_resources_in_predicate");

        List<String> symbioteIds= requestParams.get("federatedResourceId");
        List<String> resourceFederations= requestParams.get("federationId");

        BooleanBuilder builder=new BooleanBuilder();
        QFederatedResource federatedResource = QFederatedResource.federatedResource;

        if(symbioteIds!=null) //if symbioteid of the resource is in the list
            builder.and(federatedResource.symbioteId.in(symbioteIds));

        if(resourceFederations!=null)//if federations of the resource contain any federation in the list
                builder.and(federatedResource.federations.any().in(resourceFederations));

//      if(resourceFederations!=null) {//if federations of the resource contain all in the list
//            for(String rf: resourceFederations) {
//                predicate = federatedResource.federations.any().eq(rf);//federatedResource.federations.any().in(resourceFederation);
//                builder.and(predicate);
//            }
//        }

        return searchService.listPredicate(httpHeaders, builder);
    }

//    @GetMapping("/list_resources_directly_by_predicate/")
//    public ResponseEntity listResourcesDirectlyByPredicate(@RequestHeader HttpHeaders httpHeaders,
//                                                   @QuerydslPredicate(root = FederatedResource.class) Predicate predicate) {
//        log.trace("Request to /list_resources_directly_by_predicate");
//
//        return searchService.listPredicate(httpHeaders, predicate);
//
//    }

}
