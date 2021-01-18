#!/bin/bash -l

aggVCF=$1
output_path=$2
output=$3
sample_list=$4
test_vcf_list=${5:-none}
chrtag=${6:-no}
bedfile=${7:-none}

ml numpy
ml scikit-allel
ml bcftools
ml vcftools
ml sequencing-qc-stats

# Merge test VCFs and aggregated VCF

# Aggregate test VCFs if separated
if [[ "$test_vcf_list" != "none" ]]; then
	echo "Aggregating testing VCFs in $output_path..."

	# Aggregate test VCFs if list provided
	aggregated_test=`echo ${test_vcf_list}`

	# Read lines in file if list is provided in txt
	if [[ "$test_vcf_list"  == *"txt"* ]]; then

		test_vcf_list=`echo $(cat $test_vcf_list) | sed 's/ /,/g'`

	fi

	# Aggregate list of VCFs
	if [[ "$test_vcf_list"  == *","* ]]; then

		test_list=`echo ${test_vcf_list} | sed 's/,/ /g'`

		# Aggregate depending on bed file and chrom tag
		if [[ "$bedfile" == "none" ]]; then

			if [[ "$chrtag" == "yes" ]]; then

				bcftools merge ${test_list} \
						   -r chr1,chr2,chr3,chr4,chr5,chr6,chr7,chr8,chr9,chr10,chr11,chr12,chr13,chr14,chr15,chr16,chr17,chr18,chr19,chr20,chr21,chr22 \
						   | sed 's/chr//g' \
						   | bcftools view -o $output_path/tmp.test_samples.vcf.gz -Oz
			else

				bcftools merge ${test_list} \
						   -r 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22 \
						   -o $output_path/tmp.test_samples.vcf.gz -Oz
			fi

		else

			bcftools merge ${test_list} -o $output_path/tmp.test_samples.merged.vcf.gz -Oz
			tabix $output_path/tmp.test_samples.merged.vcf.gz

			if [[ "$chrtag" == "yes" ]]; then

				bcftools view $output_path/tmp.test_samples.merged.vcf.gz -R ${bedfile} \
					| sed 's/chr//g' | bgzip > $output_path/tmp.test_samples.vcf.gz

			else

				bcftools view $output_path/tmp.test_samples.merged.vcf.gz -R ${bedfile} \
					 -o $output_path/tmp.test_samples.vcf.gz -Oz

			fi

			rm $output_path/tmp.test_samples.merged.vcf.gz*

		fi

		aggregated_test=`echo "$output_path/tmp.test_samples.vcf.gz"`

	elif [[ "$bedfile" != "none" ]]; then

		bcftools view ${test_vcf_list} \
				-R ${bedfile} \
				-o $output_path/tmp.test_samples.vcf.gz -Oz

		aggregated_test=`echo "$output_path/tmp.test_samples.vcf.gz"`
	fi

	# Keep only required samples (in case original VCFs are multisample)
	echo "Filtering aggregated test VCF ${aggregated_test} by samples.."	
	bcftools view ${aggregated_test} -s ${sample_list} \
				| sed 's/ID=AD\,Number=R/ID=AD\,Number=\./g' \
				| bcftools view -o $output_path/tmp2.test_samples.vcf.gz -Oz
	bcftools index $output_path/tmp2.test_samples.vcf.gz
	rm $output_path/tmp.test_samples.vcf.gz*

	# Normalize merged VCF
	echo "Normalizing aggregated test VCF.."
	bcftools norm $output_path/tmp2.test_samples.vcf.gz --check-ref w \
				-f /mnt/lustre/scratch/CBRA/data/indexed_genomes/bwa/hs37d5/hs37d5.fa \
				-o $output_path/tmp3.test_samples.vcf.gz -Oz
	bcftools index $output_path/tmp3.test_samples.vcf.gz
	rm $output_path/tmp2.test_samples.vcf.gz*

	# Merge with model VCF
	echo "Merging aggregated test and model VCFs.."
	bcftools merge ${aggVCF} $output_path/tmp3.test_samples.vcf.gz \
				   -o $output_path/aggregated_test.vcf.gz -Oz
	rm $output_path/tmp3.test_samples.vcf.gz*

	aggVCF=`echo $output_path/aggregated_test.vcf.gz`
fi

# Check new variants for each test VCF
csvs_simulation.py --aggregated_vcf $aggVCF \
                   --output $output_path/$output \
	 	   --samples $sample_list\
	           --mode "eval"


