Ext.define('Ung.apps.captive-portal.view.Status', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-captive-portal-status',
    itemId: 'status',
    title: 'Status'.t(),
    scrollable: true,

    layout: 'border',
    items: [{
        region: 'center',
        border: false,
        bodyPadding: 10,
        scrollable: 'y',
        items: [{
            xtype: 'component',
            cls: 'app-desc',
            html: '<img src="/icons/apps/captive-portal.svg" width="80" height="80"/>' +
                '<h3>Captive Portal</h3>' +
                '<p>' + 'Captive Portal allows administrators to require network users to complete a defined process, such as logging in or accepting a network usage policy, before accessing the internet.'.t() + '</p>'
        }, {
            xtype: 'applicense',
            hidden: true,
            bind: {
                hidden: '{!license || !license.trial}'
            }
        }, {
            xtype: 'appstate',
        }, {
            xtype: 'fieldset',
            title: '<i class="fa fa-clock-o"></i> ' + 'Active Sessions'.t(),
            padding: 10,
            margin: '20 0',
            cls: 'app-section',

            layout: 'fit',

            collapsed: true,
            disabled: true,
            bind: {
                collapsed: '{!state.on}',
                disabled: '{!state.on}'
            },
            items: [{
                xtype: 'ungrid',
                itemId: 'activeUsers',
                minHeight: 150,
                trackMouseOver: false,
                sortableColumns: true,
                enableColumnHide: false,

                emptyText: 'No Active Sessions',

                bind: {
                    store: {
                        data: '{activeUsers}'
                    }
                },

                plugins: ['gridfilters'],

                columns: [{
                    header: 'User Address'.t(),
                    dataIndex: 'userAddress',
                    width: Renderer.usernameWidth,
                    filter: Renderer.stringFilter
                }, {
                    header: 'User Name'.t(),
                    dataIndex: 'userName',
                    width: Renderer.usernameWidth,
                    flex: 1,
                    filter: Renderer.stringFilter
                }, {
                    header: 'Login Time'.t(),
                    dataIndex: 'sessionCreation',
                    width: Renderer.timestampWidth,
                    renderer: Renderer.timestamp,
                    filter: Renderer.timestampFilter
                }, {
                    header: 'Session Count'.t(),
                    dataIndex: 'sessionCounter',
                    width: Renderer.sizeWidth,
                    renderer: Renderer.count,
                    filter: Renderer.numericFilter
                }, {
                    header: 'Logout'.t(),
                    xtype: 'actioncolumn',
                    width: Renderer.actionWidth,
                    align: 'center',
                    iconCls: 'fa fa-minus-circle',
                    handler: 'externalAction',
                    action: 'logoutUser'
                }],
                bbar: [ '@refresh', '@reset']
            }]
        }, {
            xtype: 'appreports'
        }]
    }, {
        region: 'west',
        border: false,
        width: Math.ceil(Ext.getBody().getViewSize().width / 4),
        split: true,
        items: [{
            xtype: 'appsessions',
            region: 'north',
            height: 200,
            split: true,
        }, {
            xtype: 'appmetrics',
            region: 'center'
        }],
        bbar: [{
            xtype: 'appremove',
            width: '100%'
        }]
    }]
});
