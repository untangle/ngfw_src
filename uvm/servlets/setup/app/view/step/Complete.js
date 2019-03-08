Ext.define('Ung.Setup.Complete', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.Complete',

    title: 'Finish'.t(),
    description: '',

    layout: 'center',
    items: [{
        xtype: 'container',
        layout: {
            type: 'vbox',
            align: 'middle'
        },
        items: [{
            xtype: 'component',
            style: { textAlign: 'center' },
            html: '<h1 style="margin: 0;">' + Ext.String.format('The {0} Server is now configured.', rpc.oemName) + '</h1><br/><br/>You are now ready to configure the applications.'.t()
        }, {
            xtype: 'button',
            margin: '30 0 0 0',
            scale: 'medium',
            text: 'Go to Dashboard'.t(),
            iconCls: 'fa fa-check',
            handler: function () {
                Ext.MessageBox.wait('Loading User Interface...'.t(), 'Please Wait'.t());
                //and set a flag so the wizard wont run again
                rpc.jsonrpc.UvmContext.wizardComplete(function (result, ex) {
                    if (ex) { Util.handleException(ex); return; }
                    window.location.href = '/admin/index.do';
                });
            }
        }]
    }]
});
