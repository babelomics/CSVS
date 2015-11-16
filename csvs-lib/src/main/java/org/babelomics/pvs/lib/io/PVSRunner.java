package org.babelomics.pvs.lib.io;

import org.babelomics.pvs.lib.models.Variant;
import org.opencb.commons.io.DataReader;
import org.opencb.commons.io.DataWriter;
import org.opencb.commons.run.Runner;
import org.opencb.commons.run.Task;

import java.util.List;

/**
 * @author Alejandro Alemán Ramos <alejandro.aleman.ramos@gmail.com>
 */
public class PVSRunner extends Runner<Variant> {
    public PVSRunner(DataReader<Variant> reader, List<? extends DataWriter<Variant>> dataWriters, List<Task<Variant>> tasks, int batchSize) {
        super(reader, dataWriters, tasks, batchSize);
    }

    public PVSRunner(DataReader<Variant> reader, List<? extends DataWriter<Variant>> dataWriters, List<Task<Variant>> tasks) {
        super(reader, dataWriters, tasks);
    }
}
