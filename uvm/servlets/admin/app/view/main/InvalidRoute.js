Ext.define('Ung.view.main.InvalidRoute', {
    extend: 'Ext.container.Container',
    alias: 'widget.invalidroute',
    itemId: 'invalidRoute',
    cls: 'invalid-route',
    layout: {
        type: 'vbox',
        align: 'center'
    },
    padding: 50,

    items: [{
        xtype: 'component',
        style: {
            textAlign: 'center',
            fontSize: '14px'
        },
        html: '<i class="fa fa-warning fa-3x" style="color: #999;"></i>' +
            '<div><h1>Ooops... Error</h1><p>Sorry, the page you are looking for was not found!</p></div>'
    }, {
        xtype: 'button',
        iconCls: 'fa fa-home fa-lg',
        scale: 'medium',
        margin: '20 0 0 0',
        focusable: false,
        text: 'Go to Dashboard'.t(),
        href: '#',
        hrefTarget: '_self'
    }]
});
