package eu.h2020.symbiote.pr.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/21/2018.
 */
public class ResourcesDeletedMessage {

    private List<String> deletedIds;

    @JsonCreator
    public ResourcesDeletedMessage(@JsonProperty(value = "deletedIds") List<String> deletedIds) {
        this.deletedIds = deletedIds;
    }

    public List<String> getDeletedIds() { return deletedIds; }
    public void setDeletedIds(List<String> deletedIds) { this.deletedIds = deletedIds; }
}
