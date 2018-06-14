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
                                                   @RequestParam(value="id", required = false) List<String> aggregationIds,
                                                   @RequestParam(value="federationId", required = false) List<String> resourceFederations,
                                                   @RequestParam(value="observes_property", required = false) List<String> observes_property,
                                                   @RequestParam(value="resource_type", required = false) String resourceType,
                                                   @RequestParam(value="location_name", required = false) List<String> locationName,//String locationName,
                                                   @RequestParam(value="location_lat", required = false) Double locationLat,
                                                   @RequestParam(value="location_long", required = false) Double locationLong,
                                                   @RequestParam(value = "max_distance", required = false) Double maxDistance,
                                                   @RequestParam(value="resource_trust", required = false) Double resourceTrust,
                                                   @RequestParam(value="adaptive_trust", required = false) Double adaptiveTrust,
                                                   @RequestParam(value="sort", required = false) String sort
    ) {

        log.trace("Request to /list_resources_in_predicate");

        BooleanBuilder builder=new BooleanBuilder();
        QFederatedResource federatedResource = QFederatedResource.federatedResource;

        if(p!=null) { //find federatedResources with the specified fields in the predicate
            builder.and(p);
        }

        if(resourceNames!=null) //find federatedResources where resourceName is in the list
            builder.and(federatedResource.cloudResource.resource.name.in(resourceNames));

        if(resourceDescriptions!=null) //find federatedResources where description (list of strings) contains all strings specified. "and" is required instead of "andAnyOf"
            for(String rD: resourceDescriptions)
                builder.and(federatedResource.cloudResource.resource.description.any().eq(rD));

        if(observes_property!=null) //find federatedResources where Observes_property (list of strings) contains all strings specified.
            for(String oP: observes_property) {
                QSensor qsensor = federatedResource.cloudResource.resource.as(QSensor.class);
                builder.and(federatedResource.cloudResource.resource.as(QSensor.class).observesProperty.any().eq(oP));
            }

        if(aggregationIds!=null) //find federatedResources with symbioteid in the specified list
            builder.and(federatedResource.aggregationId.in(aggregationIds));

        if(resourceFederations!=null)//if federatedResource belongs to any of the federations specified
                builder.and(federatedResource.federations.any().in(resourceFederations));

        //TODO: fix querydsl problem with instanceOf use to remove the added resourceType field
        if(resourceType!=null) {//find federatedResources where resource is instanceof resourceType.  federatedResource's resourceType contains the simple name of resource's class.
            builder.and(federatedResource.resourceType.eq(resourceType));
        }

        //TODO: fix querydsl deep path initialization problem to remove the added location specific fields if possible
        if(locationName!=null)//find federatedResources locatedAt specified location
            builder.and(federatedResource.locatedAt.name.in(locationName));

        if (locationLong!= null && maxDistance==null) //find federatedResources with the specified longitude. Resource of type Device with location of type QWGS84Location, null otherwise.
            builder.and(federatedResource.locationCoords.get(0).eq(locationLong));

        if (locationLat!= null && maxDistance==null) //find federatedResources with the specified latitude. Resource of type Device with location of type QWGS84Location, null otherwise.
            builder.and(federatedResource.locationCoords.get(1).eq(locationLat));

        //TODO: fix to build geospatial query predicates directly using querydsl if possible
        Circle locationNear = null;//find federatedResources that are near (within radius) of the specified location coordinates
        if(locationLat != null && locationLong != null && maxDistance !=null)
            locationNear = new Circle(locationLong, locationLat, maxDistance);


        if (resourceTrust!= null) { //find federatedResources with resourceTrust greater or equal than the specified value.
                builder.and(federatedResource.cloudResource.federationInfo.resourceTrust.goe(resourceTrust));
        }

          // if (adaptiveTrust!= null) //find federatedResources with adaptiveTrust greater or equal than the specified value.
            //builder.and(federatedResource.federatedResourceInfoMap.adaptiveTrust.goe(adaptiveTrust));


        Sort sortOrder = null; //if federatedResources need to be sorted, order by the specified field in direction specified
        if(sort!=null)
            sortOrder= new Sort(new Sort.Order((sort.split(" ", 2)[1].contains("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(" ", 2)[0]));

        return searchService.listByPredicate(httpHeaders, builder, sortOrder, locationNear);

    }

}
