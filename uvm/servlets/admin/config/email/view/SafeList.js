Ext.define('Ung.config.email.view.SafeList', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-email-safelist',
    itemId: 'safe_list',
    helpSource: 'email_safe_list',
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

        bind: '{globalSL}',

        tbar: ['@addInline', '->', '@import', '@export'],
        recordActions: ['delete'],
        // listProperty: '',
        emptyRow: {
            emailAddress: 'email@' + rpc.hostname + '.com'
        },

        columns: [{
            header: 'Email Address'.t(),
            dataIndex: 'emailAddress',
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                bind: '{record.emailAddress}',
                emptyText: '[enter email]'.t(),
                vtype: 'email'
            }
        }]
    }, {
        xtype: 'grid',
        reference: 'userSafeListGrid',
        region: 'east',

        width: '50%',
        split: true,

        title: 'Per User Safe Lists'.t(),

        viewConfig: {
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-exclamation-triangle fa-2x"></i> <br/>No Data!</p>',
        },
        selModel: {
            selType: 'checkboxmodel'
        },

        bind: '{userSL}',

        tbar: [{
            text: 'Purge Selected'.t(),
            iconCls: 'fa fa-circle fa-red',
            handler: 'purgeUserSafeList',
            disabled: true,
            bind: {
                disabled: '{!userSafeListGrid.selection}'
            }
        }],

        columns: [{
            header: 'Account Address'.t(),
            dataIndex: 'emailAddress',
            flex: 1
        }, {
            header: 'Safe List Size'.t(),
            width: 150,
            dataIndex: 'count',
            align: 'right'
        }, {
            // todo: the show detail when available data
            header: 'Show Detail'.t()
        }],
    }]

});
