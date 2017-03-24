/*global
 Ext, Ung, i18n, rpc, setTimeout, clearTimeout, console, window, document, Highcharts, testMode
 */

Ext.define('Ung.panel.Reports', {
    extend : 'Ext.container.Container',
    layout: 'border',
    name: 'panelReports',
    border: false,
    autoRefreshEnabled: false,
    autoRefreshInterval: 10, //seconds
    extraConditions: null,
    reportsManager: null,
    dashboardWidgets: null,
    mixins: [
        'Ext.mixin.Responsive'
    ],
    responsiveFormulas: {
        large: 'width >= 1280',
        medium: 'width >= 1024 && width < 1280',
        small: 'width <= 1024',
        insideSettingsWin: function () {
            return false;
        }
    },
    items: [{
        region: 'north',
        itemId: 'reportsNorth',
        height: 44,
        border: false,
        hidden: false,
        bodyStyle: {
            background: '#555'
        },
        layout: {
            type: 'hbox',
            align: 'middle'
        },
        items: [],
        plugins: 'responsive',
        responsiveConfig: {
            'small || medium': {
                hidden: false
            },
            'large': {
                hidden: true
            },
            'insideSettingsWin && small': {
                hidden: false
            },
            'insideSettingsWin && !small': {
                hidden: true
            }
        }
    }, {
        xtype: 'grid',
        region: 'west',
        itemId: 'categoryList',
        cls: 'category-list',
        title: i18n._('Select Category'),
        width: 180,
        hideHeaders: true,
        collapsed: false,
        collapsible: true,
        animCollapse: false,
        titleCollapse: true,
        floatable: false,
        split: true,
        resizable: false,
        border: false,
        store: Ext.create('Ext.data.Store', {
            fields: ['text', 'category'],
            data: []
        }),
        columns: [{
            width: 30,
            renderer: Ext.bind(function (value, metaData, record) {
                if (record.getData().category === 'All') {
                    return '<i class="material-icons">format_list_bulleted</i>';
                }
                return '<img src="' + record.getData().icon + '"/>';
            }, this)
        }, {
            flex: 1,
            dataIndex: 'text'
        }],
        viewConfig: {
            getRowClass: function () {
                return 'category-item';
            }
        },
        plugins: 'responsive',
        responsiveConfig: {
            'small || medium': {
                hidden: true
            },
            'large': {
                hidden: false
            },
            'insideSettingsWin': {
                hidden: true
            }
        }
    }, {
        region: 'center',
        itemId: 'reportsMain',
        layout: 'card',
        border: false,
        items: [{
            xtype: 'container',
            layout: 'column',
            itemId: 'categorySelector',
            userCls: 'category-selector',
            scrollable: true,
            border: false
        }, {
            xtype: 'container',
            layout: 'border',
            border: false,
            items: [{
                xtype: 'grid',
                region: 'west',
                itemId: 'entryList',
                cls: 'entry-list',
                title: i18n._('Select Report'),
                hideHeaders: true,
                width: 280,
                collapsed: false,
                collapsible: true,
                animCollapse: false,
                titleCollapse: true,
                floatable: false,
                split: true,
                resizable: false,
                border: false,
                store: Ext.create('Ext.data.Store', {
                    fields: ['text', 'inDashboard'],
                    data: []
                }),
                columns: [],
                viewConfig: {
                    getRowClass: function (record) {
                        var entry = record.getData().entry;
                        if (!entry.readOnly && entry.type !== 'EVENT_LIST') {
                            return 'custom';
                        }
                    }
                },
                plugins: 'responsive',
                responsiveConfig: {
                    'small': {
                        hidden: true
                    },
                    'medium || large': {
                        hidden: false
                    }
                }
            }, {
                region: 'center',
                layout: 'card',
                itemId: 'entryView',
                border: false,
                items: [{
                    xtype: 'container',
                    itemId: 'entrySelector',
                    //layout: 'column',
                    padding: '0',
                    scrollable: true
                }, {
                    itemId: 'reportView',
                    layout: 'border',
                    border: false,
                    items: [{
                        region: 'center',
                        itemId: 'reportPanel',
                        layout: 'card',
                        border: false
                    }]
                }]
            }]
        }]
    }],
    selectedCategory: null,
    beforeDestroy: function () {
        Ext.destroy(this.subCmps);
        this.callParent(arguments);
    },
    initComponent: function () {
        if (!rpc.reportsEnabled) {
            this.items = [{
                region: 'center',
                xtype: 'panel',
                bodyStyle: {
                    padding: '10px'
                },
                html: i18n._("Reports application is required for this feature. Please install and enable the Reports application.")
            }];
            this.callParent(arguments);
            return;
        }

        var me = this;
        this.subCmps = [];
        this.callParent(arguments);

        this.subCmps.push(this.startDateWindow);
        this.subCmps.push(this.endDateWindow);

        this.filterFeature = Ext.create('Ung.grid.feature.GlobalFilter', {});


        this.reportsMain = this.down('#reportsMain');
        this.categoryList = this.down('#categoryList');
        this.categorySelector = this.down('#categorySelector');
        this.entryList = this.down('#entryList');
        this.entrySelector = this.down('#entrySelector');
        this.entryView = this.down('#entryView');

        this.reportView = this.down('#reportView');
        this.reportPanel = this.down('#reportPanel');

        this.extraConditionsPanel = Ext.create("Ung.panel.ExtraConditions", {
            parentPanel: this
        });


        this.down('#reportsNorth').add({
            xtype: 'button',
            itemId: 'categoriesMenuBtn',
            cls: 'categories-menu-btn',
            scale: 'medium',
            textAlign: 'left',
            text: i18n._('All Categories'),
            //icon: _categoryMenuItems[0].icon,
            width: 195,
            margin: '0 0 0 5',
            plugins: 'responsive',
            responsiveConfig: {
                'small || medium': {
                    hidden: false
                },
                'large': {
                    hidden: true
                },
                'insideSettingsWin': {
                    hidden: true
                }
            }
        }, {
            xtype: 'button',
            itemId: 'entriesMenuBtn',
            scale: 'medium',
            textAlign: 'left',
            text: i18n._('Select a Category first'),
            width: 200,
            margin: '0 0 0 5',
            //hidden: true,
            plugins: 'responsive',
            responsiveConfig: {
                'small': {
                    hidden: false
                },
                'medium || large': {
                    hidden: true
                }
            }
        });

        this.reportView.add(this.extraConditionsPanel);
        this.reportPanel.add({
            xtype: 'panel',
            itemId: 'reportContainer',
            cls: 'report-container',
            border: false,
            layout: 'border',
            items: [{
                xtype: 'panel',
                region: 'center',
                itemId: 'reportChart',
                layout: 'card',
                border: false,
                items: [{
                    xtype: 'panel',
                    itemId: 'highchart',
                    border: false
                }, {
                    xtype: 'panel',
                    itemId: 'textentry',
                    border: false,
                    textAlign: 'center',
                    bodyStyle: {
                        fontSize: '16px',
                        padding: '10px'
                    }
                }],
                listeners: {
                    resize: Ext.bind(function () {
                        if (this.chart) {
                            this.chart.reflow();
                        }
                    }, this)
                }
            }, {
                xtype: 'grid',
                itemId: 'reportData',
                region: 'east',
                title: i18n._('Current Data'),
                width: 330,
                split: true,
                collapsible: true,
                collapsed: true,
                animCollapse: false,
                resizable: true,
                floatable: false,
                titleCollapse: true,
                border: false,
                store:  Ext.create('Ext.data.Store', {
                    fields: [],
                    data: []
                }),
                columns: [{
                    flex: 1
                }],
                tbar: ['->', {
                    xtype: 'button',
                    text: i18n._('Export'),
                    name: "Export",
                    tooltip: i18n._('Export Data to File'),
                    iconCls: 'icon-export',
                    handler: Ext.bind(this.exportReportDataHandler, this)
                }]
            }],
            tools: [{
                xtype: 'button',
                text: '<span style="vertical-align: middle;">' + i18n._("Add to Dashboard") + '</span>',
                cls: 'action-button material-button',
                itemId: 'dashboardReportBtn',
                //iconCls: 'icon-add-row',
                margin: '0 3',
                scale: 'medium',
                hidden: !Ung.Main.webuiMode || this.hideCustomization,
                handler: Ext.bind(function (btn) {
                    this.dashboardAction(btn);
                }, this)
            }, {
                xtype: 'button',
                text: '<i class="material-icons">edit</i> <span style="vertical-align: middle;">' + i18n._("Customize") + '</span>',
                cls: 'action-button material-button',
                margin: '0 3',
                scale: 'medium',
                hidden: !Ung.Main.webuiMode || this.hideCustomization,
                name: "edit",
                //tooltip: i18n._('Advanced report customization'),
                //iconCls: 'icon-edit',
                handler: Ext.bind(this.customizeReport, this)
            }, {
                xtype: 'button',
                text: '<i class="material-icons">file_download</i> <span style="vertical-align: middle;">' + i18n._("Download") + '</span>',
                cls: 'action-button material-button',
                margin: '0 3',
                scale: 'medium',
                hidden: !Ung.Main.webuiMode || this.hideCustomization,
                name: "download",
                itemId: 'downloadBtn',
                handler: Ext.bind(this.downloadReport, this)
            }, {
                xtype: 'button',
                text: '<i class="material-icons" style="color: red;">delete</i> <span style="vertical-align: middle;">' + i18n._("Delete") + '</span>',
                cls: 'action-button material-button',
                name: "remove",
                itemId: "removeEntryBtn",
                scale: 'medium',
                hidden: !Ung.Main.webuiMode || this.hideCustomization,
                handler: Ext.bind(function () {
                    var record = this.entryList.getSelectionModel().getSelection()[0];
                    if (record.getData().inDashboard) {
                        Ext.MessageBox.confirm(i18n._('Warning'),
                            i18n._('Deleting this report will remove also the Widget from Dashboard!') + '<br/><br/>' +
                            i18n._('Do you want to continue?'),
                            Ext.bind(function (btn) {
                                if (btn == 'yes') {
                                    this.removeReport(record);
                                }
                            }, this));
                    } else {
                        this.removeReport(record);
                    }
                }, this)
            }]
        }, {
            xtype: 'grid',
            itemId: 'eventContainer',
            cls: 'report-container',
            //stateful: true,
            //stateId: "eventGrid",
            border: false,
            viewConfig: {
                enableTextSelection: true
            },
            store: Ext.create('Ext.data.Store', {
                fields: [],
                data: [],
                proxy: {
                    type: 'memory',
                    reader: {
                        type: 'json'
                    }
                }
            }),
            columns: [{
                flex: 1
            }],
            tools: [{
                xtype: 'button',
                text: i18n._('Add to Dashboard'),
                cls: 'action-button material-button',
                itemId: 'dashboardEventBtn',
                margin: '0 3',
                scale: 'medium',
                hidden: !Ung.Main.webuiMode,
                handler: Ext.bind(function (btn) {
                    this.dashboardAction(btn);
                }, this)
            }, {
                xtype: 'button',
                text: '<i class="material-icons">edit</i> <span style="vertical-align: middle;">' + i18n._("Customize") + '</span>',
                cls: 'action-button material-button',
                margin: '0 3',
                scale: 'medium',
                hidden: !Ung.Main.webuiMode || this.hideCustomization,
                name: "edit",
                handler: Ext.bind(this.customizeReport, this)
            }],
            plugins: ['gridfilters'],
            features: [this.filterFeature],
            dockedItems: [{
                xtype: 'toolbar',
                dock: 'top',
                items: [i18n._('Filter:'), {
                    xtype: 'textfield',
                    name: 'searchField',
                    hideLabel: true,
                    width: 130,
                    listeners: {
                        change: {
                            fn: function () {
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
                    handler: function () {
                        this.filterFeature.updateGlobalFilter(this.searchField.getValue(), this.caseSensitive.getValue());
                    },
                    scope: this
                }, {
                    xtype: 'button',
                    iconCls: 'icon-clear-filter',
                    text: i18n._('Clear Filters'),
                    tooltip: i18n._('Filters can be added by clicking on column headers arrow down menu and using Filters menu'),
                    handler: Ext.bind(function () {
                        this.eventContainer.clearFilters();
                        this.searchField.setValue("");
                    }, this)
                }, '->', {
                    xtype: 'button',
                    text: i18n._('Export'),
                    name: "Export",
                    tooltip: i18n._('Export Events to File'),
                    iconCls: 'icon-export',
                    handler: Ext.bind(this.exportEventsHandler, this)
                }]
            }]
        });

        this.reportPanel.addDocked({
            xtype: 'toolbar',
            dock: 'bottom',
            items: [{
                xtype: 'combo',
                width: 120,
                name: "limitSelector",
                hidden: true,
                editable: false,
                valueField: "value",
                displayField: "name",
                queryMode: 'local',
                value: 1000,
                store: Ext.create('Ext.data.Store', {
                    fields: ["value", "name"],
                    data: [{
                        value: 1000,
                        name: "1000 " + i18n._('Events')
                    }, {
                        value: 10000,
                        name: "10000 " + i18n._('Events')
                    }, {
                        value: 50000,
                        name: "50000 " + i18n._('Events')
                    }]
                })
            }, {
                xtype: 'button',
                name: 'startDateButton',
                text: i18n._('One day ago'),
                initialLabel:  i18n._('One day ago'),
                width: 132,
                tooltip: i18n._('Select Start date and time'),
                handler: Ext.bind(function (button) {
                    this.startDateWindow.show();
                }, this)
            }, {
                xtype: 'tbtext',
                text: '-'
            }, {
                xtype: 'button',
                name: 'endDateButton',
                text: i18n._('Present'),
                initialLabel:  i18n._('Present'),
                width: 132,
                tooltip: i18n._('Select End date and time'),
                handler: Ext.bind(function (button) {
                    this.endDateWindow.show();
                }, this)
            }, {
                xtype: 'button',
                text: i18n._('Refresh'),
                name: "refresh",
                tooltip: i18n._('Flush Events from Memory to Database and then Refresh'),
                iconCls: 'icon-refresh',
                handler: Ext.bind(function () {
                    this.refreshHandler();
                }, this)
            }, {
                xtype: 'button',
                name: 'auto_refresh',
                text: i18n._('Auto Refresh'),
                enableToggle: true,
                pressed: false,
                tooltip: Ext.String.format(i18n._('Auto Refresh every {0} seconds'), this.autoRefreshInterval),
                iconCls: 'icon-autorefresh',
                handler: Ext.bind(function (button) {
                    if (button.pressed) {
                        this.startAutoRefresh();
                    } else {
                        this.stopAutoRefresh();
                    }
                }, this)
            }, {
                text: i18n._('Reset View'),
                name: "resetView",
                hidden: true,
                tooltip: i18n._('Restore default columns positions, widths and visibility'),
                handler: Ext.bind(function () {
                    if (!this.entry || !Ung.panel.Reports.isEvent(this.entry)) {
                        return;
                    }
                    var gridEvents = this.down("grid[name=gridEvents]");
                    Ext.state.Manager.clear(gridEvents.stateId);
                    gridEvents.reconfigure(undefined, gridEvents.defaultTableConfig.columns);
                }, this)
            }]
        });

        this.startDateWindow = Ext.create('Ung.window.SelectDateTime', {
            title: i18n._('Start date and time'),
            dateTimeEmptyText: i18n._('start date and time'),
            buttonObj: this.down('button[name=startDateButton]')
        });
        this.endDateWindow = Ext.create('Ung.window.SelectDateTime', {
            title: i18n._('End date and time'),
            dateTimeEmptyText: i18n._('end date and time'),
            buttonObj: this.down('button[name=endDateButton]')
        });

        this.reportContainer = this.down('#reportContainer');
        this.reportData = this.down('#reportData');
        this.reportChart = this.down('#reportChart');
        this.eventContainer = this.down('#eventContainer');

        this.searchField = this.down('textfield[name=searchField]');
        this.caseSensitive = this.down('checkbox[name=caseSensitive]');
        this.limitSelector = this.down("combo[name=limitSelector]");

        this.categoryList = this.down('#categoryList');
        this.categoryList.addListener('select', Ext.bind(function (rowModel, record) {
            this.selectedCategory = record;
            if (record.getData().category === 'All') {
                this.reportsMain.setActiveItem(0);
                this.entryView.setActiveItem(0);
                this.down('#entriesMenuBtn').setText(i18n._('Select a Category first'));
            } else {
                this.reportsMain.setActiveItem(1);
                this.entryView.setActiveItem(0);
                this.down('#entriesMenuBtn').setText(i18n._('Select Report'));
                this.loadReportEntries();
            }
            this.down('#categoriesMenuBtn').setText(record.getData().text).setIcon(record.getData().icon);
            //this.down('#extraConditions').setHidden(false);
        }, this));

        this.entryList = this.down('#entryList');
        this.entryList.reconfigure([{
            width: 30,
            renderer: Ext.bind(function (value, metaData, record) {
                return '<i class="material-icons">' + this.setEntryIcon(record.getData().entry) + '</i>';
            }, this)
        }, {
            flex: 1,
            dataIndex: 'text',
            renderer: Ext.bind(function (value, metaData, record) {
                return i18n._(value);
            }, this)
        }, {
            width: 24,
            hidden: !Ung.Main.webuiMode,
            renderer: Ext.bind(function (value, metaData, record) {
                if (!record.getData().entry.readOnly) {
                    return '<i class="material-icons" style="font-size: 14px; color: #999;">brush</i>';
                }
            }, this)
        }, {
            width: 24,
            hidden: !Ung.Main.webuiMode,
            dataIndex: 'inDashboard',
            renderer: Ext.bind(function (value, metaData, record) {
                if (record.getData().inDashboard) {
                    return '<i class="material-icons" style="font-size: 16px; color: #999;">home</i>';
                }
            }, this)
        }]);
        this.entryList.addListener('select', Ext.bind(function (rowModel, record) {
            this.entryView.setActiveItem(1);
            this.entry = record.getData().entry;

            this.down('#removeEntryBtn').setHidden(this.entry.readOnly || !Ung.Main.webuiMode || this.hideCustomization);

            if (this.entry.type === 'EVENT_LIST') {
                this.limitSelector.show();
                this.loadEventEntry(this.entry);
            } else {
                this.limitSelector.hide();
                this.loadReportEntry(this.entry);
            }
            this.down('#entriesMenuBtn').setText('<i class="material-icons">' + record.getData().icon  + '</i> <span style="vertical-align: middle;">' + record.getData().text + '</span>');
            if (Ung.Main.webuiMode) {
                this.setDashboardButton();
            }
        }, this));

        if (Ung.Main.webuiMode) {
            this.getDashboardWidgets().then(function (result) {
                me.dashboardSettings = result;
                me.dashboardWidgets = result.widgets.list;

                me.loadAllReports().then(function (reports) {
                    me.allReports = reports;
                    me.buildCategoryList();
                    Ung.Main.viewport.down('#panelCenter').setLoading(false);
                }, function (exception) {
                    Ung.Util.handleException(exception);
                });
            }, function (exception) {
                Ung.Util.handleException(exception);
            });
        } else {
            me.loadAllReports().then(function (reports) {
                me.allReports = reports;
                me.buildCategoryList();
            }, function (exception) {
                Ung.Util.handleException(exception);
            });
        }
    },

    getDashboardWidgets: function () {
        var deferred = new Ext.Deferred();
        rpc.dashboardManager.getSettings(Ext.bind(function (result, exception) {
            if (exception) { deferred.reject(exception); }
            deferred.resolve(result);
        }, this));
        return deferred.promise;
    },

    setDashboardButton: function () {
        if (!this.entry) {
            return;
        }
        var i, addedToDashboard = false, btn;
        if (this.entry.type !== 'EVENT_LIST') {
            btn = this.down('#dashboardReportBtn');
        } else {
            btn = this.down('#dashboardEventBtn');
        }

        for (i = 0; i < this.dashboardWidgets.length; i += 1) {
            if (!addedToDashboard && this.dashboardWidgets[i].entryId === this.entry.uniqueId) {
                addedToDashboard = true;
            }
        }
        btn.addAction = !addedToDashboard;
        btn.setText(addedToDashboard ? i18n._("Remove from Dashboard") : i18n._("Add to Dashboard"));
        //btn.setIconCls(addedToDashboard ? 'icon-delete-row' : 'icon-add-row');
    },

    dashboardAction: function (btn) {
        Ung.dashboard.reportEntriesModified = true;
        var i;
        if (btn.addAction) {
            this.dashboardWidgets.push({
                displayColumns: this.entry.displayColumns,
                enabled: true,
                entryId: this.entry.uniqueId,
                javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                refreshIntervalSec: 60,
                timeframe: 3600,
                type: 'ReportEntry'
            });
        } else {
            for (i = 0; i < this.dashboardWidgets.length; i += 1) {
                if (this.dashboardWidgets[i].entryId === this.entry.uniqueId) {
                    this.dashboardWidgets.splice(i, 1);
                }
            }
        }
        this.dashboardSettings.widgets = {
            javaClass: 'java.util.LinkedList',
            list: this.dashboardWidgets
        };
        rpc.dashboardManager.setSettings(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            btn.setText(btn.addAction ? i18n._("Remove from Dashboard") : i18n._("Add to Dashboard"));
            //btn.setIconCls(btn.addAction ? 'icon-delete-row' : 'icon-add-row');

            var rec = this.entryList.getSelectionModel().getSelected();
            rec.items[0].set('inDashboard', btn.addAction, {commit: true});

            Ung.Util.userActionToast('<span style="color: #FFF;">' + this.entry.title + '</span> ' + (btn.addAction ? i18n._('was added to Dashboard') : i18n._('was removed from Dashboard')));

            btn.addAction = !btn.addAction;
            this.entry.inDashboard = btn.addAction;
        }, this), this.dashboardSettings);
    },

    loadAllReports: function () {
        var deferred = new Ext.Deferred();
        var allReports = {}, i, entry;

        rpc.reportsManager.getReportEntries(Ext.bind(function (result, exception) {
            if (exception) { deferred.reject(exception); }

            for (i = 0; i < result.list.length; i += 1) {
                entry = result.list[i];
                entry.selModel = Ext.create('Ung.grid.ReportItemModel', {
                    text : i18n._(entry.title),
                    type: entry.type
                });
                if (!allReports.hasOwnProperty(entry.category)) {
                    allReports[entry.category] = [entry];
                } else {
                    allReports[entry.category].push(entry);
                }
            }
            deferred.resolve(allReports);
        }, this));

        return deferred.promise;
    },

    loadCategoryReports: function (category) {
        var deferred = new Ext.Deferred();
        rpc.reportsManager.getReportEntries(Ext.bind(function (result, exception) {
            if (exception) { deferred.reject(exception); }
            deferred.resolve(result.list);
        }, this), category);
        return deferred.promise;
    },

    reloadReports: function () {
        var category = this.selectedCategory.getData().category, me = this;
        this.loadCategoryReports(category).then(function (reports) {
            if (!me.allReports.hasOwnProperty(category)) {
                return;
            }
            me.allReports[category] = reports;
            me.categoryList.getSelectionModel().deselectAll();
            me.categoryList.getSelectionModel().select(me.selectedCategory);
        }, function (exception) {
            Ung.Util.handleException(exception);
        });
    },

    buildCategoryList: function () {
        var _categoryMenuItems = [], _categorySideItems = [], _item, i,
            _skinPath = '/skins/' + rpc.skinSettings.skinName + '/images/admin/',
            staticItems = [
                { text : i18n._('All Categories'), category : 'All'},
                { text : i18n._('Hosts'), category : 'Hosts', icon : _skinPath + 'config/icon_config_hosts.png' },
                { text : i18n._('Devices'), category : 'Devices', icon : _skinPath + 'config/icon_config_devices.png'},
                { text : i18n._('Network'), category : 'Network', icon : _skinPath + 'config/icon_config_network.png' },
                { text : i18n._('Administration'), category : 'Administration', icon : _skinPath + 'config/icon_config_admin.png' },
                { text : i18n._('Events'), category : 'Events', icon : _skinPath + 'config/icon_config_events.png' },
                { text : i18n._('System'), category : 'System', icon : _skinPath + 'config/icon_config_system.png' },
                { text : i18n._('Shield'), category : 'Shield', icon : _skinPath + 'config/icon_config_shield.png' }
            ];

        for (i = 0; i < staticItems.length; i += 1) {
            _item = Ext.create('Ung.grid.ReportItemModel', {
                text : staticItems[i].text,
                category : staticItems[i].category,
                icon : staticItems[i].icon
            });
            staticItems[i].model = _item;
            _categoryMenuItems.push(staticItems[i]);
            _categorySideItems.push(_item);
        }

        // add installed applications
        Ung.Main.getReportsManager().getCurrentApplications(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }

            var currentApplications = result.list;
            if (currentApplications) {
                var app;
                for (i = 0; i < currentApplications.length; i += 1) {
                    app = currentApplications[i];
                    if (app.name != 'branding-manager' && app.name != 'live-support') {

                        _item = Ext.create('Ung.grid.ReportItemModel', {
                            text: i18n._(app.displayName),
                            category: app.displayName,
                            icon: '/skins/' + rpc.skinSettings.skinName + '/images/admin/apps/' + app.name + '_80x80.png'
                        });

                        _categoryMenuItems.push({
                            text: i18n._(app.displayName),
                            category: app.displayName,
                            icon: '/skins/' + rpc.skinSettings.skinName + '/images/admin/apps/' + app.name + '_80x80.png',
                            model: _item
                        });

                        _categorySideItems.push(_item);
                    }
                }
            }

            if (this.categoryList.getStore()) {
                this.categoryList.getStore().loadData(_categorySideItems);
            } else {
                return;
            }


            this.buildCategorySelector(_categorySideItems);
            this.categoryList.setSelection(_categorySideItems[0]);

            if (this.initEntry) {
                this.categoryList.getSelectionModel().select(this.categoryList.getStore().findRecord('category', this.initEntry.category));
            }

            this.down('#categoriesMenuBtn').setMenu(Ext.create('Ext.menu.Menu', {
                //itemId: 'categoriesMenu',
                cls: 'categories-menu',
                plain: true,
                items: _categoryMenuItems,
                width: 195,
                shadow: false,
                listeners: {
                    click: Ext.bind(function (menu, item) {
                        this.categoryList.getSelectionModel().select(item.model);
                        //this.down('[name=northEntriesBtn]').setText(i18n._('Select Report/Event'));
                    }, this)
                }
            }));
        }, this));
    },

    buildCategorySelector: function (categories) {
        var i, j, panels = [], reports, me = this;
        for (i = 1; i < categories.length; i += 1) {
            reports = this.allReports[categories[i].getData().category];
            if(reports === undefined){
                continue;
            }
            for (j = 0; j < reports.length; j += 1) {
                reports[j].icon = this.setEntryIcon(reports[j]);
            }
            panels.push(Ext.create('Ung.panel.ReportCategorySelector', {
                parentCt: me,
                category: categories[i],
                reports: reports
            }));
        }
        this.down('#categorySelector').add(panels);
    },

    loadReportEntries: function () {
        var entries, entry, listEntries = [], menuEntries = [], selectionEntries = [], i, _icon, _that = this, btnHtml;

        entries = this.allReports[this.selectedCategory.getData().category];

        for (i = 0; i < entries.length; i += 1) {
            _icon = this.setEntryIcon(entries[i]);
            entry = Ext.create('Ung.grid.ReportItemModel', {
                text : entries[i].title,
                entry: entries[i],
                entryId: entries[i].uniqueId,
                icon: _icon,
                inDashboard: Ung.Main.webuiMode ? this.inDashboard(entries[i]) : null
            });
            listEntries.push(entry);

            menuEntries.push({
                text: '<i class="material-icons" style="font-size: 20px;">' + _icon + '</i> <span>' + i18n._(entries[i].title) + '</span>',
                model: entry
            });

            btnHtml = '<i class="material-icons">' + _icon + '</i>';

            btnHtml += '<span class="ttl">' + i18n._(entries[i].title) + '</span><br/><span class="dsc">' + i18n._(entries[i].description) + '</span></br>';

            if (this.inDashboard(entries[i])) {
                btnHtml += '<i class="material-icons in-dashboard">home</i> <span class="icon-label">in Dashboard</span>';
            }
            if (!entries[i].readOnly) {
                btnHtml += '<i class="material-icons not-read-only">brush</i> <span class="icon-label">Custom</span>';
            }

            selectionEntries.push({
                xtype: 'button',
                html: btnHtml,
                cls: (!entries[i].readOnly && entries[i].type !== 'EVENT_LIST') ? 'entry-btn custom' : 'entry-btn',
                width: 300,
                border: false,
                textAlign: 'left',
                item: entry,
                handler: function () {
                    _that.entryList.getSelectionModel().select(this.item);
                }
            });
        }
        this.entryList.getStore().loadData(listEntries);
        this.down('#entriesMenuBtn').setMenu(Ext.create('Ext.menu.Menu', {
            plain: true,
            items: menuEntries,
            width: 200,
            shadow: false,
            listeners: {
                click: Ext.bind(function (menu, item) {
                    this.entryList.getSelectionModel().select(item.model);
                }, this)
            }
        }));
        this.entrySelector.removeAll(true);

        // not adding header when in App Settings Win
        if (!this.category) {
            this.entrySelector.add({
                xtype: 'container',
                cls: 'entry-selector-header',
                html: '<img src="' + this.selectedCategory.getData().icon + '"/>' + '<span>' + this.selectedCategory.getData().text + '</span>'
            });
        }
        this.entrySelector.add(selectionEntries);

        if (this.initEntry) {
            this.entryList.getSelectionModel().select(this.entryList.getStore().findRecord('entryId', this.initEntry.uniqueId));
            this.initEntry = null;
        }
    },

    setEntryIcon: function (entry) {
        var icon;
        switch (entry.type) {
        case 'TEXT':
            icon = 'subject';
            break;
        case 'EVENT_LIST':
            icon = 'format_list_bulleted';
            break;
        case 'PIE_GRAPH':
            icon = 'pie_chart';
            if (entry.pieStyle === 'COLUMN' || entry.pieStyle === 'COLUMN_3D') {
                icon = 'insert_chart';
            } else {
                if (entry.pieStyle === 'DONUT' || entry.pieStyle === 'DONUT_3D') {
                    icon = 'donut_large';
                }
            }
            break;
        case 'TIME_GRAPH':
        case 'TIME_GRAPH_DYNAMIC':
            if (entry.timeStyle.indexOf('BAR') >= 0) {
                icon = 'insert_chart';
            } else {
                icon = 'show_chart';
            }
            break;
        default:
            icon = 'subject';
        }
        return icon;
    },

    inDashboard: function (entry) {
        if (Ung.Main.webuiMode) {
            var addedToDashboard = false, i;
            for (i = 0; i < this.dashboardWidgets.length; i += 1) {
                if (!addedToDashboard && this.dashboardWidgets[i].entryId === entry.uniqueId) {
                    addedToDashboard = true;
                }
            }
            return addedToDashboard;
        }
        return null;
    },

    loadReportEntry: function (entry) {
        this.down('#downloadBtn').setHidden(entry.type === 'TEXT' || entry.type === 'EVENT_LIST' || this.hideCustomization);
        if (entry.type === 'TEXT') {
            this.reportChart.down('#textentry').update('');
            this.reportChart.setActiveItem('textentry');
        } else {
            this.down('#highchart').body.dom.innerHTML = '';
            var tbar = this.down('#highchart').getDockedItems('toolbar[dock="top"]');
            if (tbar.length > 0) {
                tbar[0].destroy();
            }
            this.reportChart.setActiveItem('highchart');

            var chartTypeToolbar = [], button, i;

            if (entry.type === 'PIE_GRAPH') {
                var pieStyleButtons = [
                    { pieStyle: 'PIE', icon: 'pie_chart', text: i18n._("Pie") },
                    { pieStyle: 'PIE_3D', icon: 'pie_chart', text: i18n._("3D Pie") },
                    { pieStyle: 'DONUT', icon: 'donut_large', text: i18n._("Donut") },
                    { pieStyle: 'DONUT_3D', icon: 'donut_large', text: i18n._("3D Donut") },
                    { pieStyle: 'COLUMN', icon: 'insert_chart', text: i18n._("Column") },
                    { pieStyle: 'COLUMN_3D', icon: 'insert_chart', text: i18n._("3D Column") }
                ];

                for (i = 0; i < pieStyleButtons.length; i += 1) {
                    button = pieStyleButtons[i];
                    chartTypeToolbar.push({
                        xtype: 'button',
                        pressed: entry.pieStyle === button.pieStyle,
                        //iconCls: button.iconCls,
                        text: '<i class="material-icons">' + button.icon + '</i> ' + button.text,
                        pieStyle: button.pieStyle,
                        handler: Ext.bind(function (btn) {
                            Ext.Array.each(this.down('#highchart').getDockedItems('toolbar[dock="top"]')[0].query('button'), function (_button) {
                                _button.setPressed(false);
                            });
                            btn.setPressed(true);
                            //this.chart.destroy();
                            entry.pieStyle = btn.pieStyle;
                            this.chart = Ung.charts.categoriesChart(entry, this.chartData, this.down('#highchart').body, false);
                        }, this)
                    });
                }
                this.down('#highchart').addDocked({
                    xtype: 'toolbar',
                    dock: 'top',
                    border: false,
                    items: chartTypeToolbar
                });
            }

            if (entry.type === 'TIME_GRAPH' || entry.type === 'TIME_GRAPH_DYNAMIC') {
                var timeStyleButtons = [
                    {timeStyle: 'LINE', icon: 'show_chart', text: i18n._('Line')},
                    {timeStyle: 'AREA', icon: 'show_chart', text: i18n._('Area')},
                    {timeStyle: 'AREA_STACKED', icon: 'show_chart', text: i18n._('Stacked Area')},
                    {timeStyle: 'BAR', icon: 'insert_chart', text: i18n._('Grouped Columns')},
                    {timeStyle: 'BAR_OVERLAPPED', icon: 'insert_chart', text: i18n._('Overlapped Columns')},
                    {timeStyle: 'BAR_STACKED', icon: 'insert_chart', text: i18n._('Stacked Columns')}
                ];

                for (i = 0; i < timeStyleButtons.length; i += 1) {
                    button = timeStyleButtons[i];
                    chartTypeToolbar.push({
                        xtype: 'button',
                        pressed: entry.timeStyle === button.timeStyle,
                        //iconCls: timeStyle.iconCls,
                        text: '<i class="material-icons">' + button.icon + '</i> ' + button.text,
                        timeStyle: button.timeStyle,
                        handler: Ext.bind(function (btn) {
                            Ext.Array.each(this.down('#highchart').getDockedItems('toolbar[dock="top"]')[0].query('button'), function (_button) {
                                _button.setPressed(false);
                            });
                            btn.setPressed(true);
                            entry.timeStyle = btn.timeStyle;
                            //this.chart = Ung.charts.timeSeriesChart(entry, this.chartData, this.down('#highchart').body, false, false);
                            Ung.charts.updateSeriesType(entry, this.chart);
                        }, this)
                    });
                }
                this.down('#highchart').addDocked({
                    xtype: 'toolbar',
                    dock: 'top',
                    border: false,
                    items: chartTypeToolbar
                });
            }
        }

        /*
         if (this.chart) {
         this.chart.destroy();
         this.chart = null;
         Highcharts.charts = Highcharts.charts.filter(function (n) { return n != undefined; });
         }
         */

        this.reportPanel.setActiveItem('reportContainer');
        this.reportContainer.setTitle({
            text: '<span class="ttl">' + i18n._(entry.title) + '</span>' + '<br/>' + '<span class="dsc">' + i18n._(entry.description) + '</span>',
            border: false,
            padding: '5px'
        });

        this.setLoading(i18n._('Loading report...'));
        this.reportData.getStore().loadData([]);

        rpc.reportsManager.getDataForReportEntry(Ext.bind(function (result, exception) {
            this.setLoading(false);
            if (Ung.Util.handleException(exception)) {
                return;
            }
            this.chartData = result.list;

            // add a new time prop because the datagrid alters the time_trunc, causing charting issues
            for (var i = 0; i < this.chartData.length; i += 1) {
                if (this.chartData[i].time_trunc) {
                    if ( this.chartData[i].time_trunc.time != null )
                        this.chartData[i].time = this.chartData[i].time_trunc.time;
                    else
                        this.chartData[i].time = this.chartData[i].time_trunc;
                }
            }

            switch (entry.type) {
                case 'TEXT':
                    break;
                case 'TIME_GRAPH':
                case 'TIME_GRAPH_DYNAMIC':
                    this.chart = Ung.charts.timeSeriesChart(entry, result.list, this.down('#highchart').body, false, false);
                    break;
                default:
                    this.chart = Ung.charts.categoriesChart(entry, result.list, this.down('#highchart').body, false, false);
            }
            this.loadReportData(this.chartData);
        }, this), entry, this.startDateWindow.serverDate, this.endDateWindow.serverDate, this.extraConditions, -1);
        Ung.TableConfig.getColumnsForTable(entry.table, this.extraConditionsPanel.columnsStore);
    },

    loadReportData: function (data) {
        var i, column;
        if (!this.entry) {
            return;
        }

        if (this.entry.type === 'TEXT') {
            this.reportData.setColumns([{
                dataIndex: 'data',
                header: i18n._("data"),
                width: 100,
                flex: 1
            }, {
                dataIndex: 'value',
                header: i18n._("value"),
                width: 100
            }]);

            var infos = [], reportData = [], value;
            if (data.length > 0 && this.entry.textColumns != null) {
                for (i = 0; i < this.entry.textColumns.length; i += 1) {
                    column = this.entry.textColumns[i].split(" ").splice(-1)[0];
                    value = Ext.isEmpty(data[0][column]) ? 0 : data[0][column];
                    infos.push(value);
                    reportData.push({data: column, value: value});
                }
            }

            this.reportChart.down('#textentry').update(Ext.String.format.apply(Ext.String.format, [i18n._(this.entry.textString)].concat(infos)));
            /*
             this.reportChart.add({
             xtype: 'component',
             itemId: 'text-entry',
             html: Ext.String.format.apply(Ext.String.format, [i18n._(this.entry.textString)].concat(infos)),

             });
             */
            this.reportData.getStore().loadData(reportData);
        }

        if (this.entry.type === 'PIE_GRAPH') {
            this.reportData.setColumns([{
                dataIndex: this.entry.pieGroupColumn,
                header: this.entry.pieGroupColumn,
                width: 100,
                flex: 1
            }, {
                dataIndex: 'value',
                header: i18n._("value"),
                width: 100
            }, {
                xtype: 'actioncolumn',
                menuDisabled: true,
                width: 20,
                items: [{
                    iconCls: 'icon-row icon-filter',
                    tooltip: i18n._('Add Condition'),
                    handler: Ext.bind(function (view, rowIndex, colIndex, item, e, record) {
                        this.buildWindowAddCondition();
                        data = {
                            column: this.entry.pieGroupColumn,
                            operator: "=",
                            value: record.get(this.entry.pieGroupColumn)
                        };
                        this.windowAddCondition.setCondition(data);
                    }, this)
                }]
            }]);
            this.reportData.getStore().loadData(data);
        }

        if (this.entry.type === 'TIME_GRAPH' || this.entry.type === 'TIME_GRAPH_DYNAMIC') {
            var zeroFn = function (val) {
                return (val == null) ? 0 : val;
            };
            var timeFn = function (val) {
                return (val == null) ? 0 : i18n.timestampFormat(val);
            };

            var storeFields = [{name: 'time_trunc', convert: timeFn}];

            var reportDataColumns = [{
                dataIndex: 'time_trunc',
                header: i18n._("Timestamp"),
                width: 130,
                flex: this.entry.timeDataColumns.length > 2 ? 0 : 1,
                sorter: function (rec1, rec2) {
                    var t1, t2;
                    if ( rec1.getData().time != null && rec2.getData().time != null ) {
                        t1 = rec1.getData().time;
                        t2 = rec2.getData().time;
                    } else {
                        t1 = rec1.getData();
                        t2 = rec2.getData();
                    }
                    return (t1 > t2) ? 1 : (t1 === t2) ? 0 : -1;
                }
            }];
            var seriesRenderer = null, title;
            if (!Ext.isEmpty(this.entry.seriesRenderer)) {
                seriesRenderer =  Ung.panel.Reports.getColumnRenderer(this.entry.seriesRenderer);
            }

            for (i = 0; i < this.entry.timeDataColumns.length; i += 1) {
                column = this.entry.timeDataColumns[i].split(" ").splice(-1)[0];
                title = seriesRenderer ? seriesRenderer(column) + ' [' + column + ']' : column;
                storeFields.push({name: column, convert: zeroFn, type: 'integer'});
                reportDataColumns.push({
                    dataIndex: column,
                    header: title,
                    width: this.entry.timeDataColumns.length > 2 ? 60 : 90
                });
            }

            this.reportData.setStore(Ext.create('Ext.data.Store', {
                fields: storeFields,
                data: []
            }));
            this.reportData.setColumns(reportDataColumns);
            this.reportData.getStore().loadData(data);
        }
    },

    loadEventEntry: function (entry) {
        var store, tableConfig, state, i, col;

        /*
         if (this.chart) {
         this.chart.destroy();
         this.chart = null;
         }
         */

        this.reportPanel.setActiveItem('eventContainer');

        //this.down('[name=northEntriesBtn]').setText('<i class="material-icons" style="font-size: 20px;">' + this.setEntryIcon(entry) + '</i> <span>' + entry.title + '</span>');

        if (!entry.defaultColumns) {
            entry.defaultColumns = [];
        }

        this.eventContainer.setTitle({
            text: '<span class="ttl">' + entry.title + '</span>' + '<br/>' + '<span class="dsc">' + entry.description + '</span>',
            border: false,
            padding: '5px'
        });
        //this.eventContainer.stateId = 'eventGrid-' + (entry.category ? (entry.category.toLowerCase().replace(' ', '_') + '-') : '') + entry.table;

        tableConfig = Ext.clone(Ung.TableConfig.getConfig(entry.table));

        if (!tableConfig) {
            console.log('Warning: table "' + entry.table + '" is not defined');
            tableConfig = {
                fields: [],
                columns: []
            };
        } else {
            var columnsNames = {};
            for (i = 0; i < tableConfig.columns.length; i += 1) {
                col = tableConfig.columns[i];
                columnsNames[col.dataIndex] = true;
                if ((entry.defaultColumns.length > 0) && (entry.defaultColumns.indexOf(col.dataIndex) < 0)) {
                    col.hidden = true;
                }
                if (col.stateId === undefined) {
                    col.stateId = col.dataIndex;
                }
            }
            for (i = 0; i < entry.defaultColumns.length; i += 1) {
                col = entry.defaultColumns[i];
                if (!columnsNames[col]) {
                    console.log('Warning: column "' + col + '" is not defined in the tableConfig for ' + entry.table);
                }
            }
            this.eventContainer.defaultTableConfig = tableConfig;
        }

        store = Ext.create('Ext.data.Store', {
            fields: tableConfig.fields,
            data: [],
            proxy: {
                type: 'memory',
                reader: {
                    type: 'json'
                }
            }
        });

        this.eventContainer.reconfigure(store, tableConfig.columns);

        state = Ext.state.Manager.get(this.eventContainer.stateId);
        if (state && state.columns !== undefined) {
            // Performing a state restore to a dynamic grid is very picky.
            // If you decide to revisit, see the test procedures in
            // https://bugzilla.untangle.com/show_bug.cgi?id=12594
            Ext.suspendLayouts();
            this.eventContainer.getView().getHeaderCt().purgeCache();
            this.eventContainer.applyState(state);
            this.eventContainer.updateLayout();

            Ext.each(this.eventContainer.getColumns(), function (column, index) {
                if (column.hidden == true) {
                    column.setVisible(true);
                    column.setVisible(false);
                } else {
                    column.setVisible(false);
                    column.setVisible(true);
                }
            });
            Ext.resumeLayouts(true);
        }

        this.eventContainer.getStore().addFilter(this.filterFeature.globalFilter);
        this.refreshHandler();
        Ung.TableConfig.getColumnsForTable(entry.table, this.extraConditionsPanel.columnsStore);
    },

    refreshHandler: function () {
        if (this.autoRefreshEnabled) {
            return;
        }
        this.refreshEntry();
    },
    autoRefresh: function () {
        if (!this.autoRefreshEnabled) {
            return;
        }
        this.refreshEntry();
    },
    refreshEntry: function () {
        if (!this.entry) {
            return;
        }
        if (this.entry.type === 'EVENT_LIST') {
            this.refreshEvents();
        } else {
            this.refreshReportData();
        }
    },
    refreshReportData: function () {
        if (!this.entry) {
            return;
        }
        if (!this.autoRefreshEnabled) { this.reportContainer.setLoading(i18n._('Refreshing report...')); }
        rpc.reportsManager.getDataForReportEntry(Ext.bind(function (result, exception) {
            this.reportContainer.setLoading(false);
            if (Ung.Util.handleException(exception)) {
                return;
            }
            var i;
            this.chartData = result.list;

            if (this.entry.type === 'TIME_GRAPH' || this.entry.type === 'TIME_GRAPH_DYNAMIC') {
                // add a new time prop because the datagrid alters the time_trunc, causing charting issues
                for (i = 0; i < this.chartData.length; i += 1) {
                    if ( this.chartData[i].time_trunc.time != null )
                        this.chartData[i].time = this.chartData[i].time_trunc.time;
                    else
                        this.chartData[i].time = this.chartData[i].time_trunc;
                }
                this.chart = Ung.charts.timeSeriesChart(this.entry, this.chartData, this.down('#highchart').body, false, false);
            } else {
                Ung.charts.setCategoriesSeries(this.entry, this.chartData, this.chart);
            }

            if (this != null && this.rendered && this.autoRefreshEnabled) {
                Ext.Function.defer(this.autoRefresh, this.autoRefreshInterval * 1000, this);
            }

            if (this.entry.type !== 'EVENT_LIST') {
                this.loadReportData(this.chartData);
            }
        }, this), this.entry, this.startDateWindow.serverDate, this.endDateWindow.serverDate, this.extraConditions, -1);
    },
    refreshEvents: function () {
        if (!this.entry) {
            return;
        }
        var limit = this.limitSelector.getValue();
        if (!this.autoRefreshEnabled) { this.setLoading(i18n._('Querying Database...')); }
        rpc.reportsManager.getEventsForDateRangeResultSet(Ext.bind(function (result, exception) {
            this.setLoading(false);
            if (Ung.Util.handleException(exception)) {
                return;
            }
            this.loadResultSet(result);
        }, this), this.entry, this.extraConditions, limit, this.startDateWindow.serverDate, this.endDateWindow.serverDate);
    },
    startAutoRefresh: function (setButton) {
        if (!this.entry) {
            this.down('button[name=auto_refresh]').toggle(false);
            return;
        }

        this.autoRefreshEnabled = true;
        this.down('button[name=refresh]').disable();
        this.autoRefresh();
    },
    stopAutoRefresh: function (setButton) {
        this.autoRefreshEnabled = false;
        if (setButton) {
            this.down('button[name=auto_refresh]').toggle(false);
        }
        this.down('button[name=refresh]').enable();
    },

    loadNextChunkCallback: function (result, exception) {
        if (Ung.Util.handleException(exception)) {
            return;
        }
        var newEvents = result;
        // If we got results append them to the current events list, and make another call for more
        if (newEvents != null && newEvents.list != null && newEvents.list.length != 0) {
            this.events.push.apply(this.events, newEvents.list);
            if (!this.autoRefreshEnabled) { this.setLoading(i18n._('Fetching Events...') + ' (' + this.events.length + ')'); }
            this.reader.getNextChunk(Ext.bind(this.loadNextChunkCallback, this), 1000);
            return;
        }
        // If we got here, then we either reached the end of the resultSet or ran out of room display the results
        if (this.eventContainer != null && this.eventContainer.getStore() != null) {
            this.eventContainer.getStore().getProxy().setData(this.events);
            this.eventContainer.getStore().load();
        }
        this.setLoading(false);

        if (this != null && this.rendered && this.autoRefreshEnabled) {
            Ext.Function.defer(this.autoRefresh, this.autoRefreshInterval * 1000, this);
        }
    },

    loadResultSet: function (result) {
        var i;
        this.events = [];

        if (testMode) {
            //var emptyRec = {};
            var length = Math.floor((Math.random() * 5000));
            var fields = this.eventContainer.getStore().getModel().getFields();
            for (i = 0; i < length; i += 1) {
                this.events.push(this.getTestRecord(i, fields));
            }
        }

        this.reader = result;
        if (this.reader) {
            if (!this.autoRefreshEnabled) { this.setLoading(i18n._('Fetching Events...')); }
            this.reader.getNextChunk(Ext.bind(this.loadNextChunkCallback, this), 1000);
        } else {
            this.loadNextChunkCallback(null);
        }
    },

    getColumnList: function () {
        var columns = this.eventContainer.getColumns(), columnList = '', i;
        for (i = 0; i < columns.length; i += 1) {
            if (i !== 0) {
                columnList += ",";
            }
            if (columns[i].dataIndex != null) {
                columnList += columns[i].dataIndex;
            }
        }
        return columnList;
    },

    exportReportDataHandler: function () {
        if (!this.entry) {
            return;
        }
        var processRow = function (row) {
            var data = [], j, innerValue;
            for (j = 0; j < row.length; j += 1) {
                innerValue = row[j] == null ? '' : row[j].toString();
                data.push('"' + innerValue.replace(/"/g, '""') + '"');
            }
            return data.join(",") + '\r\n';
        };

        var records = this.reportData.getStore().getRange(), list = [], columns = [], headers = [], i, j, row;
        var gridColumns = this.reportData.getColumns();
        for (i = 0; i < gridColumns.length; i += 1) {
            if (gridColumns[i].initialConfig.dataIndex) {
                columns.push(gridColumns[i].initialConfig.dataIndex);
                headers.push(gridColumns[i].initialConfig.header);
            }
        }
        list.push(processRow(headers));
        for (i = 0; i < records.length; i += 1) {
            row = [];
            for (j = 0; j < columns.length; j += 1) {
                row.push(records[i].get(columns[j]));
            }
            list.push(processRow(row));
        }
        var content = list.join('');
        var fileName = this.entry.title.trim().replace(/ /g, '_') + '.csv';
        Ung.Util.download(content, fileName, 'text/csv');
    },

    exportEventsHandler: function () {
        if (!this.entry) {
            return;
        }
        var startDate = this.startDateWindow.serverDate;
        var endDate = this.endDateWindow.serverDate;

        Ext.MessageBox.wait(i18n._("Exporting Events..."), i18n._("Please wait"));
        var name = this.entry.title.trim().replace(/ /g, '_');
        var downloadForm = document.getElementById('downloadForm');
        downloadForm['type'].value = "eventLogExport";
        downloadForm['arg1'].value = name;
        downloadForm['arg2'].value = Ext.encode(this.entry);
        downloadForm['arg3'].value = Ext.encode(this.extraConditions);
        downloadForm['arg4'].value = this.getColumnList();
        downloadForm['arg5'].value = startDate ? startDate.getTime() : -1;
        downloadForm['arg6'].value = endDate ? endDate.getTime() : -1;
        downloadForm.submit();
        Ext.MessageBox.hide();
    },

    buildWindowAddCondition: function () {
        var me = this;
        if (!this.windowAddCondition) {
            this.windowAddCondition = Ext.create("Ung.EditWindow", {
                title: i18n._("Add Condition"),
                grid: null,
                height: 150,
                width: 600,
                sizeToRack: false,
                // size to grid on show
                sizeToGrid: false,
                center: true,
                items: [{
                    xtype: "panel",
                    bodyStyle: 'padding:10px 10px 0px 10px;',
                    items: [{
                        xtype: "component",
                        margin: '0 0 10 0',
                        html: i18n._("Add a condition using report data:")
                    }, {
                        xtype: "container",
                        layout: "column",
                        defaults: {
                            margin: '0 10 0 0'
                        },
                        items: [{
                            xtype: "textfield",
                            name: "column",
                            width: 180,
                            readOnly: true
                        }, {
                            xtype: 'combo',
                            width: 90,
                            name: "operator",
                            editable: false,
                            valueField: "name",
                            displayField: "name",
                            queryMode: 'local',
                            value: "=",
                            store: ["=", "!=", ">", "<", ">=", "<=", "like", "not like", "is", "is not", "in", "not in"]
                        }, {
                            xtype: "textfield",
                            name: "value",
                            emptyText: i18n._("[no value]"),
                            width: 180
                        }]
                    }]
                }],
                updateAction: function () {
                    var data = {
                        column: this.down("[name=column]").getValue(),
                        operator: this.down("[name=operator]").getValue(),
                        value: this.down("[name=value]").getValue()
                    };
                    me.extraConditionsPanel.expand();
                    me.extraConditionsPanel.fillCondition(data);
                    this.cancelAction();
                },
                setCondition: function (data) {
                    this.show();
                    this.down("[name=column]").setValue(data.column);
                    this.down("[name=operator]").setValue(data.operator);
                    this.down("[name=value]").setValue(data.value);
                },
                isDirty: function () {
                    return false;
                },
                closeWindow: function () {
                    this.hide();
                }
            });
            this.subCmps.push(this.windowAddCondition);
        }
    },

    customizeReport: function () {
        if (!this.entry) {
            return;
        }
        if (!this.winReportEditor) {
            var me = this;
            this.winReportEditor = Ext.create('Ung.window.ReportEditor', {
                sizeToComponent: this.reportPanel,
                title: i18n._("Advanced report customization"),
                parentCmp: this,
                entry: this.entry,
                grid: {
                    //reconfigure: function () {}
                },
                isDirty: function () {
                    return false;
                },
                updateAction: function () {
                    Ung.window.ReportEditor.prototype.updateAction.apply(this, arguments);
                    me.entry = this.record.getData();
                    me.loadReportEntry(me.entry);
                }
            });
            this.subCmps.push(this.winReportEditor);
        }
        var record = Ext.create('Ext.data.Model', this.entry);
        this.winReportEditor.populate(record);
        this.winReportEditor.show();
    },

    downloadReport: function () {
        if (!this.chart) {
            return;
        }
        this.chart.exportChart();
    },

    removeReport: function (record) {
        if (!this.entry) {
            return;
        }
        var i;
        // remove widget from dashboard if exists
        if (record.getData().inDashboard) {
            for (i = 0; i < this.dashboardWidgets.length; i += 1) {
                if (this.dashboardWidgets[i].entryId === this.entry.uniqueId) {
                    this.dashboardWidgets.splice(i, 1);
                }
            }
            this.dashboardSettings.widgets = {
                javaClass: 'java.util.LinkedList',
                list: this.dashboardWidgets
            };
            rpc.dashboardManager.setSettings(Ext.bind(function (result, exception) {
                if (Ung.Util.handleException(exception)) {
                    return;
                }
                Ung.dashboard.reportEntriesModified = true;
            }, this), this.dashboardSettings);
        }

        this.reportContainer.setLoading('<span style="color: red; font-weight: bold;">' + i18n._('Deleting report') + ':</span> ' + this.entry.title + '...');
        Ung.Main.getReportsManager().removeReportEntry(Ext.bind(function (result, exception) {
            this.reportContainer.setLoading(false);
            if (Ung.Util.handleException(exception)) {
                return;
            }
            Ung.dashboard.reportEntriesModified = true;
            this.initEntry = null;
            this.reloadReports();
            Ung.Util.userActionToast('<span style="color: #FFF;">' + this.entry.title + '</span> ' + i18n._('deleted successfully'));
        }, this), this.entry);

    },

    viewEventsForReport: function () {
        if (!this.entry) {
            return;
        }
        this.entry = Ext.clone(this.entry);
        this.entry.type = 'EVENT_LIST';
        this.loadEventEntry(this.entry);
    },

    statics: {
        isEvent: function (entry) {
            return "com.untangle.app.reports.EventEntry" == entry.javaClass;
        },
        getColumnRenderer: function (columnName) {
            if (!this.columnRenderers) {
                this.columnRenderers = {
                    "policy_id": function (value) {
                        if (Ung.Main.webuiMode) {
                            var name = Ung.Main.getPolicyName(value);
                            if (name != null) {
                                return name;
                            }
                            return value;
                        }
                        return value;
                    },
                    "protocol": function (value) {
                        if (!Ung.panel.Reports.protocolMap) {
                            Ung.panel.Reports.protocolMap = {
                                0: "HOPOPT (0)",
                                1: "ICMP (1)",
                                2: "IGMP (2)",
                                3: "GGP (3)",
                                4: "IP-in-IP (4)",
                                5: "ST (5)",
                                6: "TCP (6)",
                                7: "CBT (7)",
                                8: "EGP (8)",
                                9: "IGP (9)",
                                10: "BBN-RCC-MON (10)",
                                11: "NVP-II (11)",
                                12: "PUP (12)",
                                13: "ARGUS (13)",
                                14: "EMCON (14)",
                                15: "XNET (15)",
                                16: "CHAOS (16)",
                                17: "UDP (17)",
                                18: "MUX (18)",
                                19: "DCN-MEAS (19)",
                                20: "HMP (20)",
                                21: "PRM (21)",
                                22: "XNS-IDP (22)",
                                23: "TRUNK-1 (23)",
                                24: "TRUNK-2 (24)",
                                25: "LEAF-1 (25)",
                                26: "LEAF-2 (26)",
                                27: "RDP (27)",
                                28: "IRTP (28)",
                                29: "ISO-TP4 (29)",
                                30: "NETBLT (30)",
                                31: "MFE-NSP (31)",
                                32: "MERIT-INP (32)",
                                33: "DCCP (33)",
                                34: "3PC (34)",
                                35: "IDPR (35)",
                                36: "XTP (36)",
                                37: "DDP (37)",
                                38: "IDPR-CMTP (38)",
                                39: "TP++ (39)",
                                40: "IL (40)",
                                41: "IPv6 (41)",
                                42: "SDRP (42)",
                                43: "IPv6-Route (43)",
                                44: "IPv6-Frag (44)",
                                45: "IDRP (45)",
                                46: "RSVP (46)",
                                47: "GRE (47)",
                                48: "MHRP (48)",
                                49: "BNA (49)",
                                50: "ESP (50)",
                                51: "AH (51)",
                                52: "I-NLSP (52)",
                                53: "SWIPE (53)",
                                54: "NARP (54)",
                                55: "MOBILE (55)",
                                56: "TLSP (56)",
                                57: "SKIP (57)",
                                58: "IPv6-ICMP (58)",
                                59: "IPv6-NoNxt (59)",
                                60: "IPv6-Opts (60)",
                                62: "CFTP (62)",
                                64: "SAT-EXPAK (64)",
                                65: "KRYPTOLAN (65)",
                                66: "RVD (66)",
                                67: "IPPC (67)",
                                69: "SAT-MON (69)",
                                70: "VISA (70)",
                                71: "IPCU (71)",
                                72: "CPNX (72)",
                                73: "CPHB (73)",
                                74: "WSN (74)",
                                75: "PVP (75)",
                                76: "BR-SAT-MON (76)",
                                77: "SUN-ND (77)",
                                78: "WB-MON (78)",
                                79: "WB-EXPAK (79)",
                                80: "ISO-IP (80)",
                                81: "VMTP (81)",
                                82: "SECURE-VMTP (82)",
                                83: "VINES (83)",
                                84: "TTP (84)",
                                85: "NSFNET-IGP (85)",
                                86: "DGP (86)",
                                87: "TCF (87)",
                                88: "EIGRP (88)",
                                89: "OSPF (89)",
                                90: "Sprite-RPC (90)",
                                91: "LARP (91)",
                                92: "MTP (92)",
                                93: "AX.25 (93)",
                                94: "IPIP (94)",
                                95: "MICP (95)",
                                96: "SCC-SP (96)",
                                97: "ETHERIP (97)",
                                98: "ENCAP (98)",
                                100: "GMTP (100)",
                                101: "IFMP (101)",
                                102: "PNNI (102)",
                                103: "PIM (103)",
                                104: "ARIS (104)",
                                105: "SCPS (105)",
                                106: "QNX (106)",
                                107: "A/N (107)",
                                108: "IPComp (108)",
                                109: "SNP (109)",
                                110: "Compaq-Peer (110)",
                                111: "IPX-in-IP (111)",
                                112: "VRRP (112)",
                                113: "PGM (113)",
                                115: "L2TP (115)",
                                116: "DDX (116)",
                                117: "IATP (117)",
                                118: "STP (118)",
                                119: "SRP (119)",
                                120: "UTI (120)",
                                121: "SMP (121)",
                                122: "SM (122)",
                                123: "PTP (123)",
                                124: "IS-IS (124)",
                                125: "FIRE (125)",
                                126: "CRTP (126)",
                                127: "CRUDP (127)",
                                128: "SSCOPMCE (128)",
                                129: "IPLT (129)",
                                130: "SPS (130)",
                                131: "PIPE (131)",
                                132: "SCTP (132)",
                                133: "FC (133)",
                                134: "RSVP-E2E-IGNORE (134)",
                                135: "Mobility (135)",
                                136: "UDPLite (136)",
                                137: "MPLS-in-IP (137)",
                                138: "manet (138)",
                                139: "HIP (139)",
                                140: "Shim6 (140)",
                                141: "WESP (141)",
                                142: "ROHC (142)"
                            };
                        }
                        return (value != null) ? Ung.panel.Reports.protocolMap[value] || value.toString() : "";
                    },
                    "interface": function (value) {
                        if (!Ung.panel.Reports.interfaceMap) {
                            var interfacesList = [], i;
                            try {
                                interfacesList = rpc.reportsManager.getInterfacesInfo().list;
                            } catch (e) {
                                Ung.Util.rpcExHandler(e);
                            }
                            interfacesList.push({ interfaceId: 250, name: "OpenVPN" }); // 0xfa
                            interfacesList.push({ interfaceId: 251, name: "L2TP" }); // 0xfb
                            interfacesList.push({ interfaceId: 252, name: "Xauth" }); // 0xfc
                            interfacesList.push({ interfaceId: 253, name: "GRE" }); // 0xfd
                            Ung.panel.Reports.interfaceMap = {};
                            for (i = 0; i < interfacesList.length; i += 1) {
                                Ung.panel.Reports.interfaceMap[interfacesList[i].interfaceId] = interfacesList[i].name;
                            }
                        }
                        return (value != null) ? Ung.panel.Reports.interfaceMap[value] || value.toString() : "";
                    }
                };
            }
            return this.columnRenderers[columnName];
        }
    }
});

