/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.identity.sso.agent.util;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.util.SecurityManager;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.signature.XMLSignature;
import org.opensaml.Configuration;
import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.ArtifactResolve;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.xml.ConfigurationException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.XMLObjectBuilder;
import org.opensaml.xml.io.Marshaller;
import org.opensaml.xml.io.MarshallerFactory;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallerFactory;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.security.x509.X509Credential;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.SignableXMLObject;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.Signer;
import org.opensaml.xml.signature.X509Data;
import org.opensaml.xml.util.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.wso2.carbon.identity.sso.agent.exception.SSOAgentException;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

public class SSOAgentUtils {

    private static Logger LOGGER = Logger.getLogger(SSOAgentConstants.LOGGER_NAME);
    private static boolean isBootStrapped = false;

    private static Random random = new Random();
    private static final int ENTITY_EXPANSION_LIMIT = 0;

    private SSOAgentUtils() {
    }

    /**
     * Generates a unique Id for Authentication Requests
     *
     * @return generated unique ID
     */

    public static String createID() {

        byte[] bytes = new byte[20]; // 160 bit

        random.nextBytes(bytes);

        char[] charMapping = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p'};

        char[] chars = new char[40];

        for (int i = 0; i < bytes.length; i++) {
            int left = (bytes[i] >> 4) & 0x0f;
            int right = bytes[i] & 0x0f;
            chars[i * 2] = charMapping[left];
            chars[i * 2 + 1] = charMapping[right];
        }

        return String.valueOf(chars);
    }

    public static void doBootstrap() throws SSOAgentException {
        if (!isBootStrapped) {
            try {
                DefaultBootstrap.bootstrap();
                isBootStrapped = true;
            } catch (ConfigurationException e) {
                throw new SSOAgentException("Error in bootstrapping the OpenSAML2 library", e);
            }
        }
    }

    /**
     * Sign the SAML AuthnRequest message
     *
     * @param authnRequest
     * @param signatureAlgorithm
     * @param cred
     * @return
     * @throws SSOAgentException
     */
    public static AuthnRequest setSignature(AuthnRequest authnRequest, String signatureAlgorithm,
                                        X509Credential cred) throws SSOAgentException {
        doBootstrap();
        return setSignatureValue(authnRequest, signatureAlgorithm, cred);
    }

    /**
     * Sign the SAML AuthnRequest message
     *
     * @param logoutRequest
     * @param signatureAlgorithm
     * @param cred
     * @return
     * @throws SSOAgentException
     */
    public static LogoutRequest setSignature(LogoutRequest logoutRequest, String signatureAlgorithm,
                                             X509Credential cred) throws SSOAgentException {

        return setSignatureValue(logoutRequest, signatureAlgorithm, cred);
    }

    /**
     * Sign SAML2 Artifact Resolve.
     *
     * @param artifactResolve    ArtifactResolve object to be signed.
     * @param signatureAlgorithm Signature algorithm.
     * @param cred               X509 Credential.
     * @return Signed Artifact Resolve object.
     * @throws SSOAgentException
     */
    public static ArtifactResolve setSignature(ArtifactResolve artifactResolve, String signatureAlgorithm,
                                               X509Credential cred) throws SSOAgentException {

        return setSignatureValue(artifactResolve, signatureAlgorithm, cred);
    }

    /**
     * Add signature to any singable XML object.
     * @param xmlObject Singable xml object.
     * @param signatureAlgorithm Signature algorithm to be used.
     * @param cred X509 Credentials.
     * @param <T> Singable XML object with signature.
     * @return Singable XML object with signature.
     * @throws SSOAgentException If error occurred.
     */
    public static <T extends SignableXMLObject> T setSignatureValue(T xmlObject, String signatureAlgorithm,
                                                                    X509Credential cred)
            throws SSOAgentException {

        try {
            Signature signature = setSignatureRaw(signatureAlgorithm, cred);
            xmlObject.setSignature(signature);

            List<Signature> signatureList = new ArrayList<>();
            signatureList.add(signature);

            // Marshall and Sign
            MarshallerFactory marshallerFactory =
                    org.opensaml.xml.Configuration.getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(xmlObject);

            marshaller.marshall(xmlObject);

            org.apache.xml.security.Init.init();
            Signer.signObjects(signatureList);
            return xmlObject;
        } catch (Exception e) {
            throw new SSOAgentException("Error while signing the SAML Request message", e);
        }
    }

