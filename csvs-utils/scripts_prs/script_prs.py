import sys
import traceback
import logging

import json


# EXAMPLE USE:
# python script_prs.py pgs_all_metadata_scores_modify.csv
# python /home/groldan/apps/CSVS_PRS/CSVS/csvs-utils/scripts_prs/script_prs.py pgs_all_metadata_scores_modify.csv > inserts_prs.txt


# Script distinct ancestry
#var list1 = db.getCollection('Prs').distinct("ad.sources.ancestry")
#var list2 = db.getCollection('Prs').distinct("ad.scores.ancestry")
#var list3 = db.getCollection('Prs').distinct("ad.listPgs.ancestry")
#var resumentmp = list1.concat(list2);
#var resumen = unique = [... new Set(resumentmp.concat(list3))];

#print(resumen)
#---------------


file = sys.argv[1]

def formatData(data):
    return '"{0}"'.format(data)



def formatListObjects(ad):
    result = "["
    resultTmp = []
    for adItem in ad:
        # print(adItem)
        resultTmp.append( "{" + ','.join('"{0}"'.format(ad.strip()) for ad in adItem) + "}")
    result = ",".join(resultTmp) + "]"
    return result

def insertPRS(prsData):
    
    # print(prsData)
    # ancestryDistribution
    #adSources = formatListObjects(prsData["ad"]["sources"])
    #adScores = formatListObjects(prsData["ad"]["scores"])
    #adListPgs = formatListObjects(prsData["ad"]["listPgs"])
    #efos = formatListObjects(prsData["efos"])

    query = json.dumps(prsData)
    print("db.getCollection('Prs').insert(" +query +  ");");

    #print ("db.getCollection('Prs').insert({\"id\":" + formatData(prsData["id"]) +
     #     ", \"efos\":" + efos + 
      #    ", \"numVar\": NumberInt(" + str(prsData["numVar"]) + ")"+ 
       #   ", \"ad.sources\": " + adSources + 
        #  ", \"ad.scores\": " + adScores + 
         # ", \"ad.listPgs\": " + adListPgs + 
        #  ", \"releaseDate\": " + prsData["releaseDate"] + 
        #  "});")

def getAncestryDistribution(ad):
    result = []
    # print(ad)
    if ad != "":
        for adItem in ad.split("|"):
            listAdItem = adItem.split(":")
            # print(listAdItem)
            result.append({"ancestry": listAdItem[0], "value": float(listAdItem[1])})
    return result

def getEFOS(idData, labelData):
    result = []
    # print("EFOS: " + idData + " " + labelData) 
    if idData != "":
        labels = labelData.split("|")
        ids = idData.split("|")
        for i in range(0, len(ids)):
            result.append({"id": ids[i], "label":labels[i]})
    return result

with open(file) as f:
    i=0
    #print ("ID" + "\t" + "Mapped Trait(s) (EFO label)" + "\t" + "Mapped Trait(s) (EFO ID)" + "\t" +
    #"Ancestry Distribution (%) - Source of Variant Associations (GWAS)" + "\t" + 
    #"Ancestry Distribution (%) - Score Development/Training" + "\t" +)
    #"Ancestry Distribution (%) - PGS Evaluation" )
    #"Release Date")
    #

    for line in f:
        if not i == 0:
            prs = line.replace('\n','').replace('"','').split('\t')
            #print(prs)
            efos = getEFOS(prs[4],prs[3])
            sources = getAncestryDistribution(prs[15])
            scores = getAncestryDistribution(prs[16])
            listPgs = getAncestryDistribution(prs[17])
            prsData = {
                "idPgs": prs[0],
                "numVar": int(prs[8]),
                "releaseDate": prs[19]
            }
            if efos:
                prsData["efos"] = efos
            if scores:
                prsData["scores"] = scores
            if listPgs:
                prsData["listPgs"] = listPgs
            if sources:
                prsData["sources"] = sources
            #print(prsData)
            #print("")
            insertPRS(prsData)
            #print("")
            #print("---------------")
        i = i+1
