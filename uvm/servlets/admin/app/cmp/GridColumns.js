Ext.define('Ung.cmp.GridColumns', {
    singleton: true,
    alternateClassName: 'Column',

    ruleId: {
        header: 'Rule Id'.t(),
        width: 70,
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
        width: 70
    },

    flagged: {
        xtype: 'checkcolumn',
        header: 'Flagged'.t(),
        dataIndex: 'flagged',
        resizable: false,
        width: 70
    },

    blocked: {
        xtype: 'checkcolumn',
        header: 'Blocked'.t(),
        dataIndex: 'blocked',
        resizable: false,
        width: 70
    },

    live: {
        xtype: 'checkcolumn',
        header: 'Enabled'.t(),
        dataIndex: 'live',
        resizable: false,
        width: 70
    },

    description: {
        header: 'Description',
        width: 200,
        dataIndex: 'description',
        renderer: function (value) {
            return value || '<em>no description<em>';
        }
    },

    conditions: {
        header: 'Conditions'.t(),
        flex: 1,
        dataIndex: 'conditions',
        renderer: 'conditionsRenderer'
    },

    bypass: {
        header: 'Bypass'.t(),
        xtype: 'checkcolumn',
        dataIndex: 'bypass',
        width: 70
    }
});

