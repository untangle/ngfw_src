Ext.define('Ung.cmp.AppLicense', {
    extend: 'Ext.form.FieldSet',
    alias: 'widget.applicense',
    title: '<i class="fa fa-file-text"></i> ' + 'License'.t(),

    padding: 10,
    margin: '20 0',
    cls: 'app-section',

    layout: {
        type: 'hbox',
        align: 'middle'
    },

    viewModel: {
        formulas: {
            licenseMessage: function (get) {
                return Util.getLicenseMessage(get('license'));
            }
        }
    },

    items: [{
        xtype: 'component',
        padding: '3 5',
        bind: {
            html: '<i class="fa fa-exclamation-triangle fa-orange"></i> <strong>' + '{licenseMessage}' + '</strong>'
        }
    }, {
        xtype: 'button',
        html: 'Buy Now'.t(),
        iconCls: 'fa fa-shopping-cart',
        bind: {
            href: Util.getStoreUrl() + '?action=buy&libitem=untangle-libitem-{props.name}&' + Util.getAbout()
        }
    }]
});
