Ext.define('Ung.view.reports.EntryModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.reports-entry',


    data: {
        startDate: new Date((Math.floor(rpc.systemManager.getMilliseconds()/1800000) * 1800000  - 3600 * 24 * 1000) + (new Date().getTimezoneOffset() * 60000) + rpc.timeZoneOffset),
        endDate: new Date((Math.floor(rpc.systemManager.getMilliseconds()/1800000) * 1800000) + (new Date().getTimezoneOffset() * 60000) + rpc.timeZoneOffset),
        tillNow: true,
        _currentData: [],
        sqlFilterData: [],
        autoRefresh: false,
        // fetching: false // set in Reports ViewModel
    },

    // stores: {
    //     _sqlConditionsStore: {
    //         data: '{_sqlConditions}',
    //         proxy: {
    //             type: 'memory',
    //             reader: {
    //                 type: 'json'
    //             }
    //         }
    //     }
    // },

    formulas: {
        _reportCard: function (get) {
            if (get('entry.type') === 'TEXT') { return 'textreport'; }
            if (get('entry.type') === 'EVENT_LIST') { return 'eventreport'; }
            return 'graphreport';
        },


        _approximation: {
            get: function (get) {
                return get('entry.approximation') || 'sum';
            },
            set: function (value) {
                this.set('entry.approximation', value !== 'sum' ? value : null);
            }
        },

        _sqlConditions: {
            get: function (get) {
               return get('entry.conditions') || [];
            },
            set: function (value) {
                this.set('entry.conditions', value);
                this.set('_sqlTitle', '<i class="fa fa-filter"></i> ' + 'Sql Conditions:'.t() + ' (' + value.length + ')');
               // return get('entry.conditions') || [];
            },
        },

        _props: function (get) {
            return get('entry').getData();
        },

        _colorsStr: {
            get: function (get) {
                if (get('entry.colors')) {
                    return get('entry.colors').join(',');
                } else {
                    return '';
                }
            },
            set: function (value) {
                var str = value.replace(/ /g, '');
                if (value.length > 0) {
                    this.set('entry.colors', value.split(','));
                } else {
                    this.set('entry.colors', null);
                }
            }
        },

        // _colors: {
        //     get: function (get) {
        //         return get('report.colors');
        //     },
        //     set: function (value) {
        //         console.log(this.get('report.colors'));
        //     }
        // },


        _sd: {
            get: function (get) {
                return get('startDate');
            },
            set: function (value) {
                var sd = new Date(this.get('startDate'));
                sd.setDate(value.getDate());
                sd.setMonth(value.getMonth());
                sd.setFullYear(value.getFullYear());
                this.set('startDate', sd);
            }
        },
        _st: {
            get: function (get) {
                return get('startDate');
            },
            set: function (value) {
                var sd = new Date(this.get('startDate'));
                sd.setHours(value.getHours());
                sd.setMinutes(value.getMinutes());
                this.set('startDate', sd);
            }
        },
        _ed: {
            get: function (get) {
                return get('endDate');
            },
            set: function (value) {
                var ed = new Date(this.get('endDate'));
                ed.setDate(value.getDate());
                ed.setMonth(value.getMonth());
                ed.setFullYear(value.getFullYear());
                this.set('endDate', ed);
            }
        },
        _et: {
            get: function (get) {
                return get('endDate');
            },
            set: function (value) {
                var ed = new Date(this.get('endDate'));
                ed.setHours(value.getHours());
                ed.setMinutes(value.getMinutes());
                this.set('endDate', ed);
            }
        },


        reportHeading: function (get) {
            if (get('entry.readOnly')) {
                return '<h2>' + get('entry.title').t() + '</h2><p>' + get('entry.description').t() + '</p>';
            }
            return '<h2>' + get('entry.title') + '</h2><p>' + get('entry.description') + '</p>';
        },
        // enableIcon: function (get) {
        //     return get('entry.enabled') ? 'fa-green' : 'fa-flip-horizontal fa-grey';
        // },

        isTimeGraph: function (get) {
            if (!get('entry.type')) {
                return false;
            }
            return get('entry.type') === 'TIME_GRAPH';
        },
        isTimeGraphDynamic: function (get) {
            if (!get('entry.type')) {
                return false;
            }
            return get('entry.type') === 'TIME_GRAPH_DYNAMIC';
        },
        isPieGraph: function (get) {
            if (!get('entry.type')) {
                return false;
            }
            return get('entry.type') === 'PIE_GRAPH';
        },

        isGraphEntry: function (get) {
            return get('isTimeGraph') || get('isTimeGraphDynamic') || get('isPieGraph');
        },

        isTextEntry: function (get)  {
            return get('entry.type') === 'TEXT';
        }
    }

});
