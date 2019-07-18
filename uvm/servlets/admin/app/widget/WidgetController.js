Ext.define('Ung.widget.WidgetController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.widget',

    control: {
        '#': {
            beforedestroy: 'onBeforeDestroy',
            afterrender: 'onAfterRender'
        },
        '#header': {
            render: 'headerRender'
        },
        '#menu': {
            render: 'menuRender'
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

    queuedSaveTaskWait: 0,
    queuedSaveTaskMaxWait: 1000,
    queuedSaveTaskDelay: 100,
    onAfterRender: function (widget) {
        var me = this, vm = me.getViewModel(), eventGrid = widget.down('eventreport > ungrid');
        // add to queue non report widgets when enabled
        vm.bind('{widget.enabled}', function (enabled) {
            if (enabled && (Ext.isFunction(widget.fetchData) || widget.getXType() === 'reportwidget')) {
                widget.visible = true;
                DashboardQueue.add(widget);
            }
        });

        // highlight only widgets for which conditions apply
        if (widget.getXType() === 'reportwidget') {
            vm.bind('{query.conditions}', function (conditions) {
                widget.lastFetchTime = null;
                if (conditions.length === 0) {
                    widget.unmask();
                    return;
                }

                if (!TableConfig.containsColumns(
                        vm.get('entry.table'),
                        Ext.Array.map(conditions, function(condition){
                            return condition.get('column');
                        })
                        )) {
                    widget.mask();
                } else {
                    widget.unmask();
                }
            });
        }

        // for EVENT_LIST widgets save displayColumns setting on column hide/show
        if (eventGrid) {
            me.queuedSaveTask = new Ext.util.DelayedTask( Ext.bind(function(){
                this.queuedSaveTaskWait = this.queuedSaveTaskWait - this.queuedSaveTaskDelay;
                if(this.queuedSaveTaskWait > 0){
                    this.queuedSaveTask.delay( this.queuedSaveTaskDelay );
                }else{
                    Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings)
                        .then(function () {
                        });
                }
            }, me) );

            eventGrid.addListener('columnhide', function () {
                me.updateWidgetColumns(eventGrid);
            });
            eventGrid.addListener('columnshow', function () {
                me.updateWidgetColumns(eventGrid);
            });
        }
    },


    updateWidgetColumns: function (grid) {
        var me = this, vm = me.getViewModel(), columns = [];
        Ext.Array.each(grid.getColumns(), function (col) {
            if (!col.isHidden()) {
                columns.push(col.dataIndex);
            }
        });
        vm.set('widget.displayColumns', columns);
        me.queuedSaveTaskWait = me.queuedSaveTaskMaxWait;
        me.queuedSaveTask.delay( me.queuedSaveTaskDelay );
    },

    // onRender: function (widget) {
    //     var me = this;
    //     widget.getEl().on({
    //         mouseleave: function () {
    //             me.closeMenu();
    //         }
    //     });
    // },

    headerRender: function (cmp) {
        var me = this, wg = me.getView();
        cmp.getEl().on({
            click: function (e) {
                // on refresh
                if (e.target.dataset.action === 'refresh') {
                    wg.lastFetchTime = null; // reset fetch time
                    DashboardQueue.addFirst(wg); // add it first
                }
                // show menu
                if (e.target.dataset.action === 'menu') {
                    wg.down('#menu').setStyle({ zIndex: 999 });
                    wg.addCls('showmenu');
                }
            }
        });
    },

    menuRender: function (cmp) {
        var me = this, wg = me.getView(), vm = me.getViewModel();
        cmp.getEl().on({
            click: function (e) {
                var action = e.target.dataset.action;

                if (!action) { return; }
                // close menu but only when not selecting size
                if (action.indexOf('size') < 0) {
                    me.closeMenu();
                }

                // on download
                if (action === 'download') {
                    var chart = wg.down('graphreport').getController().chart;
                    if (chart) {
                        chart.exportChart({
                            filename: vm.get('entry.title').trim().replace(/ /g, '_')
                        }, {
                            chart: {
                                backgroundColor: '#FFFFFF'
                            }
                        });
                    }
                }

                // on export
                if (action === 'export') {
                    var grid = wg.down('grid');
                    var exportForm = document.getElementById('exportGridSettings');

                    var data = Ext.Array.pluck(grid.getStore().getRange(), 'data');
                    Ext.Array.forEach(data, function (rec) {
                        delete rec._id;
                    });
                    exportForm.gridName.value = vm.get('entry.title').trim().replace(/ /g, '_') + '-WIDGET-';
                    exportForm.gridData.value = Ext.encode(data);
                    exportForm.submit();
                }

                // on reports
                if (action === 'reports') {
                    Ung.app.redirectTo(e.target.dataset.url);
                }
                // on size
                if (action.indexOf('size') >= 0) {
                    var newSize = e.target.dataset.action.replace('size-', '');
                    vm.set('widget.size', newSize.toUpperCase());
                }

                // on save
                if (action === 'save') {
                    vm.set('widget.refreshIntervalSec', parseInt(wg.down('#menu').getEl().dom.getElementsByTagName('select')[0].value, 10));
                    Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings)
                        .then(function () {
                            wg.lastFetchTime = null; // reset fetch time
                            DashboardQueue.addFirst(wg);
                        });
                }
            }
        });
    },

    closeMenu: function () {
        var view = this.getView();
        view.removeCls('showmenu');
        if (!view.down('#menu')) { return; }
        Ext.defer(function () {
            view.down('#menu').setStyle({ zIndex: -1 });
        }, 300);
    },


    init: function (view) {
        var vm = view.getViewModel(), entryType;
        if (vm.get('entry')) {
            switch(vm.get('entry.type')) {
            case 'PIE_GRAPH':
            case 'TIME_GRAPH':
            case 'TIME_GRAPH_DYNAMIC':
                entryType = 'graphreport'; break;
            case 'TEXT':
                entryType = 'textreport'; break;
            case 'EVENT_LIST':
                entryType = 'eventreport'; break;
            }

            view.add({
                xtype: entryType,
                itemId: 'report-widget',
                height: 250,
                isWidget: true
            });
        }
    },

    onBeforeDestroy: function (widget) {
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
