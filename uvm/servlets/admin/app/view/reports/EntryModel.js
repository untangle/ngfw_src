Ext.define('Ung.view.reports.EntryModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.reports-entry',


    data: {
        startDate: new Date(Math.floor(rpc.systemManager.getMilliseconds()/1800000) * 1800000  - 3600 * 24 * 1000),
        endDate: new Date(Math.floor(rpc.systemManager.getMilliseconds()/1800000) * 1800000),
        tillNow: true
    },

    stores: {
        conditions: {
            data: '{_sqlConditions}'
        }
    },

    formulas: {
        _approximation: {
            get: function (get) {
                return get('entry.approximation') || 'sum';
            },
            set: function (value) {
                this.set('entry.approximation', value !== 'sum' ? value : null);
            }
        },


        _sqlConditions: function (get) {
            return get('entry.conditions') || [];
        },

        _props: function (get) {
            return get('entry').getData();
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

        // startTime: {
        //     get: function (get) {
        //         return get('startDate');
        //     },
        //     set: function (value) {
        //         console.log(this.get('startDate'));
        //         var sd = new Date(this.get('startDate'));
        //         sd.setHours(value.getHours());
        //         sd.setMinutes(value.getMinutes());
        //         this.set('startDate', sd);
        //     }
        // },
        // endDate: function (get) {
        //     var ed = new Date(rpc.systemManager.getMilliseconds());
        //     ed.setSeconds(0);
        //     ed.setMilliseconds(0);
        //     return ed;
        // },
        // endTime: {
        //     get: function (get) {
        //         return get('endDate');
        //     },
        //     set: function (value) {
        //         var ed = new Date(this.get('endDate'));
        //         ed.setHours(value.getHours());
        //         ed.setMinutes(value.getMinutes());
        //         this.set('endDate', ed);
        //     }
        // },




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
            return get('entry.type').indexOf('TIME_GRAPH') >= 0;
        },
        isPieGraph: function (get) {
            if (!get('entry.type')) {
                return false;
            }
            return get('entry.type').indexOf('PIE_GRAPH') >= 0;
        },
    }

});
