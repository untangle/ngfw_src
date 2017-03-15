Ext.define('Ung.view.reports.ReportsModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.reports',

    data: {
        isNodeReporting: false,
        activeCard: 'allCategoriesCard', // allCategoriesCard, categoryCard, reportCard
        category: null,
        report: null,
        entry: null,
        categoriesData: null,
        startDateTime: null,
        endDateTime: null
    },

    formulas: {
        isCategorySelected: function (get) {
            return get('activeCard') !== 'allCategoriesCard';
        },

        areCategoriesHidden: function (get) {
            return !get('isCategorySelected') || get('isNodeReporting');
        },

        reportHeading: function (get) {
            if (get('report.readOnly')) {
                return '<h2>' + get('report.title').t() + '</h2><p>' + get('report.description').t() + '</p>';
            }
            return '<h2>' + get('report.title') + '</h2><p>' + get('report.description') + '</p>';
        },

        isTimeGraph: function (get) {
            if (!get('report.type')) {
                return false;
            }
            return get('report.type').indexOf('TIME_GRAPH') >= 0;
        },
        isPieGraph: function (get) {
            if (!get('report.type')) {
                return false;
            }
            return get('report.type').indexOf('PIE_GRAPH') >= 0;
        },

        startDate: function (get) {
            if (!get('startDateTime')) {
                return 'One day ago'.t();
            }
            return Ext.Date.format(get('startDateTime'), 'M j, h:i a');
        },
        endDate: function (get) {
            if (!get('endDateTime')) {
                return 'Present'.t();
            }
            return Ext.Date.format(get('endDateTime'), 'M j, h:i a');
        },
        startTimeMax: function (get) {
            var now = new Date(),
                ref = new Date(get('startDateTime'));
            if (now.getYear() === ref.getYear() && now.getMonth() === ref.getMonth() && now.getDate() === ref.getDate()) {
                return now;
            }
        },

        customizeTitle: function (get) {
            if (get('report.readOnly')) {
                return 'Customize'.t() + ' <span style="font-weight: 300; color: #777;">(' + 'Readonly report! Changes can be saved as a new custom report!' + ')</span>';
            }
            return 'Customize'.t();
        },

        isWidget: function (get) {
            return Ext.getStore('widgets').findRecord('entryId', get('report.uniqueId')) ? true : false;
        }
    },

    stores: {
        categories: {
            model: 'Ung.model.Category',
            data: '{categoriesData}'
        }
        /*
        tables: {
            data: '{tablesData}'
        }
        */
    }
});
