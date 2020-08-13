#!/bin/bash -l

model=$1
outpath=$2
name=$3

sbatch --job-name=${name}-csvs-get-outliers \
       --mem=32G --cpus-per-task=4 \
       --output=${outpath}/${name}-csvs-get-outliers.out \
       --error=${outpath}/${name}-csvs-get-outliers.err \
       /apps/ngs/sequencing-qc-stats/1.0/sequencing-qc-stats/vcf-qc/launch_csvs_get_outliers.sh $model $outpath $name


