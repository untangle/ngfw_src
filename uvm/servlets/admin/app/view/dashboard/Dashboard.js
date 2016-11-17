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
        region: 'north',
        weight: 20,
        border: false,
        height: 44,
        itemId: 'apps-topnav',
        bodyStyle: {
            background: '#555',
            padding: '0 5px'
        },
        layout: {
            type: 'hbox',
            align: 'middle'
        },
        defaults: {
            xtype: 'button',
            baseCls: 'heading-btn'
        },
        items: [{
            html: Ung.Util.iconTitle('Manage Widgets'.t(), 'settings-16'),
            handler: 'managerHandler',
            bind: {
                hidden: '{managerOpen}'
            }
        }, {
            xtype: 'component',
            flex: 1
        }, {
            html: 'Sessions'.t(),
            href: '#sessions',
            hrefTarget: '_self'
        }, {
            html: 'Hosts'.t(),
            href: '#hosts',
            hrefTarget: '_self'
        }, {
            html: 'Devices'.t(),
            href: '#devices',
            hrefTarget: '_self'
        }]
    }, {
        region: 'west',
        weight: 30,
        collapsible: true,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        //border: false,
        header: false,
        shadow: false,
        animCollapse: false,
        collapsed: true,
        collapseMode: 'mini',
        bind: {
            collapsed: '{!managerOpen}'
        },
        //titleCollapse: true,
        floatable: false,
        cls: 'widget-manager',
        split: {
            size: 0
        },
        items: [{
            xtype: 'container',
            cls: 'heading',
            height: 44,
            border: false,
            layout: {
                type: 'hbox',
                align: 'stretch',
                pack: 'center'
            },
            items: [{
                xtype: 'component',
                flex: 1,
                padding: 10,
                html: 'Manage Widgets'.t()
            }, {
                xtype: 'button',
                width: 44,
                baseCls: 'manager-close-btn',
                html: '<i class="material-icons">close</i>',
                handler: 'managerHandler'
            }]
        }, {
            xtype: 'grid',
            reference: 'dashboardNav',
            forceFit: true,
            flex: 1,
            width: 300,
            border: false,
            header: false,
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
                renderer: 'widgetTitleRenderer'
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
        }, /*{
            xtype: 'component',
            html: '<table>' +
                    '<tr><td style="text-align: right;"><i class="material-icons" style="color: #999; font-size: 16px; vertical-align: middle;">info</i></td><td>' + 'drag widgets to sort them'.t() + '</td></tr>' +
                    '</table>',
            style: {
                color: '#555',
                fontSize: '11px',
                background: '#efefef'
            },
            padding: 5,
            border: false
        }*/]
    }, {
        xtype: 'container',
        region: 'center',
        reference: 'dashboard',
        baseCls: 'dashboard',
        padding: 8,
        scrollable: true
    }],
    listeners: {
        //afterrender: 'onAfterRender',
        showwidgeteditor: 'showWidgetEditor'
    }
});