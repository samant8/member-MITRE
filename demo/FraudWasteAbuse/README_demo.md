README: Demo / Testing
======================

 A. First Build and Configure your setup.
  Optionally "build" to clean and build Tangerine from scratch, yielding this demo.
```
	./configure.sh [build]
```

And now for all subsequent operation, note that some additional Python 2.x libraries are used
for demo execution.  For convenience all such libraries are installed using Pip locally.

```
    # TANGERINE is where you have checkout or installed this client code.
    FWA_DEMO=$TANGERINE/demo/FraudWasteAbuse
    export PYTHONPATH=$FWA_DEMO/piplib:$FWA_DEMO/apps/esri
```

 B. Field Tangerine Server

Follow the installation instructions to deploy Tangerine webapp (tangerine.war) on a Tomcat 8.x server.
This server could be on your laptop (aka localhost) or a remote server. Wherever that server is, please note 
the hostname as TANGERINE_SERVER.  The default port is 8080 and is not part of the TANGERINE_SERVER value.
That server installation also involves setting up a local MongoDB 3.x server, which is not referenced here
at all because all databasing work is managed through the Tangerine web service.  However, if you are interested
look at the data in Mongo after a few runs.

 C. Record Analytic Tool Configuration

* NetOwl server is required for this demonstration to run; 
Please note this as the base URL, ```NETOWL_URL``` in the form "http://server:port"
* ESRI ArcGIS online services are used.  If you run the demo pipeline behind a firewall, 
set shell variables ```http_proxy``` and ```https_proxy``` as ```PROXYHOST:PORT``` as you see fit

Test your environment quickly to see if at least the Esri routine runs:

```
  # 
  # export http_proxy=http://myproxy.mycom.com
  python apps/esri/esri_travel_calculator.py  --to "500 Sea World Dr, San Diego, CA" --from "2121 San Diego Ave, 92110"
```

 D. Running the Demonstration

The demonstration pipeline makes use of the Tangerine Test Client, aka "client.jar"
for interacting with the Tangerine Server.  The interaction with NetOwl is managed
by the "netowlapp.jar".  Esri integration occurs solely through the python script, ```esri_travel_calculator.py``` 

Running the entire demonstration on a single Claim data file is helpful to understand the 
combined data analytics that occur on a "unit" of data -- a unit being here a single claim.

```
   # Run from ./demo folder
   cd ./demo/uc1
   
   # PYTHONPATH is fixed inside script.
   # Run claims data through the demo:
   #   provide remote Tangerine server,
   #   URL for NetOwl, 
   #   And clean output folders prior to run.
   #
   python ./demo-pipeline.py --claims ./data/AllClaims.csv --pii ./data/Person.csv \
       --server TANGERINE_SERVER --netowl-server NETOWL_URL --clean
```

The above invocation generates interim output in ```working``` folder, with a final
result for claims in ```results```

 E. Walk through Demonstration

Consult the *Demonstration Analytics* section of the User Manual for Tangerine (../doc/TangerineDemo.htm)
