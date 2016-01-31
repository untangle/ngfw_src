Ext.define('Webui.config.dashboardManager', {
    extend: 'Ung.ConfigWin',
    name: 'dashboardManager',
    helpSource: 'dashboard_manager',
    displayName: 'Dashboard Manager',
    initComponent: function() {
        this.breadcrumbs = [{
            title: i18n._('Dashboard Manager')
        }];
        this.widgetsConfig = [{
            name: 'Information',
            title: i18n._('Information'),
            displayMode: 'small',
            singleInstance: true
        },{
            name: 'Server',
            title: i18n._('Server'),
            displayMode: 'small',
            singleInstance: true
        },{
            name: 'Sessions',
            title: i18n._('Sessions'),
            displayMode: 'small',
            singleInstance: true
        },{
            name: 'HostsDevices',
            title: i18n._('Hosts & Devices'),
            displayMode: 'small',
            singleInstance: true
        },{
            name: 'Hardware',
            title: i18n._('Hardware'),
            displayMode: 'small',
            singleInstance: true
        },{
            name: 'Memory',
            title: i18n._('Memory'),
            displayMode: 'small',
            singleInstance: true
        },{
            name: 'Network',
            title: i18n._('Network'),
            displayMode: 'small',
            singleInstance: true
        },{
            name: 'CPULoad',
            title: i18n._('CPU Load'),
            displayMode: 'small',
            singleInstance: true
        },{
            name: 'ReportEntry',
            title: i18n._('Report'),
            displayMode: 'big',
            hasRefreshInterval: true
        },{
            name: 'EventEntry',
            title: i18n._('Events'),
            displayMode: 'big',
            hasRefreshInterval: true
        }];
        this.widgetsMap = Ung.Util.createRecordsMap(this.widgetsConfig, "name");
        this.buildGridDashboardWidgets();
        this.buildTabPanel([this.gridDashboardWidgets]);
        this.callParent(arguments);
    },
    needDashboardReload: false,
    closeWindow: function() {
        this.hide();
        Ext.destroy(this);
        if(this.needDashboardReload) {
            Ung.dashboard.loadDashboard();
        }
    },
    getDashboardWidgets: function(handler) {
        if (!this.isVisible()) {
            return;
        }
        rpc.dashboardManager.getSettings(Ext.bind(function(result, exception) {
            var widgets = [];
            if(result && result.widgets && result.widgets.list) {
                widgets = result.widgets.list;
            }
            this.dashboardSettings = result;
            handler({javaClass:"java.util.LinkedList", list: widgets}, exception);
        }, this));
    },
    buildEntrySelector: function() {
        this.entrySelector = Ext.create("Ext.container",{
            dataIndex: "entryId",
            getValue: function() {
                return "111";
                
            },
            setValue: function(value) {
                this.entryId = value;
            },
            layout: "border",
            height: 500,
            width: '100%',
            items: [{
                xtype : 'treepanel',
                region : 'west',
                autoScroll : true,
                rootVisible : false,
                title : i18n._('Reports'),
                enableDrag : false,
                width : 220,
                minWidth : 65,
                maxWidth : 350,
                split : true,
                store : Ext.create('Ext.data.TreeStore', {
                    root : {
                        expanded : true,
                        children : []
                    }
                }),
                selModel : {
                    selType : 'rowmodel',
                    listeners : {
                        select : Ext.bind(function(rowModel, record, rowIndex, eOpts) {
                            var category = record.get("category");
                        }, this)
                    }
                }
            }, {
                name: 'entriesGrid',
                xtype: 'grid',
                header: false,
                border: false,
                margin: '0 0 10 0',
                hideHeaders: true,
                store:  Ext.create('Ext.data.Store', {
                    fields: ["category", "title", "description"],
                    data: []
                }),
                reserveScrollbar: true,
                columns: [{
                    dataIndex: 'title',
                    flex: 1,
                    renderer: function( value, metaData, record, rowIdx, colIdx, store ) {
                        var description = record.get("description");
                        if(description) {
                            metaData.tdAttr = 'data-qtip="' + Ext.String.htmlEncode( i18n._(description) ) + '"';
                        }
                        return i18n._(value);
                    }
                }],
                selModel: {
                    selType: 'rowmodel',
                    listeners: {
                        select: Ext.bind(function( rowModel, record, rowIndex, eOpts ) {
                        }, this)
                    }
                }
            }]
        });
    },
    // Dashboard Widgets Grid
    buildGridDashboardWidgets: function(columns, groupField) {
        var me = this;
        this.gridDashboardWidgets = Ext.create('Ung.grid.Panel',{
            helpSource: 'dashboard_manager_dashboard_widgets',
            settingsCmp: this,
            hasReorder: true,
            addAtTop: false,
            title: i18n._("Dashboard Widgets"),
            dataFn: Ext.bind(this.getDashboardWidgets, this),
            recordJavaClass: "com.untangle.uvm.DashboardWidgetSettings",
            emptyRow: {
                "type": "",
                "refreshIntervalSec": 0
            },
            fields: [{
                name: "type",
                type: 'string'
            },{
                name: "refreshIntervalSec"
            }],
            columns: [{
                header: i18n._("Widget Type"),
                dataIndex: "type",
                width: 120,
                renderer: Ext.bind(function(value) {
                    var widgetConfig = this.widgetsMap[value];
                    return widgetConfig? widgetConfig.title: value;
                }, this)
            }, {
                header: i18n._("Size"),
                dataIndex: "type",
                renderer: Ext.bind(function(value, metaData, record) {
                    var widgetConfig = this.widgetsMap[record.get("type")];
                    return widgetConfig? widgetConfig.displayMode: "";
                }, this)
            }, {
                header: i18n._("Details"),
                dataIndex: "type",
                width: 200,
                flex: 1,
                renderer: Ext.bind(function(value, metaData, record) {
                    if((value == "ReportEntry" || value == "EventEntry") && rpc.reportsEnabled && Ung.dashboard.reportsMap && Ung.dashboard.eventsMap) {
                        var entryId = record.get("entryId");
                        var entry = (value == "ReportEntry") ? Ung.dashboard.reportsMap[entryId] : Ung.dashboard.eventsMap[entryId];
                        if(entry) {
                            return "<b>"+entry.category+"</b> "+entry.title;
                        }
                    }
                    return "";
                }, this)
            }]
        });
        this.entrySelector = {
                xtype: 'container',
                dataIndex: 'entryId',
                getValue: function() {
                    if(this.currentType=="ReportEntry") {
                        return this.down("[name=reportEntryId]").getValue();
                    } else if(this.currentType=="EventEntry") {
                        return this.down("[name=eventEntryId]").getValue();
                    } else {
                        return null;
                    }
                },
                setValue: function(value) {
                    if(this.currentType=="ReportEntry") {
                        this.down("[name=reportEntryId]").setValue(value);
                    } else if(this.currentType=="EventEntry") {
                        this.down("[name=eventEntryId]").setValue(value);
                    }
                    
                },
                setType: function(type) {
                    this.currentType = type;
                    var reportEntryId = this.down("[name=reportEntryId]");
                    var eventEntryId = this.down("[name=eventEntryId]");
                    reportEntryId.setVisible(this.currentType=="ReportEntry");
                    reportEntryId.setDisabled(this.currentType!="ReportEntry");
                    if(this.currentType!="ReportEntry") {
                        reportEntryId.setValue("");
                    }
                    
                    eventEntryId.setVisible(this.currentType=="EventEntry");
                    eventEntryId.setDisabled(this.currentType!="EventEntry");
                    if(this.currentType!="EventEntry") {
                        eventEntryId.setValue("");
                    }
                }
        };
        if(!rpc.reportsEnabled) {
            this.entrySelector.defaults = {
                xtype:'displayfield',
                labelWidth: 150,
                width: 500
            };
            this.entrySelector.items = [{
                name: 'reportEntryId',
                fieldLabel: i18n._("Report Id")
            }, {
                name: 'eventEntryId',
                fieldLabel: i18n._("Events Id")
            }];
        } else {
            //this.buildEntrySelector();
            this.entrySelector.defaults = {
                xtype: 'combo',
                allowBlank: false,
                forceSelection: true,
                anyMatch: true,
                valueField: "uniqueId",
                displayField: "category_title",
                queryMode: 'local',
                labelWidth: 150,
                width: 500,
                listConfig: {
                    minWidth: 500
                },
                tpl: Ext.create('Ext.XTemplate',
                    '<ul class="x-list-plain"><tpl for=".">',
                        '<li role="option" class="x-boundlist-item"><b>{category}</b> - {title}</li>',
                    '</tpl></ul>'
                )
            };
            this.entrySelector.items = [{
                name: 'reportEntryId',
                fieldLabel: i18n._("Select Report"),
                emptyText: i18n._("[enter report]"),
                store: Ext.create('Ext.data.JsonStore', {
                    fields: ["uniqueId", "category", "title", {name: 'category_title', calculate: function(data) {
                        return data.category + " - " + data.title;
                    }}],
                    data: Ung.dashboard.reportEntries,
                    sorters: ['category', 'title']
                })
            }, {
                name: 'eventEntryId',
                fieldLabel: i18n._("Select Events"),
                emptyText: i18n._("[enter events]"),
                store: Ext.create('Ext.data.JsonStore', {
                    fields: ["uniqueId", "category", "title", {name: 'category_title', calculate: function(data) {
                        return data.category + " - " + data.title;
                    }}],
                    data: Ung.dashboard.eventEntries,
                    sorters: ['category', 'title']
                })
            }];
        }
        this.gridDashboardWidgets.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            rowEditorLabelWidth: 150,
            inputLines: [{
                xtype: "combo",
                dataIndex: "type",
                margin: '0 0 10 0',
                allowBlank: false,
                fieldLabel: i18n._("Widget Type"),
                editable: false,
                store: [],
                queryMode: 'local',
                listeners: {
                    'select': {
                        fn: Ext.bind(function(combo, ewVal, oldVal) {
                            this.gridDashboardWidgets.rowEditor.syncComponents();
                        }, this )
                    }
                }
            }, {
                xtype: "container",
                dataIndex: "refreshIntervalSec",
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'numberfield',
                    name: "refreshIntervalSec",
                    labelWidth: 150,
                    fieldLabel: i18n._( "Refresh Interval" )
                }, {
                    xtype: 'label',
                    html: i18n._( "(seconds)")+" - "+i18n._( "Enter 0 for no Auto Refresh" ),
                    cls: 'boxlabel'
                }],
                setValue: function(value) {
                    this.down('numberfield[name="refreshIntervalSec"]').setValue(value);
                },
                getValue: function() {
                    return this.down('numberfield[name="refreshIntervalSec"]').getValue();
                },
                setReadOnly: function(val) {
                    this.down('numberfield[name="refreshIntervalSec"]').setReadOnly(val);
                }
            }, this.entrySelector],
            populate: function(record, addMode) {
                //do not show already existing widgets that allow single instances
                var typeCombo = this.down("combo[dataIndex=type]");
                var entryId = this.down('[dataIndex=entryId]');
                var currentType = record.get("type");
                entryId.setType(currentType);
                var gridList = this.grid.getList();
                var existingTypesMap = Ung.Util.createRecordsMap(gridList, "type");
                var availableTypes = [], widget;
                for(var i=0; i < me.widgetsConfig.length; i++) {
                    widget = me.widgetsConfig[i];
                    if(!rpc.reportsEnabled && addMode && (widget.name == "ReportEntry" || widget.name == "EventEntry")) {
                        continue;
                    }
                    if(currentType == widget.name || !(widget.singleInstance && existingTypesMap[widget.name])) {
                        availableTypes.push([widget.name, widget.title]);
                    }
                }
                typeCombo.setStore(availableTypes);
                Ung.RowEditorWindow.prototype.populate.apply(this, arguments);
            },
            syncComponents: function () {
                if(!this.cmps) {
                    this.cmps = {
                        typeCmp: this.down('combo[dataIndex=type]'),
                        refreshIntervalSec: this.down('[dataIndex=refreshIntervalSec]'),
                        entryId: this.down('[dataIndex=entryId]')
                    };
                }
                var type = this.cmps.typeCmp.getValue();
                var widgetConfig = me.widgetsMap[type] || {};

                this.cmps.refreshIntervalSec.setVisible(widgetConfig.hasRefreshInterval);
                this.cmps.refreshIntervalSec.setDisabled(!widgetConfig.hasRefreshInterval);
                this.cmps.entryId.setType(type);
                this.cmps.entryId.setVisible( type == "ReportEntry" || type == "EventEntry" );
                this.cmps.entryId.setDisabled( type != "ReportEntry" && type != "EventEntry" );
            }
        }));
    },
    save: function(isApply) {
        Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
        this.needDashboardReload = true;
        var widgets = this.gridDashboardWidgets.getList();
        this.dashboardSettings.widgets= { javaClass:"java.util.LinkedList", list: widgets };
        rpc.dashboardManager.setSettings(Ext.bind(function(result, exception) {
            Ext.MessageBox.hide();
            if(Ung.Util.handleException(exception)) return;
            if (!isApply) {
                this.closeWindow();
            } else {
                this.clearDirty();
            }
        }, this), this.dashboardSettings);

    }
});

//# sourceURL=dashboardManager.js