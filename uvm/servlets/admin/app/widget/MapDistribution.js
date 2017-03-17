Ext.define('Ung.widget.MapDistribution', {
    extend: 'Ext.container.Container',
    alias: 'widget.mapdistributionwidget',

    controller: 'widget',

    hidden: true,
    border: false,
    baseCls: 'widget',
    cls: 'adding',

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    bind: {
        hidden: '{!widget.enabled}'
    },

    refreshIntervalSec: 5,

    items: [{
        xtype: 'container',
        layout: {
            type: 'hbox',
            align: 'top'
        },
        cls: 'header',
        style: {
            height: '50px'
        },
        items: [{
            xtype: 'component',
            flex: 1,
            html: '<h1>' + 'Map Distribution'.t() + '</h1>'
        }, {
            xtype: 'container',
            margin: '10 5 0 0',
            layout: {
                type: 'hbox',
                align: 'middle'
            },
            items: [{
                xtype: 'button',
                baseCls: 'action',
                text: '<i class="material-icons">refresh</i>',
                listeners: {
                    click: 'fetchData'
                }
            }]
        }]
    }, {
        xtype: 'container',
        html: 'Under construction'
    }],

    fetchData: function () {
        var me = this;
        me.fireEvent('afterdata');
    }
});
