Ext.define('Ung.cmp.AppMemory', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.appmemory',

    title: 'Memory Usage'.t(),
    border: false,
    controller: 'appmemory',
    viewModel: true,

    disabled: true,
    bind: {
        disabled: '{!state.on}',
    },

    layout: 'fit',
    items: [{
        xtype: 'component',
        reference: 'appchart',
        width: '100%',
        height: '100%'
    }]
});
