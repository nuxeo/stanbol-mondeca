package org.nuxeo.stanbol.mondeca;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;

import javax.xml.namespace.QName;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSite;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteException;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.site.linkeddata.impl.CoolUriDereferencer;
import org.nuxeo.stanbol.mondeca.impl.ConnectedRequestType;
import org.nuxeo.stanbol.mondeca.impl.ConnectionRequestType;
import org.nuxeo.stanbol.mondeca.impl.ConnectionResponseType;
import org.nuxeo.stanbol.mondeca.impl.GetTopicRequestType;
import org.nuxeo.stanbol.mondeca.impl.ITM;
import org.nuxeo.stanbol.mondeca.impl.ITMService;
import org.nuxeo.stanbol.mondeca.impl.TopicType;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

/**
 * Adapter to make EntityHub reference sites able to perform query on an ITM instance through the SOAP
 * WebService.
 */
@Component(configurationFactory = true, policy = ConfigurationPolicy.REQUIRE, specVersion = "1.1")
@Service
@Properties(value = {@Property(name = SiteConfiguration.ID)})
public class ITMEntitySource implements ReferencedSite {

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

    protected String id;

    protected DefaultFieldMapperImpl fieldMappings;

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
        id = getFromPropertiesOrEnv(properties, SiteConfiguration.ID);
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

        // TODO: make field mappings configurable
        fieldMappings = new DefaultFieldMapperImpl(ValueConverterFactory.getDefaultInstance());
    }

    public void activate(ComponentContext ce) throws ConfigurationException, ReferencedSiteException {
        @SuppressWarnings("unchecked")
        Dictionary<String,String> properties = ce.getProperties();
        configure(properties);
        // check the connection to fail early in case of bad parameters
        logout(connect());
    }

    protected String connect() throws ReferencedSiteException {
        ConnectionResponseType connection = itmPort.connection(connectionParams);
        if (!connection.isSuccessfull()) {
            throw new ReferencedSiteException(connection.getMessage());
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
        fieldMappings = null;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Entity getEntity(String id) throws ReferencedSiteException {
        checkEnabled();
        String connectionId = connect();
        try {
            GetTopicRequestType topicRequest = new GetTopicRequestType();
            topicRequest.setConnectionID(connectionId);
            topicRequest.setGetMetaData(true);
            topicRequest.setLight(false);
            topicRequest.setPsi(id);
            TopicType topic = itmPort.getTopic(topicRequest);
            return topicToEntity(topic);
        } finally {
            logout(connectionId);
        }
    }

    protected Entity topicToEntity(TopicType topic) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public QueryResultList<Representation> find(FieldQuery query) throws ReferencedSiteException {
        checkEnabled();
        // TODO
        return null;
    }

    @Override
    public QueryResultList<Entity> findEntities(FieldQuery query) throws ReferencedSiteException {
        checkEnabled();
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public QueryResultList<String> findReferences(FieldQuery query) throws ReferencedSiteException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream getContent(String id, String contentType) throws ReferencedSiteException {
        try {
            return new CoolUriDereferencer().dereference(id, contentType);
        } catch (IOException e) {
            throw new ReferencedSiteException(String.format(
                "Could not fetch content for %s id with content type %s", id, contentType), e);
        }
    }

    @Override
    public FieldQueryFactory getQueryFactory() {
        return DefaultQueryFactory.getInstance();
    }

    @Override
    public SiteConfiguration getConfiguration() {
        // TODO: the SiteConfiguration API is implementation specific. Is this really usefull?
        return null;
    }

    @Override
    public boolean supportsLocalMode() {
        return false;
    }

    @Override
    public boolean supportsSearch() {
        return true;
    }

    @Override
    public FieldMapper getFieldMapper() {
        return fieldMappings;
    }

    protected void checkEnabled() throws ReferencedSiteException {
        if (itmPort == null) {
            throw new ReferencedSiteException("Mondeca ITM service endpoint is not configured"
                                              + " on referenced site with id: " + id);
        }
    }
}
