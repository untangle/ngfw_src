Ext.define ('Ung.model.Report', {
    extend: 'Ext.data.Model' ,
    fields: [
        'category', 'colors', 'conditions', 'defaultColumns', 'description',
        'displayOrder', 'enabled',
        'javaClass',
        'orderByColumn',
        'orderDesc',

        'pieGroupColumn',
        'pieNumSlices',
        'pieStyle',
        'pieSumColumn',

        'readOnly',
        'seriesRenderer',
        'table',
        'textColumns',
        'textString',

        'timeDataColumns',
        'timeDataDynamicAggregationFunction',
        'timeDataDynamicAllowNull',
        'timeDataDynamicColumn',
        'timeDataDynamicLimit',
        'timeDataDynamicValue',
        'timeDataInterval',
        'timeStyle',
        'title',
        'type',
        'uniqueId',
        'units',


        {
            name: 'icon',
            calculate: function (entry) {
                var icon;
                switch (entry.type) {
                case 'TEXT':
                    icon = 'fa-align-left';
                    break;
                case 'EVENT_LIST':
                    icon = 'fa-list-ul';
                    break;
                case 'PIE_GRAPH':
                    icon = 'fa-pie-chart';
                    if (entry.pieStyle === 'COLUMN' || entry.pieStyle === 'COLUMN_3D') {
                        icon = 'fa-bar-chart';
                    } else {
                        if (entry.pieStyle === 'DONUT' || entry.pieStyle === 'DONUT_3D') {
                            icon = 'fa-pie-chart';
                        }
                    }
                    break;
                case 'TIME_GRAPH':
                case 'TIME_GRAPH_DYNAMIC':
                    icon = 'fa-line-chart';
                    if (entry.timeStyle.indexOf('BAR') >= 0) {
                        icon = 'fa-bar-chart';
                    } else {
                        if (entry.timeStyle.indexOf('AREA') >= 0) {
                            icon = 'fa-area-chart';
                        }
                    }
                    break;
                default:
                    icon = 'fa-align-left';
                }
                return icon;
            }
        }
    ],
    proxy: {
        type: 'memory',
        reader: {
            type: 'json'
            //rootProperty: 'list'
        }
    }
});
