# Stanbol Mondeca

This project is an OSGi bundle extension for the [Apache Stanbol
(Incubating)] [1] server to make Stanbol register an instance of the
[Mondeca ITM] [2] web service as referenced site on the Entity Hub.

TODO: add a concrete use case and sample ontology snippet here.

[1]: http://incubator.apache.org/stanbol
[2]: http://www.mondeca.com

## License

This connector is realeased under the terms of the ASL 2.0.

## Building and deploying

To build the jar and run the tests:

    mvn install

The bundle jar can then be found in the `target/` subfolder.

To build and deploy on a live Stanbol instance running on
`http://localhost:8080` just do:

    mvn install -o -DskipTests -PinstallBundle \
        -Dsling.url=http://localhost:8080/system/console

Then go to the OSGi system console to set the URI and credentials
of your Mondeca ITM instance. Typically the URL looks like:

    http://localhost:8080/system/console/configMgr/org.nuxeo.stanbol.mondeca.ITMReferencedSite


## From a Nuxeo Document Management server

Install Nuxeo DM 5.4.2+ and the [Semantic Entities add-on] [3] from
the marketplace. Don't forget to [configure] [4] the add-on to point to
your own Stanbol instance.

[3]: https://connect.nuxeo.com/nuxeo/site/marketplace/package/semantic-entities-1.0.0
[4]: https://doc.nuxeo.com/display/NXDOC/Semantic+Entities+Installation+and+Configuration


## About Nuxeo

Nuxeo provides a modular, extensible Java-based [open source software
platform for enterprise content management] [5] and packaged applications
for [document management] [6], [digital asset management] [7] and
[case management] [8]. Designed by developers for developers, the Nuxeo
platform offers a modern architecture, a powerful plug-in model and
extensive packaging capabilities for building content applications.

[5]: http://www.nuxeo.com/en/products/ep
[6]: http://www.nuxeo.com/en/products/document-management
[7]: http://www.nuxeo.com/en/products/dam
[8]: http://www.nuxeo.com/en/products/case-management

More information on: <http://www.nuxeo.com/>
