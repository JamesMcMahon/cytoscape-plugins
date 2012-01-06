KAM Navigator Plugin
==============================

This cytoscape plugin allows users to navigate Knowledge Assembly Model (KAMs) created using the BELFramework.

License
-------
This project is licensed under the terms of the [GPL v3](http://www.gnu.org/licenses/gpl-3.0.txt).

Building
--------

To build this plugin you will need

-   [ANT](http://ant.apache.org/)
-   BELFramework Webservice Plugin
-   BELFramework 1.2.2 or greater installed
-   Cytoscape 2.7.x or 2.8.x installed

Make sure the latest BELFramework Webservice Plugin is built and copied to the `lib/` folder.

Once installed you will need to configure the *HOME* location of each

-   Change the `BELFRAMEWORK_HOME` property in build.properties to point to the BELFramework installation folder.
-   Change the `CYTOSCAPE_HOME` property in build.properties to poi nt to the Cytoscape installation folder.

To build the project use the following commands

-   `ant package`

    Builds the plugin ready to install into Cytoscape.

-   `ant deploy`

    Packages the plugin and copies to the plugins folder of your Cytoscape installation.

    *Important* - Also make sure the BELFramework Webservice Plugin is deployed as a cytoscape plugin.


Setting up Eclipse
------------------

To set up the [Eclipse IDE](http://www.eclipse.org/) for developing this plugin

-   Check out the [belframework project](https://belframework-org@github.com/belframework-org/belframework.git)

    `git clone https://belframework-org@github.com/belframework-org/belframework.git`

-   In Eclipse go to *File -> New -> Java Project* and uncheck *Use default location*.

-   Name your project kam-navigator-plugin.

-   Enter the location of your local kam-navigator-plugin folder.

    `/path/to/git/clone/belframework/kam-navigator-plugin`

-   Hit Ok and a new project will be created.

-   Configure classpath

    -   Add `lib/belframework_webservice_1.0.jar` to the project classpath.
    -   Add `BELFrameworkWebAPIClient-1.2.2.jar` (or later version) from BELFRAMEWORK_HOME/lib/webapiclient to the project classpath.
    -   Add `cytoscape.jar` from CYTOSCAPE_HOME/ to the project classpath.

-   (optional) To build with ANT drag the build.xml to the ANT view.