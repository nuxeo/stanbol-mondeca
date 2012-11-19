package org.nuxeo.stanbol.mondeca.temis;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBException;

import org.apache.clerezza.rdf.core.LiteralFactory;
import org.apache.clerezza.rdf.core.MGraph;
import org.apache.clerezza.rdf.core.serializedform.Parser;
import org.apache.stanbol.enhancer.servicesapi.ContentItem;
import org.nuxeo.stanbol.temis.engine.TemisLuxidEnhancementEngine;

/**
 * Enhancement engine to collect the RDF/XML output of a Luxid service
 * configured to run Mondeca ITM as annotator as part of the UIMA chain.
 */
public class ITMTemisLuxidEnhancementEngine extends TemisLuxidEnhancementEngine {

    public static final String DEFAULT_CAS_CONSUMER = "MondecaAllRDFValid";

    protected void handleLuxidOutput(ContentItem ci,
            LiteralFactory literalFactory, MGraph g, String luxidInput,
            String luxidOutput) throws JAXBException {
        final Parser parser = Parser.getInstance();
        InputStream is = new ByteArrayInputStream(luxidOutput.getBytes(Charset.forName("UTF-8")));
        // TODO: make it possible to ITM annotations to the Stanbol enhancer vocabulary
        g.addAll(parser.parse(is, "application/rdf+xml"));
    }
}
