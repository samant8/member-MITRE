import os
import csv
import sys

inputPath = sys.argv[1]
allHeaders = []
allResults = {}
totalRows = 0

# Create the dictionay keys
for (dirpath, dirnames, filenames) in os.walk(inputPath):
    for filename in filenames:
        with open(os.path.join(dirpath, filename), 'rb') as csvfile:
            firstLine = True
            reader = csv.reader(csvfile, delimiter=',')
            for row in reader:
                for column in row:
                    if firstLine:
                        allHeaders.insert(len(allHeaders), column)
                        allResults[column] = []
                firstLine = False

# create array of values corresponding to the header
for (dirpath, dirnames, filenames) in os.walk(inputPath):
    for filename in filenames:
        with open(os.path.join(dirpath, filename), 'rb') as csvfile:
            firstLine = True
            reader = csv.reader(csvfile, delimiter=',')
            for row in reader:
                if firstLine:
                    currentHeaders = row
                    firstLine = False
                else:
                    i = 0
                    for column in row:
                        allResults[currentHeaders[i]].insert(totalRows, column)
                        i += 1
                    totalRows += 1
                    for header in allHeaders:
                        if len(allResults[header]) < totalRows:
                            allResults[header].insert(len(allResults[header]), "")
# write single csv file
keys = []
with open('UC1.csv', 'wb') as csvfile:
    writer = csv.writer(csvfile, delimiter=',')
    i = 0
    for key in sorted(allResults.iterkeys()):
        keys.insert(i, key)
        i += 1
    writer.writerow(keys)
    i = 0
    while i < totalRows:
        results = []
        j = 0
        for key in keys:
            results.insert(j, allResults[key][i])
            j += 1
        writer.writerow(results)
        i += 1
