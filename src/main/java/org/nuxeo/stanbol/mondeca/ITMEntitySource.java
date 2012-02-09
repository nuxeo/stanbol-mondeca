package org.nuxeo.stanbol.mondeca;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;

import javax.xml.namespace.QName;

import org.apache.felix.scr.annotations.Property;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.EntityDereferencer;
import org.apache.stanbol.entityhub.servicesapi.site.EntitySearcher;
import org.nuxeo.stanbol.mondeca.impl.ConnectedRequestType;
import org.nuxeo.stanbol.mondeca.impl.ConnectionRequestType;
import org.nuxeo.stanbol.mondeca.impl.ConnectionResponseType;
import org.nuxeo.stanbol.mondeca.impl.ITM;
import org.nuxeo.stanbol.mondeca.impl.ITMService;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

/**
 * Adapter to make EntityHub reference sites able to perform query on an ITM instance through the SOAP
 * WebService.
 */
public class ITMEntitySource implements EntitySearcher, EntityDereferencer {

    public static QName SERVICE_NAME = new QName("http://itm.mondeca.com/schema", "ITMService");

    @Property
    public static final String SERVICE_WSDL_URL_PROPERTY = "stanbol.mondeca.itm.service.wsdl.url";

    @Property
    public static final String SERVICE_ACCOUNT_ID_PROPERTY = "stanbol.mondeca.itm.service.account.id";

    @Property
    public static final String SERVICE_ACCOUNT_PASSWORD_PROPERTY = "stanbol.mondeca.itm.service.account.password";

    @Property
    public static final String SERVICE_CLIENT_PROPERTY = "stanbol.mondeca.itm.service.client";

    @Property
    public static final String SERVICE_WORKSPACE_ID = "stanbol.mondeca.itm.service.workspace.id";

    @Property
    public static final String SERVICE_LANGUAGE_PROPERTY = "stanbol.mondeca.itm.service.language";

    @Property
    public static final String SERVICE_JNDI_PROVIDER_PROPERTY = "stanbol.mondeca.itm.service.jndi.provider.url";

    ITM itmPort;

    protected ConnectionRequestType connectionParams;

    /**
     * Load a required property value from OSGi context with fall-back on environment variable.
     * 
     * @throws ConfigurationException
     *             if no configuration is found in either contexts.
     */
    protected String getFromPropertiesOrEnv(Dictionary<String,String> properties, String propertyName) throws ConfigurationException {
        String envVariableName = propertyName.replaceAll("\\.", "_").toUpperCase();
        String propertyValue = System.getenv(envVariableName);
        if (properties.get(propertyName) != null) {
            propertyValue = properties.get(propertyName);
        }
        if (propertyValue == null) {
            throw new ConfigurationException(propertyName, String.format("%s is a required property",
                propertyName));
        }
        return propertyValue;
    }

    public void configure(Dictionary<String,String> properties) throws ConfigurationException {
        try {
            String url = getFromPropertiesOrEnv(properties, SERVICE_WSDL_URL_PROPERTY);
            ITMService itmService = new ITMService(new URL(url), SERVICE_NAME);
            itmPort = itmService.getITMPort();
        } catch (MalformedURLException e) {
            throw new ConfigurationException(SERVICE_WSDL_URL_PROPERTY, e.getMessage());
        }
        connectionParams = new ConnectionRequestType();
        connectionParams.setWorkspace(Integer
                .valueOf(getFromPropertiesOrEnv(properties, SERVICE_WORKSPACE_ID)));
        connectionParams.setLogin(getFromPropertiesOrEnv(properties, SERVICE_ACCOUNT_ID_PROPERTY));
        connectionParams.setPassword(getFromPropertiesOrEnv(properties, SERVICE_ACCOUNT_PASSWORD_PROPERTY));
        connectionParams.setClient(getFromPropertiesOrEnv(properties, SERVICE_CLIENT_PROPERTY));
        connectionParams.setLanguage(getFromPropertiesOrEnv(properties, SERVICE_LANGUAGE_PROPERTY));
        connectionParams
                .setJndiProviderUrl(getFromPropertiesOrEnv(properties, SERVICE_JNDI_PROVIDER_PROPERTY));
        connectionParams.setAnonymous(false);
    }

    public void activate(ComponentContext ce) throws ConfigurationException {
        @SuppressWarnings("unchecked")
        Dictionary<String,String> properties = ce.getProperties();
        configure(properties);
        // check the connection to fail early in case of bad parameters
        logout(connect());
    }

    protected String connect() throws ConfigurationException {
        ConnectionResponseType connection = itmPort.connection(connectionParams);
        if (!connection.isSuccessfull()) {
            throw new ConfigurationException(SERVICE_WSDL_URL_PROPERTY, connection.getMessage());
        }
        return connection.getIdentifier();
    }

    protected void logout(String connectionId) {
        if (connectionId != null) {
            ConnectedRequestType logoutRequest = new ConnectedRequestType();
            logoutRequest.setConnectionID(connectionId);
            itmPort.logout(logoutRequest);
        }
    }

    public void deactiveate(ComponentContext ce) throws ConfigurationException {
        itmPort = null;
        connectionParams = null;
    }

    // Searcher API

    @Override
    public QueryResultList<String> findEntities(FieldQuery query) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public QueryResultList<Representation> find(FieldQuery query) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    // Dereferencer API

    @Override
    public String getAccessUri() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean canDereference(String uri) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public InputStream dereference(String uri, String contentType) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Representation dereference(String uri) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

}
