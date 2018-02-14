package org.babelomics.csvs.lib.models;

import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.*;

import java.util.ArrayList;
import java.util.List;


/**
* @author Gema Roldán González <gema.roldan@juntadeandalucia.es>
*/
@Entity(noClassnameStored = true)
@Indexes({
        @Index(name = "index_file_c", fields = {@Field("c") }),
        @Index(name = "index_file_s", fields = {@Field("s") }),
        @Index(name = "index_file_e", fields = {@Field("e") })
})
public class Region {
    @Id
    private ObjectId id;

    @Property("c")
    private String chromosome;

    @Property("s")
    private int start;

    @Property("e")
    private int end;

    @Property("pid")
    private ObjectId idPanel;

    public Region(){
        this.chromosome = null;
        this.start = 1;
        this.end = Integer.MAX_VALUE;
    }

    public Region(String chromosome, int start) {
        this.chromosome = chromosome;
        this.start = start;
        this.end = Integer.MAX_VALUE;
    }

    public Region(String chromosome, int start, int end) {
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
    }

    public Region(String chromosome, int start, int end, Panel panel) {
        this.chromosome = chromosome;
        this.start = start;
        this.end = end;
        this.idPanel = (ObjectId) panel.getId();
    }

    public Region(String region) {
        if (region != null && !region.equals("")) {
            if (region.indexOf(':') != -1) {
                String[] fields = region.split("[:-]", -1);
                if (fields.length == 3) {
                    this.chromosome = fields[0];
                    this.start = Integer.parseInt(fields[1]);
                    this.end = Integer.parseInt(fields[2]);
                } else if (fields.length == 2) {
                    this.chromosome = fields[0];
                    this.start = Integer.parseInt(fields[1]);
                    this.end = Integer.MAX_VALUE;
                }
            } else {
                this.chromosome = region;
                this.start = 0;
                this.end = Integer.MAX_VALUE;
            }
        }
    }

    public static org.opencb.biodata.models.feature.Region parseRegion(String regionString) {
        org.opencb.biodata.models.feature.Region region = null;
        if (regionString != null && !regionString.equals("")) {
            if (regionString.indexOf(':') != -1) {
                String[] fields = regionString.split("[:-]", -1);
                if (fields.length == 3) {
                    region = new org.opencb.biodata.models.feature.Region(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2]));
                } else if (fields.length == 2) {
                    region = new org.opencb.biodata.models.feature.Region(fields[0], Integer.parseInt(fields[1]), Integer.MAX_VALUE);
                }
            } else {
                region = new org.opencb.biodata.models.feature.Region(regionString, 0, Integer.MAX_VALUE);
            }
        }
        return region;
    }

    public static List<org.opencb.biodata.models.feature.Region> parseRegions(String regionsString) {
        List<org.opencb.biodata.models.feature.Region> regions = null;
        if (regionsString != null && !regionsString.equals("")) {
            String[] regionItems = regionsString.split(",");
            regions = new ArrayList<>(regionItems.length);
            String[] fields;
            for (String regionString : regionItems) {
                if (regionString.indexOf(':') != -1) {
                    fields = regionString.split("[:-]", -1);
                    if (fields.length == 3) {
                        regions.add(new org.opencb.biodata.models.feature.Region(fields[0], Integer.parseInt(fields[1]), Integer.parseInt(fields[2])));
                    } else {
                        regions.add(null);
                    }
                } else {
                    regions.add(new org.opencb.biodata.models.feature.Region(regionString, 0, Integer.MAX_VALUE));
                }
            }
        }
        return regions;
    }

    public String getChromosome() {
        return chromosome;
    }

    public void setChromosome(String chromosome) {
        this.chromosome = chromosome;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

         Region region = ( Region) o;

        if (end != region.end) {
            return false;
        }
        if (start != region.start) {
            return false;
        }
        if (!chromosome.equals(region.chromosome)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = chromosome.hashCode();
        result = 31 * result + (int) (start ^ (start >>> 32));
        result = 31 * result + (int) (end ^ (end >>> 32));
        return result;
    }

    public boolean contains(String chr, long pos) {
        if (this.chromosome.equals(chr) && this.start <= pos && this.end >= pos) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.chromosome);

        if (this.start != 0 && this.end != Integer.MAX_VALUE) {
            sb.append(":").append(this.start).append("-").append(this.end);
        } else if (this.start != 0 && this.end == Integer.MAX_VALUE) {
            sb.append(":").append(this.start);
        }

        return sb.toString();
    }
}
