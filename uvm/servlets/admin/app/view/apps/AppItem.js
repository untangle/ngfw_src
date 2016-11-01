Ext.define('Ung.view.apps.AppItem', {
    extend: 'Ext.Button',
    xtype: 'ung.appitem',
    baseCls: 'app-item',

    viewModel: true,

    hrefTarget: '_self',

    renderTpl: [
        '<span id="{id}-btnWrap" data-ref="btnWrap" role="presentation" unselectable="on" style="{btnWrapStyle}" ' +
                '<span class="app-icon"><img src="' + resourcesBaseHref + '/skins/modern-rack/images/admin/apps/{node.name}_80x80.png" width=80 height=80/>' +
                '<span class="app-name">{node.displayName}</span>' +
                '</span>' +
            '<span class="app-state {state}"><i class="material-icons">power_settings_new</i></span>' +
        '</span>'
    ],

    initRenderData: function() {
        var data = this.callParent();
        Ext.apply(data, {
            node: this.node,
            state: this.state
        });
        return data;
    },

    listeners: {
        // afterrender: 'onItemAfterRender'
    }
});
