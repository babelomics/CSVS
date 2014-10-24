function ExomeServerWidget(args) {
    var _this = this;
    _.extend(this, Backbone.Events);

    this.id = Utils.genId("ExomeServerWidget");

    //set default args
    this.border = true;
    this.autoRender = false;
    this.targetId;
    this.width;
    this.height;

    //set instantiation args, must be last
    _.extend(this, args);

    this.rendered = false;
    if (this.autoRender) {
        this.render();
    }
}

ExomeServerWidget.prototype = {
    render: function (targetId) {
        var _this = this;
        this.targetId = (targetId) ? targetId : this.targetId;

        this.rendered = true;

    },
    draw: function () {
        var _this = this;
        console.log(EXOME_SERVER_HOST + "/variantInfoMongo");
        $.ajax({
            url: EXOME_SERVER_HOST + "/variantInfoMongo",
            dataType: 'json',
            //async:false,
            success: function (data) {
                console.log(data);
                _this.variantInfo = data;
                _this._draw();
            }
        });

    },
    _draw: function () {
        this.optValues = Ext.create('Ext.data.Store', {
            fields: ['value', 'name'],
            data: [
                {"value": "<", "name": "<"},
                {"value": "<=", "name": "<="},
                {"value": ">", "name": ">"},
                {"value": ">=", "name": ">="},
                {"value": "=", "name": "="},
                {"value": "!=", "name": "!="}
            ],
            pageSize: 20
        });

        /* main panel */
        this.panel = this._createPanel(this.targetId);

        this.summaryPanel = this._createSummaryPanel(this.variantInfo);
        //this.panel.add(this.summaryPanel);

        this.variantPanel = this._createVariantPanel();
        this.panel.add(this.variantPanel);

        this.genomeViewerPanel = this._createGenomeViewerPanel();

        //this._updateInfo();
        /* form */
    },
    _createPanel: function (targetId) {
        var _this = this;
        var panel = Ext.create('Ext.panel.Panel', {
            title: this.title,
            width: '100%',
            height: '100%',
            border: 0,
            layout: 'hbox',
            closable: false,
            renderTo: targetId,
            cls: 'ocb-border-top-lightgrey',
            tbar: {items: [
                {
                    text: 'Summary',
                    enableToggle: true,
                    pressed: false,
                    toggleGroup: 'options',
                    handler: function () {
                        //TODO
                        _this.panel.removeAll(false);
                        _this.panel.add(_this.summaryPanel);
                    }
                },
                {
                    text: 'Variants',
                    enableToggle: true,
                    pressed: true,
                    toggleGroup: 'options',
                    handler: function () {
                        //TODO
                        _this.panel.removeAll(false);
                        _this.panel.add(_this.variantPanel);
                    }
                },
                {
                    text: 'Genome Viewer',
                    enableToggle: true,
                    pressed: false,
                    toggleGroup: 'options',
                    handler: function () {

                        if (_this.grid.getStore().count() == 0) {
                            Ext.example.msg('Genove Viewer', 'You must apply some filters first!!')

                        } else {
                            _this.panel.removeAll(false);
                            _this.panel.add(_this.genomeViewerPanel);

                            var row = {};
//                            debugger

                            var selection = _this.grid.getView().getSelectionModel().getSelection();

                            if (selection.length > 0) {

                                row = selection[0].raw;
                                var region = new Region({
                                    chromosome: row.chr,
//                                    start: row.pos - 200,
                                    start: row.pos,
//                                    end: row.pos + 200
                                    end: row.pos
                                });


                                if (!_.isUndefined(_this.gv)) {
                                    _this.gv.setRegion(region);
                                }
                            } else {
                                Ext.example.msg('Genove Viewer', 'You must select one variant first!!')
                            }

                        }


                    }
                }
            ]},
            items: []
        });

        return panel;
    },
    _createVariantPanel: function () {

        this.grid = this._createGrid();
        this.form = this._createForm();

        var panel = Ext.create('Ext.panel.Panel', {
            width: '100%',
            height: '100%',
            bodyPadding: 20,
            border: 0,
            layout: {type: 'hbox', align: 'stretch'},
            cls: 'ocb-border-top-lightgrey',
            items: [
                this.form,
                this.grid
            ]
        });

        return panel;
    },
    _updateInfo: function () {
        var _this = this;
        _this.panel.setLoading(true);

        _this.sampleNames = [];

        var ctForm = Ext.getCmp(this.id + "conseq_type_panel");
        ctForm.removeAll();
        ctForm.add([
            {
                xtype: 'tbtext', text: '<span class="info">Select one or multiple conseq. type</span>'
            },
            _this._createDynCombobox("conseq_type", "Consequence Type", this.variantInfo.consequenceTypes, null)
        ]);

        _this.panel.setLoading(false);

    },
    _createSummaryPanel: function (data) {
        var _this = this;

        var cts = [];
        var ss = [];

        for (var key in data.consequenceTypes) {
            cts.push({
                name: Utils.formatText(key, '_'),
                count: data.consequenceTypes[key]
            });
        }
        console.log(cts);

        _this.ctStore = Ext.create('Ext.data.Store', {
            fields: ['name', 'count'],
            data: cts
        });

        var chartCT = Ext.create('Ext.chart.Chart', {
            xtype: 'chart',
            width: 700,
            height: 700,
            store: _this.ctStore,
            animate: true,
            shadow: true,
            legend: {
                position: 'right'
            },
            theme: 'Base:gradients',
            insetPadding: 60,
            series: [
                {
                    type: 'pie',
                    field: 'count',
                    showInLegend: true,
                    tips: {
                        trackMouse: true,
                        width: 200,
                        height: 28,
                        renderer: function (storeItem, item) {
                            //calculate percentage.
                            var total = 0;
                            _this.ctStore.each(function (rec) {
                                total += rec.get('count');
                            });
                            var name = Utils.formatText(storeItem.get('name'), "_");
                            this.setTitle(name + ': ' + Math.round(storeItem.get('count') / total * 100) + '%');
                        }
                    },
                    highlight: {
                        segment: {
                            margin: 20
                        }
                    },

                    label: {
                        field: 'name',
                        display: 'rotate',
                        contrast: true,
                        font: '10px Arial'
                    }

                }
            ]
        });

        var globalStats = new Ext.XTemplate(
            '<table cellspacing="0" style="max-width:400px;border-collapse: collapse;border:1px solid #ccc;"><thead>',
            '<th colspan="2" style="min-width:50px;border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">Global Stats</th>',
            '</thead><tbody>',

            '<tr style="border-collapse: collapse;border:1px solid #ccc;">',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;color:steelblue;font-weight:bold;white-space: nowrap;">Num variants</td>',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">{variantsCount}</td>',
            '</tr>',

            '<tr style="border-collapse: collapse;border:1px solid #ccc;">',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;color:steelblue;font-weight:bold;white-space: nowrap;">Num samples</td>',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">{samplesCount}</td>',
            '</tr>',

            '<tr style="border-collapse: collapse;border:1px solid #ccc;">',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;color:steelblue;font-weight:bold;white-space: nowrap;">Num indels</td>',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">{indelCount}</td>',
            '</tr>',

            //'<tr style="border-collapse: collapse;border:1px solid #ccc;">',
            //'<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;color:steelblue;font-weight:bold;white-space: nowrap;">Num snps</td>',
            //'<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">{snpCount}</td>',
            //'</tr>',

            '<tr style="border-collapse: collapse;border:1px solid #ccc;">',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;color:steelblue;font-weight:bold;white-space: nowrap;">Num biallelic</td>',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">{biallelicsCount}</td>',
            '</tr>',

            '<tr style="border-collapse: collapse;border:1px solid #ccc;">',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;color:steelblue;font-weight:bold;white-space: nowrap;">Num multiallelic</td>',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">{multiallelicsCount}</td>',
            '</tr>',

            '<tr style="border-collapse: collapse;border:1px solid #ccc;">',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;color:steelblue;font-weight:bold;white-space: nowrap;">Num transitions</td>',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">{transitionsCount}</td>',
            '</tr>',

            '<tr style="border-collapse: collapse;border:1px solid #ccc;">',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;color:steelblue;font-weight:bold;white-space: nowrap;">Num transversions</td>',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">{transversionsCount}</td>',
            '</tr>',

            '<tr style="border-collapse: collapse;border:1px solid #ccc;">',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;color:steelblue;font-weight:bold;white-space: nowrap;">% PASS</td>',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">{[this.pass(values)]}%</td>',
            '</tr>',

            '<tr style="border-collapse: collapse;border:1px solid #ccc;">',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;color:steelblue;font-weight:bold;white-space: nowrap;">Ti/Tv Ratio</td>',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">{[this.titv(values)]}</td>',
            '</tr>',

            '<tr style="border-collapse: collapse;border:1px solid #ccc;">',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;color:steelblue;font-weight:bold;white-space: nowrap;">Avg. Quality</td>',
            '<td style="border-collapse: collapse;border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;">{[this.avgq(values)]}</td>',
            '</tr>',

            {

                pass: function (values) {
                    var res = values.passCount / values.variantsCount;
                    return res.toFixed(2);
                },
                titv: function (values) {
                    var res = values.transitionsCount / values.transversionsCount;
                    return res.toFixed(2);
                },
                avgq: function (values) {
                    var res = values.accumulatedQuality / values.variantsCount;
                    return res.toFixed(2);
                }
            }
        );


        var items = [
            {
                xtype: 'container',
                layout: 'vbox',
                flex: 1,
                items: [
                    {
                        xtype: 'box',
                        flex: 1,
                        margin: 10,
                        data: data.globalStats,
                        tpl: globalStats
                    }
                ]
            },
            {
                xtype: 'container',
                layout: 'vbox',
                flex: 3,
                items: [
                    {
                        xtype: 'box',
                        width: 700,
                        html: '<div style="border:1px solid #ccc;padding: 5px;background-color: whiteSmoke;font-weight: bold;">Consequence type</div>'

                    },
                    chartCT
                ]
            }
        ];


        var panel = Ext.create('Ext.panel.Panel', {
//            title: 'summary',
            width: '100%',
            height: '100%',
            border: 0,
            layout: 'hbox',
            bodyPadding: 60,
            autoScroll: true,
            cls: 'ocb-border-top-lightgrey',
            items: items
        });

        return panel;
    },
    _createEffectPanel: function () {

        var _this = this;

        var panel = Ext.create('Ext.panel.Panel', {
            // title: 'Effect',
            width: '100%',
            height: '100%',
            border: 0,
            layout: 'hbox',
            cls: 'ocb-border-top-lightgrey',
            items: []
        });
        return panel;
    },
    _createGenomeViewer: function () {
        var _this = this;

        var gvpanel = Ext.create('Ext.panel.Panel', {
                title: 'Genome Viewer',
                flex: 8,
                height: '100%',
                border: 1,
                html: '<div id="' + this.id + 'genomeViewer" style="width:1200px;height:1500;position:relative;"></div>',
                listeners: {
                    afterrender: {
                        fn: function () {
                            console.log("hola");
                            var w = this.getWidth();
                            $('#' + _this.id + 'genomeViewer').width(w);

                            var region = new Region({
                                chromosome: "13",
                                start: 32889611,
                                end: 32889611
                            });


                            var genomeViewer = new GenomeViewer({
                                sidePanel: false,
                                targetId: _this.id + 'genomeViewer',
                                autoRender: true,
                                border: false,
                                resizable: true,
                                region: region,
                                trackListTitle: '',
                                drawNavigationBar: true,
                                drawKaryotypePanel: false,
                                drawChromosomePanel: false,
                                drawRegionOverviewPanel: false
                            }); //the div must exist

                            genomeViewer.draw();

                            this.sequence = new SequenceTrack({
                                targetId: null,
                                id: 1,
                                title: 'Sequence',
                                histogramZoom: 20,
                                transcriptZoom: 50,
                                height: 30,
                                visibleRegionSize: 200,
                                renderer: new SequenceRenderer(),

                                dataAdapter: new SequenceAdapter({
                                    category: "genomic",
                                    subCategory: "region",
                                    resource: "sequence",
                                    species: genomeViewer.species

                                })

                            });
                            this.gene = new GeneTrack({
                                targetId: null,
                                id: 2,
                                title: 'Gene',
                                height: 140,
                                minHistogramRegionSize: 20000000,
                                maxLabelRegionSize: 10000000,
                                minTranscriptRegionSize: 200000,

                                renderer: new GeneRenderer(),

                                dataAdapter: new CellBaseAdapter({
                                    category: "genomic",
                                    subCategory: "region",
                                    resource: "gene",
                                    species: genomeViewer.species,
                                    params: {
                                        exclude: 'transcripts.tfbs,transcripts.xrefs,transcripts.exons.sequence'
                                    },
                                    cacheConfig: {
                                        chunkSize: 100000
                                    },
                                    filters: {},
                                    options: {},
                                    featureConfig: FEATURE_CONFIG.gene
                                })
                            });
                            this.snp = new FeatureTrack({
                                targetId: null,
                                id: 4,
                                title: 'SNP',

                                minHistogramRegionSize: 12000,
                                maxLabelRegionSize: 3000,
                                featureType: 'SNP',
                                height: 100,

                                renderer: new FeatureRenderer(FEATURE_TYPES.snp),

                                dataAdapter: new CellBaseAdapter({
                                    category: "genomic",
                                    subCategory: "region",
                                    resource: "snp",
                                    params: {
                                        exclude: 'transcriptVariations,xrefs,samples'
                                    },
                                    species: genomeViewer.species,
                                    cacheConfig: {
                                        chunkSize: 10000
                                    },
                                    filters: {},
                                    options: {}
                                })
                            });

                            genomeViewer.addTrack(this.sequence);
                            genomeViewer.addTrack(this.gene);
                            genomeViewer.addTrack(this.snp);

                            _this.gv = genomeViewer;
                        }
                    }
                },
                autoRender: true
            })
            ;

        return gvpanel;
    },
    _createGenomeViewerPanel: function () {
        var _this = this;
        this.genomeViewer = this._createGenomeViewer();
        this.variantGridMini = this._createVariantGridAux();

        var panel = Ext.create('Ext.panel.Panel', {
            width: '100%',
            height: '100%',
            border: 0,
            layout: 'hbox',
            bodyPadding: 20,
            cls: 'ocb-border-top-lightgrey',
            items: [
                this.variantGridMini,
                this.genomeViewer
            ]
        });

        return panel;
    },
    _createVariantGridAux: function () {
        var _this = this;
        _this.stMini = Ext.create('Ext.data.Store', {
            //model: _this.model,
            fields: ['chr', 'pos', 'ref', 'alt'],
            data: [],
            autoLoad: false,
            //proxy: {
            //type: 'pagingmemory'
            //},
            pageSize: 10

        });

        var grid = Ext.create('Ext.grid.Panel', {
                title: 'Variant',
                flex: 1,
                height: '100%',
                store: _this.stMini,
                loadMask: true,
                border: 0,
                margin: '0 20 0 0',
                hideHeaders: true,
                border: 1,
                columns: [
                    {
                        text: 'Variant',
                        flex: 1,
                        xtype: "templatecolumn",
                        tpl: '{chr}:{pos} {ref}>{alt}'
                    }
                ],
                bbar: Ext.create('Ext.PagingToolbar', {
                    store: _this.stMini,
                    displayInfo: true,
                    displayMsg: 'Displaying topics {0} - {1} of {2}',
                    emptyMsg: "No topics to display"
                })}
        );

        grid.getSelectionModel().on('selectionchange', function (sm, selectedRecord) {

            if (selectedRecord.length) {

                var row = selectedRecord[0].data;
                var chr = row.chr;
                var pos = row.pos;

                var region = new Region({
                    chromosome: chr,
                    start: pos,
                    end: pos
                });

                if (!_.isUndefined(_this.gv)) {
                    _this.gv.setRegion(region);
                }
            }
        });

        return grid;
    },
    _createForm: function () {

        var _this = this;

        var accordion = Ext.create('Ext.form.Panel', {
            border: 1,
            title: '<span class="ssel">Filters</span>',
            width: 200,
            layout: {
                type: 'accordion',
                fill: false
            },
            margin: '0 20 20 0',
            tbar: {
                items: [
                    {
                        xtype: 'button',
                        text: 'Reload',
                        handler: function () {
                            Ext.example.msg('Reload', 'Sucessfully');
                        }
                    } ,
                    {
                        xtype: 'button',
                        text: 'Clear',
                        handler: function () {
                            Ext.example.msg('Clear', 'Sucessfully');
                        }
                    },
                    '->',
                    {
                        xtype: 'button',
                        text: 'Search',
                        handler: function () {
                            _this.st.removeAll();

                            var values = _this.form.getForm().getValues();
                            _this.st.getProxy().extraParams = {};
                            console.log(values);

                            if (values.genes !== undefined) {

                                var geneValues = values.genes.split(",");
                                var wrongGenes = _this._checkGenes(geneValues);
                                if (wrongGenes.length > 0) {
                                    Ext.MessageBox.show({
                                        title: 'Form Error',
                                        msg: "Wrong Gene Name(s): " + wrongGenes.join(","),
                                        icon: Ext.MessageBox.WARNING,
                                        buttons: Ext.Msg.OK
                                    });
                                    return;
                                }

                            }

                            var formParams = {};
                            if (localStorage.bioinfo_exome_server_query) {
                                var query = localStorage.bioinfo_exome_server_query;
                                var queryValues = JSON.stringify(values);
                                if (query == queryValues) {
                                    formParams["count"] = false;
                                    formParams["prevCount"] = Ext.getStore("gridStore").getTotalCount();
                                } else {
                                    localStorage.bioinfo_exome_server_query = JSON.stringify(values);
                                    formParams["count"] = true;
                                }

                            } else {
                                localStorage.bioinfo_exome_server_query = JSON.stringify(values);
                                formParams["count"] = true;
                            }

                            for (var param in values) {
                                if (formParams[param]) {
                                    var aux = [];
                                    aux.push(formParams[param]);
                                    aux.push(values[param]);
                                    formParams[param] = aux;
                                } else {
                                    formParams[param] = values[param];
                                }
                            }
                            console.log(formParams);
                            for (var param in formParams) {
                                _this.st.getProxy().setExtraParam(param, formParams[param]);
                            }
                            _this.st.load();
                            Ext.getCmp(_this.id + "_pagingToolbar").moveFirst();
                        }
                    }
                ]
            }
        });

        var regionItems = [
            this._getRegionGeneSelector(),
            this._getRegionList(),
            this._getGenes()
        ];

        var region = Ext.create('Ext.panel.Panel', {
            title: "Region/Gene",
            items: regionItems
        });

        var controlsItems = [
            this._getControls()
        ];

        var controls = Ext.create('Ext.panel.Panel', {
            title: "Controls",
            items: controlsItems
        });

        var effectItems = [
            //this._getConsequenceType()
        ];

        var effect = Ext.create('Ext.panel.Panel', {
            title: "Effect",
            items: effectItems
        });


        accordion.add(region);
        accordion.add(controls);
        //accordion.add(effect);

        return accordion;
    },
    _createEffectGrid: function () {

        var groupingFeature = Ext.create('Ext.grid.feature.Grouping', {
            groupHeaderTpl: '{groupField}: {groupValue} ({rows.length} Item{[values.rows.length > 1 ? "s" : ""]})'
        });
        this.stEffect = Ext.create("Ext.data.Store", {
            groupField: 'featureId',
            fields: [
                {name: "featureId", type: "String"},
                {name: "featureName", type: "String"},
                {name: "featureType", type: "String"},
                {name: "featureBiotype", type: "String"},
                {name: "featureChromosome", type: "String"},
                {name: "featureStart", type: "int"},
                {name: "featureEnd", type: "int"},
                {name: "featureStrand", type: "String"},
                {name: "snpId", type: "String"},
                {name: "ancestral", type: "String"},
                {name: "alternative", type: "String"},
                {name: "geneId", type: "String"},
                {name: "transcriptId", type: "String"},
                {name: "geneName", type: "String"},
                {name: "consequenceType", type: "String"},
                {name: "consequenceTypeObo", type: "String"},
                {name: "consequenceTypeDesc", type: "String"},
                {name: "consequenceTypeType", type: "String"},
                {name: "aaPosition", type: "int"},
                {name: "aminoacidChange", type: "String"},
                {name: "codonChange", type: "String"}
            ],
            data: [],
            autoLoad: false,
            proxy: {type: 'memory'},
            pageSize: 5
        });

        var gridEffect = Ext.create('Ext.grid.Panel', {
            title: '<span class="ssel">Effect</span>',
            flex: 1,
            height: '100%',
            store: this.stEffect,
            loadMask: true,
            border: 1,
            titleCollapse: true,
            collapsible: true,
            columns: [
                {xtype: 'rownumberer'},
                {
                    text: "Position chr:start:end (strand)",
                    dataIndex: "featureChromosome",
                    xtype: "templatecolumn",
                    tpl: '{featureChromosome}:{featureStart}-{featureEnd} <tpl if="featureStrand == 1">(+)<tpl elseif="featureStrand == -1">(-)</tpl>',
                    flex: 1
                },
                {
                    text: "snp Id",
                    dataIndex: "snpId",
                    flex: 1
                },
                {
                    text: "Consequence Type",
                    dataIndex: "consequenceType",
                    xtype: "templatecolumn",
                    tpl: '{consequenceTypeObo} (<a href="http://www.sequenceontology.org/browser/current_svn/term/{consequenceType}" target="_blank">{consequenceType}</a>)',
                    flex: 1
                },
                {
                    text: "Aminoacid Change",
                    xtype: "templatecolumn",
                    tpl: '<tpl if="aminoacidChange">{aminoacidChange} - {codonChange} ({aaPosition}) <tpl else>.</tpl>  ',
                    flex: 1
                },
                {
                    text: "gene (EnsemblId)",
                    dataIndex: "geneName",
                    xtype: 'templatecolumn',
                    tpl: '<tpl if="geneName">{geneName} (<a href="http://www.ensembl.org/Homo_sapiens/Location/View?g={geneId}" target="_blank">{geneId})</a><tpl else>.</tpl>',
                    flex: 1
                },
                {
                    text: "transcript Id",
                    dataIndex: "transcriptId",
                    xtype: 'templatecolumn',
                    tpl: '<a href="http://www.ensembl.org/Homo_sapiens/Location/View?t={transcriptId}" target="_blank">{transcriptId}</a>',
                    flex: 1
                },
                {
                    text: "feature Id",
                    dataIndex: "featureId",
                    flex: 1

                },
                {
                    text: "feature Name",
                    dataIndex: "featureName",
                    flex: 1

                },
                {
                    text: "feature Type",
                    dataIndex: "featureType",
                    flex: 1

                },
                {
                    text: "feature Biotype",
                    dataIndex: "featureBiotype",
                    flex: 1

                },
                {
                    text: "ancestral",
                    dataIndex: "ancestral",
                    hidden: true,
                    flex: 1
                },
                {
                    text: "alternative",
                    dataIndex: "alternative",
                    hidden: true,
                    flex: 1
                }
            ],
            collapsed: true,
            features: [groupingFeature, {ftype: 'summary'}],
            dockedItems: [
                {
                    xtype: 'toolbar',
                    dock: 'bottom',
                    items: [
                        {
                            xtype: 'tbtext',
                            id: this.id + "numRowsLabelEffect"
                        }
                    ]
                }
            ]
        });
        return gridEffect
    },
    _createGrid: function () {
        var _this = this

        var controls = ["BIER", "1000G", "EVS"];
        var gts = ["0/0", "0/1", "1/1", "./."];

        _this.columnsGrid = [
        ];

        _this.columnsGrid.push(
            {
                text: "Variant",
                //dataIndex: 'chromosome',
                width: 150,
                xtype: "templatecolumn",
                tpl: "{chromosome}:{position}"
            });

        _this.columnsGrid.push(
            {
                text: "Alleles",
                dataIndex: 'alleles',
                width: 40
            });
        _this.columnsGrid.push(
            {
                text: "SNP Id",
                dataIndex: 'snpId',
                width: 80
            });
        _this.columnsGrid.push(
            {
                text: "Gene",
                dataIndex: 'genes'
            }
        );

        for (var i = 0; i < controls.length; i++) {
            var controlName = controls[i];
            var elem = {};
            elem.text = controls[i];

            var func = "if (record.data.controls['" + controlName + "']){return record.data.controls['" + controlName + "'].alleles;} else {return '.';}";
            var allFunc = new Function("val", "meta", "record", func);

            elem.columns = [];

            var gtAux = [];
            for (var j = 0; j < gts.length; j++) {
                var gt = gts[j] == "./." ? "-1/-1" : gts[j];
                func = "if (record.data.controls['" + controls[i] + "'] && record.data.controls['" + controls[i] + "'].genotypes['" + gt + "']) { return record.data.controls['" + controls[i] + "'].genotypes['" + gt + "'];} else {return '.';}";
                allFunc = new Function("val", "meta", "record", func);
                gtAux.push(
                    {
                        text: gts[j],
                        flex: 1,
                        renderer: allFunc
                    });
            }
            elem.columns.push({
                text: 'Genotypes',
                columns: gtAux

            });

            func = "if (record.data.controls['" + controls[i] + "']) { return record.data.controls['" + controls[i] + "'].maf.toFixed(3); } else { return '.';}";
            allFunc = new Function("val", "meta", "record", func);

            elem.columns.push({
                text: 'MAF',
                flex: 1,
                renderer: allFunc
            });

            _this.columnsGrid.push(elem);
        }


        _this.columnsGrid.push(
            {
                text: "Polyphen",
                dataIndex: 'polyphen'
            }
        );
        _this.columnsGrid.push(
            {
                text: "SIFT",
                dataIndex: 'sift'
            }
        );

        _this.columnsGrid.push(
            {
                text: "Phenotype",
                dataIndex: 'phenotype'
                //hidden:true
            }
        );

        _this.attributes = [
            {name: "chromosome", type: "String"},
            {name: "position", type: "int"}   ,
            {name: "alt", type: "String"},
            {name: "ref", type: "String"},
            {name: "snpId", type: "String"},
            {name: 'alleles', type: 'String'},
            {name: 'gene_name', type: 'string'},
            {name: 'genes', type: 'String'},
            {name: 'ct', type: 'string'},
            {name: 'maf', type: 'float'} ,
            {name: 'allele_maf', type: 'string'},
            {name: "genotypes", type: 'auto'}  ,
            {name: "effect", type: 'auto'}  ,
            {name: "controls", type: 'auto'}  ,
            {name: "transcript", type: 'String'},
            {name: "aaPos", type: 'int'}   ,
            {name: "aaChange", type: 'String'},
            {name: "phenotype", type: 'String'},
            {name: "polyphen", type: 'String', emptyCellText: '.',
                renderer: function (value) {
                    if (value = "") {
                        return ".";
                    }
                    return value;

                }} ,
            {name: "sift", type: 'String', emptyCellText: '.',
                renderer: function (value) {
                    if (value = "") {
                        return ".";
                    }
                    return value;

                }} ,

        ];
        _this.model = Ext.define('Variant', {
            extend: 'Ext.data.Model',
            fields: _this.attributes
        });
        _this.st = Ext.create('Ext.data.Store', {

                pageSize: 25,
                model: _this.model,
                groupField: 'gene_name',
                data: [],
                autoLoad: false,
                storeId: 'gridStore',
                proxy: {
//                type: 'pagingmemory',
                    model: _this.model,
                    type: 'jsonp',
                    callbackKey: 'callback',
                    callback: 'callback',
//                pageParam: undefined,
//                groupParam: undefined,
//                filterParam: undefined,
//                sortParam: undefined,

                    url: EXOME_SERVER_HOST + "/variantsMongo",
                    reader: {
//                    type: 'json',
//                    model: _this.model,
                        root: 'data',
                        totalProperty: 'count'
                    },
                    listeners: {
                        exception: function (proxy, response, operation, eOpts) {
                            Ext.MessageBox.show({
                                title: 'REMOTE EXCEPTION',
                                msg: operation.getError(),
                                icon: Ext.MessageBox.ERROR,
                                buttons: Ext.Msg.OK
                            });
                        }
                    }
//                    actionMethods: {
//                        create: 'GET',
//                        read: 'GET',
//                        update: 'GET',
//                        destroy: 'GET'
//                    }
                },
                remoteSort: false,
                method: 'post',
                listeners: {

                    load: function (store, records, successful, operation, eOpts) {

                        console.log(records.length);

                        _this.st.suspendEvents();

                        for (var i = 0; i < records.length; i++) {

                            var variant = records[i];
                            var controls = [];

                            console.log(variant);

                            var split = variant.raw.position.split("_");

                            variant.set("chromosome", variant.raw.chr);
                            variant.set("position", variant.raw.pos);

                            var controls = variant.raw.studies;
                            var controlsData = [];

                            if (controls != null && controls.length > 0) {

                                var bierRef = "";
                                var bierAlt = "";
                                var elem = {};
                                for (var j = 0; j < controls.length; j++) {
                                    var c = controls[j];
                                    if (c.studyId == "BIER") {
                                        //bierAlleles = controls[j].ref + ">" + controls[j].alt[0];
                                        bierRef = c.ref;
                                        bierAlt = c.alt[0];

                                        elem.maf = c.stats.maf;
                                        elem.genotypes = c.stats.genotypeCount;
                                        controlsData[c.studyId] = elem;
                                        variant.set("alleles", bierRef + ">" + controls[j].alt.join(","));
                                        variant.set("snpId", c.snpId);
                                        break;
                                    }
                                }


                                for (var j = 0; j < controls.length; j++) {
                                    var c = controls[j];

                                    if (c.ref == bierRef && c.alt[0] == bierAlt) {
                                        var elem = {};
                                        elem.maf = c.stats.maf;
                                        elem.genotypes = c.stats.genotypeCount;

                                        var gt01 = -1;
                                        if (c.stats.genotypeCount["0/1"] != undefined) {
                                            gt01 = c.stats.genotypeCount["0/1"];
                                        }
                                        if (c.stats.genotypeCount["1/0"] != undefined) {
                                            gt01 = (gt01 == -1) ? 0 : gt01;
                                            gt01 += c.stats.genotypeCount["1/0"];
                                        }

                                        if (gt01 != -1) {
                                            c.stats.genotypeCount["0/1"] = gt01;
                                        }
                                        controlsData[c.studyId] = elem;
                                    }

                                }

                                variant.set("controls", controlsData);

                                variant.data.controls = controlsData;

                            }

                            _this._getEffect(variant);
                            _this._getPolyphenSift(variant);
                            variant.commit();

                        }
                        _this._getPhenotypes(records);
                        _this._updateInfoVariantMini(records);


                        _this.st.resumeEvents();
                        _this.st.fireEvent('refresh');
                    }
                }
            }
        )
        ;

        var groupingFeature = Ext.create('Ext.grid.feature.Grouping', {
            groupHeaderTpl: '{groupField}: {groupValue} ({rows.length} Item{[values.rows.length > 1 ? "s" : ""]})',
            enableGroupingMenu: false,
            startCollapsed: false
        });

        var grid = Ext.create('Ext.grid.Panel', {
                    title: '<span class="ssel">Variant Info</span>',
                    flex: 1,
                    store: _this.st,
                    loadMask: true,
                    border: 1,
                    columns: this.columnsGrid,
                    plugins: 'bufferedrenderer',
                    margin: '0 0 20 0',
                    features: [
                        {ftype: 'summary'}
                    ],
                    viewConfig: {
                        emptyText: 'No records to display'
                    },
                    bbar: Ext.create('Ext.PagingToolbar', {
                        store: _this.st,
                        id: _this.id + "_pagingToolbar",
                        pageSize: 25,
                        displayInfo: true,
                        displayMsg: 'Variants {0} - {1} of {2}',
                        emptyMsg: "No variants to display",
                    }),
                    dockedItems: [
                        {
                            xtype: 'toolbar',
                            dock: 'bottom',
                            items: [
                                {
                                    xtype: 'tbtext',
                                    id: this.id + "numRowsLabel"
                                },
                                '->',
                                {
                                    xtype: 'button',
                                    //text: 'Columns',
                                    text: '<span style="font-size: 12px">Columns <span class="caret"></span></span>',
                                    handler: function (bt, e) {
                                        var menu = _this.grid.headerCt.getMenu().down('menuitem[text=Columns]').menu;
                                        menu.showBy(bt);
                                    }
                                }
                                /*{*/
                                //xtype: 'button',
                                //text: 'Export data...',
                                //handler: function () {
                                //if (!Ext.getCmp(_this.id + "exportWindow")) {
                                //var cbgItems = [];
                                //var attrList = _this._getColumnNames();

                                //cbgItems.push({
                                //boxLabel: attrList[0],
                                //name: 'attr',
                                //inputValue: attrList[0],
                                //checked: true,
                                //disabled: true
                                //});

                                //for (var i = 1; i < attrList.length; i++) {
                                //cbgItems.push({
                                //boxLabel: attrList[i],
                                //name: 'attr',
                                //inputValue: attrList[i],
                                //checked: true
                                //});
                                //}

                                //Ext.create('Ext.window.Window', {
                                //id: _this.id + "exportWindow",
                                //title: "Export attributes",
                                //height: 250,
                                //maxHeight: 250,
                                //width: 400,
                                //autoScroll: true,
                                //layout: "vbox",
                                //modal: true,
                                //items: [
                                //{
                                //xtype: 'checkboxgroup',
                                //id: _this.id + "cbgAttributes",
                                //layout: 'vbox',
                                //items: cbgItems
                                //}
                                //],
                                //buttons: [
                                //{
                                //xtype: 'textfield',
                                //id: _this.id + "fileName",
                                //emptyText: "enter file name",
                                //flex: 1
                                //},
                                //{
                                //text: 'Download',
                                //href: "none",
                                //handler: function () {
                                //var fileName = Ext.getCmp(_this.id + "fileName").getValue();
                                //if (fileName == "") {
                                //fileName = "variants";
                                //}
                                //var columns = Ext.getCmp(_this.id + "cbgAttributes").getChecked();

                                //var content = _this._exportToTab(columns);

                                //this.getEl().set({
                                //href: 'data:text/csv,' + encodeURIComponent(content),
                                //download: fileName + ".txt"
                                //});
                                //}
                                //}
                                //]
                                //}).show();
                                //}
                                //}
                                /*}*/
                            ]
                        }
                    ]
                }
            )
            ;


        grid.getSelectionModel().on('selectionchange', function (sm, selectedRecord) {

            if (selectedRecord.length) {
                console.log(selectedRecord[0].data)

                var row = selectedRecord[0].data;
                var chr = row.chromosome;
                var pos = row.position;
                var ref = row.ref;
                var alt = row.alt;

                _this.region = new Region({
                    chromosome: chr,
                    start: pos,
                    end: pos
                });

                if (!_.isUndefined(_this.gv)) {
                    _this.gv.setRegion(_this.region);
                } else {
                    console.log("GV Not defined");
                }

            }
        });

        return grid;
    },
    _updateEffectGrid: function (chr, pos, ref, alt) {

        var _this = this;

        _this.gridEffect.setLoading(true);

        var formParams = {};
        formParams['chr'] = chr;
        formParams['pos'] = pos;
        formParams['ref'] = ref;
        formParams['alt'] = alt;

//        console.log(formParams);

        _this.gridEffect.getStore().removeAll();

        $.ajax({
            url: EXOME_SERVER_HOST + "/effects",
            dataType: 'json',
            type: 'POST',
            data: formParams,
            success: function (response, textStatus, jqXHR) {
//                console.log(response);
                if (response.length > 0) {
                    _this.gridEffect.getStore().loadData(response);
                    _this.gridEffect.setTitle('<span class="ssel">Effect</span> - <spap class="info">' + chr + ':' + pos + ' ' + ref + '>' + alt + '</spap>');
                    Ext.getCmp(_this.id + "numRowsLabelEffect").setText(response.length + " effects");

                } else {
                    _this.gridEffect.getStore().removeAll();
                }
                _this.gridEffect.setLoading(false);
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.log('Error loading Effect');
                _this.gridEffect.setLoading(false);

            }
        });

    },
    _getSubColumn: function (colName) {
        var _this = this;
        var subCols = [];

        for (var i = 0; i < _this.columnsGrid.length; i++) {
            var col = _this.columnsGrid[i];

            if (col["text"] == colName && col["columns"] != null && col["columns"].length > 0) {
                var sub = col["columns"];
                for (var j = 0; j < sub.length; j++) {
                    var elem = sub[j];
                    subCols.push(elem["text"]);
                }
            }
        }
        return subCols;

    },
    _exportToTab: function (columns) {

        var _this = this;
        var colNames = [];

        var headerLine = "";
        for (var i = 0; i < columns.length; i++) {
            var col = columns[i];

            var subCols = _this._getSubColumn(col["boxLabel"]);
            if (subCols.length > 0) {
                for (var j = 0; j < subCols.length; j++) {
                    headerLine += subCols[j] + "\t";
                    colNames.push(subCols[j]);

                }
            } else {
                headerLine += col["boxLabel"] + "\t";
                colNames.push(col["boxLabel"]);
            }
            subCols.splice(0, subCols.length);

        }

        var output = "";
        output += "#" + headerLine + "\n";

        var lines = _this.st.getRange();
        for (var j = 0; j < lines.length; j++) {
            output += _this._processFileLine(lines[j].getData(), colNames);
            output += "\n";
        }

        return output;
    },
    _processFileLine: function (data, columns) {

        var line = "";
        for (var i = 0; i < columns.length; i++) {
            var col = columns[i];
            switch (col) {
                case "Variant":
                    line += data.chromosome + ":" + data.position;
                    break;
                case "Alleles":
                    line += data.ref + ">" + data.alt;
                    break;
                case "SNP id":
                    line += data.stats_id_snp;
                    break;
                case "1000G":
                    if (data.controls["1000G"]) {
                        line += data.controls["1000G"].maf + "(" + data.controls["1000G"].allele + ")";

                    } else {
                        line += ".";
                    }
                    break;
                case "BIER":
                    if (data.controls["BIER"]) {

                        line += data.controls["BIER"].maf + "(" + data.controls["BIER"].allele + ")";
                    } else {
                        line += ".";
                    }
                    break;
                case "ESP":
                    line += "-";
                    break;

                case "Gene":
                    line += data.gene_name;
                    break;
                case "Consq. Type":
                    line += data.ct;
                    break;
                case "Polyphen":
                    line += "-";
                    break;
                case "Sift":
                    line += "-";
                    break;
                case "Conservation":
                    line += "-";
                    break;

                case "Allele Ref":
                    line += data.ref;
                    break;
                case "Allele Alt":
                    line += data.alt;
                    break;

                case "MAF":
                    line += data.stats_maf;
                    break;
                case "MGF":
                    line += data.stats_mgf;
                    break;

                case "Miss. Alleles":
                    line += data.stats_miss_allele;
                    break;
                case "Miss. Genotypes":
                    line += data.stats_miss_gt;
                    break;
                case "Mendelian Errors":
                    line += data.stats_mendel_err;
                    break;
                case "Is indel?":
                    line += data.stats_is_indel;
                    break;

                case "% Controls dominant":
                    line += data.stats_cases_percent_dominant;
                    break;

                case "% Cases dominant":
                    line += data.stats_controls_percent_dominant;
                    break;
                case "% Cases recessive":
                    line += data.stats_cases_percent_recessive;
                    break;
                case "% Controls recessive":
                    line += data.stats_controls_percent_recessive;
                    break;
                default:
                    line += data[col];
            }
            line += "\t";
        }
        return line;
    },
    _getColumnNames: function () {
        var _this = this;

        var colNames = [];
        for (var i = 0; i < _this.columnsGrid.length; i++) {
            var col = _this.columnsGrid[i];
            colNames.push(col.text);
        }
        return colNames;
    },
    _updateInfoVariantMini: function (data) {

        var _this = this;
        var result = [];

        for (var i = 0; i < data.length; i++) {
            var elem = data[i];
//            console.log(elem);
            result.push({
                chr: elem.raw.chr,
                pos: elem.raw.pos,
                ref: elem.raw.studies[0].ref,
                alt: elem.raw.studies[0].alt[0]
            });
        }


//        for (var key in data) {
//            var elem = data[key];
//            result.push({
//                chr: elem.chromosome,
//                pos: elem.position,
//                ref: elem.ref,
//                alt: elem.alt
//            });
//        }

        _this.stMini.loadData(result);
    },

////
////
    /*FORM COMPONENTS*/
////
////

    _getRegionList: function () {
        var regionList = Ext.create('Ext.form.field.TextArea', {
            id: this.id + "region_list",
            name: "region_list",
            emptyText: '1:1-1000000,2:1-1000000',
            margin: '0 0 0 5',
//            value: "1:66000-90000",
            //value:"1:136048-802093",
            allowBlank: false
        });

        return Ext.create('Ext.form.Panel', {
            id: this.id + "region_list_panel",
            border: true,
            bodyPadding: "5",
            margin: "0 0 5 0",
            width: "100%",
            border: 0,
            buttonAlign: 'center',
            layout: 'vbox',
            items: [
                {
                    xtype: 'tbtext', text: '<span class="info">Enter regions (comma separated)</span>'
                },
                regionList
            ]
        });
    },
    _getGenes: function () {
        var geneList = Ext.create('Ext.form.field.TextArea', {
            id: this.id + "genes",
            name: "genes",
            emptyText: 'BRCA2,PPL',
            margin: '0 0 0 5',
            allowBlank: false,
            disabled: true
//            value: "BRCA2"
        });

        return Ext.create('Ext.form.Panel', {
            id: this.id + "genes_panel",
            border: true,
            bodyPadding: "5",
            margin: "0 0 5 0",
            width: "100%",
            border: 0,
            buttonAlign: 'center',
            layout: 'vbox',
            hidden: true,
            items: [
                {
                    xtype: 'tbtext', text: '<span class="info">Enter genes (comma separated)</span>'
                },
                geneList
            ]
        });
    },
    _getRegionGeneSelector: function () {
        var _this = this;

        var region = Ext.create('Ext.form.field.Radio', {
            id: "region_selector" + "_" + this.id,
            boxLabel: 'Region',
            inputValue: 'region',
            checked: true,
            name: 'RGSelector',
            handler: function (field, value) {
//                console.log(value);
                if (value) {
                    Ext.getCmp(_this.id + "genes_panel").hide();
                    Ext.getCmp(_this.id + "region_list_panel").show();

                    Ext.getCmp(_this.id + "region_list").enable();
                    Ext.getCmp(_this.id + "genes").disable();
                    Ext.getCmp(_this.id + "genes").setValue("");


                }
            }
        });

        var gene = Ext.create('Ext.form.field.Radio', {
            id: "gene_selector" + "_" + this.id,
            boxLabel: 'Gene',
            inputValue: 'gene',
            checked: false,
            name: 'RGSelector',
            handler: function (field, value) {
//                console.log(value);
                if (value) {
                    Ext.getCmp(_this.id + "region_list_panel").hide();
                    Ext.getCmp(_this.id + "genes_panel").show();

                    Ext.getCmp(_this.id + "genes").enable();
                    Ext.getCmp(_this.id + "region_list").disable();
                    Ext.getCmp(_this.id + "region_list").setValue("");
                }
            }

        });
        var radioGroupTest = Ext.create('Ext.form.RadioGroup', {
//            fieldLabel: 'Test',
            width: "100%",
            items: [
                region,
                gene]
        });

        return radioGroupTest;

    },
    _getConsequenceType: function () {

        return Ext.create('Ext.form.Panel', {
            border: true,
            bodyPadding: "5",
            margin: "0 0 5 0",
            width: "100%",
            buttonAlign: 'center',
            layout: 'vbox',
            border: 0,
            id: this.id + "conseq_type_panel",
            items: []
        });
    },
    _getControls: function () {
        return Ext.create('Ext.form.Panel', {
            bodyPadding: "5",
            margin: "0 0 5 0",
            width: "100%",
            buttonAlign: 'center',
            layout: 'vbox',
            border: 0,
            items: [
                {
                    xtype: 'fieldcontainer',
                    layout: 'hbox',
                    border: false,
                    width: "100%",
                    items: [
                        {
                            xtype: 'tbtext', margin: '5 0 0 0', text: '<span class="emph">1000G MAF <</span>'
                        },
                        {
                            xtype: 'textfield',
                            name: 'maf_1000g_controls',
                            margin: '0 0 0 5',
                            labelWidth: '50%',
                            width: "50%"
                        }
                    ]
                },
                {
                    xtype: 'fieldcontainer',
                    layout: 'hbox',
                    border: false,
                    width: "100%",
                    items: [
                        {
                            xtype: 'tbtext', margin: '5 0 0 0', text: '<span class="emph">EVS MAF <</span>'
                        },
                        {
                            xtype: 'textfield',
                            name: 'maf_evs_controls',
                            margin: '0 0 0 5',
                            labelWidth: '50%',
                            width: "50%"
                        }
                    ]
                },
                {
                    xtype: 'fieldcontainer',
                    layout: 'hbox',
                    border: false,
                    width: "100%",
                    items: [
                        {
                            xtype: 'tbtext', margin: '5 0 0 0', text: '<span class="emph">Spain MAF <</span>'
                        },
                        {
                            xtype: 'textfield',
                            name: 'maf_bier_controls',
                            margin: '0 0 0 5',
                            labelWidth: '50%',
                            width: "50%"
                        }
                    ]
                }
//
            ]
        });
    },
    _createCombobox: function (name, label, data, defaultValue, labelWidth, margin) {
        var _this = this;

        return Ext.create('Ext.form.field.ComboBox', {
            id: this.id + name,
            name: name,
            fieldLabel: label,
            store: data,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'value',
            value: data.getAt(defaultValue).get('value'),
            labelWidth: labelWidth,
            margin: margin,
            editable: false,
            allowBlank: false
        });
    },
    _createDynCombobox: function (name, label, data, defaultValue) {
        var _this = this;

        var dataAux = [];
        for (var key in data) {
            if (key != '.') {
                dataAux.push(key);
            }
        }

        return Ext.create('Ext.form.field.ComboBox', {
            name: name,
            emptyText: label,
            store: dataAux,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'value',
            multiSelect: true,
            delimiter: ",",
            editable: false,
            allowBlank: false,
            value: defaultValue
        });
    },
    _getEffect: function (record) {
        var _this = this;

        //var variants = [];

        //for (var i = 0; i < batch.length; i++) {
        //var record = batch[i];
        //variants.push(record.raw.chr + ":" + record.raw.pos + ":" + record.raw.studies[0].ref + ":" + record.raw.studies[0].alt[0]);
        //}

        var ct = [];
        var genes = [];

        //var url = "http://ws-beta.bioinfo.cipf.es/cellbase/rest/v3/hsapiens/genomic/variant/" + variants.join(",") + "/effect";
        //console.log(url);
        //return;
        var req = record.raw.chr + ":" + record.raw.pos + ":" + record.raw.studies[0].ref + ":" + record.raw.studies[0].alt[0];

        $.ajax({
            //url: url,
            url: "http://ws.bioinfo.cipf.es/cellbase/rest/latest/hsa/genomic/variant/" + req + "/consequence_type?of=json",
            dataType: 'json',
            async: false,
            success: function (response, textStatus, jqXHR) {
                if (response) { // {&& response.response && response.response.length > 0) {
                    for (var j = 0; j < response.length; j++) {
                        var elem = response[j];

                        if (elem.aaPosition != -1 && elem.transcriptId != "" && elem.aminoacidChange.length >= 3 && record.transcriptId === undefined && record.aaPos === undefined && record.aaChange === undefined) {
                            record.transcript = elem.transcriptId;
                            record.aaPos = elem.aaPosition;
                            record.aaChange = elem.aminoacidChange;
                        }
                        if (elem.consequenceTypeSoTerm != "") {
                            ct.push(elem.consequenceTypeSoTerm);
                        }
                        if (elem.geneName != "") {
                            genes.push(elem.geneName);
                        }


                    }

                    ct = ct.filter(function (elem, pos, self) {
                        return self.indexOf(elem) == pos;
                    });

                    genes = genes.filter(function (elem, pos, self) {
                        return self.indexOf(elem) == pos;
                    });

                    record.set("ct", ct.join(","));
                    record.set("genes", genes.join(","));

                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.log('Error loading Effect');
            }
        });
    },
    _getPolyphenSift: function (variant) {

        //console.log(variant);
        if (variant.aaPos != undefined && variant.aaPos >= 0) {
            var change = variant.aaChange.split("/")[1];
            var url = "http://ws-beta.bioinfo.cipf.es/cellbase/rest/v3/hsapiens/feature/transcript/" + variant.transcript + "/function_prediction?aaPosition=" + variant.aaPos + "&aaChange=" + change;
            //console.log(url);
            $.ajax({
                url: url,
                dataType: 'json',
                async: false,
                success: function (response, textStatus, jqXHR) {
                    var res = response.response[0];
                    if (res.numResults > 0) {
                        if (res.result[0].aaPositions[variant.aaPos]) {
                            res = res.result[0].aaPositions[variant.aaPos][change];
                            if (res !== undefined) {
                                if (res.ps != null) {
                                    variant.set("polyphen", res.ps);
                                }
                                if (res.ss != null) {
                                    variant.set("sift", res.ss);
                                }
                            }
                        }
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    console.log('Error loading PolyPhen/SIFT');
                }
            });
        }
    },
    _getPhenotypes: function (records) {

        var regs = [];
        for (var i = 0; i < records.length; i++) {

            var variant = records[i];

            var chr = variant.raw.chr;
            var pos = variant.raw.pos;
            regs.push(chr + ":" + pos + "-" + pos);

        }
        if (regs.length > 0) {
            var url = "http://ws-beta.bioinfo.cipf.es/cellbase/rest/v3/hsapiens/genomic/region/" + regs.join(",") + "/phenotype?include=phenotype";
            //console.log(url);
            $.ajax({
                url: url,
                dataType: 'json',
                async: false,
                success: function (response, textStatus, jqXHR) {

                    if (response != undefined && response.response.length > 0 && response.response.length == records.length) {
                        for (var i = 0; i < response.response.length; i++) {

                            var elem = response.response[i];
                            var phenotypes = [];
                            for (var k = 0; k < elem.numResults; k++) {

                                phenotypes.push(elem.result[k].phenotype);
                            }

                            records[i].set("phenotype", phenotypes.join(","));
                            //records[i].raw.phenotype= phenotypes.join(",");
                            records[i].commit();

                        }


                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    console.log('Error loading Phenotypes');
                }
            });
        }


    },
    _getDiseases: function (variant) {

//        console.log(variant);
        if (variant.aaPos != undefined && variant.aaPos >= 0) {
            var change = variant.aaChange.split("/")[1];
            var url = "http://ws-beta.bioinfo.cipf.es/cellbase/rest/v3/hsapiens/feature/transcript/" + variant.transcript + "/function_prediction?aaPosition=" + variant.aaPos + "&aaChange=" + change;
//            console.log(url);
            $.ajax({
                url: url,
                dataType: 'json',
                async: false,
                success: function (response, textStatus, jqXHR) {
                    var res = response.response[0];
                    if (res.numResults > 0) {
                        res = res.result[0].aaPositions[variant.aaPos][change];
                        if (res != null) {
                            variant.polyphen = res.ps
                            variant.sift = res.ss;
                            variant.set("polyphen", res.ps);
                            variant.set("sift", res.ss);
                        }
                    }
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    console.log('Error loading PolyPhen/SIFT');
                }
            });
        }
    },
    _checkGenes: function (genes) {

        var wrongGenes = [];
        var url = "http://ws-beta.bioinfo.cipf.es/cellbase/rest/v3/hsapiens/feature/gene/" + genes.join(",") + "/info?include=chromosome,start,end";

        $.ajax({
            url: url,
            dataType: 'json',
            async: false,
            success: function (response, textStatus, jqXHR) {
                if (response.response !== undefined && response.response.length > 0) {

                    for (var i = 0; i < response.response.length; i++) {
                        var geneObj = response.response[i];
                        if (geneObj.numResults == 0) {
                            wrongGenes.push(genes[i]);
                        }

                    }
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                console.log('Error loading Genes');
            }
        });
        return wrongGenes;

    }
}
;
