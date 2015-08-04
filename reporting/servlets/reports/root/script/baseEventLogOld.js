//Event Log class
Ext.define("Ung.GridEventLogBase", {
    extend: "Ext.grid.Panel",
    hasSelectors: null,
    hasAutoRefresh: null,
    reserveScrollbar: true,
    // refresh on activate Tab (each time the tab is clicked)
    refreshOnActivate: true,
    // for internal use
    rpc: null,
    helpSource: 'event_log',
    enableColumnHide: true,
    enableColumnMove: true,
    enableColumnMenu: true,
    verticalScrollerType: 'paginggridscroller',
    plugins: {
        ptype: 'bufferedrenderer',
        trailingBufferZone: 20,  // Keep 20 rows rendered in the table behind scroll
        leadingBufferZone: 50   // Keep 50 rows rendered in the table ahead of scroll
    },
    loadMask: true,
        startDate: null,
    endDate: null,
    stateful: true,
    // called when the component is initialized
    constructor: function(config) {
        this.subCmps = [];
        var modelName='Ung.GridEventLog.Store.ImplicitModel-' + Ext.id();
        Ext.define(modelName, {
            extend: 'Ext.data.Model',
            fields: config.fields
        });
        config.modelName = modelName;
        this.callParent(arguments);
    },
    beforeDestroy: function() {
        Ext.each(this.subCmps, Ext.destroy);
        this.callParent(arguments);
    },
    initComponent: function() {
        var me = this;
        this.rpc = {
            repository: {}
        };
        Ext.applyIf(this, {
            title: i18n._('Event Log'),
            name: 'EventLog',
            features:[],
            viewConfig: {}
        });
        this.stateId = 'eventLog-' +
            ( this.initialConfig.selectedApplication ?
              this.initialConfig.selectedApplication + '-' + this.initialConfig.sectionName :
              this.initialConfig.helpSource );
        this.viewConfig.enableTextSelection = true;
        this.store=Ext.create('Ext.data.Store', {
            model: this.modelName,
            data: [],
            buffered: false,
            proxy: {
                type: 'memory',
                reader: {
                    type: 'json',
                    root: 'list'
                }
            },
            autoLoad: false,
            remoteSort:false,
            remoteFilter: false
        });
        this.dockedItems = [{
            xtype: 'toolbar',
            dock: 'top',
            items: [i18n._('Filter:'), {
                xtype: 'textfield',
                name: 'searchField',
                hideLabel: true,
                width: 130,
                listeners: {
                    change: {
                        fn: function() {
                            this.filterFeature.updateGlobalFilter(this.searchField.getValue(), this.caseSensitive.getValue());
                        },
                        scope: this,
                        buffer: 600
                    }
                }
            }, {
                xtype: 'checkbox',
                name: 'caseSensitive',
                hideLabel: true,
                margin: '0 4px 0 4px',
                boxLabel: i18n._('Case sensitive'),
                handler: function() {
                    this.filterFeature.updateGlobalFilter(this.searchField.getValue(),this.caseSensitive.getValue());
                },
                scope: this
            }, {
                xtype: 'button',
                iconCls: 'icon-clear-filter',
                text: i18n._('Clear Filters'),
                tooltip: i18n._('Filters can be added by clicking on column headers arrow down menu and using Filters menu'),
                handler: Ext.bind(function () {
                    this.searchField.setValue("");
                    this.filters.clearFilters();
                }, this)
            }, {
                text: i18n._('Reset View'),
                tooltip: i18n._('Restore default columns positions, widths and visibility'),
                handler: Ext.bind(function () {
                    Ext.state.Manager.clear(this.stateId);
                    this.reconfigure(this.getStore(), this.initialConfig.columns);
                }, this)
            },'->',{
                xtype: 'button',
                id: "export_"+this.getId(),
                text: i18n._('Export'),
                name: "Export",
                tooltip: i18n._('Export Events to File'),
                iconCls: 'icon-export',
                handler: Ext.bind(this.exportHandler, this)
            }]
        }, {
            xtype: 'toolbar',
            dock: 'bottom',
            items: [{
                xtype: 'tbtext',
                hidden: !this.hasSelectors,
                id: "querySelector_"+this.getId(),
                text: ''
            }, {
                xtype: 'tbtext',
                hidden: !this.hasSelectors,
                id: "rackSelector_"+this.getId(),
                text: ''
            }, {
                xtype: 'tbtext',
                hidden: !this.hasSelectors,
                id: "limitSelector_"+this.getId(),
                text: ''
            }, {
                xtype: 'button',
                text: i18n._('From'),
                initialLabel:  i18n._('From'),
                hidden: !this.hasTimestampFilter,
                width: 132,
                tooltip: i18n._('Select Start date and time'),
                handler: function(button) {
                    me.startDateWindow.buttonObj=button;
                    me.startDateWindow.show();
                },
                scope: this
            },{
                xtype: 'tbtext',
                hidden: !this.hasTimestampFilter,
                text: '-'
            }, {
                xtype: 'button',
                text: i18n._('To'),
                initialLabel:  i18n._('To'),
                hidden: !this.hasTimestampFilter,
                width: 132,
                tooltip: i18n._('Select End date and time'),
                handler: function(button) {
                    me.endDateWindow.buttonObj=button;
                    me.endDateWindow.show();
                },
                scope: this
            },
            {
                xtype: 'button',
                id: "refresh_"+this.getId(),
                text: i18n._('Refresh'),
                name: "Refresh",
                tooltip: i18n._('Flush Events from Memory to Database and then Refresh'),
                iconCls: 'icon-refresh',
                handler:function () {
                    this.refreshHandler(true);
                },
                scope: this
            }, {
                xtype: 'button',
                hidden: !this.hasAutoRefresh,
                id: "auto_refresh_"+this.getId(),
                text: i18n._('Auto Refresh'),
                enableToggle: true,
                pressed: false,
                name: "Auto Refresh",
                tooltip: i18n._('Auto Refresh every 5 seconds'),
                iconCls: 'icon-autorefresh',
                handler: Ext.bind(function(button) {
                    if(button.pressed) {
                        this.startAutoRefresh();
                    } else {
                        this.stopAutoRefresh();
                    }
                }, this)
            }]
        }];

        for (var i in this.columns) {
            var col=this.columns[i];
            if (col.sortable === undefined) {
                col.sortable = true;
            }
            col.initialSortable = col.sortable;
            if (col.filter === undefined) {
                if (col.dataIndex != 'time_stamp') {
                    col.filter = { type: 'string' };
                } else {
                    col.filter = {
                        type: 'datetime',
                        dataIndex: 'time_stamp',
                        date: {
                            format: 'Y-m-d'
                        },
                        time: {
                            format: 'H:i:s A',
                            increment: 30
                        },
                        validateRecord : function (record) {
                            var me = this,
                            key,
                            pickerValue,
                            val1 = record.get(me.dataIndex);
                            var val = new Date(val1.time);
                            if(!Ext.isDate(val)){
                                return false;
                            }
                            val = val.getTime();
                            for (key in me.fields) {
                                if (me.fields[key].checked) {
                                    pickerValue = me.getFieldValue(key).getTime()-i18n.timeoffset;
                                    if (key == 'before' && pickerValue <= val) {
                                        return false;
                                    }
                                    if (key == 'after' && pickerValue >= val) {
                                        return false;
                                    }
                                    if (key == 'on' && (pickerValue-43200000 > val || val > pickerValue+43200000)) { //on piker value for day (selected time -/+12horus)
                                        return false;
                                    }
                                }
                            }
                            return true;
                        }
                    };
                }
            }
            if( col.stateId === undefined ){
                col.stateId = col.dataIndex;
            }
        }
        this.filterFeature=Ext.create('Ung.GlobalFiltersFeature', {});
        this.features.push(this.filterFeature);
        this.callParent(arguments);
        this.searchField=this.down('textfield[name=searchField]');
        this.caseSensitive = this.down('checkbox[name=caseSensitive]');
    },
    autoRefreshEnabled: false,
    startAutoRefresh: function(setButton) {
        this.autoRefreshEnabled=true;
        var columnModel=this.columns;
        this.getStore().sort(columnModel[0].dataIndex, "DESC");
        for (var i in columnModel) {
            columnModel[i].sortable = false;
            }
        if(setButton) {
            var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
            autoRefreshButton.toggle(true);
        }
        var refreshButton=Ext.getCmp("refresh_"+this.getId());
        refreshButton.disable();
        this.autoRefreshList();
    },
    stopAutoRefresh: function(setButton) {
        this.autoRefreshEnabled=false;
        var columnModel=this.columns;
        for (var i in columnModel) {
            columnModel[i].sortable = columnModel[i].initialSortable;
        }
        if(setButton) {
            var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
            autoRefreshButton.toggle(false);
        }
        var refreshButton=Ext.getCmp("refresh_"+this.getId());
        refreshButton.enable();
    },
    // return the list of columns in the event long as a comma separated list
    getColumnList: function() {
        var columnList = "";
        for (var i=0; i<this.fields.length ; i++) {
            if (i !== 0) {
                columnList += ",";
            }
            if (this.fields[i].mapping != null) {
                columnList += this.fields[i].mapping;
            } else if (this.fields[i].name != null) {
                columnList += this.fields[i].name;
            }
        }
        return columnList;
    },
    //Used to get dummy records in testing
    getTestRecord:function(index, fields) {
        var rec= {};
        var property;
        for (var i=0; i<fields.length ; i++) {
            property = (fields[i].mapping != null)?fields[i].mapping:fields[i].name;
            rec[property]=
                (property=='id')?index+1:
                (property=='time_stamp')?{javaClass:"java.util.Date", time: (new Date(Math.floor((Math.random()*index*12345678)))).getTime()}:
                (property.indexOf('_addr') != -1)?Math.floor((Math.random()*255))+"."+Math.floor((Math.random()*255))+"."+Math.floor((Math.random()*255))+"."+Math.floor((Math.random()*255))+"/"+Math.floor((Math.random()*32)):
                (property.indexOf('_port') != -1)?Math.floor((Math.random()*65000)):
            property+"_"+(i*index)+"_"+Math.floor((Math.random()*10));
        }
        return rec;
    },
    refreshNextChunkCallback: function(result, exception) {
        if(Ung.Util.handleException(exception)) return;

        var newEventEntries = result;

        /**
         * If we got results append them to the current events list
         * And make another call for more
         */
        if ( newEventEntries != null && newEventEntries.list != null && newEventEntries.list.length != 0 ) {
            this.eventEntries.push.apply( this.eventEntries, newEventEntries.list );
            this.setLoading(i18n._('Fetching Events...') + ' (' + this.eventEntries.length + ')');
            this.reader.getNextChunk(Ext.bind(this.refreshNextChunkCallback, this), 1000);
            return;
        }

        /**
         * If we got here, then we either reached the end of the resultSet or ran out of room
         * Display the results
         */
        if (this.settingsCmp !== null) {
            this.getStore().getProxy().data = this.eventEntries;
            this.getStore().loadPage(1);
        }
        this.setLoading(false);
    },
    // Refresh the events list
    refreshCallback: function(result, exception) {
        if(Ung.Util.handleException(exception)) return;

        this.eventEntries = [];

        if( testMode ) {
            var emptyRec={};
            var length = Math.floor((Math.random()*5000));
            for(var i=0; i<length; i++) {
                this.eventEntries.push(this.getTestRecord(i, this.fields));
            }
            this.refreshNextChunkCallback(null);
        }

        this.reader = result;
        if(this.reader) {
            this.setLoading(i18n._('Fetching Events...'));
            this.reader.getNextChunk(Ext.bind(this.refreshNextChunkCallback, this), 1000);
        } else {
            this.refreshNextChunkCallback(null);
        }
    },
    listeners: {
        "activate": {
            fn: function() {
                if( this.refreshOnActivate ) {
                    Ext.Function.defer(this.refreshHandler,1, this, [false]);
                }
            }
        },
        "deactivate": {
            fn: function() {
                if(this.autoRefreshEnabled) {
                    this.stopAutoRefresh(true);
                }
            }
        }
    },
    isDirty: function() {
        return false;
    }
});

