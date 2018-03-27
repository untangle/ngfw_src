/**
 * Dashboard view which holds the widgets and manager
 */
Ext.define('Ung.view.dashboard.Dashboard', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.ung-dashboard',
    itemId: 'dashboardMain',

    controller: 'dashboard',
    viewModel: {
        data: {
            managerVisible: false,
            timeframe: 1
        },
        formulas: {
            timeframeText: function (get) {
                if (!get('timeframe')) {
                    return 1 + ' ' + 'hour'.t() + ' (' + 'default'.t() + ')';
                }
                return get('timeframe') + ' ' + (get('timeframe') === 1 ? 'hour'.t() + ' (' + 'default'.t() + ')' : 'hours'.t());
            }
        }
    },

    config: {
        settings: null // the dashboard settings object / not used, need to check
    },

    layout: 'border',

    defaults: {
        border: false
    },

    items: [{
        region: 'west',
        dockedItems: [{
            xtype: 'toolbar',
            dock: 'top',
            ui: 'footer',
            style: { background: '#D8D8D8' },
            items: [{
                xtype: 'component',
                html: 'Manage Widgets'.t()
            }, '->', {
                iconCls: 'fa fa-cog',
                focusable: false,
                menu: {
                    plain: true,
                    showSeparator: false,
                    mouseLeaveDelay: 0,
                    items: [
                        { text: 'Import'.t(), iconCls: 'fa fa-download', handler: 'importWidgets' },
                        { text: 'Export'.t(), iconCls: 'fa fa-upload', handler: 'exportWidgets' },
                        '-',
                        { text: 'Create New'.t(), iconCls: 'fa fa-area-chart', handler: 'addWidget' },
                        '-',
                        { text: 'Reorder'.t(), iconCls: 'fa fa-sort', handler: 'reorderWidgets' },
                        '-',
                        { text: 'Reset to Defaults'.t(), iconCls: 'fa fa-rotate-left', handler: 'resetDashboard' }
                    ]
                }
            }]
        }],

        itemId: 'dashboardManager',
        reference: 'dashboardManager',
        xtype: 'grid',
        width: 250,
        minWidth: 250,
        maxWidth: 350,
        bodyBorder: false,
        animCollapse: false,
        floatable: false,
        cls: 'widget-manager',
        // disableSelection: true, // if disabled the drag/drop ordering does not work
        split: true,
        hideHeaders: true,
        rowLines: false,
        hidden: true,
        bind: {
            hidden: '{!managerVisible}'
        },
        store: 'widgets',
        viewConfig: {
            plugins: {
                ptype: 'gridviewdragdrop',
                dragText: 'Drag and drop to reorganize'.t(),
                dragZone: {
                    onBeforeDrag: function (data, e) {
                        return Ext.get(e.target).hasCls('fa-align-justify');
                    }
                }
            },
            stripeRows: false,
            getRowClass: function (record) {
                var cls = !record.get('enabled') ? 'disabled' : '';
                cls += record.get('markedForDelete') ? ' will-remove' : '';
                return cls;
            },
            listeners: {
                drop: 'onDrop'
            }
        },
        columns: [{
            width: 28,
            align: 'center',
            hidden: true,
            renderer: function (val, meta) {
                meta.tdCls = 'reorder';
                return '<i class="fa fa-align-justify"></i>';
            }
        }, {
            width: 20,
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
            xtype: 'widgetcolumn',
            width: 30,
            align: 'center',
            widget: {
                xtype: 'button',
                width: 24,
                enableToggle: true,
                iconCls: 'fa fa-trash fa-gray',
                hidden: true,
                focusable: false,
                bind: {
                    hidden: '{record.type !== "ReportEntry"}',
                    iconCls: 'fa fa-trash {record.markedForDelete ? "fa-red" : "fa-gray"}',
                },
                handler: 'removeWidget'
            }
        }],
        listeners: {
            itemmouseleave : 'onItemLeave',
            cellclick: 'onItemClick'
        },
        fbar: ['->', {
            text: 'Cancel'.t(),
            iconCls: 'fa fa-ban',
            handler: 'toggleManager'
        }, {
            text: 'Apply'.t(),
            iconCls: 'fa fa-floppy-o',
            handler: 'applyChanges'
        }]
    }, {
        region: 'center',
        reference: 'dashboard',
        itemId: 'dashboard',
        bodyCls: 'dashboard',
        bodyPadding: 8,
        border: false,
        bodyBorder: false,
        scrollable: true,
        dockedItems: [{
            xtype: 'toolbar',
            dock: 'top',
            ui: 'footer',
            style: { background: '#D8D8D8' },
            items: [{
                text: 'Settings'.t(),
                iconCls: 'fa fa-cog',
                handler: 'toggleManager',
                hidden: true,
                bind: {
                    hidden: '{managerVisible}'
                }
            }, {
                xtype: 'globalconditions',
                context: 'DASHBOARD',
                hidden: true,
                bind: {
                    hidden: '{!reportsAppStatus.installed || !reportsAppStatus.enabled}'
                }
            }, {
                xtype: 'container',
                itemId: 'since',
                margin: '0 5',
                layout: {
                    type: 'hbox',
                    align: 'middle'
                },
                hidden: true,
                bind: {
                    hidden: '{!reportsAppStatus.installed || !reportsAppStatus.enabled}'
                },
                items: [{
                    xtype: 'component',
                    margin: '0 5 0 0',
                    style: {
                        fontSize: '11px'
                    },
                    html: '<strong>' + 'Since:'.t() + '</strong>'
                }, {
                    xtype: 'button',
                    iconCls: 'fa fa-clock-o',
                    text: 'Today'.t(),
                    focusable: false,
                    menu: {
                        plain: true,
                        showSeparator: false,
                        mouseLeaveDelay: 0,
                        items: [
                            { text: '1 Hour ago'.t(), value: 1 },
                            { text: '3 Hours ago'.t(), value: 3 },
                            { text: '6 Hours ago'.t(), value: 6 },
                            { text: '12 Hours ago'.t(), value: 12 },
                            { text: '24 Hours ago'.t(), value: 24 }
                        ],
                        listeners: {
                            click: 'updateSince'
                        }
                    }
                }],
            }]
        }]
    }],
    listeners: {
        showwidgeteditor: 'showWidgetEditor'
    }
});
