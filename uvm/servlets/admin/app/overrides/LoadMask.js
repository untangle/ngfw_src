Ext.define('Ung.overrides.LoadMask', {
    override: 'Ext.LoadMask',

    msg: '',

    renderTpl: [
        '<div id="{id}-msgWrapEl" data-ref="msgWrapEl" class="{[values.$comp.msgWrapCls]}" role="presentation">',
        '<div id="{id}-msgEl" data-ref="msgEl" class="{[values.$comp.msgCls]} ',
        Ext.baseCSSPrefix, 'mask-msg-inner {childElCls}" role="presentation">',
        '<i class="fa fa-spinner fa-spin fa-2x fa-fw"></i>',
        '<div id="{id}-msgTextEl" data-ref="msgTextEl" class="',
        Ext.baseCSSPrefix, 'mask-msg-text',
        '{childElCls}" role="presentation">{msg}</div>',
        '</div>',
        '</div>'
    ],


});
