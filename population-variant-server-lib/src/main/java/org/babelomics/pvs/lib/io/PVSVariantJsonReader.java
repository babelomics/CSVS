package org.babelomics.pvs.lib.io;

import org.opencb.biodata.models.variant.VariantSource;
import org.opencb.opencga.storage.variant.json.VariantJsonReader;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public class PVSVariantJsonReader extends VariantJsonReader {
    public PVSVariantJsonReader(VariantSource source, String variantFilename, String globalFilename) {
        super(source, variantFilename, globalFilename);
    }

    @Override
    public String getHeader() {
        return "";
    }
}
