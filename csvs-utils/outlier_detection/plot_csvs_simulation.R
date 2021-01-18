library(ggplot2)
library(plyr)
library(gridExtra)

## Gives count, mean, standard deviation, standard error of the mean, and confidence interval (default 95%).
##   data: a data frame.
##   measurevar: the name of a column that contains the variable to be summarized
##   groupvars: a vector containing names of columns that contain grouping variables
##   na.rm: a boolean that indicates whether to ignore NA's
##   conf.interval: the percent range of the confidence interval (default is 95%)
summarySE <- function(data=NULL, measurevar, groupvars=NULL, na.rm=FALSE,
                      conf.interval=.95, .drop=TRUE) {

  # New version of length which can handle NA's: if na.rm==T, don't count them
  length2 <- function (x, na.rm=FALSE) {
    if (na.rm) sum(!is.na(x))
    else       length(x)
  }
  
  # This does the summary. For each group's data frame, return a vector with
  # N, mean, and sd
  datac <- ddply(data, groupvars, .drop=.drop,
                 .fun = function(xx, col) {
                   c(N    = length2  (xx[[col]], na.rm=na.rm),
                     mean = mean     (xx[[col]], na.rm=na.rm),
                     sd   = sd       (xx[[col]], na.rm=na.rm),
                     IQR   = IQR     (xx[[col]], na.rm=na.rm),
                     quantile (xx[[col]], c(0.25, 0.75), na.rm=na.rm)
                   )
                 },
                 measurevar
  )
  
  # Rename the "mean" column    
  datac <- rename(datac, c("mean" = measurevar))
  
  datac$se <- datac$sd / sqrt(datac$N)  # Calculate standard error of the mean
  
  # Calculate outliers limits
  datac$lowlim <- datac$`25%` - 1.5 * datac$IQR
  datac$uplim <-  datac$`75%` + 1.5 * datac$IQR
  
  # Confidence interval multiplier for standard error
  # Calculate t-statistic for confidence interval: 
  # e.g., if conf.interval is .95, use .975 (above/below), and use df=N-1
  ciMult <- qt(conf.interval/2 + .5, datac$N-1)
  datac$ci <- datac$se * ciMult
  
  return(datac)
}


## get_plots: Build evolution plot and boxplot with max database size
##    aggSim: table where simulation results are stored
get_plots <- function(aggregated_simulation, aggregated_test, outlier_limits, output_png=""){

  # Read simulation data and sample list
  aggSim <- read.csv(aggregated_simulation, sep='\t', check.names=FALSE)
  colnames(aggSim) <- c("sample", "ddbb_size", "nvar_vcf", "nvar_ddbb", "newvars", "perc_newvars") 
  
  if (!is.null(aggregated_test)){
    aggTest <- read.csv(aggregated_test, sep='\t', check.names=FALSE)
    colnames(aggTest) <- c("sample", "ddbb_size", "nvar_vcf", "nvar_ddbb", "newvars", "perc_newvars")
    #aggTest$ddbb_size <- aggTest$ddbb_size - 1
    #aggSim <- rbind(aggSim, aggTest)
  }

  # Calculate statistics metrics 
  sumSim <- summarySE(data=aggSim, measurevar="perc_newvars", groupvars=c("ddbb_size"))
  nsizes = dim(sumSim)[1]
  
  # Plot percentage evolutions against database size
  if(nsizes>1){
      p1 <- ggplot(data=sumSim, aes(x=ddbb_size, y=perc_newvars, color="darkslategray3")) +
            xlab("Database size (samples)") + ylab("% new variants") +          
            geom_point(show.legend = FALSE, color="darkslategray3") + 
            geom_line(show.legend = FALSE, color="darkslategray3") +
            geom_ribbon(data=sumSim, aes(ymax=uplim,ymin=lowlim), linetype=2, alpha=0.1, color="darkslategray3", show.legend = FALSE) +
            theme(axis.title=element_text(size=14)) 
  }
    
  # Get maximum database size
  boxplotsize <- max(aggSim$ddbb_size)
  fulldb<-aggSim[aggSim$ddbb_size==boxplotsize,]
  fulldbsum<-sumSim[sumSim$ddbb_size==boxplotsize,]
  
  # Get upper/lower outliers
  fulldbsum$uplim = min(max(fulldb$perc_newvars), fulldbsum$uplim)
  fulldbsum$lowlim = max(min(fulldb$perc_newvars), fulldbsum$lowlim)  
  if (!is.null(aggregated_test)){  
      outliers <- c(as.character(aggTest$sample[aggTest$perc_newvars > fulldbsum$uplim]), as.character(aggTest$sample[aggTest$perc_newvars < fulldbsum$lowlim]))
      outliers <- aggTest$sample %in% outliers
      outliers_colour <- factor(outliers, levels=c(FALSE, TRUE), labels = c("green", "red"))
  }
  else{
      outliers <- c(as.character(fulldb$sample[fulldb$perc_newvars > fulldbsum$uplim]),as.character(fulldb$sample[fulldb$perc_newvars < fulldbsum$lowlim]))
      aggTest <- fulldb[fulldb$sample %in% outliers,]
      outliers_colour <- factor(aggTest$ddbb_size, labels = c("red"))
  }
  
  p2 <- ggplot(fulldb, aes(x=ddbb_size, y=perc_newvars)) + geom_violin(color="darkslategrey", fill="darkslategray3") + 
        geom_boxplot(width=0.1, color="black", fill="grey") + xlim(c(boxplotsize-1,boxplotsize+1)) +
        xlab(paste("Max Database Size (", boxplotsize,")", sep="")) + ylab("% new variants") +   
        geom_point(data=aggTest, aes(x=ddbb_size, y=perc_newvars), colour=outliers_colour) +
        theme(axis.text.x=element_blank(), axis.ticks.x=element_blank(), axis.title=element_text(size=14)) 
  
  # Show both plots
 
  if (nsizes>1) { 
    if (output_png != "") { png(output_png, width=1200, height=600) } 
    grid.arrange(p1, p2, ncol=2) 
  } else{ 
    if (output_png != "") { png(output_png, width=400, height=600) }    
    print(p2)
  }
  if (output_png != "") { dev.off() } 

  return(c(fulldb,fulldbsum))

}

