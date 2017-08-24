#
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#                   NOTICE
#
# This software was produced for the U. S. Government
# under Basic Contract No. FA8702-17-C-0001, and is
# subject to the Rights in Noncommercial Computer Software
# and Noncommercial Computer Software Documentation
# Clause 252.227-7014 (MAY 2013)
#
# (c)2016-2017 The MITRE Corporation. All Rights Reserved.
# ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
#


import requests
import json
import argparse
from sys import exit
from urllib import quote_plus
from decimal import Decimal

parser = argparse.ArgumentParser(description='Make calls to ESRI REST API')
parser.add_argument("--from", dest="fro"
    , help="the start address")
parser.add_argument("--to", dest="to"
     , help="the destination address")
parser.add_argument("--output", dest="output"
     , help="the destination address")
args = parser.parse_args()

geocode = ("http://geocode.arcgis.com/arcgis/rest/services/World/"
             "GeocodeServer/findAddressCandidates?f=json&address=")

try:
     to_geoloc = requests.get(geocode+quote_plus(args.to)).json()
     to_geocode = to_geoloc["candidates"][0]["location"]
     fro_geoloc = requests.get(geocode+quote_plus(args.fro)).json()
     fro_geocode = fro_geoloc["candidates"][0]["location"]

     x1 = str(Decimal(fro_geocode["x"]))
     y1 = str(Decimal(fro_geocode["y"]))
     x2 = str(Decimal(to_geocode["x"]))
     y2 = str(Decimal(to_geocode["y"]))
     coords = [x1, y1, x2, y2]

     route_find = ("http://sampleserver6.arcgisonline.com/arcgis/rest/services/"
                     "NetworkAnalysis/SanDiego/NAServer"
                     "/Route/solve?stops={0}%2C{1}%3B{2}%2C{3}"
                     "&barriers=&polylineBarriers=&polygonBarriers=&outSR="
                     "&ignoreInvalidLocations=true&accumulateAttributeNames="
                     "&impedanceAttributeName=TravelTime&restrictionAttributeNames"
                     "=&attributeParameterValues=&restrictUTurns=esri"
                     "NFSBAllowBacktrack&useHierarchy=true&returnDirections"
                     "=true&returnRoutes=true&returnStops=true&returnBarriers"
                     "=false&returnPolylineBarriers=false&returnPolygonBarriers"
                     "=false&directionsLanguage=en&directionsStyleName="
                     "&outputLines=esriNAOutputLineTrueShape&findBestSequence="
                     "false&preserveFirstStop=true&preserveLastStop="
                     "true&useTimeWindows=false&startTime=0&startTimeIsUTC"
                     "=false&outputGeometryPrecision=&outputGeometryPrecisionUnits"
                     "=esriDecimalDegrees&directionsOutputType=esriDOTComplete"
                     "&directionsTimeAttributeName=TravelTime&directionsLength"
                     "Units=esriNAUMeters&returnZ=false&travelMode=&f="
                     "json").format(*coords)

     rep = requests.get(route_find)
     routes = rep.json()
     length = str(Decimal(routes["directions"][0]["summary"]["totalLength"]))
     time = str(Decimal(routes["directions"][0]["summary"]["totalTime"]))

     if args.output is None:
          print("StartLatitude,StartLongitude,EndLatitude,EndLongitude"
		       ",TotalTravelDistance,TotalTravelTime")
          print(x1+","+y1+","+x2+","+y2+","+length+","+time)
     else: 
          with open(args.output, 'w') as f:
               f.write("StartLatitude,StartLongitude,EndLatitude,EndLongitude"
			       ",TotalTravelDistance,TotalTravelTime")
               f.write("\n")
               f.write(x1+","+y1+","+x2+","+y2+","+length+","+time)
except:
        print("error")
        exit()

