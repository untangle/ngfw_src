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

    control: {
        '#': {
            beforerender: 'initDashboard'
        }
    },

    listen: {
        global: {
            nodeinstall: 'onNodeInstall',
            removewidget: 'onRemoveWidget',
            addwidget: 'onAddWidget'
        },
        store: {
            '#stats': {
                datachanged: 'onStatsUpdate'
            }
        }
    },

    init: function (view) {
        var me = this, vm = view.getViewModel();

        // update dashboard when Reports app is installed/removed or enabled/disabled
        vm.bind('{reportsEnabled}', function() {
            me.loadWidgets();
        });
        vm.set('managerOpen', false);
    },

    /**
     * before rendering the Dashboard the settings are fetched form the server
     */
    initDashboard: function () {
        var me = this,
            vm = me.getViewModel();

        // load the dashboard settings
        Rpc.loadDashboardSettings().then(function(settings) {
            me.getView().setSettings(settings);

            if (vm.get('reportsInstalled')) {
                // load unavailable apps needed for showing the widgets
                rpc.reportsManager.getUnavailableApplicationsMap(function (result, ex) {
                    if (ex) { Ung.Util.exceptionToast(ex); return false; }

                    Ext.getStore('unavailableApps').loadRawData(result.map);
                    Ext.getStore('widgets').loadData(settings.widgets.list);
                    me.loadWidgets();
                });
            } else {
                Ext.getStore('widgets').loadData(settings.widgets.list);
                me.loadWidgets();
            }
        });
    },


    /**
     * Load initial dashboard widgets
     */
    loadWidgets: function() {
        var vm = this.getViewModel(),
            dashboard = this.getView().lookupReference('dashboard'),
            widgets = Ext.getStore('widgets').getRange(),
            i, widget, widgetComponents = [], entry;

        // refresh the dashboard manager grid if the widgets were affected
        this.getView().lookupReference('dashboardNav').getView().refresh();

        for (i = 0; i < widgets.length; i += 1 ) {
            widget = widgets[i];

            if (widget.get('type') !== 'ReportEntry') {
                widgetComponents.push({
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
                        widgetComponents.push({
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
                        widgetComponents.push({
                            xtype: 'component',
                            itemId: widget.get('entryId'),
                            hidden: true
                        });
                    }
                }
            }
        }
        dashboard.removeAll(true);
        dashboard.add(widgetComponents);
    },

    /**
     * when a node is installed or removed apply changes to dashboard
     */
    onNodeInstall: function (action, node) {
        // refresh dashboard manager grid
        this.getView().lookupReference('dashboardNav').getView().refresh();

        var dashboard = this.getView().lookupReference('dashboard'),
            widgets = Ext.getStore('widgets').getRange(), widget, entry, i;

        // traverse all widgets and add/remove those with report category as the passed node
        for (i = 0; i < widgets.length; i += 1 ) {
            widget = widgets[i];
            entry = Ext.getStore('reports').findRecord('uniqueId', widget.get('entryId'));
            if (entry && entry.get('category') === node.displayName) {
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
            return '<i class="material-icons">' + (value ? 'check_box' : 'check_box_outline_blank') + '</i>';
        }
        var entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));

        if (!entry || Ext.getStore('unavailableApps').first().get(entry.get('category')) || !vm.get('reportsRunning')) {
            return '<i class="material-icons" style="color: #F00;">info_outline</i>';
        }
        return '<i class="material-icons">' + (value ? 'check_box' : 'check_box_outline_blank') + '</i>';
    },

    removeRenderer: function (value, meta, record) {
        return '<i class="material-icons" style="color: red; font-size: 16px;">delete</i>';
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
            return '<span style="font-weight: 600; ' + (!enabled ? 'color: #999;' : '') + '">' + record.get('type') + '</span><br/><span style="font-size: 10px; color: #AAA;">Common</span>';
        }
        if (vm.get('reportsInstalled')) {
            entry = Ext.getStore('reports').findRecord('uniqueId', value);
            if (entry) {
                unavailApp = Ext.getStore('unavailableApps').first().get(entry.get('category'));
                title = '<span style="font-weight: 600; ' + ((unavailApp || !enabled) ? 'color: #999;' : '') + '">' + (entry.get('readOnly') ? entry.get('title').t() : entry.get('title')) + '</span>';

                if (entry.get('timeDataInterval') && entry.get('timeDataInterval') !== 'AUTO') {
                    title += '<span style="text-transform: lowercase; color: #333; font-weight: 300;"> per ' + entry.get('timeDataInterval') + '</span>';
                }
                if (unavailApp) {
                    title += '<br/><span style="font-size: 10px; color: #AAA;">' + entry.get('category') + '</span>';
                } else {
                    title += '<br/><span style="font-size: 10px; color: #AAA;">' + entry.get('category') + '</span>';
                }
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
            return '<span style="color: #999;">' + 'App Widget'.t() + '</span>';
        }
    },


    /**
     * Method which sends modified dashboard settings to backend to be saved
     */
    applyChanges: function () {
        // because of the drag/drop reorder the settins widgets are updated to respect new ordering
        this.getView().getSettings().widgets.list = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');
        rpc.dashboardManager.setSettings(function (result, ex) {
            if (ex) { Ung.Util.exceptionToast(ex); return; }
            Ung.Util.successToast('<span style="color: yellow;">Dashboard Saved!</span>');
            Ext.getStore('widgets').sync();
        }, this.getView().getSettings());

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
                    Ung.Util.exceptionToast('This entry is not available and it should be removed!');
                }

            }
        }

        if (cellIndex === 1) {
            // highlights in the dashboard the widget which receives click event in the manager grid
            widgetCmp = dashboard.down('#' + record.get('entryId')) || dashboard.down('#' + record.get('type'));
            if (widgetCmp && !widgetCmp.isHidden()) {
                dashboard.addCls('highlight');
                widgetCmp.addCls('highlight-item');
                dashboard.scrollTo(0, dashboard.getEl().getScrollTop() + widgetCmp.getEl().getY() - 121, {duration: 500});
            }
        }

        if (cellIndex === 2) {
            // remove widget
            record.drop();
        }
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
    onDrop: function (node, data, overModel, dropPosition) {
        var dashboard = this.getView().lookupReference('dashboard');
        //console.log(data.view.getStore().findExact('entryId', data.records[0].get('entryId')));
        //console.log(data.records);

        var widgetMoved = this.getView().down('#' + data.records[0].get('entryId')) || this.getView().down('#' + data.records[0].get('type'));
        var widgetDropped = this.getView().down('#' + overModel.get('entryId')) || this.getView().down('#' + overModel.get('type'));

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
        var me = this;
        Ext.MessageBox.confirm('Warning'.t(),
            'This will overwrite the current dashboard settings with the defaults.'.t() + '<br/><br/>' +
            'Do you want to continue?'.t(),
            function (btn) {
                if (btn === 'yes') {
                    rpc.dashboardManager.resetSettingsToDefault(function (result, ex) {
                        if (ex) { Ung.Util.exceptionToast(ex); return; }
                        Ung.Util.successToast('Dashboard reset done!');
                        me.initDashboard();
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

    onAddWidget: function (widget, entry) {
        var dashboard = this.getView().lookupReference('dashboard');
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
    },

    onStatsUpdate: function() {
        var vm = this.getViewModel();
        vm.set('stats', Ext.getStore('stats').first());

        // get devices
        // @todo: review this based on oler implementation
        rpc.deviceTable.getDevices(function (result, ex) {
            if (ex) { Ung.Util.exceptionToast(ex); return false; }
            vm.set('deviceCount', result.list.length);
        });
    }


});
