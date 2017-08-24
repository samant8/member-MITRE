```
   _____                .__               .__
  /  _  \   ____ _____  |  | ___.__. _____|__| ______
 /  /_\  \ /    \\__  \ |  |<   |  |/  ___/  |/  ___/
/    |    \   |  \/ __ \|  |_\___  |\___ \|  |\___ \
\____|__  /___|  (____  /____/ ____/____  >__/____  >
        \/     \/     \/     \/         \/        \/
___________             .__
\_   _____/__  ___ ____ |  |__ _____    ____    ____   ____
 |    __)_\  \/  // ___\|  |  \\__  \  /    \  / ___\_/ __ \
 |        \>    <\  \___|   Y  \/ __ \|   |  \/ /_/  >  ___/
/_______  /__/\_ \\___  >___|  (____  /___|  /\___  / \___  >
        \/      \/    \/     \/     \/     \//_____/      \/

  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
An initiative of the Analytic Technology Industry Roundtable
```


Tangerine
===========
MITRE's contribution to the Analysis Exchange is a reference implementation that 
touches on the development and use of the full spectrum of items needed in an AE.

This prototype is called **Tangerine**;  As of Fall 2017, we are leading up to v1.x
to support the AE Model v1.0.

Getting Started
-------------

1. Review the documentation ./doc/TangerineDemo.htm once you have checked out the project
2. Build the project with Maven:

   ``` mvn install ```

If you are trying use Esri adapter(s), then the ArcGIS Runtime Java SDK and examples on github.com 
are helpful for setting up the Esri runtime. See https://github.com/Esri/arcgis-runtime-samples-java. 
Synopsis: Once checked out, use ./gradlew build to install gradle and build the Esri projects.  This
automates the setup of Esri downloads very swiftly.  You must configure you ~/.gradle settings, e.g. 
proxies, if behind a firewall.
