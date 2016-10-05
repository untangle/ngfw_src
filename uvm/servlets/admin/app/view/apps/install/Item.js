Ext.define('Ung.view.apps.install.Item', {
    extend: 'Ext.Button',
    xtype: 'ung.appinstallitem',

    baseCls: 'app-item install',

    hrefTarget: '_self',

    renderTpl: [
        '<span id="{id}-btnWrap" data-ref="btnWrap" role="presentation" unselectable="on" style="{btnWrapStyle}" ' +
                'class="{btnWrapCls} {btnWrapCls}-{ui} {splitCls}{childElCls}">' +
                '<span class="app-icon"><img src="/skins/modern-rack/images/admin/apps/{node.name}_80x80.png" width=80 height=80/>' +
                '<span class="app-name">{node.displayName}</span>' +
                '</span>' +
                '<div class="app-install"><i class="material-icons">get_app</i></div>' +
                '<div class="spinner"><div class="bounce1"></div><div class="bounce2"></div><div class="bounce3"></div></div>' +
                '<div class="app-done"><i class="material-icons">check</i></div>' +
        '</span>'
    ],

    initRenderData: function() {
        var data = this.callParent();
        data.node = this.node;
        return data;
    },
    listeners: {
        click: 'installNode'
    }
});
