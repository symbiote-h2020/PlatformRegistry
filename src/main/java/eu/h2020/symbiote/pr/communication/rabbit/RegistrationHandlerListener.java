package eu.h2020.symbiote.pr.communication.rabbit;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.pr.services.RegistrationHandlerService;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class is used as a simple listener.
 *
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/20/2018.
 */
@Component
public class RegistrationHandlerListener {
    private static Log log = LogFactory.getLog(RegistrationHandlerListener.class);

    private RegistrationHandlerService registrationHandlerService;

    @Autowired
    public RegistrationHandlerListener(RegistrationHandlerService registrationHandlerService) {
        this.registrationHandlerService = registrationHandlerService;
    }

    /**
     * Spring AMQP Listener for Resource Registration requests from Registration Handler.
     *
     * @param cloudResources a list of add or update requests coming from Registration Handler
     * @return a list of the newly registered/updated CloudResources
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "${rabbit.queueName.platformRegistry.update}",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete = "${rabbit.exchange.platformRegistry.autodelete}",
                            exclusive = "false",
                            arguments= {
                                    @Argument(
                                            name = "x-message-ttl",
                                            value="${spring.rabbitmq.template.reply-timeout}",
                                            type="java.lang.Integer")}),
                    exchange = @Exchange(
                            value = "${rabbit.exchange.platformRegistry.name}",
                            ignoreDeclarationExceptions = "true",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete  = "${rabbit.exchange.platformRegistry.autodelete}",
                            internal = "${rabbit.exchange.platformRegistry.internal}",
                            type = "${rabbit.exchange.platformRegistry.type}"),
                    key = "${rabbit.routingKey.platformRegistry.update}")
    )
    public List<CloudResource> addOrUpdate(List<CloudResource> cloudResources) {
        log.trace("Received resource add or update request from registration Handler: " +
                ReflectionToStringBuilder.toString(cloudResources));

        // ToDo: rework this to return proper error messages and/or do not requeue the request
        try {
            return registrationHandlerService.addOrUpdatePlatformResources(cloudResources);
        } catch (Exception e) {
            log.info("Exception thrown during saving platform resources", e);
        }

        return new ArrayList<>();
    }


    /**
     * Spring AMQP Listener for Resource Removal requests from Registration Handler.
     *
     * @param internalIds contains a list of resource internal ids to be deleted
     * @return a list of the removed internalIds
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${rabbit.queueName.platformRegistry.delete}",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete = "${rabbit.exchange.platformRegistry.autodelete}",
                            exclusive = "false",
                            arguments= {
                                    @Argument(
                                            name = "x-message-ttl",
                                            value="${spring.rabbitmq.template.reply-timeout}",
                                            type="java.lang.Integer")}),
                    exchange = @Exchange(
                            value = "${rabbit.exchange.platformRegistry.name}",
                            ignoreDeclarationExceptions = "true",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete  = "${rabbit.exchange.platformRegistry.autodelete}",
                            internal = "${rabbit.exchange.platformRegistry.internal}",
                            type = "${rabbit.exchange.platformRegistry.type}"),
                    key = "${rabbit.routingKey.platformRegistry.delete}")
    )
    public List<String> removeFederatedResources(List<String> internalIds) {
        log.trace("Received resource removal request from registration Handler: " +
                ReflectionToStringBuilder.toString(internalIds));

        // ToDo: rework this to return proper error messages and/or do not requeue the request
        try {
            return registrationHandlerService.removePlatformResources(internalIds);
        } catch (Exception e) {
            log.info("Exception thrown during removing platform resources", e);
        }

        return new ArrayList<>();
    }


    /**
     * Spring AMQP Listener for sharing resource requests from Registration Handler.
     *
     * @param resourcesToBeShared a map with key the federationId and value another map, which has as key
     *                            the internalId of the resource to be shared and as value its bartering status
     * @return a list of the updated CloudResources
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${rabbit.queueName.platformRegistry.share}",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete = "${rabbit.exchange.platformRegistry.autodelete}",
                            exclusive = "false",
                            arguments= {
                                    @Argument(
                                            name = "x-message-ttl",
                                            value="${spring.rabbitmq.template.reply-timeout}",
                                            type="java.lang.Integer")}),
                    exchange = @Exchange(
                            value = "${rabbit.exchange.platformRegistry.name}",
                            ignoreDeclarationExceptions = "true",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete  = "${rabbit.exchange.platformRegistry.autodelete}",
                            internal = "${rabbit.exchange.platformRegistry.internal}",
                            type = "${rabbit.exchange.platformRegistry.type}"),
                    key = "${rabbit.routingKey.platformRegistry.share}")
    )
    public List<CloudResource> shareResources(Map<String, Map<String, Boolean>> resourcesToBeShared) {
        log.trace("Received shareResources request from registration Handler: " +
                ReflectionToStringBuilder.toString(resourcesToBeShared));

        // ToDo: rework this to return proper error messages and/or do not requeue the request
        try {
            return registrationHandlerService.shareResources(resourcesToBeShared);
        } catch (Exception e) {
            log.info("Exception thrown during sharing platform resources", e);
        }

        return new ArrayList<>();
    }


    /**
     * Spring AMQP Listener for unsharing resource requests from Registration Handler.
     *
     * @param resourcesToBeUnshared a map with key the federationId and value the list of internalIds to be unshared
     *                              from the federation
     * @return a list of the updated CloudResources
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${rabbit.queueName.platformRegistry.unshare}",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete = "${rabbit.exchange.platformRegistry.autodelete}",
                            exclusive = "false",
                            arguments= {
                                    @Argument(
                                            name = "x-message-ttl",
                                            value="${spring.rabbitmq.template.reply-timeout}",
                                            type="java.lang.Integer")}),
                    exchange = @Exchange(
                            value = "${rabbit.exchange.platformRegistry.name}",
                            ignoreDeclarationExceptions = "true",
                            durable = "${rabbit.exchange.platformRegistry.durable}",
                            autoDelete  = "${rabbit.exchange.platformRegistry.autodelete}",
                            internal = "${rabbit.exchange.platformRegistry.internal}",
                            type = "${rabbit.exchange.platformRegistry.type}"),
                    key = "${rabbit.routingKey.platformRegistry.unshare}")
    )
    public List<CloudResource> unshareResources(Map<String, List<String>> resourcesToBeUnshared) {
        log.trace("Received shareResources request from registration Handler: " +
                ReflectionToStringBuilder.toString(resourcesToBeUnshared));

        // ToDo: rework this to return proper error messages and/or do not requeue the request
        try {
            return registrationHandlerService.unshareResources(resourcesToBeUnshared);
        } catch (Exception e) {
            log.info("Exception thrown during unsharing platform resources", e);
        }

        return new ArrayList<>();
    }
}
