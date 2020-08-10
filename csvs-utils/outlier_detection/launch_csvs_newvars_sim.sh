#!/bin/bash -l

aggVCF=$1
output=$2
sample=$3
ddbbsize=${4:-all}
bedfile=${5-none}

ml numpy
ml scikit-allel
ml bcftools
ml sequencing-qc-stats

# Filter aggregated VCF by bedfile if provided
if [[ "$bedfile" != "none" ]]; then

    echo "Intersecting aggregated VCF with BED file..."
    outpath=`dirname $output`
    newagg=`basename $aggVCF | sed 's/vcf\.gz/intersect\.vcf\.gz/g'`
    bcftools view ${aggVCF} -R ${bedfile} -o ${outpath}/${newagg} -Oz
    aggVCF=`echo "${outpath}/${newagg}"`

fi

# Run simulation
csvs_simulation.py --aggregated_vcf $aggVCF \
                   --output $output \
                   --samples $sample \
                   --mode "sim" \
                   --ddbb_size $ddbbsize
