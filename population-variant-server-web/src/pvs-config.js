PVS_HOST = "http://test.babelomics.org/pvs/rest";
CELLBASE_VERSION = "v3";
CELLBASE_HOST = "http://wwwdev.ebi.ac.uk/cellbase/webservices/rest";
CELLBASE_HOST = "http://bioinfo.hpc.cam.ac.uk/cellbase/webservices/rest";
// CELLBASE_HOST = "http://bioinfo.hpc.cam.ac.uk/cellbase/webservices/rest";
PVS_SEPARATOR = "#-#";

var AVAILABLE_SPECIES = {
    "text": "Species",
    "items": [
        {
            "text": "Vertebrates",
            "items": [
                {
                    "text": "Homo sapiens",
                    "assembly": "GRCh37.p10",
                    "region": {"chromosome": "13", "start": 32889611, "end": 32889611},
                    "chromosomes": ["1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "X", "Y", "MT"],
                    "url": "ftp://ftp.ensembl.org/pub/release-75/"
                },
            ]
        }
    ]
};

/** Reference to a species from the list to be shown at start **/
var DEFAULT_SPECIES = AVAILABLE_SPECIES.items[0].items[0];
