Ext.define('Ung.widget.WidgetController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.widget',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            afterdata: 'onAfterData',
            beforedestroy: 'onBeforeRemove'
            // show: 'onShow'
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
        var me = this;
        cmp.getEl().on({
            click: function (e) {
                var vm = me.getViewModel();
                if (e.target.dataset.action === 'refresh') {
                    me.addToQueue();
                }

                // if (e.target.dataset.action === 'redirect') {
                //     Ung.app.redirectTo('#reports/' + me.getViewModel().get('entry.url'), true);
                // }

                if (e.target.dataset.action === 'style') {
                    var idx;

                    if (vm.get('entry.type').indexOf('TIME_GRAPH') >= 0) {
                        var timeStyles = ['LINE', 'AREA', 'BAR', 'BAR_OVERLAPPED', 'BAR_STACKED'];
                        var timeStyle = vm.get('entry.timeStyle');
                        idx = Ext.Array.indexOf(timeStyles, timeStyle);
                        if (idx < timeStyles.length - 1) {
                            vm.set('entry.timeStyle', timeStyles[idx + 1]);
                        } else {
                            vm.set('entry.timeStyle', timeStyles[0]);
                        }
                    }

                    if (vm.get('entry.type').indexOf('PIE_GRAPH') >= 0) {
                        var pieStyles = ['PIE', 'PIE_3D', 'DONUT', 'DONUT_3D', 'COLUMN', 'COLUMN_3D'];
                        var pieStyle = vm.get('entry.pieStyle');
                        idx = Ext.Array.indexOf(pieStyles, pieStyle);
                        if (idx < pieStyles.length - 1) {
                            vm.set('entry.pieStyle', pieStyles[idx + 1]);
                        } else {
                            vm.set('entry.pieStyle', pieStyles[0]);
                        }
                    }

                }

                if (e.target.dataset.action === 'settings') {
                    if (me.getView().up('#dashboard').down('window')) {
                        me.getView().up('#dashboard').down('window').close();
                    }

                    me.getView().up('#dashboard').getController().showWidgetEditor(vm.get('widget'), vm.get('entry'));

                    // me.settingsWin = me.getView().add({
                    //     xtype: 'window',
                    //     width: me.getView().getWidth() - 40,
                    //     height: me.getView().getHeight() - 100,
                    //     // height: '90%',
                    //     modal: true,
                    //     header: false,
                    //     title: 'Widget Settings',
                    //     constrain: true,
                    //     layout: 'fit',
                    //     items: [{
                    //         xtype: 'form',
                    //         border: false,
                    //         bodyPadding: 10,
                    //         layout: {
                    //             type: 'vbox',
                    //             align: 'stretch'
                    //         },
                    //         // defaults: {
                    //         //     labelWidth: 150,
                    //         //     // labelAlign: 'top',
                    //         //     width: 250,
                    //         // },
                    //         items: [{
                    //             xtype: 'component',
                    //             html: '<strong>' + 'Refresh Interval'.t() + '</strong>:<br/> <span style="font-size: 11px; color: #777;">' + 'Leave blank for no Auto Refresh'.t() + '</span>'
                    //         }, {
                    //             xtype: 'container',
                    //             layout: { type: 'hbox', align: 'middle' },
                    //             margin: '2 0 10 0',
                    //             items: [{
                    //                 xtype: 'numberfield',
                    //                 width: 50,
                    //                 maxValue: 600,
                    //                 minValue: 10,
                    //                 allowBlank: true,
                    //                 margin: '0 5 0 0',
                    //                 bind: '{widget.refreshIntervalSec}'
                    //             }, {
                    //                 xtype: 'component',
                    //                 style: {
                    //                     fontSize: '11px',
                    //                     color: '#777'
                    //                 },
                    //                 html: '(seconds)'.t()
                    //             }]
                    //         }, {
                    //             xtype: 'component',
                    //             html: '<strong>' + 'Timeframe'.t() + '</strong>:<br/> <span style="font-size: 11px; color: #777;">' + 'The number of hours to query the latest data. Leave blank for last day.'.t() + '</span>'
                    //         }, {
                    //             xtype: 'container',
                    //             layout: { type: 'hbox', align: 'middle' },
                    //             margin: '2 0',
                    //             items: [{
                    //                 xtype: 'numberfield',
                    //                 width: 50,
                    //                 maxValue: 72,
                    //                 minValue: 1,
                    //                 allowBlank: true,
                    //                 margin: '0 5 0 0',
                    //                 bind: '{_timeframe}'
                    //             }, {
                    //                 xtype: 'component',
                    //                 style: {
                    //                     fontSize: '11px',
                    //                     color: '#777'
                    //                 },
                    //                 html: '(hours)'.t()
                    //             }]
                    //         }]
                    //     }],
                    //     buttons: [{
                    //         text: 'Remove'.t(),
                    //         iconCls: 'fa fa-trash'
                    //     }, '->', {
                    //         text: 'Cancel'.t(),
                    //         iconCls: 'fa fa-ban',
                    //         handler: function (btn) {
                    //             btn.up('window').close();
                    //         }
                    //     }, {
                    //         text: 'Save'.t(),
                    //         iconCls: 'fa fa-save',
                    //         handler: 'onSave'
                    //     }],
                    //     listeners: {
                    //         beforeclose: 'onSettingsBeforeClose'
                    //     }
                    // });
                    // me.settingsWin.show();
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
        setTimeout(function () {
            widget.removeCls('adding');
        }, 100);


        widget.getViewModel().bind('{widget.enabled}', function (enabled) {
            if (enabled && Ext.isFunction(widget.fetchData)) {
                DashboardQueue.add(widget);
            }
        });
        // widget.getViewModel().bind('{widget.timeframe}', function (tf) {
        //     if (enabled && Ext.isFunction(widget.fetchData)) {
        //         Ung.view.dashboard.Queue.add(widget);
        //     }
        // });
        widget.getViewModel().notify();
    },

    onAfterData: function () {
        var widget = this.getView();
        Ung.view.dashboard.Queue.next();
        if (widget.refreshIntervalSec && widget.refreshIntervalSec > 0) {
            widget.refreshTimeoutId = setTimeout(function () {
                Ung.view.dashboard.Queue.add(widget);
            }, widget.refreshIntervalSec * 1000);
        }
    },

    onBeforeRemove: function (widget) {
        // remove widget from queue if; important if removal is happening while fetching data
        // Ung.view.dashboard.Queue.remove(widget);
        if (widget.tout) {
            clearTimeout(widget.tout);
            widget.tout = null;
        }
    },

    onShow: function (widget) {
        console.log('onShow');
        widget.removeCls('adding');
        // console.log('on show', widget.getViewModel().get('widget.type'));
        // if (Ext.isFunction(widget.fetchData)) {
        //     Ung.view.dashboard.Queue.add(widget);
        // }
    },

    addToQueue: function () {
        // var widget = this.getView();
        // if (widget.refreshTimeoutId) {
        //     clearTimeout(widget.refreshTimeoutId);
        // }
        DashboardQueue.addFirst(this.getView());
    },

    // cancelEdit: function (btn) {
    //     var me = this, vm = this.getViewModel();
    //     if (me.settingsWin) {
    //         me.settingsWin.close();
    //     }
    //     // btn.up('window').close();
    // },

    // onSave: function () {
    //     var me = this, vm = this.getViewModel();
    //     // save is done in Dashboard Controller
    //     Ext.fireEvent('saveWidget', function () {
    //         vm.get('widget').commit();
    //         me.settingsWin.close();
    //         me.addToQueue();
    //     });
    // },

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
            Rpc.asyncData('rpc.deviceTable.getDevices')
                .then(function (result) {
                    // reset the number of devices
                    Ext.Array.each(me.getView().query('interfaceitem'), function (intfCmp) {
                        intfCmp.getViewModel().set('devicesCount', 0);
                    });

                    Ext.Array.each(result.list, function (device) {
                        var intfCmp = me.getView().down('#intf_' + device.interfaceId), devNo;
                        if (intfCmp) {
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
