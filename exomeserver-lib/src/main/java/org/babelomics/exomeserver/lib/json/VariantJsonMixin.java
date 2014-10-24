package org.babelomics.exomeserver.lib.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.opencb.biodata.models.variant.effect.VariantAnnotation;

import java.util.Map;
import java.util.Set;

/**
 * @author Alejandro Alemán Ramos <aaleman@cipf.es>
 */
public abstract class VariantJsonMixin {

    @JsonIgnore
    public abstract VariantAnnotation getAnnotation();

    @JsonIgnore
    public abstract Map<String, Set<String>> getHgvs();

}
