/*global
 Ext, Ung, i18n, rpc, console
*/

Ext.define('Webui.config.dashboardManager', {
    extend: 'Ext.panel.Panel',
    name: 'dashboardManager',
    helpSource: 'dashboard_manager',
    displayName: 'Dashboard Manager',
    flex: 1,
    padding: '0 0 0 0',
    layout: 'fit',
    border: false,
    initComponent: function () {
        this.items = this.buildPanelDashboardWidgets();
        this.callParent(arguments);
    },
    needDashboardReload: false,
    closeWindow: function () {
        if (this.needDashboardReload) {
            Ung.dashboard.loadDashboard();
        }
        this.hide();
        Ext.destroy(this);
    },
    getDashboardWidgets: function (handler) {
        if (!this.isVisible()) {
            return;
        }
        rpc.dashboardManager.getSettings(Ext.bind(function (result, exception) {
            var widgets = [];
            if (result && result.widgets && result.widgets.list) {
                widgets = result.widgets.list;
            }
            this.dashboardSettings = result;
            handler({javaClass: 'java.util.LinkedList', list: widgets}, exception);
        }, this));
    },
    resetSettingsToDefault: function () {
        Ext.MessageBox.confirm(i18n._('Warning'),
                i18n._('This will overwrite the current dashboard settings with the defaults.') + '<br/><br/>' +
                i18n._('Do you want to continue?'),
                Ext.bind(function (btn, text) {
                if (btn == 'yes') {
                    rpc.dashboardManager.resetSettingsToDefault(Ext.bind(function (result, exception) {
                        if (Ung.Util.handleException(exception)) {
                            return;
                        }
                        this.needDashboardReload = true;
                        this.gridDashboardWidgets.reload();
                        var me = this;
                        Ext.Function.defer(function () {
                            me.save();
                        }, 300);
                    }, this));
                }
            }, this));
    },

    // Dashboard Widgets Panel
    buildPanelDashboardWidgets: function () {
        this.gridDashboardWidgets = Ext.create('Ung.grid.Panel', {
            cls: 'dashboard-grid',
            settingsCmp: this,
            hasDelete: false,
            hasEdit: false,
            addAtTop: false,
            header: false,
            hideHeaders: true,
            border: false,
            padding: '0 0 0 0',
            flex: 1,
            scale: 'medium',
            viewConfig: {
                plugins: {
                    ptype: 'gridviewdragdrop',
                    dragText: 'Drag and drop to reorganize'
                }
            },
            bbar: [{
                xtype: 'button',
                scale: 'medium',
                cls: 'material-button',
                text: '<i class="material-icons">replay</i> <span>' +  i18n._('Reset') + '</span>',
                handler: Ext.bind(function () {
                    this.resetSettingsToDefault();
                }, this)
            }, '->', {
                xtype: 'button',
                scale: 'medium',
                cls: 'material-button',
                text: '<i class="material-icons">close</i> <span>' +  i18n._('Close') + '</span>',
                handler: Ext.bind(function () {
                    if (this.gridDashboardWidgets.isDirty()) {
                        Ext.MessageBox.confirm(i18n._('Warning'), i18n._('There are unsaved settings which will be lost. Do you want to continue?'),
                            Ext.bind(function (btn) {
                                if (btn == 'yes') {
                                    this.gridDashboardWidgets.clearDirty();
                                    this.up('[name=dashboardManager]').hide();
                                }
                            }, this));
                    } else {
                        this.up('[name=dashboardManager]').hide();
                    }
                }, this)
            }, {
                xtype: 'button',
                scale: 'medium',
                cls: 'material-button',
                text: '<i class="material-icons" style="color: green;">save</i> <span>' +  i18n._('Apply') + '</span>',
                handler: Ext.bind(function () {
                    this.save();
                }, this)
            }],
            dataFn: Ext.bind(this.getDashboardWidgets, this),
            recordJavaClass: 'com.untangle.uvm.DashboardWidgetSettings',
            emptyRow: {
                enabled: true,
                type: ''
            },
            fields: [{
                name: 'type',
                type: 'string'
            }, {
                name: 'refreshIntervalSec'
            }],
            columns: [{
                xtype: 'checkcolumn',
                width: 35,
                dataIndex: 'enabled',
                renderer: function (value, metaData, record) {
                    var entry = null, isInstallable = false;
                    if (record.data.entryId) {
                        if (!rpc.reportsEnabled) {
                            isInstallable = true;
                        } else {
                            entry = Ung.dashboard.reportsMap[record.data.entryId];
                            if (Ung.dashboard.unavailableApplicationsMap[entry.category]) {
                                isInstallable = true;
                            }
                        }
                    }
                    if (isInstallable) {
                        return '<i class="material-icons" style="color: #FFB300;">warning</i>';
                    }
                    if (value) {
                        return '<i class="material-icons" style="color: rgba(103,189,74,.9);">visibility</i>';
                    }
                    return '<i class="material-icons" style="color: #777;">visibility_off</i>';
                }
            }, {
                header: i18n._('Details'),
                dataIndex: 'type',
                width: 200,
                flex: 1,
                renderer: Ext.bind(function (value, metaData, record) {
                    if (value === 'ReportEntry') {
                        var _isInstallable = false;
                        var _app = record.get('entryId').split('-');
                        _app.pop();
                        Ext.each(rpc.rackView.installable.list, function (app) {
                            if (!_isInstallable && _app.join(' ') === app.displayName.toLowerCase()) {
                                _isInstallable = true;
                            }
                        });
                        if (rpc.reportsEnabled && Ung.dashboard.reportsMap) {
                            var entryId = record.get('entryId');
                            var entry = Ung.dashboard.reportsMap[entryId];
                            if (entry) {
                                return '<span style="font-size: 10px; color: #999;">' + i18n._(entry.category).toUpperCase() + (_isInstallable ? ' (' + i18n._('install required') + ')' : '') + '</span> <br/> ' + i18n._(entry.title);
                            }
                        } else {
                            return '<span style="font-size: 10px; color: #999;">' + _app.join(' ').toUpperCase() + ' (' + i18n._('install required') + ')</span><br/> Report';
                        }
                    }
                    if (value !== 'ReportEntry') {
                        return value;
                    }
                    return '';
                }, this)
            }, {
                xtype: 'actioncolumn',
                dataIndex: 'type',
                width: 30,
                handler: Ext.bind(function (view, rowIndex, colIndex, item, e, record) {
                    this.gridDashboardWidgets.stopEditing();
                    this.gridDashboardWidgets.rowEditor.populate(record);
                    this.gridDashboardWidgets.rowEditor.show();
                }, this),
                renderer: function (value) {
                    if (value === 'ReportEntry') {
                        return '<i class="material-icons action-edit">mode_edit</i>';
                    }
                }
            }, {
                xtype: 'actioncolumn',
                dataIndex: 'type',
                width: 30,
                //cls: 'action-delete',
                handler: Ext.bind(function (view, rowIndex, colIndex, item, e, record) {
                    this.gridDashboardWidgets.stopEditing();
                    this.gridDashboardWidgets.updateChangedData(record, 'deleted');
                }, this),

                renderer: function (value) {
                    if (value === 'ReportEntry') {
                        return '<i class="material-icons">clear</i>';
                    }
                }
            }]
        });

        this.gridDashboardWidgets.setRowEditor(Ext.create('Ung.RowEditorWindow', {
            rowEditorLabelWidth: 100,
            inputLines: [{
                xtype: 'container',
                dataIndex: 'entryId',
                margin: '0 0 10 0',
                items: [{
                    name: 'reportEntryId',
                    fieldLabel: i18n._('Select Report'),
                    labelAlign: 'top',
                    emptyText: i18n._('[enter report]'),
                    store: Ext.create('Ext.data.JsonStore', {
                        fields: ['uniqueId', 'category', 'title', {name: 'category_title', calculate: function (data) {
                            return data.category + ' - ' + data.title;
                        }}],
                        data: Ung.dashboard.reportEntries,
                        sorters: ['category', 'title']
                    })
                }],
                defaults: {
                    xtype: 'combo',
                    allowBlank: false,
                    forceSelection: true,
                    anyMatch: true,
                    valueField: 'uniqueId',
                    displayField: 'category_title',
                    queryMode: 'local',
                    width: 300,
                    tpl: Ext.create('Ext.XTemplate',
                        '<ul class="x-list-plain report-selector"><tpl for=".">',
                        '<li role="option" class="x-boundlist-item"><span>{category}</span><br/>{title}</li>',
                        '</tpl></ul>'
                        ),
                    listeners: {
                        'select': {
                            fn: Ext.bind(function (combo, newVal, oldVal) {
                                var rowEditor = this.gridDashboardWidgets.rowEditor;
                                rowEditor.syncComponents();
                            }, this)
                        }
                    }
                },
                getValue: function () {
                    return this.down('[name=reportEntryId]').getValue();
                },
                setValue: function (value) {
                    this.down('[name=reportEntryId]').setValue(value);
                }
            }, {
                xtype: 'container',
                dataIndex: 'refreshIntervalSec',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype: 'numberfield',
                    name: 'refreshIntervalSec',
                    minValue: 10,
                    fieldLabel: i18n._('Refresh Interval')
                }, {
                    xtype: 'label',
                    html: i18n._('(seconds)') + ' - ' + i18n._('Leave blank for no Auto Refresh'),
                    cls: 'boxlabel'
                }],
                setValue: function (value) {
                    this.down('numberfield[name="refreshIntervalSec"]').setValue(value);
                },
                getValue: function () {
                    return this.down('numberfield[name="refreshIntervalSec"]').getValue();
                },
                setReadOnly: function (val) {
                    this.down('numberfield[name="refreshIntervalSec"]').setReadOnly(val);
                }
            }, {
                xtype: 'container',
                dataIndex: 'timeframe',
                layout: 'column',
                margin: '0 0 5 0',
                items: [{
                    xtype: 'numberfield',
                    name: 'timeframe',
                    minValue: 1,
                    maxValue: 24,
                    fieldLabel: i18n._('Timeframe')
                }, {
                    xtype: 'label',
                    html: i18n._('(hours)') + ' - ' + i18n._('The number of hours to query the latest data. Leave blank for last day.'),
                    cls: 'boxlabel'
                }],
                setValue: function (value) {
                    var timeframeValue = value ? value / 3600 : '';
                    this.down('numberfield[name="timeframe"]').setValue(timeframeValue);
                },
                getValue: function () {
                    var hours = this.down('numberfield[name="timeframe"]').getValue();
                    return Ext.isEmpty(hours) ? '' : hours * 3600;
                },
                setReadOnly: function (val) {
                    this.down('numberfield[name="timeframe"]').setReadOnly(val);
                }
            }],
            populate: function (record, addMode) {
                if (addMode) {
                    record.set('refreshIntervalSec', '120');
                    record.set('timeframe', '3600');
                    record.set('type', 'ReportEntry');
                }
                Ung.RowEditorWindow.prototype.populate.apply(this, arguments);
            },
            syncComponents: function () {
                if (!this.cmps) {
                    this.cmps = {
                        refreshIntervalSec: this.down('[dataIndex=refreshIntervalSec]'),
                        timeframe: this.down('[dataIndex=timeframe]'),
                        entryId: this.down('[dataIndex=entryId]')
                    };
                }
            }
        }));


        this.panelDashboardWidgets = Ext.create('Ext.panel.Panel', {
            name: 'panelDashboardWidgets',
            helpSource: 'dashboard_manager_dashboard_widgets',
            cls: 'ung-panel',
            padding: '0 0 0 0',
            border: false,
            autoScroll: true,
            layout: 'fit',
            defaults: {
                xtype: 'fieldset'
            },
            items: [this.gridDashboardWidgets ]
        });
        return this.panelDashboardWidgets;
    },
    save: function (isApply) {
        Ext.MessageBox.wait(i18n._('Saving...'), i18n._('Please wait'));
        this.needDashboardReload = true;
        var widgets = this.gridDashboardWidgets.getList();
        this.dashboardSettings.widgets = { javaClass: 'java.util.LinkedList', list: widgets };
        rpc.dashboardManager.setSettings(Ext.bind(function (result, exception) {
            Ext.MessageBox.hide();
            if (Ung.Util.handleException(exception)) {
                return;
            }
            if (!isApply) {
                Ung.dashboard.loadDashboard();
                this.gridDashboardWidgets.clearDirty();
            }
        }, this), this.dashboardSettings);

    }
});

//# sourceURL=dashboardManager.js