Ext.define("Ung.GlobalFiltersFeature", {
    extend: "Ext.ux.grid.FiltersFeature",
    encode: false,
    local: true,
    init: function (grid) {
        Ext.applyIf(this,{
            globalFilter: {
                value: "",
                caseSensitive: false
                }
        });
        this.callParent(arguments);
    },
    getRecordFilter: function() {
        var me = this;
        var globalFilterFn = this.globalFilterFn;
        var parentFn = Ext.ux.grid.FiltersFeature.prototype.getRecordFilter.call(this);
        return function(record) {
            return parentFn.call(me, record) && globalFilterFn.call(me, record);
        };
    },
    updateGlobalFilter: function(value, caseSensitive) {
        if(caseSensitive !== null) {
            this.globalFilter.caseSensitive=caseSensitive;
        }
        if(!this.globalFilter.caseSensitive) {
            value=value.toLowerCase();
        }
        this.globalFilter.value = value;
            this.reload();
    },
    globalFilterFn: function(record) {
        //TODO: 1) support regular exppressions
        //2) provide option to search in displayed columns only
        var inputValue = this.globalFilter.value,
        caseSensitive = this.globalFilter.caseSensitive;
        if(inputValue.length === 0) {
            return true;
        }
        var fields = record.fields.items,
        fLen   = record.fields.length,
        f, val;

        for (f = 0; f < fLen; f++) {
            val = record.get(fields[f].name);
            if(val == null) {
                continue;
            }
            if(typeof val == 'boolean' || typeof val == 'number') {
                val=val.toString();
            } else if(typeof val == 'object') {
                if(val.time != null) {
                    val = i18n.timestampFormat(val);
                }
            }
            if(typeof val == 'string') {
                if(caseSensitive) {
                    if(val.indexOf(inputValue) > -1) {
                        return true;
                    }
                } else {
                    if(val.toLowerCase().indexOf(inputValue) > -1) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
});