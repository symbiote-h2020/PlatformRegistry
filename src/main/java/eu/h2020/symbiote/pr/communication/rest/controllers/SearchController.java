package eu.h2020.symbiote.pr.communication.rest.controllers;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import eu.h2020.symbiote.cloud.model.internal.FederatedResource;
import eu.h2020.symbiote.cloud.model.internal.QFederatedResource;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.pr.services.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.querydsl.binding.QuerydslPredicate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
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
                                                   @RequestParam(value="name", required = false) List<String> resourceNames,
                                                   @RequestParam(value="description", required = false) List<String> resourceDescriptions,
                                                   @RequestParam(value="id", required = false) List<String> symbioteIds,
                                                   @RequestParam(value="federationId", required = false) List<String> resourceFederations,
                                                   @RequestParam(value="observes_property", required = false) List<String> observes_property,
                                                   @RequestParam(value="resource_type", required = false) String resourceType,
                                                   @RequestParam(value="location_name", required = false) List<String> locationName,//String locationName,
                                                   @RequestParam(value="location_lat", required = false) Double locationLat,
                                                   @RequestParam(value="sort", required = false) String sort,
                                                   @RequestParam(value="location_long", required = false) Double locationLong,
                                                   @RequestParam(value = "max_distance", required = false) Double maxDistance
    ) {

        log.trace("Request to /list_resources_in_predicate");

        BooleanBuilder builder=new BooleanBuilder();
        QFederatedResource federatedResource = QFederatedResource.federatedResource;

        if(resourceNames!=null) //if resourceName is specified, check if resource name is in the list
            builder.and(federatedResource.cloudResource.resource.name.in(resourceNames));

        if(resourceDescriptions!=null) //check if all strings in Description exist in federatedResource
            for(String rD: resourceDescriptions)
                builder.and(federatedResource.cloudResource.resource.description.any().eq(rD));

        if(observes_property!=null) //Description is a List so if all of the strings in the list. used and instead of andAnyOf
            for(String oP: observes_property) {
                QSensor qsensor = federatedResource.cloudResource.resource.as(QSensor.class);
                builder.and(federatedResource.cloudResource.resource.as(QSensor.class).observesProperty.any().eq(oP));
            }

        if(symbioteIds!=null) //if symbioteid of the resource is in the list
            builder.and(federatedResource.symbioteId.in(symbioteIds));

        if(resourceFederations!=null)//if federations of the resource contain any federation in the list
                builder.and(federatedResource.federations.any().in(resourceFederations));

        //TODO: use instanceOf instead of the added resourceType field
        if(resourceType!=null) {//if resourceType specified
            builder.and(federatedResource.resourceType.eq(resourceType));
        }

        //TODO: fix querydsl deep path initialization problem to remove the added location specific fields  if possible
        if(locationName!=null)
            builder.and(federatedResource.locatedAt.name.in(locationName));

        if (locationLat!= null && maxDistance==null) { //if location latitude is specified, check if exists. Applies to QWGS84Location locations only
            builder.and(federatedResource.locatedAt.as(QWGS84Location.class).latitude.eq(locationLat));
        }

        if (locationLong!= null && maxDistance==null) { //if location latitude is specified, check if exists. Applies to QWGS84Location locations only
            builder.and(federatedResource.locatedAt.as(QWGS84Location.class).longitude.eq(locationLong));
        }

        Sort sortOrder = null;
        if(sort!=null)
            sortOrder= new Sort(new Sort.Order((sort.split(" ", 2)[1].contains("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(" ", 2)[0]));

        //TODO: fix to build geospatial query predicates directly using querydsl if possible
        Circle locationNear = null;
        if(locationLat != null && locationLong != null && maxDistance !=null)
            locationNear = new Circle(locationLong, locationLat, maxDistance);

        return searchService.listByPredicate(httpHeaders, builder, sortOrder, locationNear);

    }

}
