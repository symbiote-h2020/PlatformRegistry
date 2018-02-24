package eu.h2020.symbiote.pr.communication.rabbit;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.cloud.model.internal.FederatedCloudResource;
import eu.h2020.symbiote.pr.services.ResourceService;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@Component
public class RegistrationHandlerListener {
    private static Log log = LogFactory.getLog(RegistrationHandlerListener.class);

    private ResourceService resourceService;

    @Autowired
    public RegistrationHandlerListener(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Spring AMQP Listener for Resource Registration requests from Registration Handler.
     *
     * @param federatedCloudResources Contains resource registration request coming from Registration Handler
     * @return a map containing the internalIds and the federationIds inside each federation
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "${rabbit.queueName.platformRegistry.rhRegistrationRequest}",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete = "${rabbit.exchange.platformRegistry.autodelete}",
                            exclusive = "false"),
                    exchange = @Exchange(
                            value = "${rabbit.exchange.platformRegistry.name}",
                            ignoreDeclarationExceptions = "true",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete  = "${rabbit.exchange.platformRegistry.autodelete}",
                            internal = "${rabbit.exchange.platformRegistry.internal}",
                            type = "${rabbit.exchange.platformRegistry.type}"),
                    key = "${rabbit.routingKey.platformRegistry.rhRegistrationRequest}")
    )
    public Map<String, Map<String,String>> registerResources(List<FederatedCloudResource> federatedCloudResources) {
        log.trace("Received resource registration request from registration Handler: " +
                ReflectionToStringBuilder.toString(federatedCloudResources));

        // ToDo: rework this to return proper error messages and/or do not requeue the request
        try {
            return resourceService.savePlatformResources(federatedCloudResources);
        } catch (Exception e) {
            log.info("Exception thrown during saving platform resources", e);
        }

        return new HashMap<>();
    }

    /**
     * Spring AMQP Listener for Resource Update requests from Registration Handler.
     *
     * @param updatedResourceIds the federatedIds of the updated resources
     * @return a map containing the internalIds and the federationIds inside each federation
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "${rabbit.queueName.platformRegistry.rhUpdateRequest}",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete = "${rabbit.exchange.platformRegistry.autodelete}",
                            exclusive = "false"),
                    exchange = @Exchange(
                            value = "${rabbit.exchange.platformRegistry.name}",
                            ignoreDeclarationExceptions = "true",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete  = "${rabbit.exchange.platformRegistry.autodelete}",
                            internal = "${rabbit.exchange.platformRegistry.internal}",
                            type = "${rabbit.exchange.platformRegistry.type}"),
                    key = "${rabbit.routingKey.platformRegistry.rhUpdateRequest}")
    )
    public List<String> updateResources(List<CloudResource> updatedResourceIds) {
        log.trace("Received resource update request from registration Handler: " +
                ReflectionToStringBuilder.toString(updatedResourceIds));

        // ToDo: rework this to return proper error messages and/or do not requeue the request
        try {
            return resourceService.updatePlatformResources(updatedResourceIds);
        } catch (Exception e) {
            log.info("Exception thrown during updating platform resources", e);
        }

        return new ArrayList<>();
    }

    /**
     * Spring AMQP Listener for Resource Removal requests from Registration Handler.
     *
     * @param resourceIds Contains a list of resource federationIds to be deleted
     * @return a list of the federationIds of the removed resources
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${rabbit.queueName.platformRegistry.rhRemovalRequest}",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete = "${rabbit.exchange.platformRegistry.autodelete}",
                            exclusive = "false"),
                    exchange = @Exchange(
                            value = "${rabbit.exchange.platformRegistry.name}",
                            ignoreDeclarationExceptions = "true",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete  = "${rabbit.exchange.platformRegistry.autodelete}",
                            internal = "${rabbit.exchange.platformRegistry.internal}",
                            type = "${rabbit.exchange.platformRegistry.type}"),
                    key = "${rabbit.routingKey.platformRegistry.rhRemovalRequest}")
    )
    public List<String> removeResources(List<String> resourceIds) {
        log.trace("Received resource removal request from registration Handler: " +
                ReflectionToStringBuilder.toString(resourceIds));

        // ToDo: rework this to return proper error messages and/or do not requeue the request
        try {
            return resourceService.removePlatformResources(resourceIds);
        } catch (Exception e) {
            log.info("Exception thrown during removing platform resources", e);
        }

        return new ArrayList<>();
    }
}
