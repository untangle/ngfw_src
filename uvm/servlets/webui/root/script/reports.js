Ext.define('Ung.panel.Reports', {
    extend: 'Ext.panel.Panel',
    settingsCmp: null,

    initComponent: function() {
        if(!this.title) {
            this.title = i18n._('Reports');
        }
        if (!this.isReportsAppInstalled()) {
            this.items = [{
                xtype: 'component',
                html: i18n._("Event Logs require the Reports application. Please install and enable the Reports application.")
            }];
            this.callParent(arguments);
            return;
        }
        var reportEntries = this.getReportingManagerNew().getReportEntries().list;
        var reportsList = [];
        for(var i=0; i<reportEntries.length; i++) {
            reportsList.push([i, reportEntries[i].title]);
        }
        this.items = [{
            xtype: 'combo',
            margin: 10,
            fieldLabel: i18n._("Select Report"),
            width: 500,
            store: reportsList,
            queryMode: 'local',
            editable: false,
            listeners: {
                "change": {
                    fn: Ext.bind(function(elem, newValue) {
                        this.loadReport(reportEntries[newValue]);
                    }, this)
                }
            }
        }, {
            xtype: 'container',
            name:'chartContainer',
            html: ""
        }];
        this.callParent(arguments);
        this.chartContainer = this.down("container[name=chartContainer]");
    },
    loadReport: function(rep) {
        this.chartContainer.removeAll();
        var data = this.getReportingManagerNew().getDataForReportEntry(rep, null, null, -1).list;
        console.log(rep);
        console.log(data);
        var chart = {xtype: 'component', html: ""};
        if(rep.type == 'PIE_GRAPH') {
            var dataStore = Ext.create('Ext.data.JsonStore', {
                fields: [rep.pieGroupColumn, 'value' ],
                data: data
            }); 

            chart = {
                xtype: 'polar',
                theme: 'default-gradients',
                border: false,
                width: '100%',
                height: 500,
                store: dataStore,
                insetPadding: 50,
                innerPadding: 20,
                legend: {
                    docked: 'bottom'
                },
                interactions: ['rotate', 'itemhighlight'],
                sprites: [{
                    type: 'text',
                    text: rep.title,
                    fontSize: 22,
                    width: 100,
                    height: 30,
                    x: 40, // the sprite x position
                    y: 20  // the sprite y position
                }, {
                    type: 'text',
                    text: rep.description,
                    x: 12,
                    y: 425
                }],
                series: [{
                    type: 'pie',
                    angleField: 'value',
                    label: {
                        field: rep.pieGroupColumn,
                        calloutLine: {
                            length: 60,
                            width: 3
                            // specifying 'color' is also possible here
                        }
                    },
                    highlight: true,
                    tooltip: {
                        trackMouse: true,
                        renderer: function(storeItem, item) {
                            this.setHtml(storeItem.get(rep.pieGroupColumn) + ": "+ storeItem.get('value') +" Hits");
                        }
                    }
                }]
            };
        }
        this.chartContainer.add(chart); 
    },
    isDirty: function() {
        return false;
    },
    getReportingManagerNew: function(forceReload) {
        if (forceReload || rpc.reportingManagerNew === undefined) {
            try {
                rpc.reportingManagerNew = this.getNodeReporting().getReportingManagerNew();
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return rpc.reportingManagerNew;
    },
    
    // get node reporting
    getNodeReporting: function(forceReload) {
        if (forceReload || this.nodeReporting === undefined) {
            try {
                this.nodeReporting = rpc.nodeManager.node("untangle-node-reporting");
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.nodeReporting;
    },
    // is reports node installed
    isReportsAppInstalled: function(forceReload) {
        if (forceReload || this.reportsAppInstalledAndEnabled === undefined) {
            try {
                if (!this.getNodeReporting()) {
                    this.reportsAppInstalledAndEnabled = false;
                } else {
                    if (this.nodeReporting.getRunState() == "RUNNING"){
                        this.reportsAppInstalledAndEnabled = true;
                    } else {
                        this.reportsAppInstalledAndEnabled = false;
                    }
                }
            } catch (e) {
                Ung.Util.rpcExHandler(e);
            }
        }
        return this.reportsAppInstalledAndEnabled;
    }
});