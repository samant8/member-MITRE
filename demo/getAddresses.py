import os

f = open('addresses.txt', 'r')
while True:
    line1 = f.readline().strip().replace("\n", "")
    line2 = f.readline().strip().replace("\n", "")
    if not line2: break  # EOF
    if "San Diego" in line2:
        print line1+" "+line2
