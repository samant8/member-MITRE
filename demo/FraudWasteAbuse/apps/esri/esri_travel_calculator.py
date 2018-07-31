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
from urllib import quote_plus
import requests
# from decimal import Decimal

def drive_time_and_distance(fromAddr, toAddr):

    geocode = ("http://geocode.arcgis.com/arcgis/rest/services/World/"
             "GeocodeServer/findAddressCandidates?f=json&address=")

    to_geoloc = requests.get(geocode + quote_plus(toAddr)).json()
    to_geocode = to_geoloc["candidates"][0]["location"]
    fro_geoloc = requests.get(geocode + quote_plus(fromAddr)).json()
    fro_geocode = fro_geoloc["candidates"][0]["location"]

    x1 = fro_geocode["x"]
    y1 = fro_geocode["y"]
    x2 = to_geocode["x"]
    y2 = to_geocode["y"]
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
    trip_length = routes["directions"][0]["summary"]["totalLength"]
    trip_time = routes["directions"][0]["summary"]["totalTime"]

    return {
        "StartLatitude":y1, "StartLongitude":x1,
        "EndLatitude":y2, "EndLongitude":x2,
        "TotalTravelDistance":trip_length,
        "TotalTravelTime":trip_time
        }

def str_ordinate(l):
    return '%3.8f' % (l)

def str_float(f):
    return '%3.5f' % (f)

def write_result(fh, dct):
    '''
    Write out the data as From, To, Trip Distance, Trip Time
    as CSV.  Field order is specifically:
    "StartLatitude", "StartLongitude", "EndLatitude", "EndLongitude", "TotalTravelDistance", "TotalTravelTime"
    Decimal degrees,
    Distance in meters,
    Travel time in minutes(?)

    @param fh: file handle
    @param dct: data from drive time calc
    '''
    fh.write(",".join(["StartLatitude", "StartLongitude",
                      "EndLatitude", "EndLongitude",
                      "TotalTravelDistance", "TotalTravelTime"]))
    fh.write("\n")
    row = [ str_ordinate(dct["StartLatitude"]),
            str_ordinate(dct["StartLongitude"]),
            str_ordinate(dct["EndLatitude"]),
            str_ordinate(dct["EndLongitude"]),
            '%5.1f' % (dct["TotalTravelDistance"]),  # INTEGER, METERS
            '%3.2f' % (dct["TotalTravelTime"])  # FLOAT, MINUTES
            ]
    fh.write(','.join(row))
    fh.write("\n")

    return None


if __name__ == "__main__":
    import argparse
    import sys

    parser = argparse.ArgumentParser(description='Make calls to ESRI REST API')
    parser.add_argument("--from", dest="fro", help="the start address")
    parser.add_argument("--to", dest="to", help="the destination address")
    parser.add_argument("--output", dest="output", help="the output file")
    args = parser.parse_args()

    try:
        result = drive_time_and_distance(args.fro, args.to)
        if args.output:
            f = open(args.output, 'w')
        else:
            f = sys.stdout

        write_result(f, result)
        if args.output:
            f.close()
    except Exception, err:
        print("ERROR: " + str(err))
