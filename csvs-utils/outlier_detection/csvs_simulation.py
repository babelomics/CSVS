#!/usr/bin/env python

import argparse
import numpy as np
import allel
from datetime import datetime

def main():

	parser = argparse.ArgumentParser(description = 'Create simulation to model increasement of variants over a dataset')
	parser.add_argument('--aggregated_vcf', help = 'Multisampled VCF with all dataset samples in columns', required = True)
	parser.add_argument('--output', help = 'Tab-separated file with results for the simulation', required = True)
	parser.add_argument('--samples', help = 'List of sample identifiers (comma separated) to use for testing in "sim" model or for evaluating in "eval" mode. If not provided all samples in aggregated_vcf are used.')
	parser.add_argument('--mode', help = 'Determine which mode you are using the script. Options: "sim" (Default), "eval"', default = "sim")
	parser.add_argument('--ddbb_size', help = 'Determine which size of database we want to model. Options: "all" (Default), "max"', default = "all")

	args = parser.parse_args()

	print "Reading aggregated VCF..."
	read_start = datetime.now()
	aggVCF = allel.read_vcf(args.aggregated_vcf, fields=['calldata/GT', 'samples']);
	print "Reading DONE in %s..." % (datetime.now()-read_start)

	# Run simulation if mode sim
	if args.mode == "sim":
		simulate_model(aggVCF, args.samples, args.output, args.ddbb_size)
		print "Simulation FINISHED. Total time: %s" % (datetime.now()-read_start)
	elif args.mode == "eval":
		evaluate_model(aggVCF, args.samples, args.output)
	else:
		print 'ERROR: Mode is not recognized. Please, introduce "eval" or "sim" for --mode input'



def simulate_model(aggVCF, samples, outfile, ddbb_size="all"):

	print "Preparing variant matrix..."
	matrix_start = datetime.now()
	matrix = np.any(aggVCF['calldata/GT'] != [-1, -1], axis=2)
	if (np.all(matrix)):
			matrix = np.any(aggVCF['calldata/GT'] != [0, 0], axis=2)
	print "Matrix DONE in %s..." % (datetime.now()-matrix_start)

	# Use all available samples if not provided by user
	if samples == None:
		samples = aggVCF['samples']
	else:
			samples = samples.split(",")
	nsamples = len(aggVCF['samples'])

	# Open output and write headers
	outf = open(outfile,'w') 
	outf.write("Test Sample\tDDBB Size\t# Variants in Test\t# Variants in DDBB\t# New Variants\t% New Variants\n")

	pairs = np.empty([nsamples, nsamples], dtype=int)
	for sample in samples:

		# Get test VCF
		i = aggVCF['samples'].tolist().index(sample)
		vcftest = matrix[:,i]
		nvarvcf = sum(vcftest)
		dbsamples = range(0,i) + range(i+1,nsamples)

		if ddbb_size == "all":
			dbsize = range(0, len(dbsamples))
		else:
			dbsize = [len(dbsamples)]

		# Calculate matrix of relations
		#print "Adding common variants in %s to matrix..." % (sample)
		#start = datetime.now()
		#pairs[i,i] = sum((matrix[:,i] & matrix[:,i]))
		#for pair in dbsamples:
		#	pairs[i, pair] = sum((matrix[:,i] & matrix[:,pair]))
		#print pairs[i,:]
		#print "DONE in %s" % (datetime.now()-start)

		# Compare test VCF against iterative databases
		print "Testing for %s (%s/%s)..." % (sample, i+1, nsamples)
		for idx in dbsize:

			start = datetime.now()
			print "    Creating database with %s samples..." % (idx+1)
			# Get variables in database
			dbidxs = dbsamples[0:idx+1]
			dbvar  = np.any(matrix[:,dbidxs], axis=1)
			nvardb = sum(dbvar)

			# Get new variables
			nnewvars=sum(np.logical_and(vcftest, np.logical_not(dbvar)))

			#print "    New variants      = %s " % (nnewvars)
			#print "    Variants in test  = %s " % (nvarvcf)
			#print "    New variants perc = %.2f " % (100*float(nnewvars)/float(nvarvcf))
			print "    DONE in %s" % (datetime.now()-start)

			outf.write("%s	%s	%s	%s	%s	%f\n" % (sample,(idx+1),nvarvcf,nvardb,nnewvars,100*float(nnewvars)/float(nvarvcf)))

	outf.close() 
	#np.savetxt(outfile + ".matrix", pairs, delimiter=',')

def evaluate_model(aggVCF, test_samples, outfile):

	test_samples = test_samples.split(",")

	# Prepare matrix
	print "Preparing variant matrix..."
	matrix_start = datetime.now()
	#matrix = np.any(aggVCF['calldata/GT'] != [-1, -1], axis=2)
	matrix = np.any(aggVCF['calldata/GT'] != [-1, -1], axis=2)
	if (np.all(matrix)):
			matrix = np.any(aggVCF['calldata/GT'] != [0, 0], axis=2)
	print "Matrix DONE in %s..." % (datetime.now()-matrix_start)

	# Separate database from test samples
	allsamples = aggVCF['samples']
	dbidxs = [i for i, sample in enumerate(allsamples) if sample not in test_samples]
	dbsize = len(dbidxs)
	dbvar  = np.any(matrix[:,dbidxs], axis=1)
	nvardb = sum(dbvar)

	# Open output and write headers
	outf = open(outfile,'w')
	outf.write("Test Sample\tDDBB Size\t# Variants in Test\t# Variants in DDBB\t# New Variants\t% New Variants\n")

	# Check new variants for each test sample
	nsamples = len(test_samples)
	count = 0
	for s in test_samples:

		count += 1
		start = datetime.now()
		print "Testing for %s (%s/%s)..." % (s, count, nsamples)
		i = aggVCF['samples'].tolist().index(s)
		vcftest = matrix[:,i]
		nvarvcf = sum(vcftest)

		# Get new variables
		nnewvars = sum(np.logical_and(vcftest, np.logical_not(dbvar)))
		print "    DONE in %s" % (datetime.now()-start)

		outf.write("%s	%s	%s	%s	%s	%f\n" % (s,dbsize,nvarvcf,nvardb,nnewvars,100*float(nnewvars)/float(nvarvcf)))

	outf.close()

if __name__ == '__main__':
	main()
