/*
 * Copyright (c) 2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 *
 */

package org.wso2.carbon.identity.sso.agent.bean;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.opensaml.common.xml.SAMLConstants;
import org.wso2.carbon.identity.sso.agent.AESDecryptor;
import org.wso2.carbon.identity.sso.agent.util.SSOAgentConstants;
import org.wso2.carbon.identity.sso.agent.exception.SSOAgentException;
import org.wso2.carbon.identity.sso.agent.openid.AttributesRequestor;
import org.wso2.carbon.identity.sso.agent.security.SSOAgentCarbonX509Credential;
import org.wso2.carbon.identity.sso.agent.security.SSOAgentX509Credential;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class SSOAgentConfig {

    private static final Logger LOGGER = Logger.getLogger(SSOAgentConstants.LOGGER_NAME);
    private static final String ARGUMENT = "sun.java.command";

    private Boolean isSAML2SSOLoginEnabled = false;
    private Boolean isOpenIdLoginEnabled = false;
    private Boolean isOAuth2SAML2GrantEnabled = false;

    private String saml2SSOURL = null;
    private String openIdURL = null;
    private String oauth2SAML2GrantURL = null;
    private Set<String> skipURIs = new HashSet<String>();

    private Map<String, String[]> queryParams = new HashMap<String, String[]>();

    private SAML2 saml2 = new SAML2();
    private OpenID openId = new OpenID();
    private OIDC oidc = new OIDC();
    private OAuth2 oauth2 = new OAuth2();
    private String requestQueryParameters;
    private Boolean enableHostNameVerification = false;
    private Boolean enableSSLVerification = false;
    private InputStream keyStoreStream;
    private String keyStorePassword;
    private KeyStore keyStore;
    private String privateKeyPassword;
    private String privateKeyAlias;
    private String idpPublicCertAlias;

    public Boolean getEnableHostNameVerification() {
        return enableHostNameVerification;
    }

    public Boolean getEnableSSLVerification() {
        return enableSSLVerification;
    }

    public String getRequestQueryParameters() {
        return requestQueryParameters;
    }

    public Boolean isSAML2SSOLoginEnabled() {
        return isSAML2SSOLoginEnabled;
    }

    public Boolean isOpenIdLoginEnabled() {
        return isOpenIdLoginEnabled;
    }

    public Boolean isOAuth2SAML2GrantEnabled() {
        return isOAuth2SAML2GrantEnabled;
    }

    public String getSAML2SSOURL() {
        return saml2SSOURL;
    }

    public void setSAML2SSOURL(String saml2SSOURL) {
        this.saml2SSOURL = saml2SSOURL;
    }

    public String getOpenIdURL() {
        return openIdURL;
    }

    public void setOpenIdURL(String openIdURL) {
        this.openIdURL = openIdURL;
    }

    public String getOAuth2SAML2GrantURL() {
        return oauth2SAML2GrantURL;
    }

    public void setOAuth2SAML2GrantURL(String oauth2SAML2GrantURL) {
        this.oauth2SAML2GrantURL = oauth2SAML2GrantURL;
    }

    public Set<String> getSkipURIs() {
        return skipURIs;
    }

    public void setSkipURIs(Set<String> skipURIs) {
        this.skipURIs = skipURIs;
    }

    public Map<String, String[]> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String[]> queryParams) {
        this.queryParams = queryParams;
    }

    public SAML2 getSAML2() {
        return saml2;
    }

    public OAuth2 getOAuth2() {
        return oauth2;
    }

    public OpenID getOpenId() {
        return openId;
    }

    public OIDC getOidc() { return oidc; }

    public void setSAML2SSOLoginEnabled(Boolean isSAML2SSOLoginEnabled) {
        this.isSAML2SSOLoginEnabled = isSAML2SSOLoginEnabled;
    }

    public void setOpenIdLoginEnabled(Boolean isOpenIdLoginEnabled) {
        this.isOpenIdLoginEnabled = isOpenIdLoginEnabled;
    }

    public void setOAuth2SAML2GrantEnabled(Boolean isOAuth2SAML2GrantEnabled) {
        this.isOAuth2SAML2GrantEnabled = isOAuth2SAML2GrantEnabled;
    }

    private InputStream getKeyStoreStream() {
        return keyStoreStream;
    }

    public void setKeyStoreStream(InputStream keyStoreStream) {
        if (this.keyStoreStream == null) {
            this.keyStoreStream = keyStoreStream;
        }
    }

    public String getPrivateKeyPassword() {
        return privateKeyPassword;
    }

    public String getPrivateKeyAlias() {
        return privateKeyAlias;
    }

    public String getIdPPublicCertAlias() {
        return idpPublicCertAlias;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public KeyStore getKeyStore() throws org.wso2.carbon.identity.sso.agent.exception.SSOAgentException {
        if (keyStore == null) {
            setKeyStore(readKeyStore(getKeyStoreStream(), getKeyStorePassword()));
        }
        return keyStore;
    }

    public void setKeyStore(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    public void initConfig(Properties properties) throws SSOAgentException {

        String decodedPassword;
        boolean isReadpassword = false;
        char[] password = null;

        // Get copy of properties for looping in order to avoid ConcurrentModificationException.
        Properties copyOfProperties = new Properties();
        copyOfProperties.putAll(properties);

        // Path of the password file.
        String filePath = System.getProperty("catalina.home") + SSOAgentConstants.SSOAgentConfig.PASSWORD_FILEPATH;

        // Looping through properties to check the encrypted property value by checking the prefix Enc:.
        for (Map.Entry<Object, Object> entry : copyOfProperties.entrySet()) {
            if (String.valueOf(entry.getValue()).startsWith("Enc:")) {
                if (!System.getProperty(ARGUMENT).contains("password")) {

                    // Check whether the password has been already read.
                    if (!isReadpassword) {
                        Path path = Paths.get(filePath);
                        try (BufferedReader reader = Files.newBufferedReader(path, Charset.forName("UTF-8"))) {
                            StringBuilder currentLine = new StringBuilder();

                            // Read the password from the password file.
                            currentLine.append(reader.readLine());
                            if (currentLine.length() > 0) {
                                password = new char[currentLine.length()];
                                currentLine.getChars(0, currentLine.length(), password, 0);
                                currentLine = null;
                            }
                            isReadpassword = true;
                            if (Files.deleteIfExists(path)) {
                                LOGGER.info("Deleted the temporary password file at " + path);
                            }
                        } catch (IOException ex) {
                            throw new SSOAgentException("Error while reading the file ", ex);
                        }
                    }
                } else if (!isReadpassword) {

                    // Read password from the console.
                    System.out.print("Enter password for decryption:");
                    password = System.console().readPassword();
                    isReadpassword = true;
                }
                if (ArrayUtils.isEmpty(password)) {
                    LOGGER.log(Level.SEVERE, "Can't find the password to decrypt the encrypted values.");
                    return;
                }

                // Get the encrypted property value.
                String encryptedValue = String.valueOf(entry.getValue());

                // Remove the Enc: prefix and get the actual encrypted value.
                if (encryptedValue.split(":").length > 1) {
                    decodedPassword = AESDecryptor.decrypt(String.valueOf(entry.getValue()).split
                            (":")[1].trim(), password);

                    // Remove the encrypted property value and replace with decrypted property value (plain text)
                    properties.remove(String.valueOf(entry.getKey()));
                    properties.setProperty(String.valueOf(entry.getKey()), decodedPassword);
                } else {
                    LOGGER.log(Level.SEVERE, "Encrypted value is not in the correct format. Encrypted value " +
                            "must contain the encrypted value with Enc: as prefix.");
                    return;
                }
            }
        }

        // Delete the stored password from memory by filling with zeros.
        if (password != null) {
            Arrays.fill(password, (char) 0);
        }
        privateKeyPassword = properties.getProperty("PrivateKeyPassword");
        privateKeyAlias = properties.getProperty("PrivateKeyAlias");
        idpPublicCertAlias = properties.getProperty("IdPPublicCertAlias");
        requestQueryParameters = properties.getProperty("SAML.Request.Query.Param");
        if (properties.getProperty("SSL.EnableSSLVerification") != null) {
            enableSSLVerification = Boolean.parseBoolean(properties.getProperty("SSL.EnableSSLVerification"));
        }
        if (properties.getProperty("SSL.EnableSSLHostNameVerification") != null) {
            enableHostNameVerification =
                    Boolean.parseBoolean(properties.getProperty("SSL.EnableSSLHostNameVerification"));
        }
        String isSAML2SSOLoginEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.ENABLE_SAML2_SSO_LOGIN);
        if (isSAML2SSOLoginEnabledString != null) {
            isSAML2SSOLoginEnabled = Boolean.parseBoolean(isSAML2SSOLoginEnabledString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.ENABLE_SAML2_SSO_LOGIN +
                    " not configured. Defaulting to \'false\'");
            isSAML2SSOLoginEnabled = false;
        }

        String isOpenIdLoginEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.ENABLE_OPENID_SSO_LOGIN);
        if (isOpenIdLoginEnabledString != null) {
            isOpenIdLoginEnabled = Boolean.parseBoolean(isOpenIdLoginEnabledString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.ENABLE_OPENID_SSO_LOGIN +
                    " not configured. Defaulting to \'false\'");
            isOpenIdLoginEnabled = false;
        }

        String isSAML2OAuth2GrantEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.ENABLE_OAUTH2_SAML2_OAUTH2_GRANT);
        if (isSAML2OAuth2GrantEnabledString != null) {
            isOAuth2SAML2GrantEnabled = Boolean.parseBoolean(isSAML2OAuth2GrantEnabledString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.ENABLE_OAUTH2_SAML2_OAUTH2_GRANT +
                    " not configured. Defaulting to \'false\'");
            isOAuth2SAML2GrantEnabled = false;
        }

        saml2SSOURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2_SSO_URL);
        openIdURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.OPENID_URL);
        oauth2SAML2GrantURL = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.OAUTH2_SAML2_GRANT_URL);

        String skipURIsString = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SKIP_URIS);
        if (!StringUtils.isBlank(skipURIsString)) {
            String[] skipURIArray = skipURIsString.split(",");
            for (String skipURI : skipURIArray) {
                skipURIs.add(skipURI);
            }
        }

        String queryParamsString = properties.getProperty(SSOAgentConstants.SSOAgentConfig.QUERY_PARAMS);
        if (!StringUtils.isBlank(queryParamsString)) {
            String[] queryParamsArray = queryParamsString.split("&");
            Map<String, List<String>> queryParamMap = new HashMap<String, List<String>>();
            if (queryParamsArray.length > 0) {
                for (String queryParam : queryParamsArray) {
                    String[] splitParam = queryParam.split("=");
                    if (splitParam.length == 2) {
                        if (queryParamMap.get(splitParam[0]) != null) {
                            queryParamMap.get(splitParam[0]).add(splitParam[1]);
                        } else {
                            List<String> newList = new ArrayList<String>();
                            newList.add(splitParam[1]);
                            queryParamMap.put(splitParam[0], newList);
                        }
                    }

                }
                for (Map.Entry<String, List<String>> entry : queryParamMap.entrySet()) {
                    String[] valueArray = entry.getValue().toArray(new String[entry.getValue().size()]);
                    queryParams.put(entry.getKey(), valueArray);
                }
            }
        }

        saml2.httpBinding = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.HTTP_BINDING);
        if (saml2.httpBinding == null || saml2.httpBinding.isEmpty()) {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.HTTP_BINDING +
                    " not configured. Defaulting to \'" + SAMLConstants.SAML2_POST_BINDING_URI + "\'");
            saml2.httpBinding = SAMLConstants.SAML2_POST_BINDING_URI;
        }
        saml2.spEntityId = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.SP_ENTITY_ID);
        saml2.acsURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.ACS_URL);
        saml2.idPEntityId = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.IDP_ENTITY_ID);
        saml2.idPURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.IDP_URL);
        saml2.attributeConsumingServiceIndex = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ATTRIBUTE_CONSUMING_SERVICE_INDEX);

        String isSLOEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_SLO);
        if (isSLOEnabledString != null) {
            saml2.isSLOEnabled = Boolean.parseBoolean(isSLOEnabledString);
        } else {
            LOGGER.info("\'" + SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_SLO +
                    "\' not configured. Defaulting to \'false\'");
            saml2.isSLOEnabled = false;
        }
        saml2.sloURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.SLO_URL);

        String isAssertionSignedString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_ASSERTION_SIGNING);
        if (isAssertionSignedString != null) {
            saml2.isAssertionSigned = Boolean.parseBoolean(isAssertionSignedString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_ASSERTION_SIGNING +
                    " not configured. Defaulting to \'false\'");
            saml2.isAssertionSigned = false;
        }

        String isAssertionEncryptedString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_ASSERTION_ENCRYPTION);
        if (isAssertionEncryptedString != null) {
            saml2.isAssertionEncrypted = Boolean.parseBoolean(isAssertionEncryptedString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_ASSERTION_ENCRYPTION +
                    " not configured. Defaulting to \'false\'");
            saml2.isAssertionEncrypted = false;
        }

        String isResponseSignedString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_RESPONSE_SIGNING);
        if (isResponseSignedString != null) {
            saml2.isResponseSigned = Boolean.parseBoolean(isResponseSignedString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_RESPONSE_SIGNING +
                    " not configured. Defaulting to \'false\'");
            saml2.isResponseSigned = false;
        }

        if (saml2.isResponseSigned() || saml2.isAssertionSigned()) {
            String signatureValidatorImplClass = properties.getProperty(
                    SSOAgentConstants.SSOAgentConfig.SAML2.SIGNATURE_VALIDATOR);
            if (signatureValidatorImplClass != null) {
                saml2.signatureValidatorImplClass = signatureValidatorImplClass;
            } else {
                LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.SIGNATURE_VALIDATOR +
                                       " not configured.");
            }
        }

        String isRequestSignedString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_REQUEST_SIGNING);
        if (isRequestSignedString != null) {
            saml2.isRequestSigned = Boolean.parseBoolean(isRequestSignedString);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_REQUEST_SIGNING +
                    " not configured. Defaulting to \'false\'");
            saml2.isRequestSigned = false;
        }

        String isPassiveAuthnString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.IS_PASSIVE_AUTHN);
        if (isPassiveAuthnString != null) {
            saml2.isPassiveAuthn = Boolean.parseBoolean(isPassiveAuthnString);
        } else {
            LOGGER.log(Level.FINE, "\'" + SSOAgentConstants.SSOAgentConfig.SAML2.IS_PASSIVE_AUTHN +
                    "\' not configured. Defaulting to \'false\'");
            saml2.isPassiveAuthn = false;
        }

        String isForceAuthnString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.IS_FORCE_AUTHN);
        if (isForceAuthnString != null) {
            saml2.isForceAuthn = Boolean.parseBoolean(isForceAuthnString);
        } else {
            LOGGER.log(Level.FINE, "\'" + SSOAgentConstants.SSOAgentConfig.SAML2.IS_FORCE_AUTHN +
                    "\' not configured. Defaulting to \'false\'");
            saml2.isForceAuthn = false;
        }

        saml2.relayState = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.RELAY_STATE);
        saml2.postBindingRequestHTMLPayload = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.SAML2.POST_BINDING_REQUEST_HTML_PAYLOAD);
        saml2.artifactResolveURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.ARTIFACT_RESOLVE_URL);
        saml2.enableArtifactResolveSigning = StringUtils.equals(
                properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.ENABLE_ARTIFACT_RESOLVE_SIGNING), "true");

        oauth2.tokenURL = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.OAuth2.TOKEN_URL);
        oauth2.clientId = properties.getProperty(SSOAgentConstants.SSOAgentConfig.OAuth2.CLIENT_ID);
        oauth2.clientSecret = properties.getProperty(SSOAgentConstants.SSOAgentConfig.OAuth2.CLIENT_SECRET);

        openId.providerURL = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.OpenID.PROVIDER_URL);
        openId.returnToURL = properties.getProperty(SSOAgentConstants.SSOAgentConfig.OpenID.RETURN_TO_URL);

        String isAttributeExchangeEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.OpenID.ENABLE_ATTRIBUTE_EXCHANGE);
        if (isAttributeExchangeEnabledString != null) {
            openId.isAttributeExchangeEnabled = Boolean.parseBoolean(isAttributeExchangeEnabledString);
        } else {
            LOGGER.log(Level.FINE, "\'" + SSOAgentConstants.SSOAgentConfig.OpenID.ENABLE_ATTRIBUTE_EXCHANGE +
                    "\' not configured. Defaulting to \'true\'");
            openId.isAttributeExchangeEnabled = true;
        }

        String isDumbModeEnabledString = properties.getProperty(
                SSOAgentConstants.SSOAgentConfig.OpenID.ENABLE_DUMB_MODE);
        if (isAttributeExchangeEnabledString != null) {
            openId.isDumbModeEnabled = Boolean.parseBoolean(isDumbModeEnabledString);
        } else {
            LOGGER.log(Level.FINE, "\'" + SSOAgentConstants.SSOAgentConfig.OpenID.ENABLE_DUMB_MODE +
                    "\' not configured. Defaulting to \'false\'");
            openId.isDumbModeEnabled = false;
        }

        oidc.setConsumerKey(properties.getProperty(SSOAgentConstants.SSOAgentConfig.OIDC.CONSUMER_KEY));
        oidc.setConsumerSecret(properties.getProperty(SSOAgentConstants.SSOAgentConfig.OIDC.CONSUMER_SECRET));
        oidc.setAuthzEndpoint(properties.getProperty(SSOAgentConstants.SSOAgentConfig.OIDC.OAUTH2_AUTHZ_ENDPOINT));
        oidc.setAuthzGrantType(properties.getProperty(SSOAgentConstants.SSOAgentConfig.OIDC.OAUTH2_GRANT_TYPE));
        oidc.setCallBackUrl(properties.getProperty(SSOAgentConstants.SSOAgentConfig.OIDC.CALL_BACK_URL));
        oidc.setOIDCLogoutEndpoint(properties.getProperty(SSOAgentConstants.SSOAgentConfig.OIDC.OIDC_LOGOUT_ENDPOINT));
        oidc.setSessionIFrameEndpoint(properties.getProperty(SSOAgentConstants.SSOAgentConfig.OIDC
                .OIDC_SESSION_IFRAME_ENDPOINT));
        oidc.setScope(properties.getProperty(SSOAgentConstants.SSOAgentConfig.OIDC.SCOPE));
        oidc.setPostLogoutRedirectUri(properties.getProperty(SSOAgentConstants.SSOAgentConfig.OIDC
                .POST_LOGOUT_REDIRECT_RUI));

        if (properties.getProperty("KeyStore") != null) {
            try {
                keyStoreStream = new FileInputStream(properties.getProperty("KeyStore"));
            } catch (FileNotFoundException e) {
                throw new SSOAgentException("Cannot find file " + properties.getProperty("KeyStore"), e);
            }
        }
        keyStorePassword = properties.getProperty("KeyStorePassword");

        // Check if the assertion validity timeStampSkew is set in config file
        // If that is set, use that as the timeskewperiod
        String timeStampSkew = properties.getProperty(SSOAgentConstants.SSOAgentConfig.SAML2.TIME_STAMP_SKEW);
        if (timeStampSkew != null) {
            saml2.timeStampSkewInSeconds = Integer.parseInt(timeStampSkew);
        } else {
            LOGGER.log(Level.FINE, SSOAgentConstants.SSOAgentConfig.SAML2.TIME_STAMP_SKEW +
                                   " not configured. Defaulting to 300s");
        }


        SSLContext sc;
        try {
            // Get SSL context
            sc = SSLContext.getInstance("SSL");
            doHostNameVerification();
            TrustManager[] trustManagers = doSSLVerification();

            sc.init(null, trustManagers, new java.security.SecureRandom());
            SSLSocketFactory sslSocketFactory = sc.getSocketFactory();
            HttpsURLConnection.setDefaultSSLSocketFactory(sslSocketFactory);

        } catch (Exception e) {
            throw new SSOAgentException("An error in initializing SSL Context");
        }
    }

    public void verifyConfig() throws SSOAgentException {

        if (isSAML2SSOLoginEnabled && saml2SSOURL == null) {
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.SAML2_SSO_URL + "\' not configured");
        }

        if (isOpenIdLoginEnabled && openIdURL == null) {
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.OPENID_URL + "\' not configured");
        }

        if (!isSAML2SSOLoginEnabled && isOAuth2SAML2GrantEnabled) {
            throw new SSOAgentException(
                    "SAML2 SSO Login is disabled. Cannot use SAML2 Bearer Grant type for OAuth2");
        }

        if (isSAML2SSOLoginEnabled && isOAuth2SAML2GrantEnabled && oauth2SAML2GrantURL == null) {
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.OAUTH2_SAML2_GRANT_URL + "\' not configured");
        }

        if (isSAML2SSOLoginEnabled && saml2.spEntityId == null) {
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.SAML2.SP_ENTITY_ID + "\' not configured");
        }

        if (isSAML2SSOLoginEnabled && saml2.acsURL == null) {
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.SAML2.ACS_URL + "\' not configured");
        }

        if (isSAML2SSOLoginEnabled && saml2.idPEntityId == null) {
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.SAML2.IDP_ENTITY_ID + "\' not configured");
        }

        if (isSAML2SSOLoginEnabled && saml2.idPURL == null) {
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.SAML2.IDP_URL + "\' not configured");
        }

        if (isSAML2SSOLoginEnabled && saml2.attributeConsumingServiceIndex == null) {
            LOGGER.log(Level.FINE,
                    "\'" + SSOAgentConstants.SSOAgentConfig.SAML2.ATTRIBUTE_CONSUMING_SERVICE_INDEX +
                            "\' not configured. " + "No attributes of the Subject will be requested");
        }

        if (isSAML2SSOLoginEnabled && saml2.isSLOEnabled && saml2.sloURL == null) {
            throw new SSOAgentException("Single Logout enabled, but SLO URL not configured");
        }

        if (isSAML2SSOLoginEnabled &&
                (saml2.isAssertionSigned || saml2.isAssertionEncrypted || saml2.isResponseSigned ||
                        saml2.isRequestSigned) && saml2.ssoAgentX509Credential == null) {
            LOGGER.log(Level.FINE,
                    "\'SSOAgentX509Credential\' not configured. Defaulting to " +
                            SSOAgentCarbonX509Credential.class.getName());
        }

        if (isSAML2SSOLoginEnabled &&
                (saml2.isAssertionSigned || saml2.isResponseSigned) &&
                saml2.ssoAgentX509Credential.getEntityCertificate() == null) {
            throw new SSOAgentException("Public certificate of IdP not configured");
        }

        if (isSAML2SSOLoginEnabled &&
                (saml2.isRequestSigned || saml2.isAssertionEncrypted) &&
                saml2.ssoAgentX509Credential.getPrivateKey() == null) {
            throw new SSOAgentException("Private key of SP not configured");
        }

        if (isOpenIdLoginEnabled && openId.providerURL == null) {
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.OpenID.PROVIDER_URL + "\' not configured");
        }

        if (isOpenIdLoginEnabled && openId.returnToURL == null) {
            throw new SSOAgentException("\'" +
                    SSOAgentConstants.SSOAgentConfig.OpenID.RETURN_TO_URL + "\' not configured");
        }

        if (isOpenIdLoginEnabled && openId.attributesRequestor == null) {
            LOGGER.log(Level.FINE, "\'" +
                    SSOAgentConstants.SSOAgentConfig.OpenID.PROVIDER_URL +
                    "\' not configured. " + "No attributes of the Subject will be fetched");
        }

        if (isSAML2SSOLoginEnabled && isOAuth2SAML2GrantEnabled && oauth2.tokenURL == null) {
            throw new SSOAgentException("OAuth2 Token endpoint not configured");
        }

        if (isSAML2SSOLoginEnabled && isOAuth2SAML2GrantEnabled && oauth2.clientId == null) {
            throw new SSOAgentException("OAuth2 Client Id not configured");
        }

        if (isSAML2SSOLoginEnabled && isOAuth2SAML2GrantEnabled && oauth2.clientSecret == null) {
            throw new SSOAgentException("OAuth2 Client Secret not configured");
        }

    }

    /**
     * get the key store instance
     *
     * @param is            KeyStore InputStream
     * @param storePassword password of key store
     * @return KeyStore instant
     * @throws org.wso2.carbon.identity.sso.agent.exception.SSOAgentException if fails to load key store
     */
    private KeyStore readKeyStore(InputStream is, String storePassword) throws
                                                                               org.wso2.carbon.identity.sso.agent.exception.SSOAgentException {

        if (storePassword == null) {
            throw new org.wso2.carbon.identity.sso.agent.exception.SSOAgentException("KeyStore password can not be null");
        }

        try {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(is, storePassword.toCharArray());
            return keyStore;
        } catch (Exception e) {

            throw new org.wso2.carbon.identity.sso.agent.exception.SSOAgentException("Error while loading key store file", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {

                    throw new org.wso2.carbon.identity.sso.agent.exception.SSOAgentException("Error while closing input stream of key store", ignored);
                }
            }
        }
    }

    private void doHostNameVerification(){
        if (!this.getEnableHostNameVerification()) {
            // Create empty HostnameVerifier
            HostnameVerifier hv = new HostnameVerifier() {
                public boolean verify(String urlHostName, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        }
    }

    private TrustManager[] doSSLVerification() throws Exception {
        TrustManager[] trustManagers = null;
        if (this.getEnableSSLVerification()) {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(this.getKeyStore());
            trustManagers = tmf.getTrustManagers();
        } else {
            // Create a trust manager that does not validate certificate chains
            trustManagers = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(java.security.cert.X509Certificate[] certs,
                                               String authType) {
                }

                public void checkServerTrusted(java.security.cert.X509Certificate[] certs,
                                               String authType) {
                }
            } };
        }
        return trustManagers;
    }

    public class SAML2 {

        private String httpBinding = null;
        private String spEntityId = null;
        private String acsURL = null;
        private String idPEntityId = null;
        private String idPURL = null;
        private Boolean isSLOEnabled = false;
        private String sloURL = null;
        private String attributeConsumingServiceIndex = null;
        private SSOAgentX509Credential ssoAgentX509Credential = null;
        private Boolean isAssertionSigned = false;
        private Boolean isAssertionEncrypted = false;
        private Boolean isResponseSigned = false;
        private Boolean isRequestSigned = false;
        private Boolean isPassiveAuthn = false;
        private Boolean isForceAuthn = false;
        private String relayState = null;
        private String signatureValidatorImplClass = null;
        private int timeStampSkewInSeconds = 300;
        private String artifactResolveURL = null;
        private boolean enableArtifactResolveSigning;

        public void setArtifactResolveURL(String artifactResolveURL) {

            this.artifactResolveURL = artifactResolveURL;
        }

        public String getArtifactResolveURL() {

            return artifactResolveURL;
        }

        public void setEnableArtifactResolveSigning(boolean enableArtifactResolveSigning) {

            this.enableArtifactResolveSigning = enableArtifactResolveSigning;
        }

        public boolean isEnableArtifactResolveSigning() {

            return enableArtifactResolveSigning;
        }

        /**
         * The html page that will auto-submit the SAML2 to the IdP.
         * This should be in valid HTML syntax, with following section within the
         * auto-submit form.
         * "&lt;!--$saml_params--&gt;"
         * This section will be replaced by the SAML2 parameters.
         * <p/>
         * If the parameter value is empty, null or doesn't have the above
         * section, the default page will be shown
         */
        private String postBindingRequestHTMLPayload = null;

        public String getHttpBinding() {
            return httpBinding;
        }

        public void setHttpBinding(String httpBinding) {
            this.httpBinding = httpBinding;
        }

        public String getSPEntityId() {
            return spEntityId;
        }

        public void setSPEntityId(String spEntityId) {
            this.spEntityId = spEntityId;
        }

        public String getACSURL() {
            return acsURL;
        }

        public void setACSURL(String acsURL) {
            this.acsURL = acsURL;
        }

        public String getIdPEntityId() {
            return idPEntityId;
        }

        public void setIdPEntityId(String idPEntityId) {
            this.idPEntityId = idPEntityId;
        }

        public String getIdPURL() {
            return idPURL;
        }

        public void setIdPURL(String idPURL) {
            this.idPURL = idPURL;
        }

        public Boolean isSLOEnabled() {
            return isSLOEnabled;
        }

        public String getSLOURL() {
            return sloURL;
        }

        public void setSLOURL(String sloURL) {
            this.sloURL = sloURL;
        }

        public String getAttributeConsumingServiceIndex() {
            return attributeConsumingServiceIndex;
        }

        public void setAttributeConsumingServiceIndex(String attributeConsumingServiceIndex) {
            this.attributeConsumingServiceIndex = attributeConsumingServiceIndex;
        }

        public SSOAgentX509Credential getSSOAgentX509Credential() {
            return ssoAgentX509Credential;
        }

        public void setSSOAgentX509Credential(SSOAgentX509Credential ssoAgentX509Credential) {
            this.ssoAgentX509Credential = ssoAgentX509Credential;
        }

        public Boolean isAssertionSigned() {
            return isAssertionSigned;
        }

        public Boolean isAssertionEncrypted() {
            return isAssertionEncrypted;
        }

        public Boolean isResponseSigned() {
            return isResponseSigned;
        }

        public Boolean isRequestSigned() {
            return isRequestSigned;
        }

        public Boolean isPassiveAuthn() {
            return isPassiveAuthn;
        }

        public Boolean isForceAuthn() {
            return isForceAuthn;
        }

        public String getRelayState() {
            return relayState;
        }

        public void setRelayState(String relayState) {
            this.relayState = relayState;
        }

        public String getPostBindingRequestHTMLPayload() {
            return postBindingRequestHTMLPayload;
        }

        public void setPostBindingRequestHTMLPayload(String postBindingRequestHTMLPayload) {
            this.postBindingRequestHTMLPayload = postBindingRequestHTMLPayload;
        }

        public void setSLOEnabled(Boolean isSLOEnabled) {
            this.isSLOEnabled = isSLOEnabled;
        }

        public void setAssertionSigned(Boolean isAssertionSigned) {
            this.isAssertionSigned = isAssertionSigned;
        }

        public void setAssertionEncrypted(Boolean isAssertionEncrypted) {
            this.isAssertionEncrypted = isAssertionEncrypted;
        }

        public void setResponseSigned(Boolean isResponseSigned) {
            this.isResponseSigned = isResponseSigned;
        }

        public void setRequestSigned(Boolean isRequestSigned) {
            this.isRequestSigned = isRequestSigned;
        }

        public void setPassiveAuthn(Boolean isPassiveAuthn) {
            this.isPassiveAuthn = isPassiveAuthn;
        }

        public void setForceAuthn(Boolean isForceAuthn) {
            this.isForceAuthn = isForceAuthn;
        }

        public String getSignatureValidatorImplClass() {
            return signatureValidatorImplClass;
        }

        public int getTimeStampSkewInSeconds() {
            return timeStampSkewInSeconds;
        }
    }

    public class OpenID {

        private String mode = null;
        private String providerURL = null;
        private String returnToURL = null;
        private String claimedId = null;
        private AttributesRequestor attributesRequestor = null;
        private boolean isAttributeExchangeEnabled = false;
        private boolean isDumbModeEnabled = false;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getProviderURL() {
            return providerURL;
        }

        public void setProviderURL(String providerURL) {
            this.providerURL = providerURL;
        }

        public String getReturnToURL() {
            return returnToURL;
        }

        public void setReturnToURL(String returnToURL) {
            this.returnToURL = returnToURL;
        }

        public String getClaimedId() {
            return claimedId;
        }

        public void setClaimedId(String claimedId) {
            this.claimedId = claimedId;
        }

        public AttributesRequestor getAttributesRequestor() {
            return attributesRequestor;
        }

        public void setAttributesRequestor(AttributesRequestor attributesRequestor) {
            this.attributesRequestor = attributesRequestor;
        }

        public boolean isAttributeExchangeEnabled() {
            return isAttributeExchangeEnabled;
        }

        public void setAttributeExchangeEnabled(boolean isAttributeExchangeEnabled) {
            this.isAttributeExchangeEnabled = isAttributeExchangeEnabled;
        }

        public boolean isDumbModeEnabled() {
            return isDumbModeEnabled;
        }

        public void setDumbModeEnabled(boolean isDumbModeEnabled) {
            this.isDumbModeEnabled = isDumbModeEnabled;
        }
    }

    public class OIDC {

        private String consumerKey = StringUtils.EMPTY;
        private String consumerSecret = StringUtils.EMPTY;
        private String authzEndpoint = StringUtils.EMPTY;
        private String authzGrantType = StringUtils.EMPTY;
        private String callBackUrl = StringUtils.EMPTY;
        private String OIDCLogoutEndpoint = StringUtils.EMPTY;
        private String sessionIFrameEndpoint = StringUtils.EMPTY;

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        private String scope = StringUtils.EMPTY;
        private String postLogoutRedirectUri = StringUtils.EMPTY;

        public String getConsumerKey() {
            return consumerKey;
        }

        public void setConsumerKey(String consumerKey) {
            this.consumerKey = consumerKey;
        }

        public String getAuthzEndpoint() {
            return authzEndpoint;
        }

        public void setAuthzEndpoint(String authzEndpoint) {
            this.authzEndpoint = authzEndpoint;
        }
        public String getAuthzGrantType() {
            return authzGrantType;
        }

        public void setAuthzGrantType(String authzGrantType) {
            this.authzGrantType = authzGrantType;
        }

        public String getCallBackUrl() {
            return callBackUrl;
        }

        public void setCallBackUrl(String callBackUrl) {
            this.callBackUrl = callBackUrl;
        }

        public String getOIDCLogoutEndpoint() {
            return OIDCLogoutEndpoint;
        }

        public void setOIDCLogoutEndpoint(String OIDCLogoutEndpoint) {
            this.OIDCLogoutEndpoint = OIDCLogoutEndpoint;
        }

        public String getSessionIFrameEndpoint() {
            return sessionIFrameEndpoint;
        }

        public void setSessionIFrameEndpoint(String sessionIFrameEndpoint) {
            this.sessionIFrameEndpoint = sessionIFrameEndpoint;
        }

        public String getConsumerSecret() {
            return consumerSecret;
        }

        public void setConsumerSecret(String consumerSecret) {
            this.consumerSecret = consumerSecret;
        }

        public String getPostLogoutRedirectUri() {
            return postLogoutRedirectUri;
        }

        public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
            this.postLogoutRedirectUri = postLogoutRedirectUri;
        }
    }

    public class OAuth2 {

        private String tokenURL = null;
        private String clientId = null;
        private String clientSecret = null;

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getTokenURL() {
            return tokenURL;
        }

        public void setTokenURL(String tokenURL) {
            this.tokenURL = tokenURL;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }
    }
}