## get_outliers: Determine the outliers for the fulldb introduced as argument.
## The testing samples can be the entire database (evaluation) or a set of
## samples passed as aggTest argument (test). The outliers are saved in the 
## output_outliers file or printed in stdout if not provided. 
get_outliers <- function(fulldb, aggTest, output_outliers){
 
  # Print upper and lower thresholds
  print(c(fulldb$lowlim,fulldb$uplim))    
  
  # Calculate upper outliers
  upper_outliers <- as.character(aggTest$sample[aggTest$perc_newvars > fulldb$uplim])
  upper_values <- aggTest$perc_newvars[aggTest$perc_newvars > fulldb$uplim]
  if (length(upper_outliers) > 0) {
    upper_outliers <- data.frame(upper_outliers, upper_values, "up_outlier")
    colnames(upper_outliers) <- c("SampleID", "%_New_Variants", "Outlier_Type")
  }
  
  # Calculate lower outliers
  lower_outliers <- as.character(aggTest$sample[aggTest$perc_newvars < fulldb$lowlim])
  lower_values <- aggTest$perc_newvars[aggTest$perc_newvars < fulldb$lowlim]
  if (length(lower_outliers) > 0) { 
    lower_outliers <- data.frame(lower_outliers, lower_values, "lower_outlier")  
    colnames(lower_outliers) <- c("SampleID", "%_New_Variants", "Outlier_Type")
  }
  
  # Save/Print outliers
  outliers <- rbind(upper_outliers, lower_outliers)
  write.table(outliers, file = output_outliers, quote=FALSE, sep="\t")  
   
}


## csvs_simulation: Perform simulation of the evolution in the percentage of new variants
## against size of the database. It can be plotted interactively or saved in file
##   aggregated_simulation: file where simulation results are stored
##   output_png: file to save plots in png (if not interactive)
##   output_outliers: file to save outliers. If not provided, shown by stdout.
csvs_simulation <- function(aggregated_simulation, output_png="", output_outliers="") {

  # Get plots
  fulldb <- get_plots(aggregated_simulation, c(), output_png)

  # Get upper/lower outliers
  get_outliers(fulldb, fulldb, output_outliers)

}

## csvs_test: Determine outliers over new VCFs using a previously trained model
##   aggregated_simulation: File including metrics from trained model
##   aggregated_test: File including metrics from new samples
##   output_png: file to save plots in png (if not interactive)
##   output_outliers: file to save outliers. If not provided, shown by stdout.
csvs_test <- function(aggregated_simulation, aggregated_test, output_png="", output_outliers="") {

  # Get test
  aggTest <- read.csv(aggregated_test, sep='\t', check.names=FALSE)
  colnames(aggTest) <- c("sample", "ddbb_size", "nvar_vcf", "nvar_ddbb", "newvars", "perc_newvars")

  # Get plots
  fulldb <- get_plots(aggregated_simulation, aggregated_test, output_png)

  # Get upper/lower outliers
  get_outliers(fulldb, aggTest, output_outliers)
  
}

csvs_compare_annotations<- function(annotated_simulation, output_png="", output_outliers="") {
  
  # Get annotated metrics
  annSim <- read.csv(annotated_simulation, sep='\t', check.names=FALSE)
  colnames(annSim) <- c("sample", "ddbb_size", "nvar_vcf", "nvar_ddbb", "newvars", "perc_newvars", "NGSplatform", "TechnicalData")

  ionproton <- annSim[annSim$NGSplatform == "Ion Proton",]
  nonion <- annSim[annSim$NGSplatform != "Ion Proton",]
  
  captures <- c("Agilent V3", "Ampliseq Exome", "SureSelect V6-Post", "Agilent SureSelect Human All Exon v4", "SeqCap EZ VCROME Exome Roche", "MedExome Roche")
  p <- ggplot()
  p <- p + geom_violin(data=annSim[annSim$TechnicalData == captures[1],], aes(x=ddbb_size,y=perc_newvars, colour = captures[1], fill = captures[1]), alpha = 0.3)
  p <- p + geom_violin(data=annSim[annSim$TechnicalData == captures[2],], aes(x=ddbb_size,y=perc_newvars, colour = captures[2], fill = captures[2]), alpha = 0.3)
  p <- p + geom_violin(data=annSim[annSim$TechnicalData == captures[3],], aes(x=ddbb_size,y=perc_newvars, colour = captures[3], fill = captures[3]), alpha = 0.3)
  p <- p + geom_violin(data=annSim[annSim$TechnicalData == captures[4],], aes(x=ddbb_size,y=perc_newvars, colour = captures[4], fill = captures[4]), alpha = 0.3)
  p <- p + geom_violin(data=annSim[annSim$TechnicalData == captures[5],], aes(x=ddbb_size,y=perc_newvars, colour = captures[5], fill = captures[5]), alpha = 0.3)  
  p <- p + geom_boxplot(data=annSim, aes(x=ddbb_size,y=perc_newvars), width=0.05, color="black", fill="grey")
  p <- p + scale_colour_discrete(name="Technology") + scale_fill_discrete(name="Technology")  
  p
  
}
