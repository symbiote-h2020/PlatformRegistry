package eu.h2020.symbiote.pr.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.cim.Resource;

import java.util.List;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/21/2018.
 */
public class NewResourcesMessage {
    private List<Resource> newResources;

    @JsonCreator
    public NewResourcesMessage(@JsonProperty(value = "newResources") List<Resource> newResources) {
        this.newResources = newResources;
    }

    public List<Resource> getNewResources() { return newResources; }
    public void setNewResources(List<Resource> newResources) { this.newResources = newResources; }
}
