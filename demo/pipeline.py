from subprocess import Popen, PIPE
import json
import os

#change for debugging
verbose = True

#insert voucher data
print "insert voucher data"
f = open('VoucherInsert.log', 'w')
p = Popen(['java', '-jar', 'AETClient.jar', '-insert', "Test-Claims.csv", '-type', 'voucher'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
output, err = p.communicate()
f.write(output)
f.close()
if verbose:
	print output

#string to json
response = json.loads(output)

#read flora configuration template file
print "read flora configuration template file"
with open('tangerine/flora/flquery-config-template.json') as data_file:    
    configData = json.load(data_file)
if verbose:
	print configData

#fill collections array
print "fill collections array"
configData["KBs"].append(response["collection"].replace("\n", ""))
if verbose:
	print configData

#write flora configuration file
print "write flora configuration file"
with open('tangerine/flora/flquery-config-voucher.json', 'w') as outfile:
    json.dump(configData, outfile)

#query voucher data
print "query voucher data"
f = open('ReasonerNetOwl.json', 'w')
p = Popen(['java', '-jar', 'AETClient.jar', '-reasoner', '-configuration', 'tangerine/flora/flquery-config-voucher.json', '-ontologies', 'tangerine/flora/AE.flr', '-queries', 'tangerine/flora/voucher.qry'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
output, err = p.communicate()
f.write(output)
f.close()
if verbose:
	print output

#read reasoner response
print "read reasoner response"
with open('ReasonerNetOwl.json') as data_file:    
    data = json.load(data_file)
if verbose:
	print data

#get names and addresses to enrich
names = []
namesUUID = []
addresses = []
addressesUUID = []
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

#enrich names using the netowl app
print "enrich names using the netowl app"
i = 0
for name in names:
	f = open(namesUUID[i]+'_netowlapp.xml', 'w')
	p = Popen(['java', '-jar', 'netowlapp.jar', '-text', name], stdin=PIPE, stdout=PIPE, stderr=PIPE)
	output, err = p.communicate()
	f.write(output)
	f.close()
	i += 1
	if verbose:
		print output

#enrich addresses using the netowl app
print "enrich addresses using the netowl app"
i = 0
for address in addresses:
	f = open(addressesUUID[i]+'_netowlapp.xml', 'w')
	p = Popen(['java', '-jar', 'netowlapp.jar', '-text', address], stdin=PIPE, stdout=PIPE, stderr=PIPE)
	output, err = p.communicate()
	f.write(output)
	f.close()
	i += 1
	if verbose:
		print output

#insert netowl names to the knowledge store
print "insert netowl names to the knowledge store"
for nameUUID in namesUUID:
	f = open(nameUUID+'_netowl.log', 'w')
	p = Popen(['java', '-jar', 'AETClient.jar', '-insert', nameUUID+'_netowlapp.xml', '-type', 'netowl', '-entity', 'entity:person', '-uuid', nameUUID], stdin=PIPE, stdout=PIPE, stderr=PIPE)
	output, err = p.communicate()
	f.write(output)
	f.close()
	if verbose:
		print output
	
#string to json
response = json.loads(output)

#fill collections array
print "fill collections array"
configData["KBs"].append(response["collection"].replace("\n", ""))
if verbose:
	print configData

#insert netowl addresses to the knowledge store
print "insert netowl addresses to the knowledge store"
for addressUUID in addressesUUID:
	f = open(addressUUID+'_netowl.log', 'w')
	p = Popen(['java', '-jar', 'AETClient.jar', '-insert', addressUUID+'_netowlapp.xml', '-type', 'netowl', '-entity', 'entity:address:mail', '-uuid', addressUUID], stdin=PIPE, stdout=PIPE, stderr=PIPE)
	output, err = p.communicate()
	f.write(output)
	f.close()
	if verbose:
		print output

#string to json
response = json.loads(output)

#fill collections array
print "fill collections array"
configData["KBs"].append(response["collection"].replace("\n", ""))
if verbose:
	print configData

#query voucher data for SSNs
print "query voucher data for SSNs"
f = open('ReasonerPII.json', 'w')
p = Popen(['java', '-jar', 'AETClient.jar', '-reasoner', '-configuration', 'tangerine/flora/flquery-config-voucher.json', '-ontologies', 'tangerine/flora/AE.flr', '-queries', 'tangerine/flora/pii.qry'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
output, err = p.communicate()
f.write(output)
f.close()
if verbose:
	print output

#string to json   
data = json.loads(output)

#get SSNs
print "get SSNs"
ssn = []
ssnUUID = []
for datalist in data["dataList"]:
	if "AE#SocialSecurityNumber" in datalist["query"]:
		for resultlist in datalist["resultList"]:
			for result in resultlist["result"]:
				if "?S" in result["key"]:
					ssnUUID.append(result["val"])
				if "?o" in result["key"]:
					ssn.append(result["val"])

#insert PII data
print "insert PII data"
for ssnU in ssnUUID:
	f = open(ssnU+ '_pii.log', 'w')
	p = Popen(['java', '-jar', 'AETClient.jar', '-insert','PII.csv', '-type', 'pii', '-uuid', ssnU], stdin=PIPE, stdout=PIPE, stderr=PIPE)
	output, err = p.communicate()
	f.write(output)
	f.close()
	if verbose:
		print output

#string to json
response = json.loads(output)

#fill collections array
print "fill collections array"
configData["KBs"].append(response["collection"].replace("\n", ""))
if verbose:
	print configData

#query voucher data for esri
print "query voucher data for esri"
f = open('ReasonerEsri.json', 'w')
p = Popen(['java', '-jar', 'AETClient.jar', '-reasoner', '-configuration', 'tangerine/flora/flquery-config-voucher.json', '-ontologies', 'tangerine/flora/AE.flr', '-queries', 'tangerine/flora/esri.qry'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
output, err = p.communicate()
f.write(output)
f.close()
if verbose:
	print output

#string to json   
data = json.loads(output)

#get geolocation
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

#get routes using esri
print "get routes using esri"
i = 0
for travel in travelling:
	if verbose:
		print "java -jar esriapp.jar -from " + origins[i] + " -to " + destinations[i] + " -output " + travel+'_esri.csv'
	p = Popen(['java', '-jar', 'esriapp.jar', '-from', origins[i], "-to", destinations[i], "-output", travel+'_esri.csv'], stdin=PIPE, stdout=PIPE, stderr=PIPE)
	output, err = p.communicate()
	i += 1
	if verbose:
		print output

#insert esri data
print "insert esri data"
for travel in travelling:
	f = open(travel+ '_esri.log', 'w')
	p = Popen(['java', '-jar', 'AETClient.jar', '-insert', travel+'_esri.csv', '-type', 'esri', '-uuid', travel, '-entity', "AE#Travelling"], stdin=PIPE, stdout=PIPE, stderr=PIPE)
	output, err = p.communicate()
	f.write(output)
	f.close()
	if verbose:
		print output

#string to json
response = json.loads(output)

#fill collections array
print "fill collections array"
configData["KBs"].append(response["collection"].replace("\n", ""))
if verbose:
	print configData

#write flora configuration file
print "write flora configuration file"
with open('tangerine/flora/flquery-config-global.json', 'w') as outfile:
    json.dump(configData, outfile)

