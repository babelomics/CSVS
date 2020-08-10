#!/bin/bash -l

model=$1
outpath=$2
name=$3

ml R/3.5.0

# Get script path
if [ -n $SLURM_JOB_ID ]; then
   toolpath=$(scontrol show job $SLURM_JOB_ID | awk -F= '/Command=/{print $2}' | cut -f1 -d' ')
   toolpath=`dirname $toolpath`
else
   toolpath=$(dirname "$0")
fi

# Run R
R -e "source('${toolpath}/plot_csvs_simulation.R'); csvs_test('${model}', '${outpath}/${name}.test.csv', output_outliers='${outpath}/${name}.test.outliers.txt')"
