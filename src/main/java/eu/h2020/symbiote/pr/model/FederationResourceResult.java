package eu.h2020.symbiote.pr.model;

import eu.h2020.symbiote.model.cim.Resource;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/22/2018.
 */
public class FederationResourceResult {
    private Resource resource;
    private String resourceUrl;

    public FederationResourceResult(Resource resource) {
        this.resource = resource;
//        this.resourceUrl =
    }

    public Resource getResource() { return resource; }
    public void setResource(Resource resource) { this.resource = resource; }

    public String getResourceUrl() { return resourceUrl; }
    public void setResourceUrl(String resourceUrl) { this.resourceUrl = resourceUrl; }
}
