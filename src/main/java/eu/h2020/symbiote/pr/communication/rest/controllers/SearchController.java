package eu.h2020.symbiote.pr.communication.rest.controllers;

import eu.h2020.symbiote.pr.services.SearchService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
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
}