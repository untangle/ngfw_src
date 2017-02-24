Ext.define('Ung.config.administration.Administration', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.config.administration',

    /* requires-start */
    requires: [
        'Ung.config.administration.AdministrationController',
        'Ung.config.administration.AdministrationModel',

        'Ung.store.Rule',
        'Ung.model.Rule',
        'Ung.cmp.Grid'
    ],
    /* requires-end */

    controller: 'config.administration',
    viewModel: {
        type: 'config.administration'
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
            html: '<strong>' + 'Administration'.t() + '</strong>'
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
        { xtype: 'config.administration.admin' },
        { xtype: 'config.administration.certificates' },
        { xtype: 'config.administration.snmp' },
        { xtype: 'config.administration.skins' }
    ]
});
