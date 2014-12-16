package org.babelomics.pvs.lib.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opencb.biodata.models.variant.effect.VariantAnnotation;

import java.util.Map;
import java.util.Set;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public abstract class VariantJsonMixin {

    @JsonIgnore
    abstract VariantAnnotation getAnnotation();

    @JsonIgnore
    abstract Map<String, Set<String>> getHgvs();

}
