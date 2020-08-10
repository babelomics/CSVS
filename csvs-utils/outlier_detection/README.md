# CSVS SAMPLE OUTLIER DETECTION

## Description

This tool checks the number of variants and the number of novel variants produced by one or several samples. These metrics are helpful to determine if a specific sample may be undesired given an unexpected percentage of novel variants against a model distribution. Given that the expected number of variants and novel variants could be different depending on the followed strategy (WES, WGS, panels, etc), different models were trained for that purpose.

The provided scripts allows both create new model distributions or test new vcfs against the models already created.

## Requirements

The scripts are developed to be executed in a HPC enviroment with a Slurm queue manager. This cluster must contain the following modules (__module load__):

* R >= 3.5.0 
    * *ggplot2* library
    * *plyr* library
    * *gridExtra* library
* Python 3
    * *numpy* library
    * *scikit-allel* library
* bcftools >= 1.8 

### Usage (in cluster)

**1) For creating a new model from a set of validated VCFs:**
```
csvs_simulation_launcher.sh <model_vcf> <output_dir> <log_dir> \
                            <sample_list> [<model_size>] [<bed_file>]

<model_vcf>:     Aggregated multi-sample VCF files with all samples used for model
<output_dir>:    Directory where output files for this test will be located.
<log_dir>:       Directory where log files for this test will be located.
<sample_list>:   Comma-separated string with all tested sample IDs
<model_size>:    (Optional) Allow to create sub-models of different sizes for validation purposes. Options can be "max" (default) or "all" (progressive create models with all possible sizes) 
<bed_file>:      (Optional) Capture regions from where variants should be considered in BED format.
```

The distribution of novel variants for the new model is provided at **<output_dir>/<model_size>.sim.csv**.

**2) For calculating number of novel variants for a set of new samples:**
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

The comma-separated file named **<cohort_id>.test.csv** is created in the <output_dir> directory with the percentage of novel variants per sample.


**3) For finding outliers against the stablished model:**
```
csvs_outliers_launcher.sh <model_tsv> <output_dir> <cohort_id>

<model_tsv>:     Tab-separated file with model distribution for the number of novel variants.
<output_dir>:    Directory where output where located from csvs_evaluation_launcher.sh.
<cohort_id>:     Given name (String) for the tested cohort (sample_id if only one sample).
```

A list of undesired samples whose percentages of novel variants do not follow the expected model distribution will be provided in the **<output_dir>/<cohort_id>.test.outliers.txt** output
