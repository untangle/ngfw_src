/**
 * Dashboard Controller which displays and manages the Dashboard Widgets
 * Widgets can be affected by following actions:
 * - remove/add/modify widget entry itself;
 * - install/uninstall Reports or start/stop Reports service
 * - install/uninstall Apps which can lead in a report widget to be available or not;
 * - modifying a report that is used by a widget, which requires reload of that affected widget
 */
Ext.define('Ung.view.dashboard.DashboardController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.dashboard',
    viewModel: true,
    control: {
        '#': {
            // afterrender: 'loadWidgets'
        },
        '#widgetsCmp': {
            resize: 'onResize'
        }
    },

    listen: {
        global: {
            init: 'loadWidgets',
            appinstall: 'onAppInstall',
            addRemoveReportwidget: 'onAddRemoveReportWidget', // fired from Reports view
            reportsInstall: 'loadWidgets',
            saveWidget: 'onSaveWidget'
        },
        store: {
            '#stats': {
                datachanged: 'onStatsUpdate'
            },
            // '#widgets': {
            //     add: 'onWidgetAdd'
            // },
            '#reports': {
                // datachanged: 'loadWidgets'
            }
        }
    },

    /**
     * Load initial dashboard widgets
     */
    loadWidgets: function() {
        // console.log('loadWidgets');
        var vm = this.getViewModel(),
            dashboard = this.lookupReference('dashboard'),
            widgets = Ext.getStore('widgets').getRange(),
            i, widget, entry;

        // refresh the dashboard manager grid if the widgets were affected
        this.lookupReference('dashboardNav').getView().refresh();

        dashboard.removeAll(true);

        for (i = 0; i < widgets.length; i += 1 ) {
            widget = widgets[i];

            if (widget.get('type') !== 'ReportEntry' && widget.get('type') !== 'CPULoad') {
                dashboard.add({
                    xtype: widget.get('type').toLowerCase() + 'widget',
                    itemId: widget.get('type'),
                    viewModel: {
                        data: {
                            widget: widget
                        }
                    }
                });
            }
            else {
                if (vm.get('reportsEnabled')) {
                    entry = Ext.getStore('reports').findRecord('uniqueId', widget.get('entryId'));
                    if (entry && !Ext.getStore('unavailableApps').first().get(entry.get('category')) && widget.get('enabled')) {
                        dashboard.add({
                            xtype: 'reportwidget',
                            itemId: widget.get('entryId'),
                            refreshIntervalSec: widget.get('refreshIntervalSec'),
                            viewModel: {
                                data: {
                                    widget: widget,
                                    entry: entry
                                }
                            }
                        });
                    } else {
                        dashboard.add({
                            xtype: 'component',
                            itemId: widget.get('entryId'),
                            hidden: true
                        });
                    }
                } else {
                    dashboard.add({
                        xtype: 'component',
                        itemId: widget.get('entryId'),
                        hidden: true
                    });
                }
            }
        }
        // dashboard.add(widgetComponents);
        // this.populateMenus();
    },

    onResize: function (view) {
        if (view.down('window')) {
            view.down('window').close();
        }
    },

    /**
     * when a app is installed or removed apply changes to dashboard
     */
    onAppInstall: function (action, app) {
        // refresh dashboard manager grid
        this.getView().lookupReference('dashboardNav').getView().refresh();

        var dashboard = this.getView().lookupReference('dashboard'),
            widgets = Ext.getStore('widgets').getRange(), widget, entry, i;

        // traverse all widgets and add/remove those with report category as the passed app
        for (i = 0; i < widgets.length; i += 1 ) {
            widget = widgets[i];
            entry = Ext.getStore('reports').findRecord('uniqueId', widget.get('entryId'));
            if (entry && entry.get('category') === app.displayName) {
                // remove widget placeholder
                dashboard.remove(widget.get('entryId'));
                if (action === 'install') {
                    // add real widget
                    dashboard.insert(i, {
                        xtype: 'reportwidget',
                        itemId: widget.get('entryId'),
                        refreshIntervalSec: widget.get('refreshIntervalSec'),
                        viewModel: {
                            data: {
                                widget: widget,
                                entry: entry
                            }
                        }
                    });
                } else {
                    // add widget placeholder
                    dashboard.insert(i, {
                        xtype: 'component',
                        itemId: widget.get('entryId'),
                        hidden: true
                    });
                }
            }
        }
    },

    enableRenderer: function (value, meta, record) {
        var vm = this.getViewModel();
        if (record.get('type') !== 'ReportEntry') {
            return '<i class="fa ' + (value ? 'fa-check-circle-o' : 'fa-circle-o') + ' fa-lg"></i>';
        }
        var entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));

        if (!entry || Ext.getStore('unavailableApps').first().get(entry.get('category')) || !vm.get('reportsRunning')) {
            return '<i class="fa fa-info-circle fa-lg" style="color: #a91f1f;"></i>';
        }
        return '<i class="fa ' + (value ? 'fa-check-circle-o' : 'fa-circle-o') + ' fa-lg"></i>';
    },

    settingsRenderer: function () {

    },

    /**
     * renders the title of the widget in the dashboard manager grid, based on various conditions
     */
    widgetTitleRenderer: function (value, metaData, record) {
        var vm = this.getViewModel(), entry, title, unavailApp, enabled;
        enabled = record.get('enabled');

        if (!value) {
            return '<span style="font-weight: 400; ' + (!enabled ? 'color: #999;' : '') + '">' + record.get('type') + '</span>'; // <br/><span style="font-size: 10px; color: #777;">Common</span>';
        }
        if (vm.get('reportsInstalled')) {
            entry = Ext.getStore('reports').findRecord('uniqueId', value);
            if (entry) {
                unavailApp = Ext.getStore('unavailableApps').first().get(entry.get('category'));
                title = '<span style="font-weight: 400; ' + ((unavailApp || !enabled) ? 'color: #999;' : '') + '">' + (entry.get('readOnly') ? entry.get('title').t() : entry.get('title')) + '</span>';

                if (entry.get('timeDataInterval') && entry.get('timeDataInterval') !== 'AUTO') {
                    title += '<span style="text-transform: lowercase; color: #999; font-weight: 300;"> per ' + entry.get('timeDataInterval') + '</span>';
                }
                // if (unavailApp) {
                //     title += '<br/><span style="font-size: 10px; color: #777;">' + entry.get('category') + '</span>';
                // } else {
                //     title += '<br/><span style="font-size: 10px; color: #777;">' + entry.get('category') + '</span>';
                // }
                /*
                if (entry.get('readOnly')) {
                    title += ' <i class="material-icons" style="font-size: 14px; color: #999; vertical-align: top;">lock</i>';
                }
                */
                return title;
            } else {
                return 'Some ' + 'Widget'.t();
            }
        } else {
            return '<span style="color: #999; line-height: 26px;">' + 'App Widget'.t() + '</span>';
        }
    },


    /**
     * Method which sends modified dashboard settings to backend to be saved
     */
    applyChanges: function () {
        console.log('apply');
        // because of the drag/drop reorder the settins widgets are updated to respect new ordering
        Ung.dashboardSettings.widgets.list = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');

        Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings)
        .then(function(result) {
            Util.successToast('<span style="color: yellow; font-weight: 600;">Dashboard Saved!</span>');
            Ext.getStore('widgets').sync();
        });

    },

    onSaveWidget: function (cb) {
        Ung.dashboardSettings.widgets.list = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');

        Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings)
        .then(function(result) {
            // Util.successToast('<span style="color: yellow; font-weight: 600;">Dashboard Saved!</span>');
            // Ext.getStore('widgets').sync();
            cb();
        });

    },


    managerHandler: function () {
        var state = this.getViewModel().get('managerOpen');
        this.getViewModel().set('managerOpen', !state);
    },

    onItemClick: function (cell, td, cellIndex, record, tr, rowIndex) {
        var me = this,
            dashboard = me.getView().lookupReference('dashboard'),
            vm = this.getViewModel(),
            entry, widgetCmp;

        if (cellIndex === 0) {
            // toggle visibility or show alerts

            if (record.get('type') !== 'ReportEntry') {
                record.set('enabled', !record.get('enabled'));
            } else {
                if (!vm.get('reportsInstalled')) {
                    Ext.Msg.alert('Install required'.t(), 'To enable App Widgets please install Reports first!'.t());
                    return;
                }
                if (!vm.get('reportsRunning')) {
                    Ext.Msg.alert('Reports'.t(), 'To view App Widgets enable the Reports App first!'.t());
                    return;
                }

                entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));
                widgetCmp = dashboard.down('#' + record.get('entryId'));
                if (entry && widgetCmp) {
                    if (!Ext.getStore('unavailableApps').first().get(entry.get('category'))) {
                        widgetCmp.destroy();
                        if (!record.get('enabled')) {
                            dashboard.insert(rowIndex, {
                                xtype: 'reportwidget',
                                itemId: record.get('entryId'),
                                refreshIntervalSec: record.get('refreshIntervalSec'),
                                viewModel: {
                                    data: {
                                        widget: record,
                                        entry: entry
                                    }
                                }
                            });
                            widgetCmp = dashboard.down('#' + record.get('entryId'));
                            setTimeout(function () {
                                dashboard.scrollTo(0, dashboard.getEl().getScrollTop() + widgetCmp.getEl().getY() - 121, {duration: 300 });
                            }, 100);
                        } else {
                            dashboard.insert(rowIndex, {
                                xtype: 'component',
                                itemId: record.get('entryId'),
                                hidden: true
                            });
                        }
                        record.set('enabled', !record.get('enabled'));
                    } else {
                        Ext.Msg.alert('Install required'.t(), Ext.String.format('To enable this Widget please install <strong>{0}</strong> app first!'.t(), entry.get('category')));
                    }
                } else {
                    Util.exceptionToast('This entry is not available and it should be removed!');
                }

            }
        }

        if (cellIndex === 1) {
            // highlights in the dashboard the widget which receives click event in the manager grid
            widgetCmp = dashboard.down('#' + record.get('entryId')) || dashboard.down('#' + record.get('type'));
            if (widgetCmp && !widgetCmp.isHidden()) {
                dashboard.addCls('highlight');
                widgetCmp.addCls('highlight-item');
                dashboard.scrollTo(0, dashboard.getEl().getScrollTop() + widgetCmp.getEl().getY() - 121, {duration: 100});
            }
        }

        // if (cellIndex === 3) {
        //     // remove widget
        //     record.drop();
        // }
    },

    removeWidget: function (table, rowIndex, colIndex, item, e, record) {
        record.drop();
        // console.log(record);
    },

    /**
     * removes the above set highlight
     */
    onItemLeave: function (view, record) {
        if (this.tout) {
            window.clearTimeout(this.tout);
        }
        var dashboard = this.getView().lookupReference('dashboard'), widgetCmp;
        if (record.get('type') !== 'ReportEntry') {
            widgetCmp = dashboard.down('#' + record.get('type'));
        } else {
            widgetCmp = dashboard.down('#' + record.get('entryId'));
        }
        if (widgetCmp) {
            dashboard.removeCls('highlight');
            widgetCmp.removeCls('highlight-item');
        }
    },


    /**
     * todo: after drag sort event
     */
    onDrop: function (app, data, overModel, dropPosition) {
        var dashboard = this.lookupReference('dashboard');

        var widgetMoved = dashboard.down('#' + data.records[0].get('entryId')) || this.getView().down('#' + data.records[0].get('type'));
        var widgetDropped = dashboard.down('#' + overModel.get('entryId')) || this.getView().down('#' + overModel.get('type'));

        /*
        widgetMoved.addCls('moved');

        window.setTimeout(function () {
            widgetMoved.removeCls('moved');
        }, 300);
        */

        if (dropPosition === 'before') {
            dashboard.moveBefore(widgetMoved, widgetDropped);
        } else {
            dashboard.moveAfter(widgetMoved, widgetDropped);
        }


    },

    resetDashboard: function () {
        Ext.MessageBox.confirm('Warning'.t(),
            'This will overwrite the current dashboard settings with the defaults.'.t() + '<br/><br/>' +
            'Do you want to continue?'.t(),
            function (btn) {
                if (btn === 'yes') {
                    Rpc.asyncData('rpc.dashboardManager.resetSettingsToDefault').then(function (result) {
                        Util.successToast('Dashboard reset done!');
                    });
                }
            });
    },


    onRemoveWidget: function (id) {
        var dashboard = this.getView().lookupReference('dashboard');
        if (dashboard.down('#' + id)) {
            dashboard.remove(id);
        }
    },

    onAddRemoveReportWidget: function (entry, isWidget, cb) {
        var me = this, widget, widgetCmp, dashboardCmp = me.lookupReference('dashboard');

        if (isWidget) {
            // remove it from settings
            widget = Ext.getStore('widgets').findRecord('entryId', entry.get('uniqueId'));
            if (widget) {
                Ext.getStore('widgets').remove(widget);
                Ung.dashboardSettings.widgets.list = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');
            }
        } else {
            // add it to settings
            widget = {
                displayColumns: entry.get('displayColumns'),
                enabled: true,
                entryId: entry.get('uniqueId'),
                javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                refreshIntervalSec: 60,
                timeframe: 3600,
                type: 'ReportEntry'
            };
            Ung.dashboardSettings.widgets.list.push(widget);
        }

        Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings).then(function (result) {
            if (!isWidget) {
                // add it in dashboard
                Ext.getStore('widgets').add(widget);
                // display widget in dashboard
                dashboardCmp.add({
                    xtype: 'reportwidget',
                    itemId: widget.entryId,
                    refreshIntervalSec: widget.refreshIntervalSec,
                    viewModel: {
                        data: {
                            widget: widget,
                            entry: entry
                        }
                    }
                });
                Ung.app.redirectTo('#');
            } else {
                // removed from dashboard
                dashboardCmp.remove(widget.get('entryId'));
            }
            cb();
        });
    },

    onStatsUpdate: function() {
        var vm = this.getViewModel();
        vm.set('stats', Ext.getStore('stats').first());

        // get devices
        // @todo: review this based on oler implementation
        rpc.deviceTable.getDevices(function (result, ex) {
            if (ex) { Util.exceptionToast(ex); return false; }
            vm.set('deviceCount', result.list.length);
        });
    },

    populateMenus: function () {
        var addWidgetBtn = this.getView().down('#addWidgetBtn'), categories, categoriesMenu = [], reportsMenu = [];

        if (addWidgetBtn.getMenu()) {
            addWidgetBtn.getMenu().remove();
        }

        categoriesMenu.push({
            text: 'Common',
            icon: '/skins/modern-rack/images/admin/config/icon_config_hosts.png',
            iconCls: 'menu-icon',
            menu: {
                plain: true,
                items: [{
                    text: 'Information',
                    type: 'Information'
                }, {
                    text: 'Resources',
                    type: 'Resources'
                }, {
                    text: 'CPU Load',
                    type: 'CPULoad'
                }, {
                    text: 'Network Information',
                    type: 'NetworkInformation'
                }, {
                    text: 'Network Layout',
                    type: 'NetworkLayout'
                }, {
                    text: 'Map Distribution',
                    type: 'MapDistribution'
                }],
                listeners: {
                    click: function (menu, item) {
                        if (Ext.getStore('widgets').findRecord('type', item.type)) {
                            Util.successToast('<span style="color: yellow; font-weight: 600;">' + item.text + '</span>' + ' is already in Dashboard!');
                            return;
                        }
                        var newWidget = Ext.create('Ung.model.Widget', {
                            displayColumns: null,
                            enabled: true,
                            entryId: null,
                            javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                            refreshIntervalSec: 0,
                            timeframe: null,
                            type: item.type
                        });
                        Ext.getStore('widgets').add(newWidget);
                        Ext.GlobalEvents.fireEvent('addwidget', newWidget, null);
                    }
                }
            }
        });

        if (rpc.reportsManager) {
            Rpc.asyncData('rpc.reportsManager.getCurrentApplications').then(function(result, ex) {
                categories = [
                    { displayName: 'Hosts', icon: '/skins/modern-rack/images/admin/config/icon_config_hosts.png' },
                    { displayName: 'Devices', icon: '/skins/modern-rack/images/admin/config/icon_config_devices.png' },
                    { displayName: 'Network', icon: '/skins/modern-rack/images/admin/config/icon_config_network.png' },
                    { displayName: 'Administration', icon: '/skins/modern-rack/images/admin/config/icon_config_admin.png' },
                    { displayName: 'System', icon: '/skins/modern-rack/images/admin/config/icon_config_system.png' },
                    { displayName: 'Shield', icon: '/skins/modern-rack/images/admin/apps/untangle-app-shield_17x17.png' }
                ];
                result.list.forEach(function (app) {
                    categories.push({
                        displayName: app.displayName,
                        icon: '/skins/modern-rack/images/admin/apps/' + app.name + '_17x17.png'
                    });
                });

                categories.forEach(function (category) {
                    reportsMenu = [];
                    Ext.getStore('reports').filter({
                        property: 'category',
                        value: category.displayName,
                        exactMatch: true
                    });
                    Ext.getStore('reports').getRange().forEach(function(report) {
                        reportsMenu.push({
                            text: Util.iconReportTitle(report) + ' ' + report.get('title'),
                            report: report
                        });
                    });

                    Ext.getStore('reports').clearFilter();
                    categoriesMenu.push({
                        text: category.displayName,
                        icon: category.icon,
                        iconCls: 'menu-icon',
                        menu: {
                            plain: true,
                            items: reportsMenu,
                            listeners: {
                                click: function (menu, item) {
                                    if (Ext.getStore('widgets').findRecord('entryId', item.report.get('uniqueId'))) {
                                        Util.successToast('<span style="color: yellow; font-weight: 600;">' + item.report.get('title') + '</span>' + ' is already in Dashboard!');
                                        return;
                                    }
                                    var newWidget = Ext.create('Ung.model.Widget', {
                                        displayColumns: item.report.get('displayColumns'),
                                        enabled: true,
                                        entryId: item.report.get('uniqueId'),
                                        javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                                        refreshIntervalSec: 60,
                                        timeframe: 3600,
                                        type: 'ReportEntry'
                                    });
                                    Ext.getStore('widgets').add(newWidget);
                                    Ext.GlobalEvents.fireEvent('addwidget', newWidget, item.report);
                                }
                            }
                        }
                    });
                });
                addWidgetBtn.setMenu({
                    items: categoriesMenu,
                    mouseLeaveDelay: 0
                });
            });
        } else {
            addWidgetBtn.setMenu({
                items: categoriesMenu,
                mouseLeaveDelay: 0
            });
        }
    }
});
