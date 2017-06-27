Ext.define('Ung.widget.WidgetController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.widget',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            afterdata: 'onAfterData',
            beforedestroy: 'onBeforeRemove'
        },
        '#header': {
            render: 'headerRender'
        }
    },

    listen: {
        store: {
            '#stats': {
                // used just to fetch devices count for Network Layout widget
                datachanged: 'onStatsUpdate'
            }
        }
    },

    headerRender: function (cmp) {
        var me = this, wg = me.getView(), vm = me.getViewModel();
        cmp.getEl().on({
            click: function (e) {
                // on refresh
                if (e.target.dataset.action === 'refresh') {
                    wg.lastFetchTime = null; // reset fetch time
                    DashboardQueue.addFirst(wg); // add it first
                }
                // on settings
                if (e.target.dataset.action === 'settings') {
                    // if (wg.up('#dashboard').down('window')) {
                    //     wg.up('#dashboard').down('window').close();
                    // }
                    wg.up('#dashboard').getController().showWidgetEditor(vm.get('widget'), vm.get('entry'));
                }

                // on download
                if (e.target.dataset.action === 'download') {
                    var chart = wg.down('graphreport').getController().chart;
                    if (chart) {
                        chart.exportChart();
                    }
                }

            }
        });
    },

    init: function (view) {
        var vm = view.getViewModel(), entryType;
        if (vm.get('entry')) {
            entryType = vm.get('entry.type');
            if (entryType === 'TIME_GRAPH' || entryType === 'TIME_GRAPH_DYNAMIC') {
                view.add({ xtype: 'graphreport', itemId: 'report-widget',  reference: 'chart', height: 248, widgetDisplay: true });
            }

            if (entryType === 'PIE_GRAPH') {
                view.add({ xtype: 'graphreport', itemId: 'report-widget', reference: 'chart',  height: 248, widgetDisplay: true });
            }

            if (entryType === 'TEXT') {
                view.add({ xtype: 'textreport', itemId: 'report-widget', height: 248 });
            }


            if (entryType === 'EVENT_LIST') {
                view.add({ xtype: 'eventreport', itemId: 'report-widget', height: 248 });
            }
        }
    },

    onAfterRender: function (widget) {
        widget.getViewModel().bind('{widget.enabled}', function (enabled) {
            if (enabled && (Ext.isFunction(widget.fetchData) || widget.down('#report-widget'))) {
                widget.visible = true; DashboardQueue.add(widget);
            }
        });
        widget.getViewModel().notify();
    },

    onAfterData: function () {
        var widget = this.getView();
        Ung.view.dashboard.Queue.next();
        if (widget.refreshIntervalSec && widget.refreshIntervalSec > 0) {
            widget.refreshTimeoutId = setTimeout(function () {
                DashboardQueue.add(widget);
            }, widget.refreshIntervalSec * 1000);
        }
    },

    onBeforeRemove: function (widget) {
        // remove widget from queue if; important if removal is happening while fetching data
        if (widget.tout) {
            clearTimeout(widget.tout);
            widget.tout = null;
        }
    },

    onSettingsBeforeClose: function () {
        var vm = this.getViewModel();
        if (vm.get('widget').dirty) {
            vm.get('widget').reject();
        }
    },


    // not used
    resizeWidget: function () {
        var view = this.getView();
        if (view.hasCls('small')) {
            view.removeCls('small').addCls('medium');
        } else {
            if (view.hasCls('medium')) {
                view.removeCls('medium').addCls('large');
            } else {
                if (view.hasCls('large')) {
                    view.removeCls('large').addCls('x-large');
                } else {
                    if (view.hasCls('x-large')) {
                        view.removeCls('x-large').addCls('small');
                    }
                }
            }
        }
        view.updateLayout();
    },

    // used only for Network Layout widget devices number update when stats change
    onStatsUpdate: function () {
        var me = this;
        if (me.getView().getXType() === 'networklayoutwidget') {
            // get devices
            Rpc.asyncData('rpc.hostTable.getHosts')
                .then(function (result) {
                    // reset the number of devices
                    Ext.Array.each(me.getView().query('interfaceitem'), function (intfCmp) {
                        intfCmp.getViewModel().set('devicesCount', 0);
                    });

                    Ext.Array.each(result.list, function (device) {
                        var intfCmp = me.getView().down('#intf_' + device.interfaceId), devNo;
                        if (intfCmp && device.active) {
                            devNo = intfCmp.getViewModel().get('devicesCount') || 0;
                            intfCmp.getViewModel().set('devicesCount', devNo += 1);
                        }
                    });
                }, function (ex) {
                    console.log(ex);
                });
        }
    }

});
