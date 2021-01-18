#!/bin/bash -l

aggVCF=$1
outpath=$2
logpath=$3
jobname=$4
samplelist=$5
vcflist=${6:-none}
bedfile=${7:-none}
chrtag=${8:-no}

toolpath=$(dirname "$0")

sbatch --job-name=$jobname-csvs-eval \
	   --mem=64G --cpus-per-task=8 \
	   --output=$logpath/$jobname-csvs-test.out \
	   --error=$logpath/$jobname-csvs-test.err \
           $toolpath/launch_csvs_newvars_eval.sh \
           $aggVCF $outpath ${jobname}.test.csv $samplelist $vcflist $chrtag $bedfile
