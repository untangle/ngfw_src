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
    closeWindow: function() {
        this.hide();
        Ext.destroy(this);
        Ung.dashboard.loadDashboard();
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
                    if((value == "ReportEntry" || value == "EventEntry") && Ung.dashboard.reportsEnabled && Ung.dashboard.reportsMap && Ung.dashboard.eventsMap) {
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
        var entrySelector;
        if(!Ung.dashboard.reportsEnabled) {
            entrySelector = {
                xtype:'displayfield',
                dataIndex: "entryId",
                width: 500,
                fieldLabel: i18n._("Entry Id")
            };
        } else {
            entrySelector = {
                xtype:'textfield',
                dataIndex: "entryId",
                width: 500,
                fieldLabel: i18n._("Entry Id")
            };
        }
        this.gridDashboardWidgets.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            sizeToParent: true,
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
            }, entrySelector],
            populate: function(record, addMode) {
                //do not show already existing widgets that allow single instances
                var typeCombo = this.down("combo[dataIndex=type]");
                var currentType = record.get("type");
                var gridList = this.grid.getList();
                var existingTypesMap = Ung.Util.createRecordsMap(gridList, "type");
                var availableTypes = [], widget;
                for(var i=0; i < me.widgetsConfig.length; i++) {
                    widget = me.widgetsConfig[i];
                    if(addMode && (widget.name == "ReportEntry" || widget.name == "EventEntry")) {
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

                this.cmps.entryId.setVisible( type == "ReportEntry" || type == "EventEntry" );
                this.cmps.entryId.setDisabled( type != "ReportEntry" && type != "EventEntry" );
            }
        }));
    },
    save: function(isApply) {
        Ext.MessageBox.wait(i18n._("Saving..."), i18n._("Please wait"));
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