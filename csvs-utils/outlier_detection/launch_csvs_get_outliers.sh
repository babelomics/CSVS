#!/bin/bash -l

model=$1
outpath=$2
name=$3

ml R/4.1.2
ml csvs-utils

# Get script path
cmdpath=$(which "$0")
toolpath=$(dirname "$cmdpath")

# Run R
R -e "source('${toolpath}/plot_csvs_simulation.R'); csvs_test('${model}', '${outpath}/${name}.test.csv', output_outliers='${outpath}/${name}.test.outliers.txt')"
