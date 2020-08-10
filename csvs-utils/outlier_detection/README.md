# CSVS SAMPLE OUTLIER DETECTION

## Description

This tool checks the number of variants and the number of novel variants produced by one or several samples. These metrics are helpful to determine whether the pipeline finished as expected or there were some kind of issue while running and samples are undesired. Given that the number of variants and new variants expected could be different depending on the followed strategy (WES, WGS, panels, etc), different models were trained for that purpose.

The provided scripts allows both create new models or test new vcfs against the models already created.

## Requirements

The scripts are developed to be executed in a HPC enviroment with a Slurm queue manager. This cluster must contain the following modules (__module load__):

* R >= 3.5.0 
    * //ggplot2// library
    * //plyr// library
    * //gridExtra// library
* Python 3
    * //numpy// library
    * //scikit-allel// library
* bcftools >= 1.8 

### Usage (in cluster)

1) For calculating number of novel variants for a set of new samples:
```
csvs_evaluation_launcher.sh <model_vcf> <output_dir> <log_dir> \
                            <cohort_id> <sample_list> <test_vcf> \
                            [<bed_file>] [<chr_flag>]

<model_vcf>:     Aggregated multi-sample VCF files with all samples used for model.
<output_dir>:    Directory where output files for this test will be located.
<log_dir>:       Directory where log files for this test will be located.
<cohort_id>:     Given name (String) for the tested cohort (sample_id if only one sample).
<sample_list>:   Comma-separated string with all tested sample IDs
<test_vcf>:      Single VCF or list of VCFs for tested samples (can be provided as a text file with one VCF per line).
<bed_file>:      (Optional) Capture regions from where variants should be considered in BED format.
<chr_flag>:      (Optional) Flag to determine if input VCFs have chr tag (yes/no).
```

There are two models that can be used to test the number of new variants according to the strategy:

WES: `/mnt/lustre/scratch/CBRA/projects/CSVS/QC_Test/WES/model/model_no_outliers/121wes.intersected.sorted.vcf.gz`
WGS: `/mnt/lustre/scratch/CBRA/projects/CSVS/QC_Test/WGS/model/185wgs.combined.vcf.gz`

2) For finding outliers against the stablished model:
```
csvs_outliers_launcher.sh <model_tsv> <output_dir> <cohort_id>

<model_tsv>:     Tab-separated file with model distribution for the number of novel variants.
<output_dir>:    Directory where output where located from csvs_evaluation_launcher.sh.
<cohort_id>:     Given name (String) for the tested cohort (sample_id if only one sample).
```

There are two models CSV that can be used to get outliers according to the strategy:

WES: `/mnt/lustre/scratch/CBRA/projects/CSVS/QC_Test/WES/model/model_no_outliers/121wes.sim.max.csv`

WGS: `/mnt/lustre/scratch/CBRA/projects/CSVS/QC_Test/WGS/model/sim_185wgs/185wgs.max.sim.csv`
