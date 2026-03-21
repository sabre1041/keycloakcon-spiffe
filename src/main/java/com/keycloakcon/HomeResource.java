package com.keycloakcon;

import io.quarkus.oidc.IdToken;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.LinkedHashMap;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import java.io.StringReader;

@Path("/")
@Authenticated
public class HomeResource {

    @Inject
    Template home;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @ConfigProperty(name = "quarkus.oidc.credentials.jwt.token-path")
    String jwtTokenPath;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        String username = idToken.getClaim("preferred_username");
        if (username == null) {
            username = idToken.getName();
        }

        Map<String, Object> claims = new LinkedHashMap<>();
        for (String claimName : idToken.getClaimNames()) {
            Object value = idToken.getClaim(claimName);
            claims.put(claimName, value != null ? value.toString() : "");
        }

        String svidToken = "";
        Map<String, Object> svidClaims = new LinkedHashMap<>();
        try {
            svidToken = Files.readString(Paths.get(jwtTokenPath)).trim();
            svidClaims.put("raw_token", svidToken);
            String[] parts = svidToken.split("\\.");
            if (parts.length >= 2) {
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
                JsonObject json = Json.createReader(new StringReader(payload)).readObject();
                for (String key : json.keySet()) {
                    JsonValue val = json.get(key);
                    svidClaims.put(key, val.toString());
                }
            }
        } catch (Exception e) {
            svidToken = "Unable to read token: " + e.getMessage();
        }

        return home.data("username", username).data("claims", claims).data("svidToken", svidToken).data("svidClaims", svidClaims);
    }
}
