Ext.define('Ung.config.email.Email', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.config.email',

    requires: [
        'Ung.config.email.EmailController',
        'Ung.config.email.EmailTest',

        'Ung.store.Rule',
        'Ung.model.Rule',
        'Ung.cmp.Grid'
    ],

    controller: 'config.email',

    viewModel: {
        data: {
            globalSafeList: null,
        }
    },

    dockedItems: [{
        xtype: 'toolbar',
        weight: -10,
        border: false,
        items: [{
            text: 'Back',
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            href: '#config'
        }, '-', {
            xtype: 'tbtext',
            html: '<strong>' + 'Email'.t() + '</strong>'
        }],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        border: false,
        items: ['->', {
            text: 'Apply Changes'.t(),
            scale: 'large',
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],

    items: [
        { xtype: 'config.email.outgoingserver' },
        { xtype: 'config.email.safelist' },
        { xtype: 'config.email.quarantine' }
    ]
});