Ext.define('Ung.grid.ReportItemModel', {
    extend: 'Ext.data.Model',
    fields: [
        {name: 'text',  type: 'string'},
        {name: 'icon', type: 'string'}
    ]
});

Ext.define('Ung.panel.ReportCategorySelector', {
    extend: 'Ext.panel.Panel',
    cls: 'report-category',
    margin: '5',
    padding: '5',
    height: 250,
    border: false,
    bodyStyle: {
        background: 'transparent'
    },
    initComponent: function () {
        this.callParent(arguments);

        var me = this, limit = this.reports.length > 5 ? 5 : this.reports.length, i;

        this.add({
            xtype: 'component',
            autoEl: {
                tag: 'h3',
                html: '<img src="' + this.category.getData().icon + '"/> ' + this.category.getData().text
            }
        });

        for (i = 0; i < limit; i += 1) {
            this.add({
                xtype: 'component',
                idx: i,
                autoEl: {
                    tag: 'a',
                    href: '#',
                    html: '<i class="material-icons">' + this.reports[i].icon + '</i><span>' + i18n._(this.reports[i].title) + '</span>',
                    'data-index': i
                },
                listeners: {
                    click: {
                        element: 'el',
                        fn: function (e) {
                            e.preventDefault();
                            e.stopPropagation();
                            me.parentCt.categoryList.getSelectionModel().select(me.category);
                            me.parentCt.entryList.getSelectionModel().select(this.component.idx);
                            return false;
                        }
                    }
                }
            });
        }

        if (this.reports.length > 5) {
            this.add({
                xtype: 'component',
                cls: 'view-all',
                //category: this.category,
                autoEl: {
                    tag: 'a',
                    href: '#',
                    html: (this.reports.length - 5) + ' ' + i18n._('more') + ' ...'
                },
                listeners: {
                    click: {
                        element: 'el',
                        fn: function (e) {
                            e.preventDefault();
                            e.stopPropagation();
                            me.parentCt.categoryList.getSelectionModel().select(me.category);
                            return false;
                        }
                    }
                }
            });
        }
    }
});

