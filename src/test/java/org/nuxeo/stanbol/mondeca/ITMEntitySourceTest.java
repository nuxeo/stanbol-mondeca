package org.nuxeo.stanbol.mondeca;

import java.net.MalformedURLException;

import org.junit.Test;

public class ITMEntitySourceTest {

    public boolean hasMondecaConfigEnv() {
        return System.getenv("STANBOL_MONDECA_ITM_SERVICE_WSDL_URL") != null;
    }

    @Test
    public void testConnection() throws MalformedURLException {
        if (!hasMondecaConfigEnv()) {
            // skip test
            return;
        }
        // TODO
    }
}
