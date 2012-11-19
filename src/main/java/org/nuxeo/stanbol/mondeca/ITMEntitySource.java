package org.nuxeo.stanbol.mondeca;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.NotImplementedException;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.stanbol.entityhub.core.mapping.DefaultFieldMapperImpl;
import org.apache.stanbol.entityhub.core.mapping.ValueConverterFactory;
import org.apache.stanbol.entityhub.core.model.EntityImpl;
import org.apache.stanbol.entityhub.core.model.InMemoryValueFactory;
import org.apache.stanbol.entityhub.core.query.DefaultQueryFactory;
import org.apache.stanbol.entityhub.core.query.QueryResultListImpl;
import org.apache.stanbol.entityhub.servicesapi.mapping.FieldMapper;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Representation;
import org.apache.stanbol.entityhub.servicesapi.query.Constraint;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.Site;
import org.apache.stanbol.entityhub.servicesapi.site.SiteException;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.apache.stanbol.entityhub.site.linkeddata.impl.CoolUriDereferencer;
import org.nuxeo.stanbol.mondeca.impl.AssociationType;
import org.nuxeo.stanbol.mondeca.impl.ConnectedRequestType;
import org.nuxeo.stanbol.mondeca.impl.ConnectionRequestType;
import org.nuxeo.stanbol.mondeca.impl.ConnectionResponseType;
import org.nuxeo.stanbol.mondeca.impl.GetTopicRequestType;
import org.nuxeo.stanbol.mondeca.impl.ITM;
import org.nuxeo.stanbol.mondeca.impl.ITMService;
import org.nuxeo.stanbol.mondeca.impl.LinkType;
import org.nuxeo.stanbol.mondeca.impl.NameType;
import org.nuxeo.stanbol.mondeca.impl.ObjectType.DataItems;
import org.nuxeo.stanbol.mondeca.impl.PointerType;
import org.nuxeo.stanbol.mondeca.impl.QueryResultType;
import org.nuxeo.stanbol.mondeca.impl.SearchByNameRequestType;
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
public class ITMEntitySource implements Site {

    public static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

    public static final String RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label";

    public static final String NAME_URI = "http://www.mondeca.com/system/basicontology#Name";

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

    public void activate(ComponentContext ce) throws ConfigurationException, SiteException {
        @SuppressWarnings("unchecked")
        Dictionary<String,String> properties = ce.getProperties();
        configure(properties);
        // check the connection to fail early in case of bad parameters
        logout(connect());
    }

