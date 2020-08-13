#!/bin/bash -l

aggVCF=$1
outpath=$2
logpath=$3
samplelist=$4
ddbbsize=${5:-all}
bedfile=${6:-none}

toolpath=$(dirname "$0")

if [[ "$ddbbsize" != "max" ]]; then

	while read sample; do

		sbatch --job-name=$sample-csvs-sim \
		       --mem=32G --cpus-per-task=4 \
		       --output=$logpath/$sample-csvs-sim.out \
		       --error=$logpath/$sample-csvs-sim.err \
		       $toolpath/launch_csvs_newvars_sim.sh $aggVCF $outpath/$sample.sim.csv $sample $ddbbsize $bedfile

	done < $samplelist

else

	if [[ $samplelist == *","* ]]; then
		list=`echo $samplelist`
	else
		list=`paste -s -d, samplelist`
	fi

	sbatch --job-name=$ddbbsize-csvs-qc-sim \
	       --mem=64G --cpus-per-task=8 \
	       --output=$logpath/$ddbbsize-csvs-sim.out \
	       --error=$logpath/$ddbbsize-csvs-sim.err \
               $toolpath/launch_csvs_newvars_sim.sh $aggVCF $outpath/$ddbbsize.sim.csv $list $ddbbsize $bedfile

fi




