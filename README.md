# KeycloakCon SPIFFE

Demonstrates the use of the [SPIFFE Identity Provider](https://www.keycloak.org/docs/latest/server_admin/index.html#_identity_broker_spiffe) for [Keycloak](https://www.keycloak.org/) enabling a [Quarkus](https://quarkus.io/) based to use Client Assertion for a Keycloak Confidential client to avoid the use of Client ID's and Secrets. 

## Prerequisites

* Keycloak 26.5.0+
* Kubernetes Cluster with SPIFFE deployed / OpenShift with Zero Trust Workload Identity Manager (ZTWIM)

### SPIFFE 

Within a standard Kubernetes, deploy SPIFFE using your preferred method ([Helm Charts](https://github.com/spiffe/helm-charts-hardened) recommended). Ensure that the the SPIRE Server property `default_jwt_svid_ttl` is set to a short timespan (such as 5m). When deploying Zero Trust Workload Identity Manager on OpenShift, the default value of `default_jwt_svid_ttl` is set at `5m0s` so no further modifications are necessary.

### Keycloak

Deploy an instance of Keycloak to an infrastructure provider of your choosing (traditional machine or container/Kubernetes). When starting the Keycloak server, the `client-auth-federated` and `spiffe` features must be enabled.

See the [Keycloak Configuration](#keycloak-configuration) below for steps to configure the Keycloak instance.

## Set Environment Variables

Before beginning to configure components and deploy the Quarkus application, set the following environment variables

Set the SPIFFE trust domain

```bash
SPIFFE_TRUST_DOMAIN=<spiffe_trust_domain>
```

Set the hostname for the _Ingress_ that will be created for the Quarkus application

```bash
QUARKUS_HOST=<quarkus_host>
```

Set whether the Kubernetes cluster is an OpenShift environment

```bash
OPENSHIFT=<true|false>
```

## Keycloak Configuration

Utilize these steps to configure the already deployed Keycloak instance.

### Create the keycloakcon Realm

1. Click **Manage realms**
2. Click **Create realm**
3. Enter `keycloakcon`
4. Click **Create**

### Create the SPIFFE Identity Provider

1. Within the _keycloakcon_ realm, click **Identity Providers**
2. Click **SPIFFE**. If you do not see it present in the list of available Identity Providers, ensure that Keycloak has been started with the required features enabled.
3. Leave the _alias_ at the default value (`spiffe`)
4. Enter the SPIFFE Trust Domain (in format `spiffe://$SPIFFE_TRUST_DOMAIN`)
5. Enter the location of the SPIFFE OIDC JWKs endpoint. This value can be found within the OIDC Well Known Endpoint documentation

### Create a Keycloak Client for the Quarkus Application

Create a Confidential Client representing the Quarkus application

1. Within the _keycloakcon_ realm, click **Clients**
2. Click **Create client**
3. Enter `keycloakcon-spiffe-webapp` in the _Client ID_ field and click **Next**
4. Enable **Client Authentication** and select **Service account roles** and click **Next**
5. In the _Valid redirect URIs_ field, enter the URL of the Quarkus Application and click **Save**
6. On the _Credentials_ tab, under _Client authenticator_, select **Signed JWT - Federated**
7. Enter `spiffe` in the _Identity provider_ field and enter `spiffe://$SPIFFE_TRUST_DOMAIN/ns/keycloakcon/sa/keycloakcon-spiffe-webapp` in the _Federated subject_ field. Click **Save**

Note: The values within the Federated subject may differ if you have chosen to customize either SPIFFE or the deployment of the Quarkus application

### Create Users

Create a user to authenticate to the Quarkus application

1. Within the _keycloakcon_ realm, click **Users**
2. Click **Create a new user**
3. Toggle **Email verified** to enabled
4. Enter a _Username_ (such as `keycloakcon-user`)
5. Enter an _Email_ (such as `keycloakcon-user@example.com`)
6. Enter a _First name_ (such as `KeycloakCon`)
7. Enter a _Last name_ (such as `User`)
8. Click **Create**
9. Click the _Credentials_ tab and click **Set password**
10. Fill in a desired password in the _Password_ and _Password Confirmation_ fields. Unselect _Temporary_ and then click **Save**

##  Quarkus Application Deployment

The Quarkus application is deployed to a Kubernetes/OpenShift cluster using Helm. The following variables are required

* `keycloak.issuerURL` - URL of the Keycloak `keycloakcon` realm
* `ingress.host` - Hostname that will be exposed for this application
* `openshift` - Whether to target a deployment to an OpenShift environment

Deploy the application by running the following command:

```bash
helm upgrade -i -n keycloakcon --create-namespace  keycloakcon-spiffe-webapp charts/keycloakcon-spiffe-webapp --set keycloak.issuerURL=$KEYCLOAK_ISSUER_URL --set ingress.host=$QUARKUS_HOST --set openshift=$OPENSHIFT
```

Navigate to the host as defined in the $QUARKUS_HOST field. You should be redirected to Keycloak to authenticate with the user created within the _keycloakcon_ realm. Once successfully authenticated, details related to the SPIFFE JWT used for client assertion along with the JWT of the authenticated user will be displayed.
