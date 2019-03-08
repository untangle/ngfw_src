Ext.define ('Ung.model.Report', {
    extend: 'Ext.data.Model' ,
    fields: [
        { name: 'approximation', type: 'string', defaultValue: '' },
        { name: 'category', type: 'string', defaultValue: '' },
        { name: 'colors', type: 'auto', defaultValue: null },
        { name: 'conditions', type: 'auto', defaultValue: null },
        { name: 'defaultColumns' }, //???
        { name: 'description', type: 'string', defaultValue: '' },
        { name: 'displayOrder', type: 'int' },
        { name: 'enabled', type: 'boolean', defaultValue: true },
        { name: 'javaClass', type: 'string', defaultValue: 'com.untangle.app.reports.ReportEntry' },
        { name: 'orderByColumn', type: 'string', defaultValue: null },
        { name: 'orderDesc', type: 'boolean', defaultValue: false },
        { name: 'pieGroupColumn', type: 'string', defaultValue: null },
        { name: 'pieNumSlices', type: 'int', defaultValue: 10 },
        { name: 'pieStyle' },
        { name: 'pieSumColumn', type: 'string', defaultValue: null },
        { name: 'readOnly', type: 'boolean', defaultValue: false },
        { name: 'seriesRenderer', type: 'string', defaultValue: '' },
        { name: 'table', type: 'string', defaultValue: null },
        { name: 'textColumns', type: 'auto', defaultValue: null },
        { name: 'textString', type: 'string', defaultValue: '' },
        { name: 'timeDataColumns', type: 'auto', defaultValue: null },
        { name: 'timeDataDynamicAggregationFunction', type: 'string', defaultValue: '' },
        { name: 'timeDataDynamicAllowNull', type: 'boolean', defaultValue: false },
        { name: 'timeDataDynamicColumn', type: 'string', defaultValue: '' },
        { name: 'timeDataDynamicLimit', type: 'int', defaultValue: 0 },
        { name: 'timeDataDynamicValue', type: 'string', defaultValue: '' },
        { name: 'timeDataInterval' },
        { name: 'timeStyle' },
        { name: 'title', type: 'string', defaultValue: '' },
        { name: 'type', type: 'string', defaultValue: 'TEXT' }, // TEXT, PIE_GRAPH, TIME_GRAPH, TIME_GRAPH_DYNAMIC, EVENT_LIST
        { name: 'uniqueId', type: 'string', defaultValue: null },
        { name: 'units', type: 'string', defaultValue: null },

        // computed fields
        {
            name: 'localizedTitle',
            calculate: function (entry) {
                return entry.readOnly ? entry.title.t() : entry.title;
            }
        },
        {
            name: 'localizedDescription',
            calculate: function (entry) {
                return entry.readOnly ? entry.description.t() : entry.description;
            }
        },
        {
            name: 'slug',
            calculate: function (entry) {
                var slug = '';
                if (entry.title) {
                    slug = Util.urlEncode(entry.title);
                }
                return slug;
            }
        },
        {
            name: 'categorySlug',
            calculate: function (entry) {
                return Util.urlEncode(entry.category);
            }
        },
        {
            name: 'url',
            calculate: function (entry) {
                return 'cat=' + entry.categorySlug + '&rep=' + entry.slug;
            }
        },
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
                    if (!entry.timeStyle) { return icon; }
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
