Ext.define('Ung.view.dashboard.ManagerController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.dashboardmanager',
    // viewModel: true,

    control: {
        '#': {
            afterrender: 'onAfterRender',
            show: function (grid) {
                // refresh the grid to update renderers
                grid.getView().refresh();
            }
        }
    },

    listen: {
        global: {
            // events fired by widget store
            addwidgets: 'onAddWidgets',
            removewidgets: 'onRemoveWidgets',
            updatewidget: 'onUpdateWidget',
            appinstall: 'onAppInstall',
            appremove: 'onAppRemove'
        }
    },

    onAfterRender: function (view) {
        var me = this;
        // the dasboard widgets holder
        me.dashboard = view.up('#dashboardMain').lookup('dashboard');

        /**
         * (re)load widgets when Reports App installed/removed or enabled/disabled
         */
        me.getViewModel().bind('{reportsAppStatus}', function () {
            me.loadWidgets();
        });
    },

    /**
     * Render widgets into the dashboard
     */
    loadWidgets: function() {
        var me = this, vm = me.getViewModel(),
            widgetsCmps = [], entry;

        // refresh the dashboard manager grid if the widgets were affected
        // me.lookup('dashboardManager').getView().refresh();
        me.dashboard.removeAll(true);

        Ext.getStore('widgets').each(function (record) {
            if (!record.get('enabled')) {
                widgetsCmps.push({
                    xtype: 'component',
                    itemId: record.get('itemId'),
                    hidden: true
                });
                return;
            }

            if (record.get('type') !== 'ReportEntry') {
                widgetsCmps.push({
                    xtype: record.get('type').toLowerCase() + 'widget',
                    itemId: record.get('itemId'),
                    lastFetchTime: null,
                    visible: true,
                    viewModel: {
                        data: {
                            widget: record
                        }
                    }
                });
                return;
            }

            if (vm.get('reportsAppStatus.installed') && vm.get('reportsAppStatus.enabled')) {
                entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));
                if (entry && Ext.getStore('categories').findRecord('displayName', entry.get('category'))) {
                    widgetsCmps.push({
                        xtype: 'reportwidget',
                        itemId: record.get('itemId'),
                        lastFetchTime: null,
                        visible: true,
                        viewModel: {
                            data: {
                                widget: record,
                                entry: entry
                            }
                        }
                    });
                } else {
                    // report widget is enabled but App not installed
                    widgetsCmps.push({
                        xtype: 'component',
                        itemId: record.get('itemId'),
                        hidden: true
                    });
                }
            }

        });
        me.getView().getView().refresh(); // refres the grid view
        me.dashboard.add(widgetsCmps);
    },

    // listens on widgets store add event and adds widget component to dashboard
    onAddWidgets: function (store, records) {
        var me = this, entry;

        Ext.Array.each(records, function (record) {
            if (record.get('type') !== 'ReportEntry') {
                me.dashboard.add({
                    xtype: record.get('type').toLowerCase() + 'widget',
                    itemId: record.get('itemId'),
                    lastFetchTime: null,
                    visible: true,
                    viewModel: {
                        data: {
                            widget: record
                        }
                    }
                });
            } else {
                entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));
                me.dashboard.add({
                    xtype: 'reportwidget',
                    itemId: record.get('itemId'),
                    visible: true,
                    lastFetchTime: null,
                    viewModel: {
                        data: {
                            widget: record,
                            entry: entry
                        }
                    }
                });
            }
        });
        me.applyChanges();
    },

    onRemoveWidgets: function (store, records) {
        var me = this, widgetCmp;
        Ext.Array.each(records, function (rec) {
            widgetCmp = me.dashboard.down('#' + rec.get('itemId'));
            if (widgetCmp) { widgetCmp.destroy(); }
        });
        me.applyChanges();
    },


    onUpdateWidget: function (store, record) {
        var me = this, vm = me.getViewModel(), entry,
            enabled = record.get('enabled'),
            widgetCmp = me.dashboard.down('#' + record.get('itemId')),
            index = store.indexOf(record);

        if (widgetCmp) { widgetCmp.destroy(); }

        if (enabled) {
            if (record.get('type') !== 'ReportEntry') {
                widgetCmp = me.dashboard.insert(index, {
                    xtype: record.get('type').toLowerCase() + 'widget',
                    itemId: record.get('itemId'),
                    visible: true,
                    lastFetchTime: null,
                    viewModel: {
                        data: {
                            widget: record
                        }
                    }
                });
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

                if (entry) {
                    if (Ext.getStore('categories').findRecord('displayName', entry.get('category'))) {
                        widgetCmp = me.dashboard.insert(index, {
                            xtype: 'reportwidget',
                            itemId: record.get('itemId'),
                            visible: true,
                            lastFetchTime: null,
                            viewModel: {
                                data: {
                                    widget: record,
                                    entry: entry
                                }
                            }
                        });
                        // setTimeout(function () {
                        //     me.dashboard.scrollTo(0, me.dashboard.getEl().getScrollTop() + widgetCmp.getEl().getY() - 121, {duration: 300 });
                        // }, 100);
                    } else {
                        Ext.Msg.alert('Install required'.t(), Ext.String.format('To enable this Widget please install <strong>{0}</strong> app first!'.t(), entry.get('category')));
                    }
                } else {
                    Util.handleException('This entry is not available and it should be removed!');
                }

            }
            if (widgetCmp) {
                setTimeout(function () {
                    me.dashboard.scrollTo(0, me.dashboard.getEl().getScrollTop() + widgetCmp.getEl().getY() - 101, {duration: 300 });
                }, 100);
            }
        } else {
            me.dashboard.insert(index, {
                xtype: 'component',
                itemId: record.get('itemId'),
                hidden: true
            });
        }
    },

    newReportWidget: function () {
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

    /**
     * Method which sends modified dashboard settings to backend to be saved
     */
    applyChanges: function (btn) {
        var me = this, removable = Ext.getStore('widgets').queryRecords('markedForDelete', true);
        Ext.getStore('widgets').remove(removable);

        // because of the drag/drop reorder the settings widgets are updated to respect new ordering
        Ung.dashboardSettings.widgets.list = Ext.Array.pluck(Ext.getStore('widgets').getRange(), 'data');

        Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings)
            .then(function() {
                if (btn) { // show saved toast only when Apply button is used
                    Util.successToast('<span style="color: yellow; font-weight: 600;">Dashboard Saved!</span>');
                }
                Ext.getStore('widgets').sync();
                // me.toggleManager();

                me.dashboard.items.each(function (widgetCmp) {
                    if (Ext.getStore('widgets').find('itemId', widgetCmp.getItemId()) < 0) {
                        me.dashboard.remove(widgetCmp);
                    }
                });
            });

    },

    onCancel: function () {
        var me = this, vm = me.getViewModel(),
            columns = me.getView().getColumns();

        vm.set('managerVisible', false);
        columns[0].setHidden(true);
    },

    showOrderingColumn: function () {
        var me = this, columns = me.getView().getColumns();
        columns[0].setHidden(false);
    },

    /**
     * todo: after drag sort event
     */
    onDrop: function (app, data, overModel, dropPosition) {
        var me = this,
            widgetMoved = me.dashboard.down('#' + data.records[0].get('itemId')),
            widgetDropped = me.dashboard.down('#' + overModel.get('itemId'));

        /*
        widgetMoved.addCls('moved');

        window.setTimeout(function () {
            widgetMoved.removeCls('moved');
        }, 300);
        */

        if (dropPosition === 'before') {
            me.dashboard.moveBefore(widgetMoved, widgetDropped);
        } else {
            me.dashboard.moveAfter(widgetMoved, widgetDropped);
        }
    },

    onItemClick: function (table, td, cellIndex, record, tr, rowIndex, e) {
        var me = this, widgetCmp, entry;

        if (cellIndex === 1) {
            if (Ext.Array.contains(e.target.classList, 'fa-info-circle')) {
                entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));
                if (entry) {
                    Ext.Msg.alert('Install required'.t(), Ext.String.format('To enable this Widget please install <strong>{0}</strong> app first!'.t(), entry.get('category')));
                } else {
                    Util.handleException('This entry is not available and it should be removed!');
                }
                return;
            }
            // toggle visibility or show alerts
            record.set('enabled', !record.get('enabled'));
        }

        if (cellIndex === 2) {
            // highlights in the dashboard the widget which receives click event in the manager grid
            widgetCmp = me.dashboard.down('#' + record.get('itemId'));
            if (widgetCmp && !widgetCmp.isHidden()) {
                me.dashboard.addBodyCls('highlight');
                widgetCmp.addCls('highlight-item');
                me.dashboard.scrollTo(0, me.dashboard.body.getScrollTop() + widgetCmp.getEl().getY() - 101, {duration: 100});
            }
        }
    },

    /**
     * removes the above set highlight
     */
    onItemLeave: function (view, record) {
        var me = this, widgetCmp;
        if (this.tout) {
            window.clearTimeout(this.tout);
        }
        widgetCmp = me.dashboard.down('#' + record.get('itemId'));
        if (widgetCmp) {
            me.dashboard.removeBodyCls('highlight');
            widgetCmp.removeCls('highlight-item');
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

    // renderers
    enableRenderer: function (value, meta, record) {
        meta.tdCls = 'enable';
        if (record.get('type') !== 'ReportEntry') {
            return '<i class="fa ' + (value ? 'fa-check-circle-o' : 'fa-circle-o') + ' fa-lg"></i>';
        }
        var entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));
        if (!entry || !Ext.getStore('categories').findRecord('displayName', entry.get('category'))) {
            return '<i class="fa fa-info-circle fa-lg"></i>';
        }
        return '<i class="fa ' + (value ? 'fa-check-circle-o' : 'fa-circle-o') + ' fa-lg"></i>';
    },

    /**
     * renders the title of the widget in the dashboard manager grid, based on various conditions
     */
    widgetTitleRenderer: function (value, metaData, record) {
        var me = this, vm = me.getViewModel(), entry, title, unavailApp, enabled;
        enabled = record.get('enabled');

        if (!value) {
            return '<span style="' + (!enabled ? 'font-weight: 400; color: #777;' : 'font-weight: 600; color: #000;') + '">' + record.get('type') + '</span>'; // <br/><span style="font-size: 10px; color: #777;">Common</span>';
        }
        if (vm.get('reportsAppStatus.installed')) {
            entry = Ext.getStore('reports').findRecord('uniqueId', value);
            if (entry) {
                unavailApp = Ext.getStore('categories').findRecord('displayName', entry.get('category')) ? false : true;
                title = '<span style="' + ((unavailApp || !enabled) ? 'font-weight: 400; color: #777;' : 'font-weight: 600; color: #000;') + '">' + (entry.get('readOnly') ? entry.get('title').t() : entry.get('title')) + '</span>';
                return title;
            } else {
                return 'Unknown Widget'.t();
            }
        } else {
            return '<span style="color: #999;">' + 'App Widget'.t() + '</span>';
        }
    },

    importWidgets: function () {
        var me = this;
        me.importDialog = me.getView().add({
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

    populateReportsMenu: function (menu) {
        var menuItem = menu.down('#reportsMenu'),
            root = Ext.getStore('reportstree').getRoot(), items = [], subItems = [];

        root.eachChild(function (catNode) {
            subItems = [];
            catNode.eachChild(function (repNode) { // report node
                subItems.push({
                    text: repNode.get('text')
                });
            });
            items.push({
                text: catNode.get('text'),
                menu: {
                    plain: true,
                    showSeparator: false,
                    mouseLeaveDelay: 0,
                    items: subItems
                }
            });
        });
        menuItem.setMenu({
            plain: true,
            showSeparator: false,
            mouseLeaveDelay: 0,
            items: items
        });
    },


    /**
     * when a app is installed or removed apply changes to dashboard
     */
    onAppInstall: function (displayName) {
        var me = this, entry, index, wg;
        // refresh dashboard manager grid

        Ext.getStore('widgets').each(function (record) {
            index = Ext.getStore('widgets').indexOf(record);
            entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));
            if (entry && entry.get('category') === displayName) {
                // remove widget placeholder
                me.dashboard.remove(record.get('itemId'));

                wg = me.dashboard.insert(index, {
                    xtype: 'reportwidget',
                    itemId: record.get('itemId'),
                    lastFetchTime: null,
                    visible: true,
                    viewModel: {
                        data: {
                            widget: record,
                            entry: entry,
                        }
                    }
                });
                Ext.defer(function () {
                    DashboardQueue.addFirst(wg);
                }, 500);
            }
        });
    },

    onAppRemove: function (displayName) {
        var me = this, entry, index;
        Ext.getStore('widgets').each(function (record) {
            index = Ext.getStore('widgets').indexOf(record);
            entry = Ext.getStore('reports').findRecord('uniqueId', record.get('entryId'));
            if (entry && entry.get('category') === displayName) {
                // remove widget placeholder
                me.dashboard.remove(record.get('itemId'));
                me.dashboard.insert(index, {
                    xtype: 'component',
                    itemId: record.get('itemId'),
                    hidden: true
                });
            }
        });
    }
});
