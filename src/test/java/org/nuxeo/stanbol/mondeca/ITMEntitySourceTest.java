package org.nuxeo.stanbol.mondeca;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.stanbol.entityhub.servicesapi.model.Entity;
import org.apache.stanbol.entityhub.servicesapi.model.Reference;
import org.apache.stanbol.entityhub.servicesapi.model.Text;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQuery;
import org.apache.stanbol.entityhub.servicesapi.query.FieldQueryFactory;
import org.apache.stanbol.entityhub.servicesapi.query.QueryResultList;
import org.apache.stanbol.entityhub.servicesapi.query.TextConstraint;
import org.apache.stanbol.entityhub.servicesapi.query.ValueConstraint;
import org.apache.stanbol.entityhub.servicesapi.site.ReferencedSiteException;
import org.apache.stanbol.entityhub.servicesapi.site.SiteConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

/**
 * Test ITMEntitySource that parially implements the ReferencedSite API using a Mondeca ITM service as a
 * backend.
 *
 * Note: this test needs environment variables to run against a mondeca IRM service. Otherwise all tests are
 * skipped.
 */
public class ITMEntitySourceTest {

    public static final Log log = LogFactory.getLog(ITMEntitySourceTest.class);

    protected static final String WSDL_ENV_VAR = "STANBOL_MONDECA_ITM_SERVICE_WSDL_URL";

    ITMEntitySource source;

    static boolean hasAlreadyWarned = false;

    public boolean checkMondecaConfigEnvironment() {
        return System.getenv(WSDL_ENV_VAR) != null;
    }

    public Dictionary<String,String> getTestConfig() {
        Dictionary<String,String> config = new Hashtable<String,String>();
        config.put(SiteConfiguration.ID, "test-mondeca-itm-source");
        return config;
    }

    @Before
    public void makeEntitySource() throws ConfigurationException {
        if (!checkMondecaConfigEnvironment()) {
            if (!hasAlreadyWarned) {
                log.warn(WSDL_ENV_VAR + " is not defined, skipping "
                         + ITMEntitySourceTest.class.getSimpleName());
                hasAlreadyWarned = true;
            }
            return;
        }
        source = new ITMEntitySource();
        source.configure(getTestConfig());
    }

    @Test
    public void testConnect() throws ReferencedSiteException {
        if (!checkMondecaConfigEnvironment()) {
            return;
        }
        source.logout(source.connect());
    }

    @Test
    public void testGetEntity() throws ReferencedSiteException {
        if (!checkMondecaConfigEnvironment()) {
            return;
        }
        Entity entity = source.getEntity("http://sws.geonames.org/146214/");
        assertNotNull(entity);
        assertEquals("test-mondeca-itm-source", entity.getSite());
        assertEquals("http://sws.geonames.org/146214/", entity.getId());
        Iterator<Reference> types = entity.getRepresentation().getReferences(ITMEntitySource.RDF_TYPE);
        // TODO: how to fetch a geonames feature type? (class PSI in Mondeca ITM parlance)
        assertFalse(types.hasNext());
        // Reference firstType = types.next();
        // assertEquals("TODO", firstType.getReference());
        // assertFalse(types.hasNext());

        Iterator<Text> names = entity.getRepresentation().getText(ITMEntitySource.RDFS_LABEL);
        assertTrue(names.hasNext());
        Text name = names.next();
        assertEquals("Paphos", name.getText());
        // TODO: add support for languages
        assertEquals(null, name.getLanguage());
        assertFalse(names.hasNext());
    }

    @Test
    public void testFindEntities() throws ReferencedSiteException {
        if (!checkMondecaConfigEnvironment()) {
            return;
        }
        FieldQueryFactory qf = source.getQueryFactory();
        FieldQuery query = qf.createFieldQuery();
        query.setConstraint(ITMEntitySource.RDF_TYPE, new ValueConstraint(
                "http://www.geonames.org/ontology#Feature"));
        query.setConstraint(ITMEntitySource.RDFS_LABEL, new TextConstraint("Paphos",
                TextConstraint.PatternType.none, false));
        QueryResultList<Entity> entities = source.findEntities(query);
        assertNotNull(entities);
        assertEquals(1, entities.size());
        Entity entity = entities.iterator().next();
        assertNotNull(entity);
        assertEquals("test-mondeca-itm-source", entity.getSite());
        assertEquals("http://sws.geonames.org/146214/", entity.getId());
        Iterator<Text> names = entity.getRepresentation().getText(ITMEntitySource.RDFS_LABEL);
        assertTrue(names.hasNext());
        Text name = names.next();
        assertEquals("Paphos", name.getText());
        // TODO: add support for languages
        assertEquals(null, name.getLanguage());
        assertFalse(names.hasNext());

    }
}
