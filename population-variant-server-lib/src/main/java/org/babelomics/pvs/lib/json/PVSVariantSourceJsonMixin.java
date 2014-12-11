package org.babelomics.pvs.lib.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opencb.opencga.storage.variant.json.VariantSourceJsonMixin;

import java.util.Map;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public abstract class PVSVariantSourceJsonMixin extends VariantSourceJsonMixin {
    @JsonIgnore
    public abstract Map<String, Integer> getSamplesPosition();

//    @JsonIgnore
//    public abstract Map<String, String> getMetadata();
}
