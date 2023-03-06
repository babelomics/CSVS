import sys
import traceback
import logging


# EXAMPLE USE:
# python script_secondaryFinding.py SF_webCSVS.csv

file = sys.argv[1]

def formatData(data):
    return '"{0}"'.format(data)

def insertSecondaryFinding(sf):
    omims = "[" +','.join('"{0}"'.format(omim.strip()) for omim in sf["omims"]) + "]"
    print ("db.getCollection('AnnotationSecFindings').insert({\"c\":" + formatData(sf["c"]) +
          ", \"p\": NumberInt(" + sf["p"] + ")"+ 
          ", \"r\": " + formatData(sf["r"]) + 
          ", \"a\": " + formatData(sf["a"]) + 
          ", \"gene\": " + formatData(sf["gene"]) + 
          ", \"inheritance\": " + formatData(sf["inheritance"]) + 
          ", \"omims\": " + omims + 
          ", \"genericPhenotype\": " + formatData(sf["genericPhenotype"]) + 
          "});")


with open(file) as f:
    i=0
    #print ("Variant" + "\t" + "Gene" + "\t" + "General_phenotype" + "\t" + "Inheritance" + "\t" + "OMIM"  + "\t"+  "URL_OMIM")
    for line in f:
        if not i == 0:
            datasLine = line.replace('\n','').replace('"','').split('\t')

            variant = datasLine[0]
            infoVariant = variant.replace("-","").split(":")
            omims = datasLine[4].split(",")
            secondaryFinding = {
                "c": infoVariant[0],
                "p": infoVariant[1],
                "r": infoVariant[2],
                "a": infoVariant[3],
                "gene" : datasLine[1],
                "genericPhenotype" : datasLine[2],
                "inheritance" : datasLine[3],
                "omims" : omims
            }
            insertSecondaryFinding(secondaryFinding)
        i = i+1
