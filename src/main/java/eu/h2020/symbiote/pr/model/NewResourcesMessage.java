package eu.h2020.symbiote.pr.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/21/2018.
 */
public class NewResourcesMessage {
    private List<FederatedResource> newFederatedResources;

    @JsonCreator
    public NewResourcesMessage(@JsonProperty(value = "newFederatedResources") List<FederatedResource> newFederatedResources) {
        this.newFederatedResources = newFederatedResources;
    }

    public List<FederatedResource> getNewFederatedResources() {
        return newFederatedResources;
    }

    public void setNewFederatedResources(List<FederatedResource> newFederatedResources) {
        this.newFederatedResources = newFederatedResources;
    }
}
