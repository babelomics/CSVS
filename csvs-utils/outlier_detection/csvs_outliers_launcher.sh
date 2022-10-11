#!/bin/bash -l

model=$1
outpath=$2
name=$3

cmdpath=$(which "$0")
toolpath=$(dirname "$cmdpath")

sbatch --job-name=${name}-csvs-get-outliers \
       --mem=32G --cpus-per-task=4 \
       --output=${outpath}/${name}-csvs-get-outliers.out \
       --error=${outpath}/${name}-csvs-get-outliers.err \
       $toolpath/launch_csvs_get_outliers.sh $model $outpath $name


