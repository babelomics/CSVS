#!/usr/bin/env python

import csv
import argparse
import resource
import mimetypes
import gzip
import re

def calculateCounts(samples):
    res = {"0/0": 0, "0/1":0, "1/1":0, "1/1":0, "./.":0,"total":0}

    for sampleData in samples:
        gt  = sampleData.split(":")[0]
        alleles = re.split(r'[/|]', gt)

        if len(alleles) == 2:
            allele1 = alleles[0]
            allele2 = alleles[1]

            if allele1 == "0" and allele2 == "0":
                res["0/0"] += 1
            elif allele1 == "1" and allele2 == "1":
                res["1/1"] += 1
            elif allele1 == "." or allele2 == ".":
                res["./."] += 1
            elif allele1 == "0" or allele2 =="0":
                res["0/1"] +=1
            elif allele1 == allele2:
                res["1/1"] +=1
            else:
                # 1/2 2/1....
                res["1/1"] +=1
        elif len(alleles) == 1:
            if alleles[0] == ".":
                res["./."] += 1
            elif alleles[0] == "0":
                res["0/0"] += 1
            else:
                res["1/1"] +=1

        elif len(alleles) >= 2:
            pass

    res["total"] = res["0/0"] + res["1/1"] + res["0/1"] + res["./."]

    return res

def main():
    parser = argparse.ArgumentParser(description="Calculate counts")
    parser.add_argument("--file-list", help="Comma-separated list of files", required=True)
    parser.add_argument("-o", "--output", help="Output CSV File", required=True)
    parser.add_argument("-c", "--compress", help="Compress output file (gzip)",action='store_true', default=False)

    args = parser.parse_args()

    files = args.file_list.split(",")

    if args.compress:
        out = gzip.open(args.output,"w")
    else:
        out = open(args.output,"w")

    for f in files:

        mimetype = mimetypes.guess_type(f)[1]
        if mimetype == "gzip":
            f = gzip.open(f,"r")
        else:
            f = open(f,"r")

        csvWriter = csv.writer(out, delimiter="\t")

        for rawLine in f:
            row  = []
            line = rawLine.strip()
            if not line.startswith("#"):
                splits = line.split("\t")
                row.append(splits[0])
                row.append(splits[1])
                row.append(splits[3])
                row.append(splits[4])
                row.append(splits[2])

                samples = splits[9:]
                counts = calculateCounts(samples)
                row.append(counts["0/0"])
                row.append(counts["0/1"])
                row.append(counts["1/1"])
                row.append(counts["./."])
                row.append(counts["total"])

            elif line.startswith("#CHROM"):
                row = ["chr","start","ref","alt","id", "0/0","0/1","1/1","./.", "total"]
            else:
                continue

            csvWriter.writerow(row)

        print "RAM: " + str(resource.getrusage(resource.RUSAGE_SELF).ru_maxrss)
        f.close()
    out.close()

main()
