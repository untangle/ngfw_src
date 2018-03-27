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
            afterrender: 'onAfterRender',
            activate: 'onActivate',
            deactivate: 'onDeactivate',
        },
        '#dashboard': {
            resize: 'onResize'
        }
    },

    widgetsRendered: false,

    listen: {
        global: {
            widgetaction: 'onWidgetAction'
        },
        store: {
            '#stats': {
                datachanged: 'onStatsUpdate'
            }
        }
    },

    onAfterRender: function () {
        var me = this, vm = me.getViewModel(), text;

        /**
         * Fetch dashboard settings
         */
        Rpc.asyncData('rpc.dashboardManager.getSettings')
            .then(function (result) {
                Ung.dashboardSettings = result;
                Ext.getStore('widgets').loadData(result.widgets.list);

                if (result.timeframe === 1) {
                    text = '1 Hour ago'.t();
                } else {
                    text = result.timeframe + ' Hours ago'.t();
                }

                me.getView().down('#since > button').setText(text);

                Ung.app.reportscheck();
            });

        /**
         * (re)load widgets when Reports App installed/removed or enabled/disabled
         */
        vm.bind('{reportsAppStatus}', function () {
            me.loadWidgets();
        });

        /**
         * On global conditions change refetch data based on new conditions
         * Using {query.string} because it fires only when the value changes, unlike {query} only
         */
        vm.bind('{query.string}', function () {
            Ext.Array.each(me.lookup('dashboard').query('reportwidget'), function (widgetCmp) {
                if (widgetCmp.lastFetchTime) {
                    widgetCmp.lastFetchTime = null;
                    DashboardQueue.add(widgetCmp);
                }
            });
        });

    },

    /**
     * Helper method used to slowfire resize or scroll events
     */
    debounce: function (fn, delay) {
        var timer = null;
        var me = this;
        return function () {
            clearTimeout(timer);
            timer = setTimeout(function () {
                fn.apply(me, arguments);
            }, delay);
        };
    },

    /**
     * Checks if widget is visible or not in viewport
     * Based on it's visibility it will be added to queue for fetching data
     */
    updateWidgetsVisibility: function () {
        var dashboard = this.lookup('dashboard'),
            widgets = dashboard.query('reportwidget');
        DashboardQueue.isVisible(dashboard.down('networklayoutwidget'));
        DashboardQueue.isVisible(dashboard.down('mapdistributionwidget'));
        DashboardQueue.isVisible(dashboard.down('networkinformationwidget'));
        DashboardQueue.isVisible(dashboard.down('policyoverviewwidget'));
        DashboardQueue.isVisible(dashboard.down('notificationswidget'));
        Ext.Array.each(widgets, function (widget) {
            if (widget) {
                DashboardQueue.isVisible(widget);
            }
        });
    },

    /**
     * Render widgets into the dashboard
     */
    loadWidgets: function() {
        var me = this, vm = me.getViewModel(),
            dashboard = me.lookup('dashboard'),
            widgets = Ext.getStore('widgets').getRange(),
            i, widget, entry;

        // refresh the dashboard manager grid if the widgets were affected
        me.lookup('dashboardManager').getView().refresh();
        vm.set('timeframe', Ung.dashboardSettings.timeframe || 1);
        // vm.set('theme', Ung.dashboardSettings.theme);

        dashboard.removeAll(true);
        var widgetsCmp = [];

        for (i = 0; i < widgets.length; i += 1 ) {
            widget = widgets[i];

            if (widget.get('type') !== 'ReportEntry') {
                widgetsCmp.push({
                    xtype: widget.get('type').toLowerCase() + 'widget',
                    itemId: widget.get('type'),
                    lastFetchTime: null,
                    visible: true,
                    viewModel: {
                        data: {
                            widget: widget
                        }
                    }
                });
            }
            else {
                if (vm.get('reportsAppStatus.installed') && vm.get('reportsAppStatus.enabled')) {
                    entry = Ext.getStore('reports').findRecord('uniqueId', widget.get('entryId'));

                    if (entry && !Ext.getStore('unavailableApps').first().get(entry.get('category')) && widget.get('enabled')) {
                        widgetsCmp.push({
                            xtype: 'reportwidget',
                            itemId: widget.get('entryId'),
                            lastFetchTime: null,
                            visible: true,
                            viewModel: {
                                data: {
                                    widget: widget,
                                    entry: entry
                                }
                            }
                        });
                    } else {
                        widgetsCmp.push({
                            xtype: 'component',
                            itemId: widget.get('entryId'),
                            hidden: true
                        });
                    }
                } else {
                    widgetsCmp.push({
                        xtype: 'component',
                        itemId: widget.get('entryId'),
                        hidden: true
                    });
                }
            }
        }
        dashboard.add(widgetsCmp);

        if (!me.widgetsRendered) {
            me.widgetsRendered = true;
            // add scroll/resize events
            dashboard.body.on('scroll', me.debounce(me.updateWidgetsVisibility, 500));
            dashboard.getEl().on('resize', me.debounce(me.updateWidgetsVisibility, 500));
        }
    },

    updateSince: function (menu, item) {
        var me = this, dashboard = me.lookup('dashboard');
        menu.up('button').setText(item.text);
        Ung.dashboardSettings.timeframe = item.value;

        Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings)
            .then(function() {
                Ext.Array.each(dashboard.query('reportwidget'), function (widgetCmp) {
                    widgetCmp.lastFetchTime = null;
                });
                me.updateWidgetsVisibility();
            });

    },


    onResize: function (view) {
        if (view.down('window')) {
            view.down('window').close();
        }
    },

    toggleManager: function () {
        var me = this, vm = me.getViewModel(),
            columns = me.lookup('dashboardManager').getColumns();

        vm.set('managerVisible', !vm.get('managerVisible'));
        if (!vm.get('managerVisible')) {
            columns[0].setHidden(true);
        }
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
                        lastFetchTime: null,
                        visible: true,
                        // bind: {
                        //     userCls: 'theme-{theme}'
                        // },
                        // refreshIntervalSec: widget.get('refreshIntervalSec'),
                        viewModel: {
                            data: {
                                widget: widget,
                                entry: entry,
                            }
                        }
                    });
                    Ext.defer(function () {
                        DashboardQueue.addFirst(wg);
                    }, 1000);
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
        meta.tdCls = 'enable';
        if (record.get('type') !== 'ReportEntry') {
            return '<i class="fa ' + (value ? 'fa-check-circle-o' : 'fa-circle-o') + '"></i>';
        }
        var entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));

        if (!entry || Ext.getStore('unavailableApps').first().get(entry.get('category')) || !vm.get('reportsAppStatus.enabled')) {
            return '<i class="fa fa-info-circle"></i>';
        }
        return '<i class="fa ' + (value ? 'fa-check-circle-o' : 'fa-circle-o') + '"></i>';
    },

    /**
     * renders the title of the widget in the dashboard manager grid, based on various conditions
     */
    widgetTitleRenderer: function (value, metaData, record) {
        var vm = this.getViewModel(), entry, title, unavailApp, enabled;
        enabled = record.get('enabled');

        if (!value) {
            return '<span style="' + (!enabled ? 'font-weight: 400; color: #777;' : 'font-weight: 600; color: #000;') + '">' + record.get('type') + '</span>'; // <br/><span style="font-size: 10px; color: #777;">Common</span>';
        }
        if (vm.get('reportsAppStatus.installed')) {
            entry = Ext.getStore('reports').findRecord('uniqueId', value);
            if (entry) {
                unavailApp = Ext.getStore('unavailableApps').first().get(entry.get('category'));
                title = '<span style="' + ((unavailApp || !enabled) ? 'font-weight: 400; color: #777;' : 'font-weight: 600; color: #000;') + '">' + (entry.get('readOnly') ? entry.get('title').t() : entry.get('title')) + '</span>';
                return title;
            } else {
                return 'Unknown Widget'.t();
            }
        } else {
            return '<span style="color: #999;">' + 'App Widget'.t() + '</span>';
        }
    },


    /**
     * Method which sends modified dashboard settings to backend to be saved
     */
    applyChanges: function () {
        var me = this, dashboard = me.lookup('dashboard');

        // drom record selected for removal
        Ext.getStore('widgets').each(function (record) {
            if (record.get('markedForDelete')) {
                record.drop();
            }
        });

        // because of the drag/drop reorder the settins widgets are updated to respect new ordering
        Ung.dashboardSettings.widgets.list = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');

        Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings)
            .then(function() {
                Util.successToast('<span style="color: yellow; font-weight: 600;">Dashboard Saved!</span>');
                Ext.getStore('widgets').sync();
                me.toggleManager();

                // remove widgets from dashboard if removed from store (manager)
                Ext.Array.each(dashboard.query('reportwidget'), function (widgetCmp) {
                    if (Ext.getStore('widgets').find('entryId', widgetCmp.getItemId()) < 0) {
                        dashboard.remove(widgetCmp);
                    }
                });

            });

    },

    onItemClick: function (cell, td, cellIndex, record, tr, rowIndex) {
        var me = this,
            dashboard = me.lookup('dashboard'),
            vm = this.getViewModel(),
            entry, widgetCmp;

        if (cellIndex === 1) {
            // toggle visibility or show alerts

            if (record.get('type') !== 'ReportEntry') {
                record.set('enabled', !record.get('enabled'));
            } else {
                if (!vm.get('reportsAppStatus.installed')) {
                    Ext.Msg.alert('Info'.t(), 'To enable App Widgets please install Reports first!'.t());
                    return;
                }
                if (!vm.get('reportsAppStatus.enabled')) {
                    Ext.Msg.alert('Info'.t(), 'To view App Widgets enable the Reports App first!'.t());
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
                                visible: true,
                                lastFetchTime: null,
                                // bind: {
                                //     userCls: 'theme-{theme}'
                                // },
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
                            // DashboardQueue.addFirst(widgetCmp);
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
                    Util.handleException('This entry is not available and it should be removed!');
                }

            }
        }

        if (cellIndex === 2) {
            // highlights in the dashboard the widget which receives click event in the manager grid
            widgetCmp = dashboard.down('#' + record.get('entryId')) || dashboard.down('#' + record.get('type'));
            if (widgetCmp && !widgetCmp.isHidden()) {
                dashboard.addBodyCls('highlight');
                widgetCmp.addCls('highlight-item');
                dashboard.scrollTo(0, dashboard.body.getScrollTop() + widgetCmp.getEl().getY() - 107, {duration: 100});
            }
        }
    },


    removeWidget: function (btn) {
        btn.lookupViewModel().get('record').set('markedForDelete', btn.pressed);
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
        var me = this, wg, wgCmp;
        // var widgetsList = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');
        if (action === 'add') {
            Ung.dashboardSettings.widgets.list.push(widget.getData());
        }
        if (action === 'remove') {
            wg = Ext.Array.findBy(Ung.dashboardSettings.widgets.list, function (wg) {
                return wg.entryId === widget.get('entryId');
            });
            if (wg) {
                Ext.Array.remove(Ung.dashboardSettings.widgets.list, wg);
            }
        }
        if (action === 'save') {
            wg = Ext.Array.findBy(Ung.dashboardSettings.widgets.list, function (wg) {
                return wg.entryId === widget.get('entryId');
            });
            if (wg) {
                Ext.apply(wg, widget.getData());
            }
        }

        // try to save it
        Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings).then(function () {
            var wg2 = Ext.getStore('widgets').findRecord('entryId', widget.get('entryId')) || widget;
            if (action === 'remove') {
                me.lookup('dashboard').remove(wg2.get('entryId'));
                wg2.drop();
                cb(null);
                return;
            }

            if (action === 'add') {
                Ext.getStore('widgets').add(wg2);
                wg2.commit();
                if (wg2.get('enabled')) {
                    wgCmp = me.lookup('dashboard').add({
                        xtype: 'reportwidget',
                        itemId: wg2.get('entryId'),
                        visible: true,
                        lastFetchTime: null,
                        // bind: {
                        //     userCls: 'theme-{theme}'
                        // },
                        viewModel: {
                            data: {
                                widget: wg2,
                                entry: entry
                            }
                        }
                    });
                    Ext.defer(function () {
                        DashboardQueue.addFirst(wgCmp);
                    }, 1000);
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

                wgCmp = me.lookup('dashboard').down('#' + wg2.get('entryId'));
                var idx = Ext.getStore('widgets').indexOf(wg2);

                if (wgCmp.getXType() === 'component') {
                    if (wg2.get('enabled')) {
                        wgCmp.destroy();
                        me.lookup('dashboard').insert(idx, {
                            xtype: 'reportwidget',
                            itemId: wg2.get('entryId'),
                            visible: true,
                            lastFetchTime: null,
                            // bind: {
                            //     userCls: 'theme-{theme}'
                            // },
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
                        DashboardQueue.addFirst(wgCmp);
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
            cb(wg2);
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
        var me = this, vm = me.getViewModel();
        Ext.MessageBox.confirm('Warning'.t(),
            'This will overwrite the current dashboard settings with the defaults.'.t() + '<br/><br/>' +
            'Do you want to continue?'.t(),
            function (btn) {
                if (btn === 'yes') {
                    Rpc.asyncData('rpc.dashboardManager.resetSettingsToDefault').then(function () {
                        Rpc.asyncData('rpc.dashboardManager.getSettings')
                            .then(function (result) {
                                Ung.dashboardSettings = result;
                                Ext.getStore('widgets').loadData(result.widgets.list);
                                me.loadWidgets();
                                Util.successToast('Dashboard reset done!');
                                vm.set('managerVisible', false);
                            });
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
        var me = this, widget, dashboardCmp = me.lookupReference('dashboard');

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
                type: 'ReportEntry'
            };
            Ung.dashboardSettings.widgets.list.push(widget);
        }

        Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings).then(function () {
            if (!isWidget) {
                // add it in dashboard
                Ext.getStore('widgets').add(widget);
                // display widget in dashboard
                dashboardCmp.add({
                    xtype: 'reportwidget',
                    itemId: widget.entryId,
                    visible: true,
                    lastFetchTime: null,
                    // bind: {
                    //     userCls: 'theme-{theme}'
                    // },
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
            if (ex) { Util.handleException(ex); return false; }
            vm.set('deviceCount', result.list.length);
        });
    },

    onActivate: function () {
        DashboardQueue.paused = false;
        this.updateWidgetsVisibility();
        // var me = this;
        // if (me.activated) {
        //     return;
        // }
        // me.activated = true;
    },

    onDeactivate: function () {
        DashboardQueue.paused = true;
        var vm = this.getViewModel();
        if (vm.get('managerVisible')) {
            this.toggleManager();
        }
    },

    addWidget: function () {
        this.showWidgetEditor(null, null);
    },

    showWidgetEditor: function (widget, entry) {
        var me = this;
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
    },

    reorderWidgets: function () {
        var me = this, columns = me.lookup('dashboardManager').getColumns();
        columns[0].setHidden(false);
    },

    exportWidgets: function () {
        var widgetsArr = [], w;
        Ext.getStore('widgets').each(function (widget) {
            w = widget.getData();
            delete w._id;
            widgetsArr.push(w);
        });

        Ext.MessageBox.wait('Exporting Widgets...'.t(), 'Please wait'.t());
        var exportForm = document.getElementById('exportGridSettings');
        exportForm.gridName.value = 'Widgets'.t(); // used in exported file name
        exportForm.gridData.value = Ext.encode(widgetsArr);
        exportForm.submit();
        Ext.MessageBox.hide();
    },

    importWidgets: function () {
        var me = this;
        this.importDialog = this.getView().add({
            xtype: 'window',
            title: 'Import Widgets'.t(),
            renderTo: Ext.getBody(),
            modal: true,
            layout: 'fit',
            width: 450,
            items: [{
                xtype: 'form',
                border: false,
                url: 'gridSettings',
                bodyPadding: 10,
                layout: 'anchor',
                items: [{
                    xtype: 'radiogroup',
                    name: 'importMode',
                    simpleValue: true,
                    value: 'replace',
                    columns: 1,
                    vertical: true,
                    items: [
                        { boxLabel: '<strong>' + 'Replace current widgets'.t() + '</strong>', inputValue: 'replace' },
                        { boxLabel: '<strong>' + 'Prepend to current widgets'.t() + '</strong>', inputValue: 'prepend' },
                        { boxLabel: '<strong>' + 'Append to current widgets'.t() + '</strong>', inputValue: 'append' }
                    ]
                }, {
                    xtype: 'component',
                    margin: 10,
                    html: 'with widgets from'.t()
                }, {
                    xtype: 'filefield',
                    anchor: '100%',
                    fieldLabel: 'File'.t(),
                    labelAlign: 'right',
                    allowBlank: false,
                    validateOnBlur: false
                }, {
                    xtype: 'hidden',
                    name: 'type',
                    value: 'import'
                }],
                buttons: [{
                    text: 'Cancel'.t(),
                    iconCls: 'fa fa-ban fa-red',
                    handler: function () {
                        me.importDialog.close();
                    }
                }, {
                    text: 'Import'.t(),
                    iconCls: 'fa fa-check',
                    formBind: true,
                    handler: function (btn) {
                        btn.up('form').submit({
                            waitMsg: 'Please wait while the widgets are imported...'.t(),
                            success: function(form, action) {
                                if (!action.result) {
                                    Ext.MessageBox.alert('Warning'.t(), 'Import failed.'.t());
                                    return;
                                }
                                if (!action.result.success) {
                                    Ext.MessageBox.alert('Warning'.t(), action.result.msg);
                                    return;
                                }
                                me.importHandler(form.getValues().importMode, action.result.msg);
                                me.importDialog.close();
                            },
                            failure: function(form, action) {
                                Ext.MessageBox.alert('Warning'.t(), action.result.msg);
                            }
                        });
                    }
                }]
            }],
        });
        this.importDialog.show();
    },

    importHandler: function (importMode, newData) {
        var me = this, existingData = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');

        Ext.Array.forEach(existingData, function (rec) {
            delete rec._id;
        });

        if (importMode === 'replace') {
            Ext.getStore('widgets').removeAll();
        }
        if (importMode === 'append') {
            Ext.Array.insert(existingData, existingData.length, newData);
            newData = existingData;
        }
        if (importMode === 'prepend') {
            Ext.Array.insert(existingData, 0, newData);
            newData = existingData;
        }

        Ext.getStore('widgets').loadData(newData);
        me.loadWidgets();
    }

});
