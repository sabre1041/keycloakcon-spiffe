package com.keycloakcon;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
@Priority(500)
@Unremovable
public class SpiffeClientAssertionTypeRequestFilter implements OidcRequestFilter {

    private static final Logger LOG = Logger.getLogger(SpiffeClientAssertionTypeRequestFilter.class);

    private static final String DEFAULT_ASSERTION_TYPE = "urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer";
    private static final String SPIFFE_ASSERTION_TYPE = "urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-spiffe";

    @Override
    public Uni<Void> filter(OidcRequestFilterContext context) {
        Buffer body = context.requestBody();
        if (body != null) {
            String bodyString = body.toString();
            LOG.debugf("Checking request body: %s", bodyString);
            if (bodyString.contains(DEFAULT_ASSERTION_TYPE)) {
                LOG.info("Replacing default JWT bearer assertion type with SPIFFE assertion type");
                bodyString = bodyString.replace(DEFAULT_ASSERTION_TYPE, SPIFFE_ASSERTION_TYPE);
                context.requestBody(Buffer.buffer(bodyString));
            } else {
                LOG.debug("Request body does not contain default assertion type, no replacement needed");
            }
        } else {
            LOG.warn("Request body is null, skipping assertion type replacement");
        }
        return Uni.createFrom().voidItem();
    }
}
