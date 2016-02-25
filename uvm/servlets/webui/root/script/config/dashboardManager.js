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
//         },{
//             name: 'InterfaceLoad',
//             title: i18n._('Interface Load'),
//             displayMode: 'small'
        },{
            name: 'NetworkLayout',
            title: i18n._('Network Layout'),
            displayMode: 'big',
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
        this.buildPanelDashboardWidgets();
        this.buildTabPanel([this.panelDashboardWidgets]);
        this.callParent(arguments);
    },
    needDashboardReload: false,
    closeWindow: function() {
        if(this.needDashboardReload) {
            Ung.dashboard.loadDashboard();
        }
        this.hide();
        Ext.destroy(this);
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
    resetSettingsToDefault: function() {
        Ext.MessageBox.confirm(i18n._("Warning"),
                i18n._("This will overwrite the current dashboard settings with the defaults.") + "<br/><br/>" +
                i18n._("Do you want to continue?"),
                Ext.bind(function(btn, text) {
                    if (btn == 'yes') {
                        rpc.dashboardManager.resetSettingsToDefault(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            this.needDashboardReload = true;
                            this.gridDashboardWidgets.reload();
                        }, this));
                    }
                }, this));
        
    },
    // Dashboard Widgets Panel
    buildPanelDashboardWidgets: function() {
        var me = this;
        this.gridDashboardWidgets = Ext.create('Ung.grid.Panel',{
            settingsCmp: this,
            hasReorder: true,
            addAtTop: false,
            header: false,
            flex: 1,
            bbar: [{
                xtype: 'button',
                text: i18n._('Reset dashboard to defaults'),
                iconCls: "reboot-icon",
                handler: Ext.bind(function() {
                    this.resetSettingsToDefault();
                }, this)
            }],
            dataFn: Ext.bind(this.getDashboardWidgets, this),
            recordJavaClass: "com.untangle.uvm.DashboardWidgetSettings",
            emptyRow: {
                enabled: true,
                type: ""
            },
            fields: [{
                name: "type",
                type: 'string'
            },{
                name: "refreshIntervalSec"
            }],
            columns: [{
                xtype:'checkcolumn',
                header: i18n._("Enabled"),
                dataIndex: 'enabled',
                resizable: false,
                width: 70
            }, {
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
                    if(value == "ReportEntry" || value == "EventEntry") {
                        return "<b>"+((value == "ReportEntry")?i18n._("Report Id"):i18n._("Events Id"))+":</b> " + record.get("entryId");
                    }
                    if(value == "InterfaceLoad") {
                        var interfaceVale = Ung.dashboard.Util.getInterfaceMap()[record.get("entryId")] || record.get("entryId");
                        return "<b>"+i18n._("Interface Load")+":</b> " + interfaceVale;
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
                    } else if(this.currentType == "InterfaceLoad") {
                        return this.down("[name=interfaceLoadInterfaceId]").getValue();
                    } else {
                        return null;
                    }
                },
                setValue: function(value) {
                    if(this.currentType=="ReportEntry") {
                        this.down("[name=reportEntryId]").setValue(value);
                    } else if(this.currentType=="EventEntry") {
                        this.down("[name=eventEntryId]").setValue(value);
                    } else if(this.currentType == "InterfaceLoad") {
                        this.down("[name=interfaceLoadInterfaceId]").setValue(value);
                    }
                },
                setType: function(type) {
                    this.currentType = type;
                    var reportEntryId = this.down("[name=reportEntryId]");
                    var eventEntryId = this.down("[name=eventEntryId]");
                    var interfaceLoadInterfaceId = this.down("[name=interfaceLoadInterfaceId]");
                    reportEntryId.setVisible(this.currentType=="ReportEntry");
                    reportEntryId.setDisabled(this.currentType!="ReportEntry");
                    if(rpc.reportsEnabled && this.currentType!="ReportEntry") {
                        reportEntryId.setValue("");
                    }

                    eventEntryId.setVisible(this.currentType=="EventEntry");
                    eventEntryId.setDisabled(this.currentType!="EventEntry");
                    if(rpc.reportsEnabled && this.currentType!="EventEntry") {
                        eventEntryId.setValue("");
                    }

                    interfaceLoadInterfaceId.setVisible(this.currentType=="InterfaceLoad");
                    interfaceLoadInterfaceId.setDisabled(this.currentType!="InterfaceLoad");
                    if(rpc.reportsEnabled && this.currentType!="InterfaceLoad") {
                        interfaceLoadInterfaceId.setValue("");
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
                ),
                listeners: {
                    'select': {
                        fn: Ext.bind(function(combo, newVal, oldVal) {
                            var rowEditor = this.gridDashboardWidgets.rowEditor;
                            rowEditor.syncComponents();
                            if(rpc.reportsEnabled) {
                                var type = rowEditor.cmps.typeCmp.getValue();
                                if(type=="EventEntry") {
                                    var entryId = rowEditor.cmps.entryId.getValue();
                                    var entry = Ung.dashboard.eventsMap[entryId];
                                    if(entry) {
                                        rowEditor.cmps.displayColumns.setValue(entry.defaultColumns);
                                    }
                                }
                            }
                        }, this )
                    }
                }
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
        this.entrySelector.items.push({
            xtype: 'combo',
            name: 'interfaceLoadInterfaceId',
            fieldLabel: i18n._("Select Interface"),
            emptyText: i18n._("[interface ID]"),
            allowBlank: false,
            forceSelection: true,
            anyMatch: true,
            valueField: "id",
            displayField: "name",
            queryMode: 'local',
            labelWidth: 150,
            width: 400,
            tpl: null,
            listConfig: null,
            store: Ext.create('Ext.data.ArrayStore', {
                fields: ["id", "name"],
                data: Ung.dashboard.Util.getInterfaces()
            })
        });
        
        this.gridDashboardWidgets.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            rowEditorLabelWidth: 150,
            inputLines: [{
                xtype: "checkbox",
                name: "Enabled",
                dataIndex: "enabled",
                fieldLabel: i18n._( "Enabled" ),
                width: 360
            }, {
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
                        fn: Ext.bind(function(combo, newVal, oldVal) {
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
                    minValue: 10,
                    fieldLabel: i18n._( "Refresh Interval" )
                }, {
                    xtype: 'label',
                    html: i18n._( "(seconds)")+" - "+i18n._( "Leave blank for no Auto Refresh" ),
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
            }, {
                xtype: "container",
                dataIndex: "timeframe",
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype:'numberfield',
                    name: "timeframe",
                    minValue: 1,
                    maxValue: 24,
                    labelWidth: 150,
                    fieldLabel: i18n._( "Timeframe" )
                }, {
                    xtype: 'label',
                    html: i18n._( "(hours)")+" - "+i18n._( "The number of hours to query the latest data. Leave blank for last day." ),
                    cls: 'boxlabel'
                }],
                setValue: function(value) {
                    var timeframeValue = value? value/3600:"";
                    this.down('numberfield[name="timeframe"]').setValue(timeframeValue);
                },
                getValue: function() {
                    var hours = this.down('numberfield[name="timeframe"]').getValue();
                    return Ext.isEmpty(hours)?"":hours*3600;
                },
                setReadOnly: function(val) {
                    this.down('numberfield[name="timeframe"]').setReadOnly(val);
                }
            }, this.entrySelector, {
                xtype: 'container',
                layout: {type: "vbox"},
                dataIndex: 'displayColumns',
                items: [{
                    xtype: 'container',
                    margin: '10 0 10 0',
                    html: i18n._('It is recomanded to select 4 or less Display Columns to prevent having to scroll horizontally.')
                }, {
                    xtype: 'checkboxgroup',
                    name: 'columnsGroup',
                    columns: 3,
                    vertical: true,
                    defaults: {
                        width: 250,
                        name: 'cbGroup'
                    },
                    fieldLabel: i18n._("Display Columns")
                }],
                setValue: function(value) {
                    this.columnsValue = value;
                    if(rpc.reportsEnabled) {
                        this.down('checkboxgroup').setValue({cbGroup: value});
                    }

                },
                getValue: function() {
                    if(rpc.reportsEnabled) {
                        var value = this.down('checkboxgroup').getValue();
                        if(!value.cbGroup) {
                            return null;
                        } else if(Ext.isArray(value.cbGroup)) {
                            return value.cbGroup;
                        } else {
                            return [value.cbGroup];
                        }

                    } else {
                        return this.columnsValue;
                    }
                }
            }],
            populate: function(record, addMode) {
                //do not show already existing widgets that allow single instances
                var typeCombo = this.down("combo[dataIndex=type]");
                var entryId = this.down('[dataIndex=entryId]');
                var currentType = record.get("type");
                entryId.setType(currentType);
                var gridList = this.grid.getList();
                var existingTypesMap = Ung.Util.createRecordsMap(gridList, "type");
                var availableTypes = [], widget;
                if(addMode) {
                    record.set("refreshIntervalSec", "120");
                    record.set("timeframe", "3600");
                }
                for(var i=0; i < me.widgetsConfig.length; i++) {
                    widget = me.widgetsConfig[i];
                    if(!rpc.reportsEnabled && (widget.name == "ReportEntry" || widget.name == "EventEntry") && currentType!=widget.name) {
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
                        timeframe: this.down('[dataIndex=timeframe]'),
                        entryId: this.down('[dataIndex=entryId]'),
                        displayColumns: this.down(('[dataIndex=displayColumns]')),
                        columnsGroup: this.down(('checkboxgroup[name=columnsGroup]'))
                    };
                }
                var type = this.cmps.typeCmp.getValue();
                var widgetConfig = me.widgetsMap[type] || {};

                this.cmps.refreshIntervalSec.setVisible(widgetConfig.hasRefreshInterval);
                this.cmps.refreshIntervalSec.setDisabled(!widgetConfig.hasRefreshInterval);

                this.cmps.timeframe.setVisible( type == "ReportEntry" || type == "EventEntry");
                this.cmps.timeframe.setDisabled( type != "ReportEntry" && type != "EventEntry");


                this.cmps.entryId.setType(type);
                this.cmps.entryId.setVisible( type == "ReportEntry" || type == "EventEntry"  || type == "InterfaceLoad");
                this.cmps.entryId.setDisabled( type != "ReportEntry" && type != "EventEntry" && type != "InterfaceLoad");

                this.cmps.displayColumns.setVisible( type == "EventEntry" );
                this.cmps.displayColumns.setDisabled( type != "EventEntry" );
                this.cmps.columnsGroup.removeAll();
                if(type=="EventEntry" && rpc.reportsEnabled) {
                    var entryId = this.cmps.entryId.getValue();
                    var entry = Ung.dashboard.eventsMap[entryId];
                    if(entry) {
                        var tableConfig = Ung.TableConfig.getConfig(entry.table) || { columns: [] };
                        var values_arr = this.cmps.displayColumns.columnsValue || [];
                        var items = [];
                        for ( var i = 0; i < tableConfig.columns.length; i++) {
                            items.push({
                                xtype : 'checkbox',
                                margin : '0 20 0 0',
                                inputValue: tableConfig.columns[i].dataIndex,
                                boxLabel: tableConfig.columns[i].header,
                                checked: values_arr.indexOf(tableConfig.columns[i].dataIndex) != -1
                            });
                        }
                        this.cmps.columnsGroup.add(items);
                    }
                }

            }
        }));


        this.panelDashboardWidgets = Ext.create('Ext.panel.Panel',{
            name: 'panelDashboardWidgets',
            title: i18n._('Dashboard Widgets'),
            helpSource: 'dashboard_manager_dashboard_widgets',
            cls: 'ung-panel',
            autoScroll: true,
            defaults: {
                xtype: 'fieldset'
            },
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [{
                title: i18n._("Note"),
                html: i18n._("Reports and Events wigets are displayed in the Dashboard if the Reports application is installed and enabled, and only if their associalted application is installed.")
            }, this.gridDashboardWidgets ]
        });
        return this.panelDashboardWidgets;
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
