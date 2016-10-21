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
        items: [{
            xtype: 'button',
            itemId: 'policies-menu',
            hidden: true,
            style: {
                marginRight: '5px'
            }
        }, {
            xtype: 'button',
            html: Ung.Util.iconTitle('Manage Widgets', 'settings-16'),
            handler: 'managerHandler'
        }]
    }, {
        //baseCls: 'dashboard-manager',
        title: 'Manage Widgets'.t(),
        collapsible: true,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        region: 'west',
        //border: false,
        shadow: false,
        animCollapse: false,
        bind: {
            collapsed: '{!managerOpen}'
        },
        collapsed: false,
        titleCollapse: true,
        floatable: false,
        cls: 'widget-manager',
        //split: true,
        items: [{
            xtype: 'grid',
            reference: 'dashboardNav',
            forceFit: true,
            flex: 1,
            width: 300,
            border: false,
            header: false,
            hideHeaders: true,
            //disableSelection: true,
            //trackMouseOver: false,

            viewModel: {
                stores: {
                    wg: {
                        source: {
                            type: 'widgets' // chained store
                        }
                    }
                }
            },

            bind: {
                store: '{wg}'
            },
            bodyStyle: {
                border: 0
            },
            viewConfig: {
                plugins: {
                    ptype: 'gridviewdragdrop',
                    dragText: 'Drag and drop to reorganize'
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
                width: 25,
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
                text: Ung.Util.iconTitle('Add'.t(), 'add-16')
                //handler: 'resetDashboard'
            }, '->', {
                text: Ung.Util.iconTitle('Import'.t(), 'file_download-16')
                //handler: 'applyChanges'
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
            xtype: 'component',
            html: '<table>' +
                    '<tr><td style="text-align: right; width: 50px;"><i class="material-icons" style="color: #333; font-size: 16px; vertical-align: middle;">check_box</i> | <i class="material-icons" style="color: #999; font-size: 16px; vertical-align: middle;">check_box_outline_blank</i></td><td>' +
                    'enables or disables the widget'.t() + '</td></tr>' +
                    '<tr><td style="width: 45px; text-align: right; vertical-align: top;"><i class="material-icons" style="color: #F00; font-size: 16px; vertical-align: middle;">info_outline</i></td><td>' + 'requires Reports and App to be installed'.t() + '</td></tr>' +
                    '<tr><td style="text-align: right;"><i class="material-icons" style="color: #999; font-size: 16px; vertical-align: middle;">format_line_spacing</i></td><td>' + 'drag widgets to sort them'.t() + '</td></tr>' +
                    '</table>',
            style: {
                color: '#555',
                fontSize: '11px',
                background: '#efefef'
            },
            padding: '10',
            border: false
        }]
    }, {
        xtype: 'container',
        region: 'center',
        reference: 'dashboard',
        baseCls: 'dashboard',
        scrollable: true
    }],
    listeners: {
        //afterrender: 'onAfterRender',
        showwidgeteditor: 'showWidgetEditor'
    }
});