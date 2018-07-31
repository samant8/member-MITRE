from subprocess import Popen, PIPE
import json
import sys
import csv
import os
import copy
from sys import stderr
sys.path.insert(0, './apps/esri')
sys.path.insert(0, './piplib')

# Local Scripts:
from esri_travel_calculator import drive_time_and_distance, write_result

# change for debugging
verbose = False
logging = True
exit_on_err = True

def log(logname, msg):
    fpath = os.path.join('working', logname)
    with open(fpath, 'w') as fh:
        fh.write(msg)

def report_err(errmsg):
    '''
    Simple logging to stderr
    '''
    msg = None
    if not isinstance(errmsg, str):
       msg = str(errmsg)
    else:msg = errmsg
    print stderr, msg
    if exit_on_err:
        print "EXITING DUE TO ERROR"
        sys.exit(-1)

# insert voucher data
def pipeline(voucher):
    output = None
    print "insert voucher data"
    p = Popen(['java', '-jar', 'client.jar', 'insert', voucher, 'org.mitre.tangerine.analytic.VoucherAdapter',
               '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate()
    if logging: log('VoucherInsert.log', output)
    if verbose: print output
    if err: report_err(err)

    # string to json
    response = json.loads(output)

    # read flora configuration template file
    print "read flora configuration template file"
    with open('config/flquery-config-template.json') as templateFile:
        floraTemplate = json.load(templateFile)
    configNetowlData = copy.copy(floraTemplate)
    configCurrencyData = copy.copy(floraTemplate)
    configData = copy.copy(floraTemplate)

    if verbose:
        print configData

    # fill collections array
    print "fill collections array"
    collectionId = response["collection"].replace("\n", "")
    configData["KBs"].append(collectionId)
    configCurrencyData["KBs"].append(collectionId)
    if verbose:
        print configData

    # write flora configuration file
    print "write flora configuration file"
    with open('config/flquery-config-voucher.json', 'w') as outfile:
        json.dump(configData, outfile)

    # query voucher data
    output = None
    print "query voucher data"
    p = Popen(['java', '-jar', 'client.jar', 'reason', 'config/flquery-config-voucher.json', 'config/AE.flr', 'config/voucher.qry',
               '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate()
    if logging: log('ReasonerNetOwl.json', output)
    if verbose: print output
    if err: report_err(err)

    # string to json
    data = json.loads(output)

    # get names and addresses to enrich
    names = []
    namesUUID = []
    addresses = []
    addressesUUID = []
    expenses = []
    expensesUUID = []
    for datalist in data["dataList"]:
        if "sumo#Human" in datalist["query"]:
            for resultlist in datalist["resultList"]:
                for result in resultlist["result"]:
                    if "?S" in result["key"]:
                        namesUUID.append(result["val"])
                    if "?o" in result["key"]:
                        names.append(result["val"])
        if "AE#PostalAddress" in datalist["query"]:
            for resultlist in datalist["resultList"]:
                for result in resultlist["result"]:
                    if "?S" in result["key"]:
                        addressesUUID.append(result["val"])
                    if "?o" in result["key"]:
                        addresses.append(result["val"])
        if "AE#VATravelVoucher" in datalist["query"]:
            for resultlist in datalist["resultList"]:
                for result in resultlist["result"]:
                    if "?S" in result["key"]:
                        expensesUUID.append(result["val"])
                    if "?o" in result["key"]:
                        expenses.append(result["val"])

    # enrich names using the netowl app
    print "enrich names using the netowl app"
    i = 0
    for name in names:
        output = None
        p = Popen(['java', '-jar', 'netowlapp.jar', '-server', netowlServer, '-text', name], stdin=PIPE, stdout=PIPE, stderr=PIPE)
        output, err = p.communicate()
        noxml_path = os.path.join('working', namesUUID[i] + '_netowlapp.xml')
        with open(noxml_path, 'w') as f:
            f.write(output)
        i += 1
        if verbose:
            print name
            print output
        if err: report_err(err)

    # enrich addresses using the netowl app
    print "enrich addresses using the netowl app"
    i = 0
    for address in addresses:
        output = None
        p = Popen(['java', '-jar', 'netowlapp.jar', '-server', netowlServer, '-text', address], stdin=PIPE, stdout=PIPE, stderr=PIPE)
        output, err = p.communicate()
        noxml_path = os.path.join('working', addressesUUID[i] + '_netowlapp.xml')
        with open(noxml_path, 'w') as f:
            f.write(output)
        i += 1
        if verbose:
            print address
            print output
        if err: report_err(err)


    # enrich expenses using the netowl app
    print "enrich expenses using the netowl app"
    i = 0
    for expense in expenses:
        output = None
        p = Popen(['java', '-jar', 'netowlapp.jar', '-server', netowlServer, '-text', expense], stdin=PIPE, stdout=PIPE, stderr=PIPE)
        output, err = p.communicate()
        noxml_path = os.path.join('working', expensesUUID[i] + '_netowlapp.xml')
        with open(noxml_path, 'w') as f:
            f.write(output)
        i += 1
        if verbose:
            print expense
            print output
        if err: report_err(err)

    # insert netowl names to the knowledge store
    print "insert netowl names to the knowledge store"
    for nameUUID in namesUUID:
        output = None
        noxml_path = os.path.join('working', nameUUID + '_netowlapp.xml')
        p = Popen(['java', '-jar', 'client.jar', 'insert', noxml_path, 'org.mitre.tangerine.analytic.NetOwlAdapter', '--entity', 'entity:person', '--uuid', nameUUID,
                   '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
        output, err = p.communicate()
        response = json.loads(output)
        configData["KBs"].append(response["collection"].replace("\n", ""))
        if logging: log(nameUUID + '_netowl.log', output)
        if verbose: print output
        if err: report_err(err)

    # insert netowl addresses to the knowledge store
    print "insert netowl addresses to the knowledge store"
    for addressUUID in addressesUUID:
        output = None
        noxml_path = os.path.join('working', addressUUID + '_netowlapp.xml')
        p = Popen(['java', '-jar', 'client.jar', 'insert', noxml_path, 'org.mitre.tangerine.analytic.NetOwlAdapter', '--entity', 'entity:address:mail', '--uuid', addressUUID,
                   '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
        output, err = p.communicate()
        response = json.loads(output)
        configData["KBs"].append(response["collection"].replace("\n", ""))
        configNetowlData["KBs"].append(response["collection"].replace("\n", ""))
        if logging: log(addressUUID + '_netowl.log', output)
        if verbose: print output
        if err: report_err(err)

    # insert netowl expenses to the knowledge store
    print "insert netowl expenses to the knowledge store"
    for expenseUUID in expensesUUID:
        output = None
        noxml_path = os.path.join('working', expenseUUID + '_netowlapp.xml')
        p = Popen(['java', '-jar', 'client.jar', 'insert', noxml_path, 'org.mitre.tangerine.analytic.NetOwlAdapter', '--entity', 'entity:currency:money', '--uuid', expenseUUID, '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
        output, err = p.communicate()
        response = json.loads(output)
        configData["KBs"].append(response["collection"].replace("\n", ""))
        configCurrencyData["KBs"].append(response["collection"].replace("\n", ""))
        if logging:log(addressUUID + '_netowl.log', output)
        if verbose: print output
        if err: report_err(err)

    # write flora configuration file
    output = None
    print "write flora configuration file"
    with open('config/flquery-config-netowl.json', 'w') as outfile:
        json.dump(configNetowlData, outfile)
    with open('config/flquery-config-currency.json', 'w') as outfile:
        json.dump(configCurrencyData, outfile)

    # query netowl data for CurrencyMeasures
    print "query netowl data for CurrencyMeasures"
    p = Popen(['java', '-jar', 'client.jar', 'reason', 'config/flquery-config-currency.json', 'config/AE.flr', 'config/currency.qry',
               '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate()
    if logging:log('ReasonerCurrency.json', output)
    if verbose: print output
    if err: report_err(err)

    # string to json
    data = json.loads(output)

    # get currencyMeasuresUUID
    print "get currencyMeasuresUUID"
    CuMeUUIDs = []
    VoucherIDs = ""  # there is always only going to be one
    for datalist in data["dataList"]:
        for resultlist in datalist["resultList"]:
            for result in resultlist["result"]:
                if "?cm" in result["key"]:
                    CuMeUUIDs.append(result["val"])
                if "?v" in result["key"]:
                    VoucherID = result["val"]

    # write hooked currencyMeasures
    print "write hooked currencyMeasures"
    cur_path = os.path.join('working', 'currency.txt')    
    f = open(cur_path, 'w')
    for CuMeUUID in CuMeUUIDs:
        toInsert = '{"A" : { "S" : "' + VoucherID + '", "P" : "AE#hasItemizedExpense", "O" : "' + CuMeUUID + '"}}\n'
        f.write(toInsert)
        if verbose:
            print toInsert
    f.close()

    # insert currency to the knowledge store
    output = None
    print "insert currency to the knowledge store"
    cur_path = os.path.join('working', 'currency.txt')
    p = Popen(['java', '-jar', 'client.jar', 'insert', cur_path, 'org.mitre.tangerine.analytic.GeneralAdapter',
               '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate()
    response = json.loads(output)
    configData["KBs"].append(response["collection"].replace("\n", ""))
    if logging:log('currency.log', output)
    if verbose: print output
    if err: report_err(err)

    # query voucher data for SSNs
    output = None
    print "query voucher data for SSNs"
    p = Popen(['java', '-jar', 'client.jar', 'reason', 'config/flquery-config-voucher.json', 'config/AE.flr', 'config/pii.qry',
               '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate()
    if logging:log('ReasonerPII.json', output)
    if verbose: print output
    if err: report_err(err)

    # string to json
    data = json.loads(output)

    # get ssns and uuids
    humanUUID = []
    ssn = []
    for datalist in data["dataList"]:
        for resultlist in datalist["resultList"]:
            for result in resultlist["result"]:
                if "?o" in result["key"]:
                    humanUUID.append(result["val"])
                if "?sl" in result["key"]:
                    ssn.append(result["val"])

    # insert PII data
    print "insert PII data"
    i = 0

    pii_path = os.path.join('working','temp.csv')
    for humanU in humanUUID:
        output = None
        with open(piiCSV, 'rb') as f, open(pii_path, 'wb') as csvfile:
            mycsv = csv.reader(f)
            mycsv = list(mycsv)
            writer = csv.writer(csvfile)
            writer.writerow(mycsv[0])

            for row in mycsv:
                if row[0] == ssn[i]:
                    writer.writerow(row)
        p = Popen(['java', '-jar', 'client.jar', 'insert', pii_path, 'org.mitre.tangerine.analytic.PiiAdapter', '--uuid', humanU, '--entity', 'SSN', '--key', ssn[i],
                   '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
        output, err = p.communicate()
        response = json.loads(output)
        configData["KBs"].append(response["collection"].replace("\n", ""))
        i += 1
        if logging:log(humanU + '_pii.log', output)
        if verbose: print output
        if err: report_err(err)

    # query voucher data for esri
    print "query voucher data for esri"
    output = None
    p = Popen(['java', '-jar', 'client.jar', 'reason', 'config/flquery-config-voucher.json', 'config/AE.flr', 'config/esri.qry',
               '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate()
    if logging:log('ReasonerEsri.json', output)
    if verbose: print output
    if err: report_err(err)

    # string to json
    data = json.loads(output)

    # get geolocation
    print "get geolocation"
    origins = []
    destinations = []
    travelling = []
    for datalist in data["dataList"]:
        if "AE#Travelling" in datalist["query"]:
            for resultlist in datalist["resultList"]:
                for result in resultlist["result"]:
                    if "?T" in result["key"]:
                        travelling.append(result["val"])
                    if "?originL" in result["key"]:
                        origins.append(result["val"])
                    if "?destL" in result["key"]:
                        destinations.append(result["val"])

    # get routes using esri
    output = None
    print "get routes using esri_travel_calculator"
    for travel, origin, destination in zip(travelling, origins, destinations):
        travel_path = os.path.join('working', travel + '_esri.csv')
        try:
            with open(travel_path, 'wb') as fh:
                travel_calc = drive_time_and_distance( origin, destination )
                write_result(fh, travel_calc)
                if verbose: 
                    print ">>> FROM",origin, "DEST", destination
                    print ">>> ==", repr(travel_calc)
        except Exception, err:
            print ">>> FROM",origin, "DEST", destination
            print str(err), "\nINTERIM DATA", travel_calc
            report_err(str(err))        
        
    # insert esri data
    print "insert esri data"
    for travel in travelling:
        travel_path = os.path.join('working', travel + '_esri.csv')
        output = None
        p = Popen(['java', '-jar', 'client.jar', 'insert', travel_path, 'org.mitre.tangerine.analytic.EsriAdapter', '--uuid', travel, '--entity', "AE#Travelling",
                   '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
        output, err = p.communicate()
        response = json.loads(output)
        configData["KBs"].append(response["collection"].replace("\n", ""))
        if logging: log(travel + '_esri.log', output)
        if verbose: print output
        if err: report_err(err)

    # write flora configuration file
    print "write flora configuration file"
    flora_query = 'working/{}_flora.json'.format(collectionId)
    with open(flora_query, 'w') as outfile:
        json.dump(configData, outfile)

    # one query to rule them all
    output = None
    print "final query"
    p = Popen(['java', '-jar', 'client.jar', 'reason', flora_query, 'config/AE.flr', 'config/final.qry',
               '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate()
    if err: report_err(err)
    if verbose: print output
    with open('results/{}.json'.format(collectionId), 'w') as f:
        f.write(output)

    outputType = "org.mitre.tangerine.adapter.outbound.CsvAdapter"
    p = Popen(['java', '-jar', 'client.jar', 'reason', flora_query, 'config/AE.flr', 'config/final.qry', '-o', outputType,
               '--host', demoServer, '--port', demoServerPort], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate()
    with open('results/{}.csv'.format(collectionId), 'w') as f:
        f.write(output)
    if verbose: print output
    if err: report_err(err)

    # Success
    return 0

if __name__ == "__main__":
    # -------------------------------------
    # run example,
    # pipeline.py  --claims CSV --pii CSV [ --clean ] [--netowl-server HOST ] [--server TANGERINE_HOST ]
    # -------------------------------------
    from argparse import ArgumentParser
    ap = ArgumentParser()
    ap.add_argument('--claims')
    ap.add_argument('--pii')
    ap.add_argument('--server', default='localhost')
    ap.add_argument('--netowl-server')
    ap.add_argument('--clean', action="store_true")

    args = ap.parse_args()

    # arguments
    if not args.claims:
        print("Claims data file required!")
        sys.exit(1)

    # These globals will appear in rest of the scope of this script
    claimCSV = args.claims
    piiCSV = args.pii
    demoServer = args.server
    demoServerPort = '8080'
    netowlServer = args.netowl_server

    if not os.path.exists(claimCSV):
        print ("Claims file not found! " + claimCSV)
        sys.exit(1)

    if not netowlServer:
        print("NetOwl Server is required for this demo")
        sys.exit(1)

    print "CLAIM START", claimCSV

    # Either create or clean directories as needed
    #
    for RUN_DIR in ['working', 'results']:
        _dir = os.path.abspath(RUN_DIR)
        if not os.path.isdir(_dir):
            os.makedirs(_dir)
        elif args.clean:
            import shutil
            shutil.rmtree(_dir)
            os.makedirs(_dir)

    with open(claimCSV, 'rb') as f:
        rowcount=0
        mycsv = csv.reader(f)
        header = next(mycsv, None)
        for row in mycsv:
            rowcount+=1
            print "========Row# {}=========".format(rowcount)
            working_path = os.path.join('working', 'claim.csv')
            with open(working_path, 'w') as csvfile:
                writer = csv.writer(csvfile, lineterminator='\r\n')
                writer.writerow(header)
                writer.writerow(row)
            pipeline(working_path)
