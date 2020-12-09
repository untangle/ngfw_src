Ext.define('Ung.config.local-directory.view.RadiusLog', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config-local-directory-radius-log',
    itemId: 'radius-log',
    title: 'RADIUS Log',
    scrollable: false,
    viewModel: true,

    bodyPadding: 10,
    layout: 'fit',

    items: [{
        xtype: 'fieldset',
        padding: '10 20',
        itemId: 'radius-log',
        width: '100%',
        height: '100%',
        title: 'RADIUS Server Log'.t(),
        items: [{
            xtype: 'button',
            iconCls: 'fa fa-refresh',
            text: 'Refresh',
            target: 'radiusLogFile',
            handler: 'refreshRadiusLogFile'
        }, {
            xtype: 'textarea',
            itemId: 'radiusLogFile',
            spellcheck: false,
            padding: '5 0 5 0',
            border: true,
            width: '100%',
            height: '95%',
            _neverDirty: true,
            bind: '{radiusLogFile}',
            fieldStyle: {
                'fontFamily'   : 'courier new',
                'fontSize'     : '12px'
            }
        }]
    }]
});
