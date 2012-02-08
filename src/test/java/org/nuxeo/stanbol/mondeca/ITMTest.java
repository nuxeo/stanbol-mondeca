package org.nuxeo.stanbol.mondeca;

import org.junit.Test;

public class ITMTest {

    public boolean hasMondecaConfigEnv() {
        return System.getenv("STANBOL_MONDECA_ITM_SERVICE_WSDL_URL") != null;
    }

    @Test
    public void testConnection() {
        if (!hasMondecaConfigEnv()) {
            // skip test
            return;
        }
        // TODO
    }

}
