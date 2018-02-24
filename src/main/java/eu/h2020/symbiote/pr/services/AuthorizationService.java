package eu.h2020.symbiote.pr.services;

import eu.h2020.symbiote.pr.helpers.AuthorizationServiceHelper;
import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import io.jsonwebtoken.Claims;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/22/2018.
 */
@Service
public class AuthorizationService {
    private static Log log = LogFactory.getLog(AuthorizationService.class);

    private String platformId;
    private String componentOwnerName;
    private String componentOwnerPassword;
    private String aamAddress;
    private String clientId;
    private String keystoreName;
    private String keystorePass;
    private Boolean securityEnabled;

    private IComponentSecurityHandler componentSecurityHandler;

    public AuthorizationService(@Value("${platform.id}") String platformId,
                                @Value("${symbIoTe.component.username}") String componentOwnerName,
                                @Value("${symbIoTe.component.password}") String componentOwnerPassword,
                                @Value("${symbIoTe.localaam.url}") String aamAddress,
                                @Value("${symbIoTe.component.clientId}") String clientId,
                                @Value("${symbIoTe.component.keystore.path}") String keystoreName,
                                @Value("${symbIoTe.component.keystore.password}") String keystorePass,
                                @Value("${symbIoTe.aam.integration}") Boolean securityEnabled)
            throws SecurityHandlerException {

        Assert.notNull(platformId,"platformId can not be null!");
        this.platformId = platformId;

        Assert.notNull(componentOwnerName,"componentOwnerName can not be null!");
        this.componentOwnerName = componentOwnerName;

        Assert.notNull(componentOwnerPassword,"componentOwnerPassword can not be null!");
        this.componentOwnerPassword = componentOwnerPassword;

        Assert.notNull(aamAddress,"aamAddress can not be null!");
        this.aamAddress = aamAddress;

        Assert.notNull(clientId,"clientId can not be null!");
        this.clientId = clientId;

        Assert.notNull(keystoreName,"keystoreName can not be null!");
        this.keystoreName = keystoreName;

        Assert.notNull(keystorePass,"keystorePass can not be null!");
        this.keystorePass = keystorePass;

        Assert.notNull(securityEnabled,"securityEnabled can not be null!");
        this.securityEnabled = securityEnabled;

        if (securityEnabled)
            enableSecurity();
    }

    public ResponseEntity checkListResourcesRequest(HttpHeaders httpHeaders, String serviceResponse) {
        if (securityEnabled) {
            if (httpHeaders == null)
                return AuthorizationServiceHelper.addSecurityService(
                        "HttpHeaders are null", new HttpHeaders(),
                        HttpStatus.BAD_REQUEST, serviceResponse);

            SecurityRequest securityRequest;
            try {
                securityRequest = new SecurityRequest(httpHeaders.toSingleValueMap());
                log.debug("Received SecurityRequest of listResources request to be verified: (" + securityRequest + ")");
            } catch (InvalidArgumentsException e) {
                log.info("Could not create the SecurityRequest", e);
                return AuthorizationServiceHelper.addSecurityService(
                        e.getErrorMessage(), new HttpHeaders(),
                        HttpStatus.BAD_REQUEST, serviceResponse);
            }

            Set<String> checkedPolicies;
            try {
                checkedPolicies = checkSingleLocalHomeTokenAccessPolicy(securityRequest);
            } catch (Exception e) {
                log.info("Could not verify the access policies", e);
                return AuthorizationServiceHelper.addSecurityService(
                        e.getMessage(), new HttpHeaders(),
                        HttpStatus.INTERNAL_SERVER_ERROR, serviceResponse);
            }

            if (checkedPolicies.size() >= 1) {
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                return AuthorizationServiceHelper.addSecurityService(
                        "The stored resource access policy was not satisfied",
                        new HttpHeaders(), HttpStatus.UNAUTHORIZED, serviceResponse);
            }
        } else {
            log.debug("checkAccess: Security is disabled");

            //if security is disabled in properties
            return new ResponseEntity<>("Security disabled", HttpStatus.OK);
        }
    }

    public ResponseEntity generateServiceResponse() {
        if (securityEnabled) {
            try {
                String serviceResponse = componentSecurityHandler.generateServiceResponse();
                return new ResponseEntity<>(serviceResponse, HttpStatus.OK);
            } catch (SecurityHandlerException e) {
                log.info("Failed to generate a service response", e);
                return new ResponseEntity<>(e.getErrorMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            String message = "generateServiceResponse: Security is disabled";
            log.debug(message);
            return new ResponseEntity<>(message, HttpStatus.OK);
        }

    }

    private void enableSecurity() throws SecurityHandlerException {
        securityEnabled = true;
        componentSecurityHandler = ComponentSecurityHandlerFactory.getComponentSecurityHandler(
                keystoreName,
                keystorePass,
                clientId,
                aamAddress,
                componentOwnerName,
                componentOwnerPassword);

    }

    private Set<String> checkSingleLocalHomeTokenAccessPolicy(SecurityRequest securityRequest)
            throws Exception {
        Map<String, IAccessPolicy> accessPoliciesMap = new HashMap<>();
        Map<String, String> requiredClaims = new HashMap<>();

        requiredClaims.put(Claims.ISSUER, platformId);

        // Construct policy
        IAccessPolicy policy = AccessPolicyFactory.getAccessPolicy(
                new SingleTokenAccessPolicySpecifier(AccessPolicyType.SLHTAP, requiredClaims));
        accessPoliciesMap.put("SingleLocalHomeTokenAccessPolicy", policy);

        return componentSecurityHandler.getSatisfiedPoliciesIdentifiers(accessPoliciesMap, securityRequest);
    }
}

