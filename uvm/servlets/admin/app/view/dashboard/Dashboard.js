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
            managerVisible: false
        }
    },

    config: {
        settings: null // the dashboard settings object / not used, need to check
    },

    layout: 'fit',

    dockedItems: [{
        xtype: 'dashboardmanager',
        dock: 'left',
        width: 250,
        hidden: true,
        bind: {
            hidden: '{!managerVisible}'
        }
    }],

    items: [{
        reference: 'dashboard',
        itemId: 'dashboard',
        bodyCls: 'dashboard',
        bodyPadding: 8,
        border: false,
        // bodyBorder: false,
        scrollable: true,
        dockedItems: [{
            xtype: 'toolbar',
            dock: 'top',
            ui: 'footer',
            height: 30,
            style: { background: '#D8D8D8' },
            items: [{
                text: 'Settings'.t(),
                iconCls: 'fa fa-cog',
                focusable: false,
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
            }, {
                xtype: 'component',
                style: { fontSize: '12px' },
                html: '<i class="fa fa-info-circle"></i> <strong>Reports App not installed!</strong> Report based widgets are not available.',
                hidden: true,
                bind: {
                    hidden: '{reportsAppStatus.installed}'
                }
            }, {
                xtype: 'component',
                style: { fontSize: '12px' },
                html: '<i class="fa fa-info-circle"></i> <strong>Reports App is disabled!</strong> Report based widgets are not available.',
                hidden: true,
                bind: {
                    hidden: '{!reportsAppStatus.installed || reportsAppStatus.enabled}'
                }
            }]
        }]
    }],
    listeners: {
        showwidgeteditor: 'showWidgetEditor'
    }
});
