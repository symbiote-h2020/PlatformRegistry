package eu.h2020.symbiote.pr.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/22/2018.
 */
public class FederationSearchResult {
    private List<FederatedResource> resources;

    public FederationSearchResult() {
        resources = new ArrayList<>();
    }

    @JsonCreator
    public FederationSearchResult(@JsonProperty(value = "resources") List<FederatedResource> resources) {
        if (resources != null)
            this.resources = resources;
        else
            this.resources = new ArrayList<>();
    }

    public List<FederatedResource> getResources() { return resources; }
    public void setResources(List<FederatedResource> resources) { this.resources = resources; }

    public void addFederationResourceResult(FederatedResource federatedResource) {
        resources.add(federatedResource);
    }
}
