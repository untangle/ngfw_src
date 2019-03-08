Ext.define('Ung.cmp.GridColumns', {
    singleton: true,
    alternateClassName: 'Column',

    ruleId: {
        header: 'Rule Id'.t(),
        width: Renderer.idWidth,
        align: 'right',
        resizable: false,
        dataIndex: 'ruleId',
        renderer: function(value) {
            return value < 0 ? 'new'.t() : value;
        }
    },

    enabled: {
        xtype: 'checkcolumn',
        header: 'Enable'.t(),
        dataIndex: 'enabled',
        resizable: false,
        width: Renderer.booleanWidth
    },

    flagged: {
        xtype: 'checkcolumn',
        header: 'Flagged'.t(),
        dataIndex: 'flagged',
        resizable: false,
        width: Renderer.booleanWidth + 10
    },

    blocked: {
        xtype: 'checkcolumn',
        header: 'Blocked'.t(),
        dataIndex: 'blocked',
        resizable: false,
        width: Renderer.booleanWidth + 10
    },

    description: {
        header: 'Description',
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'description',
        renderer: function (value) {
            return value || '<em>no description<em>';
        }
    },

    conditions: {
        header: 'Conditions'.t(),
        width: Renderer.conditionsWidth,
        flex: 2,
        dataIndex: 'conditions',
        renderer: function(){
            return Ung.cmp.ConditionsEditor.renderer.apply(this, arguments);
        }
    },

    reorder: {
        xtype: 'gridcolumn',
        header: '<i class="fa fa-sort"></i>',
        align: 'center',
        width: Renderer.iconWidth,
        resizable: false,
        tdCls: 'action-cell',
        menuDisabled: true,
        hideable: false,
        renderer: function() {
            return '<i class="fa fa-arrows" style="cursor: move;"></i>';
        }
    }
});

