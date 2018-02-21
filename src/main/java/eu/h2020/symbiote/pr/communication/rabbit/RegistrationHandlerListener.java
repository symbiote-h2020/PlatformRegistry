package eu.h2020.symbiote.pr.communication.rabbit;

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
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${rabbit.queueName.platformRegistry.rhRegistrationRequest}", durable = "${rabbit.exchange.platformRegistry.durable}",
                    autoDelete = "${rabbit.exchange.platformRegistry.autodelete}", exclusive = "false"),
            exchange = @Exchange(value = "${rabbit.exchange.platformRegistry.name}", ignoreDeclarationExceptions = "true",
                    durable = "${rabbit.exchange.platformRegistry.durable}", autoDelete  = "${rabbit.exchange.platformRegistry.autodelete}",
                    internal = "${rabbit.exchange.platformRegistry.internal}", type = "${rabbit.exchange.platformRegistry.type}"),
            key = "${rabbit.routingKey.platformRegistry.rhRegistrationRequest}")
    )
    public Map<String, Map<String,String>> registerResources(List<FederatedCloudResource> federatedCloudResources) {
        log.trace("Received resource registration request From registration Handler: " +
                ReflectionToStringBuilder.toString(federatedCloudResources));
        return resourceService.savePlatformResources(federatedCloudResources);
    }
}
