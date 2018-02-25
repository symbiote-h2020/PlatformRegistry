package eu.h2020.symbiote.pr.communication.rabbit;

import eu.h2020.symbiote.cloud.model.internal.ResourcesAddedOrUpdatedMessage;
import eu.h2020.symbiote.cloud.model.internal.ResourcesDeletedMessage;
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

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@Component
public class SubscriptionManagerListener {
    private static Log log = LogFactory.getLog(SubscriptionManagerListener.class);

    private ResourceService resourceService;

    @Autowired
    public SubscriptionManagerListener(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    /**
     * Spring AMQP Listener for listening to new FederationResources updates from Subscription Manager.
     * @param resourcesAddedOrUpdated message received from Subscription Manager
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "${rabbit.queueName.platformRegistry.addOrUpdateResources}",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete = "${rabbit.exchange.platformRegistry.autodelete}"
                            , exclusive = "false"),
                    exchange = @Exchange(
                            value = "${rabbit.exchange.platformRegistry.name}",
                            ignoreDeclarationExceptions = "true",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete  = "${rabbit.exchange.platformRegistry.autodelete}",
                            internal = "${rabbit.exchange.platformRegistry.internal}",
                            type = "${rabbit.exchange.platformRegistry.type}"),
                    key = "${rabbit.routingKey.platformRegistry.addOrUpdateResources}")
    )
    public void addOrUpdateResources(ResourcesAddedOrUpdatedMessage resourcesAddedOrUpdated) {
        log.trace("Received new federated resources from Subscription Manager: " +
                ReflectionToStringBuilder.toString(resourcesAddedOrUpdated));

        // ToDo: rework this to return proper error messages and/or do not requeue the request
        try {
            resourceService.addOrUpdateFederationResources(resourcesAddedOrUpdated);
        } catch (Exception e) {
            log.info("Exception thrown during saving federated resources", e);
        }
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "${rabbit.queueName.platformRegistry.removeResources}",
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
                    key = "${rabbit.routingKey.platformRegistry.removeResources}")
    )
    public void deleteResources(ResourcesDeletedMessage resourcesDeleted) {
        log.trace("Received message from Subscription Manager to remove resources: " +
                ReflectionToStringBuilder.toString(resourcesDeleted));

        // ToDo: rework this to return proper error messages and/or do not requeue the request
        try {
            resourceService.removeFederationResources(resourcesDeleted);
        } catch (Exception e) {
            log.info("Exception thrown during removing federated resources", e);
        }
    }
}
