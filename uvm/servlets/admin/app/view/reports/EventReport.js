Ext.define('Ung.view.reports.EventReport', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.eventreport',

    viewModel: {
        stores: {
            events: {
                data: '{eventsData}'
            },
            props: {
                data: '{propsData}'
            }
        }
    },
    controller: 'eventreport',

    layout: 'border',

    border: false,
    bodyBorder: false,

    defaults: {
        border: false
    },

    items: [{
        xtype: 'grid',
        reference: 'eventsGrid',
        region: 'center',
        bind: '{events}',
        emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>No Records!</p>',
        listeners: {
            select: 'onEventSelect'
        }
    }, {
        xtype: 'propertygrid',
        itemId: 'properties',
        region: 'east',
        width: 400,
        minWidth: 200,
        split: true,
        nameColumnWidth: 150,
        bind: {
            store: '{props}',
            hidden: '{!eventsGrid.selection}'
        },
        listeners: {
            beforeedit: function () {
                return false;
            }
        }
    }]
});
