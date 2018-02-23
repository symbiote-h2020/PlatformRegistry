package eu.h2020.symbiote.pr.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.Service;
import org.springframework.data.annotation.PersistenceConstructor;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/22/2018.
 */
public class FederatedResource {
    private String id;
    private Resource resource;
    private String oDataUrl;
    private String restUrl;

    public FederatedResource(Resource resource) {
        this.id = resource.getId();
        this.resource = resource;

        // Todo: Consider actual Resource validation here
        if (resource.getInterworkingServiceURL() != null) {
            this.oDataUrl = createUrl(UrlType.ODATA);
            this.restUrl = createUrl(UrlType.REST);
        }
    }

    public FederatedResource() {
        // Empty Constructor used instead of @PersistenceConstructor to avoid sprind dependencies
    }

    @JsonCreator
    public FederatedResource(@JsonProperty(value = "id") String id,
                             @JsonProperty(value = "resource") Resource resource,
                             @JsonProperty(value = "oDataUrl") String oDataUrl,
                             @JsonProperty(value = "restUrl") String restUrl) {
        this.id = id;
        this.resource = resource;
        this.oDataUrl = oDataUrl;
        this.restUrl = restUrl;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Resource getResource() { return resource; }
    public void setResource(Resource resource) { this.resource = resource; }

    public String getoDataUrl() { return oDataUrl; }
    public void setoDataUrl(String oDataUrl) { this.oDataUrl = oDataUrl; }

    public String getRestUrl() { return restUrl; }
    public void setRestUrl(String restUrl) { this.restUrl = restUrl; }

    private String createUrl(UrlType urlType) {
        if (resource instanceof Actuator)
            return createUrl(urlType, "Actuator");
        else if (resource instanceof Service)
            return createUrl(urlType, "Service");
        else
            return createUrl(urlType, "Sensor");
    }

    private String createUrl(UrlType urlType, String resourceTypeName) {
        return urlType == UrlType.ODATA ?
                resource.getInterworkingServiceURL().replaceAll("(/rap)?/*$", "")
                        +  "/rap/" + resourceTypeName + "s('" + resource.getId() + "')" :
                resource.getInterworkingServiceURL().replaceAll("(/rap)?/*$", "")
                        +  "/rap/" + resourceTypeName + "/" + resource.getId();
    }

    private enum UrlType {
        ODATA, REST
    }
}
