package eu.h2020.symbiote.pr.dummyListeners;

import eu.h2020.symbiote.cloud.model.internal.ResourcesAddedOrUpdatedMessage;
import eu.h2020.symbiote.cloud.model.internal.ResourcesDeletedMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/24/2018.
 */
@Component
public class DummySubscriptionManagerListener {
    private static Log log = LogFactory.getLog(DummySubscriptionManagerListener.class);

    private List<ResourcesAddedOrUpdatedMessage> resourcesAddedOrUpdatedMessages = new ArrayList<>();
    private List<ResourcesDeletedMessage> resourcesDeletedMessages = new ArrayList<>();

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "${rabbit.queueName.subscriptionManager.addOrUpdateFederatedResources}",
                            durable = "${rabbit.exchange.subscriptionManager.durable}",
                            autoDelete = "${rabbit.exchange.subscriptionManager.autodelete}"
                            , exclusive = "false"),
                    exchange = @Exchange(
                            value = "${rabbit.exchange.subscriptionManager.name}",
                            ignoreDeclarationExceptions = "true",
                            durable = "${rabbit.exchange.subscriptionManager.durable}",
                            autoDelete  = "${rabbit.exchange.subscriptionManager.autodelete}",
                            internal = "${rabbit.exchange.subscriptionManager.internal}",
                            type = "${rabbit.exchange.subscriptionManager.type}"),
                    key = "${rabbit.routingKey.subscriptionManager.addOrUpdateFederatedResources}")
    )
    public void addOrUpdateFederatedResources(ResourcesAddedOrUpdatedMessage newFederatedResources) {
        log.debug("Received ResourcesAddedOrUpdatedMessage from Platform Registry");
        resourcesAddedOrUpdatedMessages.add(newFederatedResources);
    }

    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "${rabbit.queueName.subscriptionManager.removeFederatedResources}",
                            durable = "${rabbit.exchange.subscriptionManager.durable}",
                            autoDelete = "${rabbit.exchange.subscriptionManager.autodelete}",
                            exclusive = "false"),
                    exchange = @Exchange(
                            value = "${rabbit.exchange.subscriptionManager.name}",
                            ignoreDeclarationExceptions = "true",
                            durable = "${rabbit.exchange.subscriptionManager.durable}",
                            autoDelete  = "${rabbit.exchange.subscriptionManager.autodelete}",
                            internal = "${rabbit.exchange.subscriptionManager.internal}",
                            type = "${rabbit.exchange.subscriptionManager.type}"),
                    key = "${rabbit.routingKey.subscriptionManager.removeFederatedResources}")
    )
    public void deleteResources(ResourcesDeletedMessage resourcesDeleted) {
        log.debug("Received ResourcesDeletedMessage from Platform Registry");
        resourcesDeletedMessages.add(resourcesDeleted);
    }

    public List<ResourcesAddedOrUpdatedMessage> getResourcesAddedOrUpdatedMessages() {
        return resourcesAddedOrUpdatedMessages;
    }

    public void setResourcesAddedOrUpdatedMessages(List<ResourcesAddedOrUpdatedMessage> resourcesAddedOrUpdatedMessages) {
        this.resourcesAddedOrUpdatedMessages = resourcesAddedOrUpdatedMessages;
    }

    public List<ResourcesDeletedMessage> getResourcesDeletedMessages() {
        return resourcesDeletedMessages;
    }

    public void setResourcesDeletedMessages(List<ResourcesDeletedMessage> resourcesDeletedMessages) {
        this.resourcesDeletedMessages = resourcesDeletedMessages;
    }

    public void clearLists() {
        resourcesAddedOrUpdatedMessages.clear();
        resourcesDeletedMessages.clear();
    }
}
