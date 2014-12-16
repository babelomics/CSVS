package org.babelomics.pvs.lib.io;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import org.opencb.biodata.formats.variant.io.VariantReader;
import org.opencb.biodata.formats.variant.io.VariantWriter;
import org.opencb.biodata.models.feature.Genotype;
import org.opencb.biodata.models.variant.Variant;
import org.opencb.biodata.models.variant.VariantSourceEntry;
import org.opencb.biodata.models.variant.stats.VariantStats;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by aleman on 13/12/14.
 */
public class PVSVariantCompressedVcfDataWriter implements VariantWriter {
    private PrintWriter printer;
    private String filename;
    private VariantReader reader;
    private List<String> format;


    public PVSVariantCompressedVcfDataWriter(VariantReader reader, String filename) {
        this.filename = filename;
        this.reader = reader;
    }

    @Override
    public boolean open() {
        try {
            printer = new PrintWriter(filename);
        } catch (FileNotFoundException e) {
            return false;
        }

        return true;
    }

    @Override
    public boolean close() {
        printer.close();
        return true;
    }

    @Override
    public boolean pre() {

        String header = parseHeader(reader.getHeader());

//        List<String> header = Splitter.on("\t").splitToList(reader.getHeader());

//        printer.append(Joiner.on("\t").join(header.subList(0, 9)) + "\n");
        printer.append(header + "\n");
        return true;
    }

    private String parseHeader(String header) {
        List<String> splits = Splitter.on("\n").splitToList(header.trim());
        List<String> finalHeader = new ArrayList<>(2);
        String chr = splits.get(splits.size() - 1);
        List<String> chrSplits = Splitter.on("\t").splitToList(chr);
        finalHeader.add(splits.get(0));
        finalHeader.add(Joiner.on("\t").join(chrSplits.subList(0, 9)));
        return Joiner.on("\n").join(finalHeader);
    }

    @Override
    public boolean post() {
        return true;
    }

    @Override
    public boolean write(Variant elem) {
        StringBuilder sb = new StringBuilder();
        sb.append(elem.getChromosome()).append("\t");
        sb.append(elem.getStart()).append("\t");
        if (elem.getId() == null) {
            sb.append(".").append("\t");
        } else {
            sb.append(elem.getId()).append("\t");
        }
        sb.append(elem.getReference()).append("\t");
        sb.append(elem.getAlternate()).append("\t");

        VariantSourceEntry file = elem.getSourceEntries().values().iterator().next();
        if (file == null) {
            // There must be a file associated with this variant
            return false;
        }

        if (file.hasAttribute("QUAL")) {
            sb.append(file.getAttribute("QUAL"));
        } else {
            sb.append(".");
        }
        sb.append("\t");

        if (file.hasAttribute("FILTER")) {
            sb.append(file.getAttribute("FILTER"));
        } else {
            sb.append(".");
        }
        sb.append("\t");

        if (format == null) {
            format = getFormatOrder(file);
        }

        if (file.getStats() != null) {
            VariantStats stats = file.getStats();

            StringBuffer sbAux = new StringBuffer();

            int i = 0;
            for (Map.Entry<Genotype, Integer> entry : stats.getGenotypesCount().entrySet()) {
                i++;

                Genotype g = entry.getKey();
                sbAux.append(g + ":" + entry.getValue());

                if (i < stats.getGenotypesCount().size()) {
                    sbAux.append(",");
                }
            }

            if (i > 0) {
                file.addAttribute("HPG_GTC", sbAux.toString());
            }

        }


        sb.append(generateInfo(file.getAttributes())).append("\t");
        sb.append(Joiner.on(":").join(format)).append("\t");

        printer.append(sb.toString()).append("\n"); // TODO aaleman: Create a Variant2Vcf converter.
        return true;
    }

    private List<String> getFormatOrder(VariantSourceEntry file) {
        return Lists.newArrayList(file.getFormat().split(":"));
    }

    private String generateInfo(Map<String, String> attributes) {
        StringBuilder sb = new StringBuilder();


        Iterator<Map.Entry<String, String>> it = attributes.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();

            if (!entry.getKey().equalsIgnoreCase("QUAL") && !entry.getKey().equalsIgnoreCase("FILTER") && !entry.getKey().equalsIgnoreCase("src")) {
                sb.append(entry.getKey());
                if (!"".equals(entry.getValue())) {
                    sb.append("=").append(entry.getValue());
                }
                if (it.hasNext()) {
                    sb.append(";");
                }
            }
        }

        if (sb.length() == 0) {
            sb.append(".");
        }

        return sb.toString();
    }


    @Override
    public boolean write(List<Variant> batch) {
        for (Variant record : batch) {
            this.write(record);
        }

        return true;
    }

    @Override
    public void includeStats(boolean stats) {
    }

    @Override
    public void includeSamples(boolean samples) {
    }

    @Override
    public void includeEffect(boolean effect) {
    }
}
