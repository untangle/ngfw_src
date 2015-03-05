// Monitor Grid class
Ext.define('Ung.MonitorGrid', {
    extend:'Ext.grid.Panel',
    selType: 'rowmodel',
    // settings component
    settingsCmp: null,
    // the default sort field
    sortField: null,
    // the default sort order
    sortOrder: null,
    // the default group field
    groupField: null,
    // the columns are sortable by default, if sortable is not specified
    columnsDefaultSortable: true,
    async: true,
    //an applicaiton selector
    appList: null,
    // the total number of records
    totalRecords: null,
    autoRefreshEnabled: false,
    stateful: true,
    plugins: {
        ptype: 'bufferedrenderer',
        trailingBufferZone: 20,  // Keep 20 rows rendered in the table behind scroll
        leadingBufferZone: 50   // Keep 50 rows rendered in the table ahead of scroll
    },
    features: [{
        ftype: 'filters',
        encode: false,
        local: true
    }, {
        ftype: 'groupingsummary'
    }],
    constructor: function(config) {
        var defaults = {
            data: [],
            viewConfig: {
                enableTextSelection: true,
                stripeRows: true,
                loadMask:{
                    msg: i18n._("Loading...")
                }
            },
            changedData: {},
            subCmps:[],
            stateId: "monitorGrid-"+config.name
        };
        Ext.applyIf(config, defaults);
        this.callParent(arguments);
    },
    initComponent: function() {
        for (var i = 0; i < this.columns.length; i++) {
            var col=this.columns[i];
            if( col.sortable == null) {
                col.sortable = this.columnsDefaultSortable;
            }
            if( col.stateId === undefined ){
                col.stateId=col.dataIndex;
            }
        }
        if(this.dataFn) {
            if(this.dataRoot === undefined) {
                this.dataRoot="list";
            }
        } else {
            this.async=false;
        }
        this.store=Ext.create('Ext.data.Store', {
            data: [],
            fields: this.fields,
            buffered: false,
            proxy: {
                type: 'memory',
                reader: {
                    type: 'json'
                }
            },
            autoLoad: false,
            sorters: this.sortField ? {
                property: this.sortField,
                direction: this.sortOrder ? this.sortOrder: "ASC"
            }: null,
            groupField: this.groupField,
            remoteSort:false,
            remoteFilter: false
        });
        this.bbar=[];
        if(this.appList!=null) {
            this.bbar.push({
                xtype: 'tbtext',
                id: "appSelectorBox_"+this.getId(),
                text: ''
            });
        }
        this.bbar.push({
            xtype: 'button',
            id: "refresh_"+this.getId(),
            text: i18n._('Refresh'),
            name: "Refresh",
            tooltip: i18n._('Refresh'),
            iconCls: 'icon-refresh',
            handler: Ext.bind(function() {
                this.reload();
            }, this)
        },{
            xtype: 'button',
            id: "auto_refresh_"+this.getId(),
            text: i18n._('Auto Refresh'),
            enableToggle: true,
            pressed: false,
            name: "Auto Refresh",
            tooltip: i18n._('Auto Refresh'),
            iconCls: 'icon-autorefresh',
            handler: Ext.bind(function() {
                var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
                if(autoRefreshButton.pressed) {
                    this.startAutoRefresh();
                } else {
                    this.stopAutoRefresh();
                }
            }, this)
        }, '-', i18n._('Filter:'), {
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
            boxLabel: i18n._('Case sensitive'),
            margin: '0 4px 0 4px',
            handler: function() {
                this.filterFeature.updateGlobalFilter(this.searchField.getValue(),this.caseSensitive.getValue());
            },
            scope: this
        }, '-', {
            text: i18n._('Clear Filters'),
            tooltip: i18n._('Filters can be added by clicking on column headers arrow down menu and using Filters menu'),
            handler: Ext.bind(function () {
                this.filters.clearFilters();
            }, this)
        },{
                text: i18n._('Clear Grouping'),
            tooltip: i18n._('Grouping can be used by clicking on column headers arrow down menu and clicking Group by this field'),
            handler: Ext.bind(function () {
                this.getStore().clearGrouping();
            }, this)
        },{
            text: i18n._('Reset View'),
            tooltip: i18n._('Restore default columns positions, widths and visibility'),
            handler: Ext.bind(function () {
                Ext.state.Manager.clear(this.stateId);
                this.reconfigure(this.getStore(), this.initialConfig.columns);
            }, this)
        });
        /*TODO: ext5
        this.filterFeature=Ext.create('Ung.GlobalFiltersFeature', {});
        this.features.push(this.filterFeature);
        */
        this.callParent(arguments);
        this.searchField=this.down('textfield[name=searchField]');
        this.caseSensitive = this.down('checkbox[name=caseSensitive]');
    },
    afterRender: function() {
        this.callParent(arguments);
        if(this.appList!=null) {
            out = [];
            out.push('<select name="appSelector" id="appSelector_' + this.getId() + '" onchange="Ext.getCmp(\''+this.getId()+'\').changeApp()">');
            for (i = 0; i < this.appList.length; i++) {
                var app = this.appList[i];
                var selOpt = (app.value === this.dataFnArg) ? "selected": "";
                out.push('<option value="' + app.value + '" ' + selOpt + '>' + app.name + '</option>');
            }
            out.push('</select>');
            Ext.getCmp('appSelectorBox_' + this.getId()).setText(out.join(""));
        }
        this.initialLoad();
    },
    setSelectedApp: function(dataFnArg) {
        this.dataFnArg=dataFnArg;
        var selObj = document.getElementById('appSelector_' + this.getId());
        for(var i=0; i< selObj.options.length; i++) {
            if(selObj.options[i].value==dataFnArg) {
                selObj.selectedIndex=i;
                this.reload();
                return;
            }
        }
    },
    getSelectedApp: function() {
        var selObj = document.getElementById('appSelector_' + this.getId());
        var result = null;
        if (selObj !== null && selObj.selectedIndex >= 0) {
            result = selObj.options[selObj.selectedIndex].value;
        }
        return result;
    },
    changeApp: function() {
        this.dataFnArg=this.getSelectedApp();
        this.reload();
    },
    initialLoad: function() {
        this.getView().setLoading(true);
        this.getData({list:[]}); //Inital load with empty data
        this.afterDataBuild(Ext.bind(function() {
            this.getStore().loadPage(1, {
                callback: function() {
                    this.getView().setLoading(false);
                },
                scope: this
            });
        }, this));
    },
    getData: function(data) {
        if(!data) {
            if(this.dataFn) {
                if (this.dataFnArg !== undefined && this.dataFnArg != null) {
                    data = this.dataFn(this.dataFnArg);
                } else {
                    data = this.dataFn();
                }
                this.data = (this.dataRoot!=null && this.dataRoot.length>0) ? data[this.dataRoot]:data;
            }
        } else {
            this.data=(this.dataRoot!=null && this.dataRoot.length>0) ? data[this.dataRoot]:data;
        }

        if(!this.data) {
            this.data=[];
        }
        return this.data;
    },
    buildData: function(handler) {
        if(this.async) {
            if (this.dataFnArg !== undefined && this.dataFnArg != null) {
                this.dataFn(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.getData(result);
                    this.afterDataBuild(handler);
                }, this),this.dataFnArg);
            } else {
                this.dataFn(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.getData(result);
                    this.afterDataBuild(handler);
                }, this));
            }
        } else {
            this.getData();
            this.afterDataBuild(handler);
        }

    },
    afterDataBuild: function(handler) {
        this.getStore().getProxy().data = this.data;
        if(handler) {
            handler();
        }
    },
    beforeDestroy: function() {
        Ext.destroy(this.subCmps);
        this.callParent(arguments);
    },
    reload: function() {
        this.getView().setLoading(true);
        Ext.defer(function(){
            this.buildData(Ext.bind(function() {
                this.getStore().loadPage(1, {
                    callback: function() {
                        this.getView().setLoading(false);
                    },
                    scope: this
                });
            }, this));
        },10, this);
    },
    startAutoRefresh: function(setButton) {
        this.autoRefreshEnabled=true;
        if(setButton) {
            var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
            autoRefreshButton.toggle(true);
        }
        var refreshButton=Ext.getCmp("refresh_"+this.getId());
        refreshButton.disable();
        this.autorefreshList();

    },
    stopAutoRefresh: function(setButton) {
        this.autoRefreshEnabled=false;
        if(setButton) {
            var autoRefreshButton=Ext.getCmp("auto_refresh_"+this.getId());
            autoRefreshButton.toggle(false);
        }
        var refreshButton=Ext.getCmp("refresh_"+this.getId());
        refreshButton.enable();
    },
    autorefreshList: function() {
        if(this!=null && this.autoRefreshEnabled && Ext.getCmp(this.id) != null) {
            this.reload();
            Ext.defer(this.autorefreshList, 9000, this);
        }
    },
    isDirty: function() {
        return false;
    }
});