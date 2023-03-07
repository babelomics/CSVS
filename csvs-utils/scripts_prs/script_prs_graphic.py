import sys
import traceback
import logging

import json


# EXAMPLE USE:
# python script_prs_graphic.py exome /mnt/lustre/scratch/CBRA/projects/CSVS/prs/exomes.csv
# python /home/groldan/apps/CSVS_PRS/CSVS/csvs-utils/scripts_prs/script_prs_graphic.py exome /mnt/lustre/scratch/CBRA/projects/CSVS/prs/exomes_sum.csv > inserts_grahics_prs_exomes.txt
# python /home/groldan/apps/CSVS_PRS/CSVS/csvs-utils/scripts_prs/script_prs_graphic.py genome /mnt/lustre/scratch/CBRA/projects/CSVS/prs/genomes_sum.csv > inserts_grahics_prs_genomes.txt


# Script distinct ancestry
#var list1 = db.getCollection('Prs').distinct("ad.sources.ancestry")
#var list2 = db.getCollection('Prs').distinct("ad.scores.ancestry")
#var list3 = db.getCollection('Prs').distinct("ad.listPgs.ancestry")
#var resumentmp = list1.concat(list2);
#var resumen = unique = [... new Set(resumentmp.concat(list3))];

#print(resumen)
#---------------


sequencingType = sys.argv[1]
file = sys.argv[2]

if sequencingType not in ["exome", "genome"]:
    print("SequencingType values: 'exome' or 'genome'");
    sys.exit()

def insertPRSGraphic( prsDataGraphic):
    
    query = json.dumps(prsDataGraphic)
    print("db.getCollection('PrsGraphic').insert(" + query + ");")


with open(file) as f:
    i=0
    # pgs_id,is_normal,min,mean,max,std.dev.,plot_y,plot_x,Decile_1,Decile_2,Decile_3,Decile_4,Decile_5,Decile_6,Decile_7,Decile_8,Decile_9
    prsGraphicTitle = []
    for line in f:
        #if i == 12:
        #    sys.exit()
        if i == 0:
            prsGraphicTitle = line.replace('\n','').replace('"','').split(',')
        if not i == 0:
            prsGraphic = line.replace('\n','').replace('"','').split(',')

            # Example: [  2  18  33  83 142 155 111  63  23   5   1]
            plot_y = prsGraphic[6].replace('[','').replace(']','').split(" ")
            # remove empty
            plot_y = [index for index in plot_y if index]

            # Example: -3.03e+01 -6.11e+00 1.81e+01 4.23e+01 6.64e+01 9.06e+01 1.15e+02 1.39e+02 1.63e+02 1.87e+02 2.12e+02 2.36e+02
            plot_x = prsGraphic[7].replace('[','').replace(']','').split(" ")

            prsDataGraphic = {
                "idPgs": prsGraphic[0],
                "seqType": sequencingType[0].upper(),
                "isNormal": prsGraphic[1],
                "min" : float(prsGraphic[2]),
                "mean" : float(prsGraphic[3]),
                "max" : float(prsGraphic[4]),
                "stdDev": float(prsGraphic[5]),
                "decile": [float(prsGraphic[8]),float(prsGraphic[9]),float(prsGraphic[10]),
                           float(prsGraphic[11]),float(prsGraphic[12]),float(prsGraphic[13]),
                           float(prsGraphic[14]),float(prsGraphic[15]),float(prsGraphic[16])],
                "plotX": [float(index) for index in plot_x],
                "plotY": [int(index) for index in plot_y]
            }
           
            #print(prsDataGraphic)
            insertPRSGraphic(prsDataGraphic)
        i = i+1