    protected String connect() throws SiteException {
        ConnectionResponseType connection = itmPort.connection(connectionParams);
        if (!connection.isSuccessfull()) {
            throw new SiteException(connection.getMessage());
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
    public Entity getEntity(String id) throws SiteException {
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

    protected Entity topicToEntity(TopicType topic) throws IllegalArgumentException, SiteException {
        return new EntityImpl(getId(), topicToRepresentation(topic), null);
    }

    protected Representation topicToRepresentation(TopicType topic) throws SiteException {
        InMemoryValueFactory factory = InMemoryValueFactory.getInstance();
        Iterator<String> uriIterator = topic.getUri().iterator();
        if (!uriIterator.hasNext()) {
            throw new SiteException(topic.getId() + " has no SPI");
        }
        Representation representation = factory.createRepresentation(uriIterator.next());
        for (String typeUri : topic.getTypeUri()) {
            representation.addReference(RDF_TYPE, typeUri);
        }
        // TODO: is the following really a good idea?
        for (AssociationType association : topic.getAssociations().getAssociation()) {
            collectLinks(representation, association.getDataItems());
            collectPointers(representation, association.getDataItems());
        }
        collectNames(representation, topic.getDataItems());
        collectLinks(representation, topic.getDataItems());
        collectPointers(representation, topic.getDataItems());
        if (fieldMappings != null && !fieldMappings.getMappings().isEmpty()) {
            Representation mappedRepresentation = factory.createRepresentation(representation.getId());
            fieldMappings.applyMappings(representation, mappedRepresentation, factory);
            return mappedRepresentation;
        }
        return representation;
    }

    protected void collectNames(Representation representation, DataItems dataItems) {
        if (dataItems == null || dataItems.getName() == null) {
            return;
        }
        for (NameType name : dataItems.getName()) {
            Iterator<String> typeIterator = name.getTypeUri().iterator();
            if (typeIterator.hasNext()) {
                representation.setNaturalText(typeIterator.next(), name.getDisplayName());
            } else {
                representation.setNaturalText(RDFS_LABEL, name.getDisplayName());
            }
        }
    }

    protected void collectLinks(Representation representation, DataItems dataItems) {
        if (dataItems == null || dataItems.getLink() == null) {
            return;
        }
        for (LinkType link : dataItems.getLink()) {
            Iterator<String> typeIterator = link.getTypeUri().iterator();
            if (typeIterator.hasNext()) {
                representation.setReference(typeIterator.next(), link.getUrl());
            }
        }
    }

    protected void collectPointers(Representation representation, DataItems dataItems) {
        if (dataItems == null || dataItems.getPointer() == null) {
            return;
        }
        for (PointerType pointer : dataItems.getPointer()) {
            Iterator<String> typeIterator = pointer.getTypeUri().iterator();
            Iterator<String> topicIterator = pointer.getTopic().getUri().iterator();
            if (typeIterator.hasNext() && topicIterator.hasNext()) {
                representation.setReference(typeIterator.next(), topicIterator.next());
            }
        }
    }

    @Override
    public QueryResultList<Representation> find(FieldQuery query) throws SiteException {
        checkEnabled();
        String connectionId = connect();
        try {
            List<Representation> representationList = new ArrayList<Representation>();
            SearchByNameRequestType searchByNameRequest = new SearchByNameRequestType();
            searchByNameRequest.setConnectionID(connectionId);
            searchByNameRequest.setFullText(false);
            searchByNameRequest.setGetMetaData(true);
            searchByNameRequest.setLight(false);
            searchByNameRequest.setIncludeReferentialTopics(false);
            if (query.getLimit() != null) {
                searchByNameRequest.setLength(query.getLimit());
            }
            searchByNameRequest.setOffSet(query.getOffset());

            Constraint typeConstraint = query.getConstraint(RDF_TYPE);
            String type = null;
            if (typeConstraint instanceof ValueConstraint) {
                // TODO: how to handle multi-valued constraints?
                type = ((ValueConstraint) typeConstraint).getValues().iterator().next().toString();
                searchByNameRequest.setClasspsi(type);
            }
            Constraint nameConstraint = query.getConstraint(NAME_URI);
            if (nameConstraint instanceof TextConstraint) {
                TextConstraint textNameConstraint = (TextConstraint) nameConstraint;
                String keyword = textNameConstraint.getTexts().iterator().next();
                switch (textNameConstraint.getPatternType()) {
                    case none:
                        searchByNameRequest.setSearchMode("exact");
                        searchByNameRequest.setWord(keyword);
                        break;
                    case wildcard:
                        if (keyword.endsWith("*")) {
                            keyword = keyword.substring(0, keyword.length() - 1);
                            if (keyword.startsWith("*")) {
                                keyword = keyword.substring(1);
                                searchByNameRequest.setSearchMode("full");
                                searchByNameRequest.setWord(keyword);
                            } else {
                                searchByNameRequest.setSearchMode("begin");
                                searchByNameRequest.setWord(keyword);
                            }
                        } else {
                            searchByNameRequest.setSearchMode("exact");
                            searchByNameRequest.setWord(keyword);
                        }
                        break;

                    default:
                        throw new SiteException("Unsupported pattern type: regex");
                }
            }
            QueryResultType results = itmPort.searchByName(searchByNameRequest);
            List<TopicType> topics = results.getTopicMap().getTopics().getTopic();
            for (TopicType topic : topics) {
                if (!topic.getUri().isEmpty()) {
                    Representation representation = topicToRepresentation(topic);
                    if (type != null) {
                        // add the type info as the Mondeca ITM topic type info might not include it.
                        representation.setReference(RDF_TYPE, type);
                    }
                    representationList.add(representation);
                }
            }
            return new QueryResultListImpl<Representation>(query, representationList, Representation.class);
        } finally {
            logout(connectionId);
        }
    }

    @Override
    public QueryResultList<Entity> findEntities(FieldQuery query) throws SiteException {
        checkEnabled();
        QueryResultList<Representation> representations = find(query);
        List<Entity> entityList = new ArrayList<Entity>();
        for (Representation representation : representations) {
            entityList.add(new EntityImpl(getId(), representation, null));
        }
        return new QueryResultListImpl<Entity>(query, entityList, Entity.class);
    }

    @Override
    public QueryResultList<String> findReferences(FieldQuery query) throws SiteException {
        throw new NotImplementedException();
    }

    @Override
    public InputStream getContent(String id, String contentType) throws SiteException {
        try {
            return new CoolUriDereferencer().dereference(id, contentType);
        } catch (IOException e) {
            throw new SiteException(String.format(
                "Could not fetch content for %s id with content type %s", id, contentType), e);
        }
    }

    @Override
    public FieldQueryFactory getQueryFactory() {
        return DefaultQueryFactory.getInstance();
    }

    @Override
    public SiteConfiguration getConfiguration() {
        throw new NotImplementedException();
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

    protected void checkEnabled() throws SiteException {
        if (itmPort == null) {
            throw new SiteException("Mondeca ITM service endpoint is not configured"
                                              + " on referenced site with id: " + id);
        }
    }
}
