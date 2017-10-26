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
        width: Renderer.booleanWidth
    },

    blocked: {
        xtype: 'checkcolumn',
        header: 'Blocked'.t(),
        dataIndex: 'blocked',
        resizable: false,
        width: Renderer.booleanWidth
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
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'conditions',
        renderer: 'conditionsRenderer'
    },

    bypass: {
        header: 'Bypass'.t(),
        xtype: 'checkcolumn',
        dataIndex: 'bypass',
        width: Renderer.booleanWidth
    }
});

