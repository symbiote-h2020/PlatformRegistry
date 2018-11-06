package eu.h2020.symbiote.pr.communication.rabbit;

import eu.h2020.symbiote.cloud.trust.model.TrustEntry;
import eu.h2020.symbiote.pr.services.TrustManagerService;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class is used as a simple listener.
 *
 * @author Ilia Pietri (ICOM)
 * @since 31/05/2018.
 */
@Component
public class TrustManagerListener {
    private static Log log = LogFactory.getLog(TrustManagerListener.class);

    private TrustManagerService trustManagerService;

    @Autowired
    public TrustManagerListener(TrustManagerService trustManagerService) {
        this.trustManagerService = trustManagerService;
    }

    /**
     * Spring AMQP Listener for listening to new FederationResources updates from Trust Manager.
     * @param resourceTrustUpdated message received from Trust Manager
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(
                            value = "${rabbit.queueName.trust.updateAdaptiveResourceTrust}",
                            durable = "${rabbit.exchange.trust.durable}",
                            autoDelete = "${rabbit.exchange.trust.autodelete}",
                            exclusive = "false",
                            arguments= {
                                    @Argument(
                                            name = "x-message-ttl",
                                            value="${spring.rabbitmq.template.reply-timeout}",
                                            type="java.lang.Integer")}
                            ),
                    exchange = @Exchange(
                            value = "${rabbit.exchange.trust}",
                            ignoreDeclarationExceptions = "true",//
                            durable = "${rabbit.exchange.trust.durable}",
                            autoDelete  = "${rabbit.exchange.trust.autodelete}",
                            internal = "${rabbit.exchange.trust.internal}",
                            type = "${rabbit.exchange.trust.type}"),
                    key = "${rabbit.routingKey.trust.updateAdaptiveResourceTrust}")
    )
    public void updateAdaptiveResourceTrust(TrustEntry resourceTrustUpdated) {
        log.info("Received updated adaptive trust of federated resource from Trust Manager: " +
                ReflectionToStringBuilder.toString(resourceTrustUpdated));

        // ToDo: rework this to return proper error messages and/or do not requeue the request
        try {
            trustManagerService.updateFedResAdaptiveResourceTrust(resourceTrustUpdated);
        } catch (Exception e) {
            log.info("Exception thrown during updating trust of federated resources", e);
        }
    }

}
