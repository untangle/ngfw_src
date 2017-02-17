Ext.define('Ung.config.system.System', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.config.system',

    requires: [
        'Ung.config.system.SystemController',
        // 'Ung.overrides.form.CheckboxGroup'
    ],

    viewModel: {
        data: {
            time: null
        }
    },

    controller: 'config.system',

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
            html: '<strong>' + 'System'.t() + '</strong>'
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

    items: [{
        xtype: 'config.system.regional'
    }, {
        xtype: 'config.system.support'
    }, {
        xtype: 'config.system.backup'
    }, {
        xtype: 'config.system.restore'
    }, {
        xtype: 'config.system.protocols'
    }, {
        xtype: 'config.system.shield'
    }]

});