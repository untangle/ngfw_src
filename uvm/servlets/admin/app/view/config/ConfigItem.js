Ext.define('Ung.view.config.ConfigItem', {
    extend: 'Ext.Button',
    xtype: 'ung.configitem',
    baseCls: 'app-item',

    viewModel: true,

    hrefTarget: '_self',

    renderTpl:
            '<span class="app-icon"><img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/config/{icon}" width=80 height=80/></span>' +
            '<span class="app-name">{name}</span>',

    initRenderData: function() {
        var data = this.callParent();
        Ext.apply(data, {
            name: this.name,
            icon: this.icon
        });
        return data;
    }
});