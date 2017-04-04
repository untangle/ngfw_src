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
            deactivate: 'onDeactivate'
        },
        '#widgetsCmp': {
            resize: 'onResize'
        }
    },

    listen: {
        global: {
            init: 'loadWidgets',
            // appinstall: 'onAppInstall',
            // addRemoveReportwidget: 'onAddRemoveReportWidget', // fired from Reports view
            reportsInstall: 'loadWidgets',
            widgetaction: 'onWidgetAction'
        },
        store: {
            '#stats': {
                datachanged: 'onStatsUpdate'
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
        this.lookup('dashboardManager').getView().refresh();

        dashboard.removeAll(true);

        for (i = 0; i < widgets.length; i += 1 ) {
            widget = widgets[i];

            if (widget.get('type') !== 'ReportEntry') {
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
                        var wg = dashboard.add({
                            xtype: 'reportwidget',
                            itemId: widget.get('entryId'),
                            // refreshIntervalSec: widget.get('refreshIntervalSec'),
                            // refreshIntervalSec: 10,
                            viewModel: {
                                data: {
                                    widget: widget,
                                    entry: entry
                                }
                            }
                        });
                        DashboardQueue.add(wg);
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

    toggleManager: function (btn) {
        var vm = this.getViewModel();
        vm.set('managerVisible', !vm.get('managerVisible'));
    },

    /**
     * when a app is installed or removed apply changes to dashboard
     */
    onAppInstall: function (action, app) {
        // refresh dashboard manager grid
        this.getView().lookupReference('dashboardManager').getView().refresh();

        var dashboard = this.getView().lookupReference('dashboard'), wg,
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
                    wg = dashboard.insert(i, {
                        xtype: 'reportwidget',
                        itemId: widget.get('entryId'),
                        // refreshIntervalSec: widget.get('refreshIntervalSec'),
                        viewModel: {
                            data: {
                                widget: widget,
                                entry: entry
                            }
                        }
                    });
                    DashboardQueue.addFirst(wg);
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
            return '<i class="fa fa-info-circle fa-lg"></i>';
        }
        return '<i class="fa ' + (value ? 'fa-check-circle-o' : 'fa-circle-o') + ' fa-lg"></i>';
    },

    /**
     * renders the title of the widget in the dashboard manager grid, based on various conditions
     */
    widgetTitleRenderer: function (value, metaData, record) {
        var vm = this.getViewModel(), entry, title, unavailApp, enabled;
        enabled = record.get('enabled');

        if (!value) {
            return '<span style="font-weight: 400; ' + (!enabled ? 'color: #777;' : 'color: #000;') + '">' + record.get('type') + '</span>'; // <br/><span style="font-size: 10px; color: #777;">Common</span>';
        }
        if (vm.get('reportsInstalled')) {
            entry = Ext.getStore('reports').findRecord('uniqueId', value);
            if (entry) {
                unavailApp = Ext.getStore('unavailableApps').first().get(entry.get('category'));
                title = '<span style="font-weight: 400; ' + ((unavailApp || !enabled) ? 'color: #777;' : 'color: #000;') + '">' + (entry.get('readOnly') ? entry.get('title').t() : entry.get('title')) + '</span>';

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
            return '<span style="color: #999;">' + 'App Widget'.t() + '</span>';
        }
    },


    /**
     * Method which sends modified dashboard settings to backend to be saved
     */
    applyChanges: function () {
        var vm = this.getViewModel();
        // because of the drag/drop reorder the settins widgets are updated to respect new ordering
        Ung.dashboardSettings.widgets.list = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');

        Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings)
        .then(function(result) {
            Util.successToast('<span style="color: yellow; font-weight: 600;">Dashboard Saved!</span>');
            Ext.getStore('widgets').sync();
            vm.set('managerVisible', false);
        });

    },

    onItemClick: function (cell, td, cellIndex, record, tr, rowIndex) {
        var me = this,
            dashboard = me.lookup('dashboard'),
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
                            widgetCmp = dashboard.insert(rowIndex, {
                                xtype: 'reportwidget',
                                itemId: record.get('entryId'),
                                // refreshIntervalSec: record.get('refreshIntervalSec'),
                                viewModel: {
                                    data: {
                                        widget: record,
                                        entry: entry
                                    }
                                }
                            });
                            // widgetCmp = dashboard.down('#' + record.get('entryId'));
                            setTimeout(function () {
                                dashboard.scrollTo(0, dashboard.getEl().getScrollTop() + widgetCmp.getEl().getY() - 121, {duration: 300 });
                            }, 100);
                            DashboardQueue.addFirst(widgetCmp);
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
                dashboard.addBodyCls('highlight');
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
            dashboard.removeBodyCls('highlight');
            widgetCmp.removeCls('highlight-item');
        }
    },

    /**
     * widget actions on add/remove/save
     */
    onWidgetAction: function (action, widget, entry, cb) {
        var me = this, wg;
        // var widgetsList = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');
        if (action === 'add') {
            Ung.dashboardSettings.widgets.list.push(widget.getData());
        }
        if (action === 'remove') {
            wg = Ext.Array.findBy(Ung.dashboardSettings.widgets.list, function (wg, idx) {
                return wg.entryId === widget.get('entryId');
            });
            if (wg) {
                Ext.Array.remove(Ung.dashboardSettings.widgets.list, wg);
            }
        }
        if (action === 'save') {
            wg = Ext.Array.findBy(Ung.dashboardSettings.widgets.list, function (wg, idx) {
                return wg.entryId === widget.get('entryId');
            });
            if (wg) {
                Ext.apply(wg, widget.getData());
            }
        }

        // try to save it
        Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings).then(function (result) {
            var wg2 = Ext.getStore('widgets').findRecord('entryId', widget.get('entryId')) || widget;
            if (action === 'remove') {
                me.lookup('dashboard').remove(wg2.get('entryId'));
                wg2.drop();
                cb();
                return;
            }

            if (action === 'add') {
                Ext.getStore('widgets').add(wg2);
                wg2.commit();
                if (wg2.get('enabled')) {
                    me.lookup('dashboard').add({
                        xtype: 'reportwidget',
                        itemId: wg2.get('entryId'),
                        viewModel: {
                            data: {
                                widget: wg2,
                                entry: entry
                            }
                        }
                    });
                } else {
                    me.lookup('dashboard').add({
                        xtype: 'component',
                        itemId: wg2.get('entryId'),
                        hidden: true
                    });
                }
            }

            if (action === 'save') {
                wg2.copyFrom(widget);
                wg2.commit();

                var wgCmp = me.lookup('dashboard').down('#' + wg2.get('entryId'));
                var idx = Ext.getStore('widgets').indexOf(wg2);

                if (wgCmp.getXType() === 'component') {
                    if (wg2.get('enabled')) {
                        wgCmp.destroy();
                        me.lookup('dashboard').insert(idx, {
                            xtype: 'reportwidget',
                            itemId: wg2.get('entryId'),
                            viewModel: {
                                data: {
                                    widget: wg2,
                                    entry: entry
                                }
                            }
                        });
                    }
                } else {
                    if (wg2.get('enabled')) {
                        DashboardQueue.add(wgCmp);
                    } else {
                        wgCmp.destroy();
                        me.lookup('dashboard').insert(idx, {
                            xtype: 'component',
                            itemId: wg2.get('entryId'),
                            hidden: true
                        });
                    }
                }
            }
            cb();
        });
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

    onDeactivate: function () {
        var vm = this.getViewModel();
        if (vm.get('managerVisible')) {
            this.toggleManager();
        }
    },

    addWidget: function () {
        this.showWidgetEditor(null, null);
    },

    showWidgetEditor: function (widget, entry) {
        me = this;
        me.addWin = me.getView().add({
            xtype: 'new-widget',
            viewModel: {
                data: {
                    widget: widget,
                    entry: entry
                }
            }
        });
        me.addWin.show();
    }
});