Ext.define("Ung.grid.feature.GlobalFilter", {
    extend: "Ext.grid.feature.Feature",
    useVisibleColumns: true,
    useFields: null,
    init: function (grid) {
        this.grid = grid;

        this.globalFilter = Ext.create('Ext.util.Filter', {
            regExpProtect: /\\|\/|\+|\\|\.|\[|\]|\{|\}|\?|\$|\*|\^|\|/gm,
            disabled: true,
            regExpMode: false,
            caseSensitive: false,
            regExp: null,
            stateId: 'globalFilter',
            searchFields: {},
            filterFn: function (record) {
                if (!this.regExp) {
                    return true;
                }
                var datas = record.getData(), key, val;
                for (key in this.searchFields) {
                    if (datas[key] !== undefined) {
                        val = datas[key];
                        if (val == null) {
                            continue;
                        }
                        if (typeof val == 'boolean' || typeof val == 'number') {
                            val = val.toString();
                        } else if (typeof val == 'object') {
                            if (val.time != null) {
                                val = i18n.timestampFormat(val);
                            }
                        }
                        if (typeof val == 'string') {
                            if (this.regExp.test(val)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            },
            getSearchValue: function (value) {
                if (value === '' || value === '^' || value === '$') {
                    return null;
                }
                if (!this.regExpMode) {
                    value = value.replace(this.regExpProtect, function (m) {
                        return '\\' + m;
                    });
                } else {
                    try {
                        new RegExp(value);
                    } catch (error) {
                        return null;
                    }
                }
                return value;
            },
            buildSearch: function (value, caseSensitive, searchFields) {
                this.searchFields = searchFields;
                this.setCaseSensitive(caseSensitive);
                var searchValue = this.getSearchValue(value);
                this.regExp = searchValue == null ? null : new RegExp(searchValue, 'g' + (caseSensitive ? '' : 'i'));
                this.setDisabled(this.regExp == null);
            }
        });

        this.grid.on("afterrender", Ext.bind(function () {
            this.grid.getStore().addFilter(this.globalFilter);
        }, this));
        this.grid.on("beforedestroy", Ext.bind(function () {
            this.grid.getStore().removeFilter(this.globalFilter);
            Ext.destroy(this.globalFilter);
        }, this));
        this.callParent(arguments);
    },
    updateGlobalFilter: function (value, caseSensitive) {
        var searchFields = {}, i, col;
        if (this.useVisibleColumns) {
            var visibleColumns = this.grid.getVisibleColumns();
            for (i = 0; i < visibleColumns.length; i += 1) {
                col = visibleColumns[i];
                if (col.dataIndex) {
                    searchFields[col.dataIndex] = true;
                }
            }
        } else if (this.searchFields != null) {
            for (i = 0; i < this.searchFields.length; i += 1) {
                searchFields[this.searchFields[i]] = true;
            }
        }
        this.globalFilter.buildSearch(value, caseSensitive, searchFields);
        this.grid.getStore().getFilters().notify('endupdate');
    }
});

Ext.define("Ung.panel.ExtraConditions", {
    extend: "Ext.panel.Panel",
    collapsible: true,
    collapsed: false,
    floatable: false,
    split: true,
    region: 'south',
    defaultCount: 1,
    autoScroll: true,
    layout: { type: 'vbox'},
    initComponent: function () {
        var i;
        this.title = Ext.String.format(i18n._("Conditions: {0}"), i18n._("None"));
        this.collapsed = Ung.Main.viewport.getHeight() < 500;
        this.columnsStore = Ext.create('Ext.data.Store', {
            sorters: "header",
            fields: ["dataIndex", "header"],
            data: []
        });
        this.items = [];
        for (i = 0; i < this.defaultCount; i += 1) {
            this.items.push(this.generateRow());
        }
        var quickAddMenu = Ext.create('Ext.menu.Menu');
        var addQuickCondition = Ext.bind(function (item) {
            this.fillCondition({
                column: item.column,
                operator: "=",
                value: item.value
            });
        }, this);
        rpc.reportsManager.getConditionQuickAddHints(Ext.bind(function (result, exception) {
            if (Ung.Util.handleException(exception)) {
                return;
            }
            if (!this.getEl()) {
                return;
            }
            var column, hintMenus = [], columnItems, i, text, value, columnRenderer, values;
            for (column in result) {
                values = result[column];
                if (values.length > 0) {
                    columnItems = [];
                    columnRenderer = Ung.panel.Reports.getColumnRenderer(column);
                    for (i = 0; i < values.length; i += 1) {
                        columnItems.push({
                            text: Ext.isFunction(columnRenderer) ? columnRenderer(values[i]) : values[i],
                            column: column,
                            value: values[i],
                            handler: addQuickCondition
                        });
                    }
                    hintMenus.push({
                        text: Ung.TableConfig.getColumnHumanReadableName(column),
                        menu: {
                            items: columnItems
                        }
                    });
                }
            }
            quickAddMenu.add(hintMenus);

        }, this));
        this.tbar = [{
            text: i18n._("Add Condition"),
            tooltip: i18n._('Add New Condition'),
            iconCls: 'icon-add-row',
            handler: function () {
                this.addRow();
            },
            scope: this
        }, {
            text: i18n._('Quick Add'),
            iconCls: 'icon-add-row',
            menu: quickAddMenu
        }, '->', {
            text: i18n._("Delete All"),
            tooltip: i18n._('Delete All Conditions'),
            iconCls: 'cancel-icon',
            handler: function () {
                this.deleteConditions();
            },
            scope: this
        }];
        this.callParent(arguments);
    },
    generateRow: function (data) {
        if (!data) {
            data = {column: "", operator: "=", value: ""};
        }
        return {
            xtype: 'container',
            layout: 'column',
            name: 'condition',
            width: '100%',
            defaults: {
                margin: 3
            },
            items: [{
                xtype: 'combo',
                columnWidth: 0.4,
                emptyText: i18n._("[enter column]"),
                dataIndex: "column",
                typeAhead: true,
                valueField: "dataIndex",
                queryMode: 'local',
                store: this.columnsStore,
                value: data.column,
                listConfig: {
                    minWidth: 520
                },
                tpl: Ext.create('Ext.XTemplate',
                    '<ul class="x-list-plain"><tpl for=".">',
                    '<li role="option" class="x-boundlist-item"><b>{header}</b> <span style="float: right;">[{dataIndex}]</span></li>',
                    '</tpl></ul>'
                ),
                // template for the content inside text field
                displayTpl: Ext.create('Ext.XTemplate',
                    '<tpl for=".">',
                    '{header} [{dataIndex}]',
                    '</tpl>'
                ),
                listeners: {
                    change: {
                        fn: function (combo, newValue, oldValue, opts) {
                            this.setConditions();
                        },
                        scope: this
                    },
                    blur: {
                        fn: function (field, e) {
                            //var skipReload = Ext.isEmpty(field.next("[dataIndex=value]").getValue());
                            this.setConditions();
                        },
                        scope: this
                    },
                    specialkey: {
                        fn: function (field, e) {
                            if (e.getKey() == e.ENTER) {
                                this.setConditions();
                            }
                        },
                        scope: this
                    }
                }
            }, {
                xtype: 'combo',
                width: 100,
                dataIndex: "operator",
                editable: false,
                valueField: "name",
                displayField: "name",
                queryMode: 'local',
                value: data.operator,
                disabled: Ext.isEmpty(data.column),
                store: ["=", "!=", ">", "<", ">=", "<=", "like", "not like", "is", "is not", "in", "not in"],
                listeners: {
                    change: {
                        fn: function (combo, newValue, oldValue, opts) {
                            var skipReload = Ext.isEmpty(combo.next("[dataIndex=value]").getValue());
                            this.setConditions(skipReload);
                        },
                        scope: this
                    }
                }
            }, {
                xtype: 'textfield',
                dataIndex: "value",
                columnWidth: 0.6,
                disabled: Ext.isEmpty(data.column),
                emptyText: i18n._("[no value]"),
                value: data.value,
                listeners: {
                    blur: {
                        fn: function (field, e) {
                            this.setConditions();
                        },
                        scope: this
                    },
                    specialkey: {
                        fn: function (field, e) {
                            if (e.getKey() == e.ENTER) {
                                this.setConditions();
                            }
                        },
                        scope: this
                    }
                }
            }, {
                xtype: 'button',
                name: "delete",
                text: i18n._("Delete"),
                handler: Ext.bind(function (button) {
                    var skipReload = Ext.isEmpty(button.prev("[dataIndex=column]").getValue());
                    this.remove(button.up("container"));
                    this.setConditions(skipReload);
                }, this)
            }]
        };
    },
    addRow: function (data) {
        this.add(this.generateRow(data));
    },
    fillCondition: function (data) {
        var added = false;
        this.bulkOperation = true;
        Ext.Array.each(this.query("container[name=condition]"), function (item, index, len) {
            if (Ext.isEmpty(item.down("[dataIndex=column]").getValue())) {
                item.down("[dataIndex=column]").setValue(data.column);
                item.down("[dataIndex=operator]").setValue(data.operator);
                item.down("[dataIndex=value]").setValue(data.value);
                added = true;
                return false;
            }
        });
        if (!added) {
            this.addRow(data);
        }
        this.bulkOperation = false;
        this.setConditions();
    },
    deleteConditions: function () {
        var me = this;
        this.bulkOperation = true;
        Ext.Array.each(this.query("container[name=condition]"), function (item, index, len) {
            if (index < me.defaultCount) {
                item.down("[dataIndex=column]").setValue("");
                item.down("[dataIndex=operator]").setValue("=");
                item.down("[dataIndex=value]").setValue("");
            } else {
                me.remove(item);
            }
        });
        this.bulkOperation = false;
        //var skipReload = !this.parentPanel.extraConditions || this.parentPanel.extraConditions.length == 0;
        this.setConditions();
    },
    setConditions: function () {
        if (this.bulkOperation) {
            return;
        }
        var conditions = [], columnValue, operator, value, isEmptyColumn;
        Ext.Array.each(this.query("container[name=condition]"), function (item, index, len) {
            columnValue = item.down("[dataIndex=column]").getValue();
            operator = item.down("[dataIndex=operator]");
            value = item.down("[dataIndex=value]");
            isEmptyColumn = Ext.isEmpty(columnValue);
            if (!isEmptyColumn) {
                conditions.push({
                    "javaClass": "com.untangle.app.reports.SqlCondition",
                    "column": columnValue,
                    "operator": operator.getValue(),
                    "value": value.getValue()
                });
            }
            operator.setDisabled(isEmptyColumn);
            value.setDisabled(isEmptyColumn);
        });
        var encodedConditions = Ext.encode(conditions);
        if (this.currentConditions != encodedConditions) {
            this.currentConditions = encodedConditions;
            this.parentPanel.extraConditions = (conditions.length > 0) ? conditions : null;
            this.setTitle(Ext.String.format(i18n._("Conditions: {0}"), (conditions.length > 0) ? conditions.length : i18n._("None")));
        }
    },
    setValue: function (extraConditions) {
        var me = this, i;
        this.bulkOperation = true;
        Ext.Array.each(this.query("container[name=condition]"), function (item, index, len) {
            me.remove(item);
        });
        if (!extraConditions) {
            for (i = 0; i < this.defaultCount; i += 1) {
                this.addRow();
            }
        } else {
            for (i = 0; i < extraConditions.length; i += 1) {
                this.addRow(extraConditions[i]);
            }
        }
        this.bulkOperation = false;
    }

});

Ext.define("Ung.window.SelectDateTime", {
    extend: "Ext.window.Window",
    date: null,
    buttonObj: null,
    modal: true,
    closeAction: 'hide',
    initComponent: function () {
        this.items = [{
            xtype: 'textfield',
            name: 'dateAndTime',
            readOnly: true,
            hideLabel: true,
            width: 180,
            emptyText: this.dateTimeEmptyText
        }, {
            xtype: 'datepicker',
            name: 'date',
            handler: function (picker, date) {
                var timeValue = this.down("timefield[name=time]").getValue();
                if (timeValue != null) {
                    date.setHours(timeValue.getHours());
                    date.setMinutes(timeValue.getMinutes());
                }
                this.setDate(date);
            },
            scope: this
        }, {
            xtype: 'timefield',
            name: 'time',
            hideLabel: true,
            margin: '5px 0 0 0',
            increment: 30,
            width: 180,
            emptyText: i18n._('Time'),
            value: Ext.Date.parse('12:00 AM', 'h:i A'),
            listeners: {
                change: {
                    fn: function (combo, newValue, oldValue, opts) {
                        if (!this.buttonObj) {
                            return;
                        }
                        var comboValue = combo.getValue();
                        if (comboValue != null) {
                            if (!this.date) {
                                var selDate = this.down("datepicker[name=date]").getValue();
                                if (!selDate) {
                                    selDate = new Date();
                                    selDate.setHours(0, 0, 0, 0);
                                }
                                this.date = new Date(selDate.getTime());
                            }
                            this.date.setHours(comboValue.getHours());
                            this.date.setMinutes(comboValue.getMinutes());
                            this.setDate(this.date);
                        }
                    },
                    scope: this
                }
            }
        }];
        this.buttons = [{
            text: i18n._("Done"),
            handler: function () {
                this.hide();
            },
            scope: this
        }, '->', {
            name: 'Clear',
            text: i18n._("Clear Value"),
            handler: function () {
                this.setDate(null);
                this.hide();
            },
            scope: this
        }];
        this.callParent(arguments);
    },
    setDate: function (date) {
        this.date = date;
        this.serverDate = (this.date) ? (new Date(this.date.getTime() - i18n.timeoffset)) : null;
        var dateStr = "";
        var buttonLabel = null;
        if (this.date) {
            var displayTime = this.date.getTime() - i18n.timeoffset;
            dateStr = i18n.timestampFormat({time: displayTime});
            buttonLabel = i18n.timestampFormat({time: displayTime});
        }
        this.down("textfield[name=dateAndTime]").setValue(dateStr);
        if (this.buttonObj) {
            this.buttonObj.setText(buttonLabel != null ? buttonLabel : this.buttonObj.initialLabel);
        }
    }
});
