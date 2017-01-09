/**
 * Dashboard view which holds the widgets and manager
 */
Ext.define('Ung.view.dashboard.Dashboard', {
    extend: 'Ext.container.Container',
    xtype: 'ung.dashboard',

    requires: [
        'Ung.view.dashboard.DashboardController',
        'Ung.view.dashboard.Queue',
        'Ung.widget.Report',
        'Ung.view.grid.ActionColumn',

        'Ung.widget.WidgetController',
        'Ung.widget.Information',
        'Ung.widget.Resources',
        'Ung.widget.CpuLoad',
        'Ung.widget.NetworkInformation',
        'Ung.widget.NetworkLayout',
        'Ung.widget.MapDistribution'
    ],

    controller: 'dashboard',
    viewModel: true,
    //viewModel: 'dashboard',

    config: {
        settings: null // the dashboard settings object
    },

    layout: 'border',

    defaults: {
        border: false
    },

    items: [{
        region: 'west',
        title: 'Manage Widgets'.t(),
        // weight: 30,
        width: 300,
        collapsible: true,
        //border: false,
        // shadow: false,
        animCollapse: false,
        // collapsed: false,
        // collapseMode: 'mini',
        titleCollapse: true,
        floatable: false,
        // cls: 'widget-manager',
        split: false,
        xtype: 'grid',
        reference: 'dashboardNav',
        // forceFit: true,
        hideHeaders: true,
        // disableSelection: true,
        // trackMouseOver: false,

        store: 'widgets',

        bodyStyle: {
            border: 0
        },
        viewConfig: {
            plugins: {
                ptype: 'gridviewdragdrop',
                dragText: 'Drag and drop to reorganize'.t(),
                dragZone: {
                    onBeforeDrag: function (data, e) {
                        return Ext.get(e.target).hasCls('drag-handle');
                    }
                }
            },
            stripeRows: false,
            getRowClass: function (record) {
                return !record.get('enabled') ? 'disabled' : '';
            },
            listeners: {
                drop: 'onDrop'
            }
        },
        columns: [{
            width: 14,
            align: 'center',
            sortable: false,
            hideable: false,
            resizable: false,
            menuDisabled: true,
            tdCls: 'drag-handle'
        }, {
            width: 30,
            align: 'center',
            sortable: false,
            hideable: false,
            resizable: false,
            menuDisabled: true,
            //handler: 'toggleWidgetEnabled',
            dataIndex: 'enabled',
            renderer: 'enableRenderer'
        }, {
            dataIndex: 'entryId',
            renderer: 'widgetTitleRenderer',
            flex: 1
        }, {
            xtype: 'actioncolumn',
            width: 30,
            align: 'center',
            sortable: false,
            hideable: false,
            resizable: false,
            menuDisabled: true,
            handler: 'removeWidget',
            renderer: function (value, meta, record) {
                return '<i class="material-icons" style="color: #999; font-size: 20px;">close</i>';
            }
        }/*, {
            xtype: 'actioncolumn',
            align: 'center',
            width: 25,
            sortable: false,
            hideable: false,
            resizable: false,
            menuDisabled: true,
            renderer: function (value, meta, record) {
                if (record.get('type') !== 'ReportEntry') {
                    return '';
                }
                return '<i style="font-size: 16px; color: #777; padding-top: 4px;" class="material-icons">settings</i>';
            }
        }*/],
        listeners: {
            itemmouseleave : 'onItemLeave',
            cellclick: 'onItemClick'
        },
        tbar: [{
            itemId: 'addWidgetBtn',
            text: Ung.Util.iconTitle('Add'.t(), 'add_circle-16'),
            // menu: Ext.create('Ext.menu.Menu', {
            //     mouseLeaveDelay: 0
            // })
        }, '->', {
            text: Ung.Util.iconTitle('Import'.t(), 'file_download-16'),
            // handler: 'applyChanges'
        }, {
            text: Ung.Util.iconTitle('Export'.t(), 'file_upload-16')
            //handler: 'applyChanges'
        }],
        bbar: [{
            text: Ung.Util.iconTitle('Reset'.t(), 'replay-16'),
            handler: 'resetDashboard'
        }, '->', {
            text: Ung.Util.iconTitle('Apply'.t(), 'save-16'),
            handler: 'applyChanges'
        }]
    }, {
        xtype: 'container',
        region: 'center',
        reference: 'dashboard',
        cls: 'dashboard',
        padding: 8,
        // scrollable: true,
        // dockedItems: [{
        //     xtype: 'toolbar',
        //     dock: 'top',
        //     border: false,
        //     items: [{
        //         xtype: 'button',
        //         text: 'Manage Widgets'.t()
        //     }]
        // }]
    }],
    listeners: {
        //afterrender: 'onAfterRender',
        showwidgeteditor: 'showWidgetEditor'
    }
});