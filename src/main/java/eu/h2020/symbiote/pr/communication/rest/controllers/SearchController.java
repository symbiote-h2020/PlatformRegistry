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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
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

        return searchService.listByPredicate(httpHeaders, null, null, null);
    }

    @GetMapping("/list_federation_resources/{federationId}")
    public ResponseEntity listFederationResources(@RequestHeader HttpHeaders httpHeaders,
                                                  @PathVariable String federationId) {
        log.trace("Request to /list_federation_resources");

        BooleanBuilder builder=new BooleanBuilder();
        QFederatedResource federatedResource = QFederatedResource.federatedResource;
        builder.and(federatedResource.federatedResourceInfoMap.containsKey(federationId));

        return searchService.listByPredicate(httpHeaders, builder, null, null);
    }

    /**
     * Endpoint for quering federated resources using HTTP GET requests,
     * filtering and sorting the available resources according to the
     * specified request parameters
     *
     * @param name             a list with the resource names
     * @param description       the resource description
     * @param id                the id for identifying the resource in the symbIoTe federation (aggregationId)
     * @param federationId      the list of federation ids
     * @param observed_property property observed by resource (sensor); can indicate more than one
     *                          observed property
     * @param resource_type     type of queried resource e.g. stationarySensor
     * @param location_name     name of resource location; can be a set of different locations
     * @param location_lat      latitude of resource location; it concerns WGS84 locations for devices
     * @param location_long     longitude of resource location; it concerns WGS84 locations for devices
     * @param max_distance      maximal distance from specified resource latitude and longitude (in meters);
     *                          the radius used for geospatial queries
     * @param sort              the field to be used for sorting the resources
     * @param httpHeaders request headers
     * @return ResponseEntity   query result as body or null along with appropriate error HTTP status code
     */
    @GetMapping("/search")
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
    ) throws UnsupportedEncodingException {
        log.trace("Request to /search");

        BooleanBuilder builder=new BooleanBuilder();
        QFederatedResource federatedResource = QFederatedResource.federatedResource;

        if(p!=null) { //find federatedResources with the specified fields in the predicate
            builder.and(p);
        }

        if(resourceNames!=null) //find federatedResources where resourceName is in the list
            builder.and(federatedResource.cloudResource.resource.name.in(resourceNames));

        if(resourceDescriptions!=null) //find federatedResources where description (list of strings) contains all strings specified. "and" is required instead of "andAnyOf"
            for(String rD: resourceDescriptions)
                builder.and(federatedResource.cloudResource.resource.description.any().eq(URLDecoder.decode(rD,"UTF-8")));

        if(observes_property!=null) //find federatedResources where Observes_property (list of strings) contains all strings specified.
            for(String oP: observes_property) {
                QSensor qsensor = federatedResource.cloudResource.resource.as(QSensor.class);
                builder.and(federatedResource.cloudResource.resource.as(QSensor.class).observesProperty.any().eq(URLDecoder.decode(oP,"UTF-8")));
            }

        if(aggregationIds!=null) { //find federatedResources with symbioteid in the specified list
            List<String> aggregationIdsDecoded=new ArrayList<>();
            for(String name:aggregationIds)
                aggregationIdsDecoded.add(URLDecoder.decode(name,"UTF-8"));
            builder.and(federatedResource.aggregationId.in(aggregationIdsDecoded));
        }

        if(resourceFederations!=null) {//if federatedResource belongs to any of the federations specified
            BooleanBuilder predicateOnFedIds = new BooleanBuilder();
            for (String fedId : resourceFederations)
                predicateOnFedIds.or(federatedResource.federatedResourceInfoMap.containsKey(URLDecoder.decode(fedId,"UTF-8")));
            builder.and(predicateOnFedIds);
        }

        //TODO: fix querydsl problem with instanceOf use to remove the added resourceType field
        if(resourceType!=null) {//find federatedResources where resource is instanceof resourceType.  federatedResource's resourceType contains the simple name of resource's class.
            builder.and(federatedResource.resourceType.eq(URLDecoder.decode(resourceType,"UTF-8")));
        }

        //TODO: fix querydsl deep path initialization problem to remove the added location specific fields if possible
        if(locationName!=null)//find federatedResources locatedAt specified location
        {
            List<String> locationNameDecoded=new ArrayList<>();
            for(String name:locationName)
                locationNameDecoded.add(URLDecoder.decode(name,"UTF-8"));
            builder.and(federatedResource.locatedAt.name.in(locationNameDecoded));
        }

        if (locationLong!= null && maxDistance==null) //find federatedResources with the specified longitude. Resource of type Device with location of type QWGS84Location, null otherwise.
            builder.and(federatedResource.locationCoords.get(0).eq(locationLong));

        if (locationLat!= null && maxDistance==null) //find federatedResources with the specified latitude. Resource of type Device with location of type QWGS84Location, null otherwise.
            builder.and(federatedResource.locationCoords.get(1).eq(locationLat));

        //TODO: fix to build geospatial query predicates directly using querydsl if possible
        Circle locationNear = null;//find federatedResources that are near (within radius) of the specified location coordinates
        if(locationLat != null && locationLong != null && maxDistance !=null)
            locationNear = new Circle(locationLong, locationLat, maxDistance);

        if (resourceTrust!= null) //find federatedResources with resourceTrust greater or equal than the specified value.
            builder.and(federatedResource.cloudResource.federationInfo.resourceTrust.goe(resourceTrust));

        if (adaptiveTrust!= null)
            builder.and(federatedResource.adaptiveTrust.goe(adaptiveTrust));

        Sort sortOrder = null; //if federatedResources need to be sorted, order by the specified field in direction specified
        if(sort!=null)
            sortOrder= new Sort(new Sort.Order((sort.split(" ", 2)[1].contains("asc")) ? Sort.Direction.ASC : Sort.Direction.DESC, sort.split(" ", 2)[0]));

        return searchService.listByPredicate(httpHeaders, builder, sortOrder, locationNear);

    }

}
