package eu.h2020.symbiote.pr.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.cloud.model.internal.FederatedCloudResource;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.pr.model.PersistentVariable;
import eu.h2020.symbiote.pr.repositories.PersistentVariableRepository;
import eu.h2020.symbiote.pr.repositories.ResourceRepository;
import io.jsonwebtoken.lang.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@Service
public class ResourceService {
    private static Log log = LogFactory.getLog(ResourceService.class);

    private ResourceRepository resourceRepository;
    private PersistentVariableRepository persistentVariableRepository;
    private PersistentVariable idSequence;
    private ObjectMapper mapper = new ObjectMapper();
    private String platformId;

    @Autowired
    public ResourceService(ResourceRepository resourceRepository,
                           PersistentVariableRepository persistentVariableRepository,
                           PersistentVariable idSequence,
                           @Value("${platformId}") String platformId) {
        this.resourceRepository = resourceRepository;
        this.persistentVariableRepository = persistentVariableRepository;
        this.idSequence = idSequence;

        Assert.notNull(platformId, "The platformId should not be null");
        this.platformId = platformId;
    }

    public Map<String, Map<String, String>> savePlatformResources(List<FederatedCloudResource> federatedCloudResources) {
        List<Resource> resourcesToSave = new LinkedList<>();
        Map<String, Map<String, String>> internalIdResourceIdMap = new HashMap<>();

        long id = (Long) idSequence.getValue();
        for (FederatedCloudResource federatedCloudResource : federatedCloudResources) {
            String serializedResource = serializeResource(federatedCloudResource.getResource());
            Map<String, String> federationResourceIdMap = new HashMap<>();

            for (Map.Entry<String,  Boolean> entry : federatedCloudResource.getFederationBarteredResourceMap().entrySet()) {
                Resource newResource = deserializeResource(serializedResource);
                String federationId = entry.getKey();
                Boolean isBartered = entry.getValue();

                if (newResource != null) {
                    newResource.setId(createNewResourceId(id));
                    newResource.setFederationId(federationId);
                    newResource.setBartered(isBartered);
                    resourcesToSave.add(newResource);
                    federationResourceIdMap.put(federationId, newResource.getId());
                    id++;
                }
            }

            if (federationResourceIdMap.size() > 0)
                internalIdResourceIdMap.put(federatedCloudResource.getInternalId(), federationResourceIdMap);
        }

        resourceRepository.save(resourcesToSave);
        idSequence.setValue(id);
        persistentVariableRepository.save(idSequence);
        return internalIdResourceIdMap;
    }

    private String serializeResource(Resource resource) {
        String string = null;

        try {
            string = mapper.writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            log.info("Problem in serializing the resource", e);
            return null;
        }
        return string;
    }

    private Resource deserializeResource(String s) {

        Resource resource;

        try {
            resource = mapper.readValue(s, Resource.class);
        } catch (IOException e) {
            log.info("Problem in deserializing the resource", e);
            return null;
        }
        return resource;
    }

    private String createNewResourceId(long id) {
        return String.format("%0" + Long.BYTES * 2 + "x@%s", id, platformId);
    }
}
