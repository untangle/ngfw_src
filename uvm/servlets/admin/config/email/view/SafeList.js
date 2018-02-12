Ext.define('Ung.config.email.view.SafeList', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-email-safelist',
    itemId: 'safe-list',
    scrollable: true,

    title: 'Safe List'.t(),

    viewModel: {
        formulas: {
            globalSafeListMap: function (get) {
                if (get('globalSafeList')) {
                    return Ext.Array.map(get('globalSafeList'), function (email) {
                        return { emailAddress: email };
                    });
                }
                return {};
            }
        },
        stores: {
            globalSL: { data: '{globalSafeListMap}' },
            userSL: { data: '{userSafeList.list}' }
        }
    },

    layout: 'border',

    items: [{
        xtype: 'ungrid',
        itemId: 'safeListStore',
        region: 'center',
        title: 'Global Safe List'.t(),

        emptyText: 'No Global Safe List email addresses defined'.t(),

        bind: '{globalSL}',

        tbar: ['@addInline', '->', '@import', '@export'],
        recordActions: ['delete'],
        emptyRow: {
        },

        topInsert: true,

        columns: [{
            header: 'Email Address'.t(),
            dataIndex: 'emailAddress',
            width: Renderer.emailWidth,
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                bind: '{record.emailAddress}',
                emptyText: '[enter email]'.t(),
                // vtype: 'email'
            },
            renderer: function (value) {
                if (!value || value.length === 0) {
                    return '<span style="color: red;">[add email address here]'.t() + '</span>';
                }
                return value;
            }
        }]
    }, {
        xtype: 'ungrid',
        reference: 'userSafeListGrid',
        region: 'east',

        width: '50%',
        split: true,

        title: 'Per User Safe Lists'.t(),

        emptyText: 'No Per User Safe List email addresses defined'.t(),

        selModel: {
            selType: 'checkboxmodel'
        },

        bind: '{userSL}',

        tbar: [{
            text: 'Purge Selected'.t(),
            iconCls: 'fa fa-circle fa-red',
            handler: 'externalAction',
            action: 'purgeUserSafeList',
            disabled: true,
            bind: {
                disabled: '{!userSafeListGrid.selection}'
            }
        }],

        columns: [{
            header: 'Account Address'.t(),
            dataIndex: 'emailAddress',
            width: Renderer.emailWidth,
            flex: 1
        }, {
            header: 'Safe List Size'.t(),
            width: Renderer.messageWidth,
            dataIndex: 'count'
        // }, {
        //     // todo: the show detail when available data
        //     header: 'Show Detail'.t(),
        //     width: Renderer.actionWidth + 20,
        }],
    }]

});
