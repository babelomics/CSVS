package org.babelomics.csvs.lib.io;

import org.opencb.commons.io.DataReader;
import org.opencb.commons.io.DataWriter;
import org.opencb.commons.run.Runner;
import org.opencb.commons.run.Task;
import org.babelomics.csvs.lib.models.Region;

import java.util.List;

/**
 * @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
 */
public class CSVSRegionsRunner extends Runner<Region> {
    public CSVSRegionsRunner(DataReader<Region> reader, List<? extends DataWriter<Region>> dataWriters, List<Task<Region>> tasks, int batchSize) {
        super(reader, dataWriters, tasks, batchSize);
    }

    public CSVSRegionsRunner(DataReader<Region> reader, List<? extends DataWriter<Region>> dataWriters, List<Task<Region>> tasks) {
        super(reader, dataWriters, tasks);
    }


}