    private static Signature setSignatureRaw(String signatureAlgorithm, X509Credential cred) throws SSOAgentException {
        Signature signature = (Signature) buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
        signature.setSigningCredential(cred);
        signature.setSignatureAlgorithm(signatureAlgorithm);
        signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);

        try {
            KeyInfo keyInfo = (KeyInfo) buildXMLObject(KeyInfo.DEFAULT_ELEMENT_NAME);
            X509Data data = (X509Data) buildXMLObject(X509Data.DEFAULT_ELEMENT_NAME);
            org.opensaml.xml.signature.X509Certificate cert =
                    (org.opensaml.xml.signature.X509Certificate) buildXMLObject(org.opensaml.xml.signature.X509Certificate.DEFAULT_ELEMENT_NAME);
            String value =
                    org.apache.xml.security.utils.Base64.encode(cred.getEntityCertificate().getEncoded());
            cert.setValue(value);
            data.getX509Certificates().add(cert);
            keyInfo.getX509Datas().add(data);
            signature.setKeyInfo(keyInfo);
            return signature;

        } catch (CertificateEncodingException e) {
            throw new SSOAgentException("Error getting certificate", e);
        }
    }

    public static void addDeflateSignatureToHTTPQueryString(StringBuilder httpQueryString,
                                                            X509Credential cred) throws SSOAgentException {
        doBootstrap();
        try {
            httpQueryString.append("&SigAlg="
                    + URLEncoder.encode(XMLSignature.ALGO_ID_SIGNATURE_RSA, "UTF-8").trim());

            java.security.Signature signature = java.security.Signature.getInstance("SHA1withRSA");
            signature.initSign(cred.getPrivateKey());
            signature.update(httpQueryString.toString().getBytes(Charset.forName("UTF-8")));
            byte[] signatureByteArray = signature.sign();

            String signatureBase64encodedString = Base64.encodeBytes(signatureByteArray,
                    Base64.DONT_BREAK_LINES);
            httpQueryString.append("&Signature="
                    + URLEncoder.encode(signatureBase64encodedString, "UTF-8").trim());
        } catch (Exception e) {
            throw new SSOAgentException("Error applying SAML2 Redirect Binding signature", e);
        }
    }

    /**
     * Builds SAML Elements
     *
     * @param objectQName
     * @return
     * @throws SSOAgentException
     */
    private static XMLObject buildXMLObject(QName objectQName) throws SSOAgentException {
        doBootstrap();
        XMLObjectBuilder builder =
                org.opensaml.xml.Configuration.getBuilderFactory()
                        .getBuilder(objectQName);
        if (builder == null) {
            throw new SSOAgentException("Unable to retrieve builder for object QName " +
                    objectQName);
        }
        return builder.buildObject(objectQName.getNamespaceURI(), objectQName.getLocalPart(),
                objectQName.getPrefix());
    }

    public static void sendPostResponse(HttpServletRequest request, HttpServletResponse response,
                                        String htmlPayload)
            throws SSOAgentException {

        Writer writer = null;
        try {
            writer = response.getWriter();
            writer.write(htmlPayload);
            response.flushBuffer();
        } catch (IOException e) {
            throw new SSOAgentException("Error occurred while writing to HttpServletResponse", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error occurred while closing Writer", e);
                }
            }
        }
    }

    /**
     * Serializing a SAML2 object into a String.
     *
     * @param xmlObject object that needs to serialized.
     * @return serialized object
     * @throws SSOAgentException
     */
    public static String marshall(XMLObject xmlObject) throws SSOAgentException {

        try {
            MarshallerFactory marshallerFactory = org.opensaml.xml.Configuration
                    .getMarshallerFactory();
            Marshaller marshaller = marshallerFactory.getMarshaller(xmlObject);
            Element element = marshaller.marshall(xmlObject);

            ByteArrayOutputStream byteArrayOutputStrm = new ByteArrayOutputStream();
            DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
            LSSerializer writer = impl.createLSSerializer();
            LSOutput output = impl.createLSOutput();
            output.setByteStream(byteArrayOutputStrm);
            writer.write(element, output);
            return byteArrayOutputStrm.toString();
        } catch (Exception e) {
            throw new SSOAgentException("Error Serializing the SAML Response", e);
        }
    }

    public static XMLObject unmarshall(String saml2SSOString) throws SSOAgentException {

        doBootstrap();

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setXIncludeAware(false);
        documentBuilderFactory.setExpandEntityReferences(false);
        try {
            documentBuilderFactory
                    .setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE, false);
            documentBuilderFactory
                    .setFeature(Constants.SAX_FEATURE_PREFIX + Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE, false);
            documentBuilderFactory
                    .setFeature(Constants.XERCES_FEATURE_PREFIX + Constants.LOAD_EXTERNAL_DTD_FEATURE, false);
            documentBuilderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        } catch (ParserConfigurationException e) {
            LOGGER.log(Level.SEVERE,
                       "Failed to load XML Processor Feature " + Constants.EXTERNAL_GENERAL_ENTITIES_FEATURE + " or " +
                       Constants.EXTERNAL_PARAMETER_ENTITIES_FEATURE + " or " + Constants.LOAD_EXTERNAL_DTD_FEATURE +
                       " or secure-processing.");
        }

        org.apache.xerces.util.SecurityManager securityManager = new SecurityManager();
        securityManager.setEntityExpansionLimit(ENTITY_EXPANSION_LIMIT);
        documentBuilderFactory
                .setAttribute(Constants.XERCES_PROPERTY_PREFIX + Constants.SECURITY_MANAGER_PROPERTY, securityManager);

        try {
            documentBuilderFactory.setIgnoringComments(true);
            Document document = getDocument(documentBuilderFactory, saml2SSOString);
            if (isSignedWithComments(document)) {
                documentBuilderFactory.setIgnoringComments(false);
                document = getDocument(documentBuilderFactory, saml2SSOString);
            }
            Element element = document.getDocumentElement();
            UnmarshallerFactory unmarshallerFactory = Configuration.getUnmarshallerFactory();
            Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
            return unmarshaller.unmarshall(element);
        } catch (ParserConfigurationException e) {
            throw new SSOAgentException("Error in unmarshalling SAML2SSO Request from the encoded String", e);
        } catch (UnmarshallingException e) {
            throw new SSOAgentException("Error in unmarshalling SAML2SSO Request from the encoded String", e);
        } catch (SAXException e) {
            throw new SSOAgentException("Error in unmarshalling SAML2SSO Request from the encoded String", e);
        } catch (IOException e) {
            throw new SSOAgentException("Error in unmarshalling SAML2SSO Request from the encoded String", e);
        }

    }

    /**
     * Return whether SAML Assertion has the canonicalization method
     * set to 'http://www.w3.org/2001/10/xml-exc-c14n#WithComments'.
     *
     * @param document
     * @return true if canonicalization method equals to 'http://www.w3.org/2001/10/xml-exc-c14n#WithComments'
     */
    private static boolean isSignedWithComments(Document document) {

        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            String assertionId = (String) xPath.compile("//*[local-name()='Assertion']/@ID")
                    .evaluate(document, XPathConstants.STRING);

            if (StringUtils.isBlank(assertionId)) {
                return false;
            }

            NodeList nodeList = ((NodeList) xPath.compile(
                    "//*[local-name()='Assertion']" +
                            "/*[local-name()='Signature']" +
                            "/*[local-name()='SignedInfo']" +
                            "/*[local-name()='Reference'][@URI='#" + assertionId + "']" +
                            "/*[local-name()='Transforms']" +
                            "/*[local-name()='Transform']" +
                            "[@Algorithm='http://www.w3.org/2001/10/xml-exc-c14n#WithComments']")
                    .evaluate(document, XPathConstants.NODESET));
            return nodeList != null && nodeList.getLength() > 0;
        } catch (XPathExpressionException e) {
            String message = "Failed to find the canonicalization algorithm of the assertion. Defaulting to: " +
                    "http://www.w3.org/2001/10/xml-exc-c14n#";
            LOGGER.log(Level.WARNING, message);
            return false;
        }
    }

    private static Document getDocument(DocumentBuilderFactory documentBuilderFactory, String samlString)
            throws IOException, SAXException, ParserConfigurationException {

        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(samlString.getBytes());
        return docBuilder.parse(inputStream);
    }
}
