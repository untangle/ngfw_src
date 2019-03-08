Ext.define('Ung.config.network.NetworkTest', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.networktest',
    alias: 'widget.networktest',

    layout: 'fit',

    dockedItems: [{
        xtype: 'toolbar',
        layout: 'fit',
        items: [{
            xtype: 'container',
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [{
                xtype: 'component',
                padding: '10 10 0 10',
                bind: {
                    html: '{description}'
                }
            }, {
                xtype: 'form',
                border: false,
                layout: {
                    type: 'hbox'
                },
                padding: 10,
                bodyStyle: {
                    background: 'transparent'
                },
                items: [{
                    xtype: 'button',
                    text: 'Run Test'.t(),
                    iconCls: 'fa fa-play',
                    margin: '0 0 0 10',
                    handler: 'runTest',
                    formBind: true
                }, {
                    xtype: 'component',
                    flex: 1
                }, {
                    xtype: 'button',
                    text: 'Clear Output'.t(),
                    iconCls: 'fa fa-eraser',
                    margin: '0 0 0 10',
                    handler: 'clearOutput'
                },{
                    xtype: 'button',
                    text: 'Export'.t(),
                    iconCls: 'fa fa-eraser',
                    margin: '0 0 0 10',
                    handler: 'exportOutput',
                    hidden: true,
                    disabled: true,
                    bind:{
                        hidden: '{exportAction != true}',
                        disabled: '{exportFilename == ""}'
                    }
                }]
            }]
        }]
    }],

    items: [{
        xtype: 'textarea',
        border: false,
        bind: {
            emptyText: '{emptyText}'
        },
        fieldStyle: {
            fontFamily: 'Courier, monospace',
            fontSize: '14px',
            background: '#1b1e26',
            color: 'lime'
        },
        // margin: 10,
        readOnly: true
    }]
});
