Ext.define('Ung.cmp.RulesController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.rules',

    control: {
        '#': {
            beforerender: 'onBeforeRender'
        }
    },

    onBeforeRender: function (view) {
        // create conditions map
        view.setConditionsMap(Ext.Array.toValueMap(view.getConditions(), 'name'));
        console.log(view.getConditionsMap());
    },

    conditionsRenderer: function (value, metaData, record) {
        var view = this.getView(),
            conds = record.get('conditions').list,
            resp = '', i, cond;
        for (i = 0; i < conds.length; i += 1) {
            resp += view.getConditionsMap()[conds[i].conditionType].displayName + '<strong>' + (conds[i].invert ? ' &ne; ' : ' = ') + conds[i].value + '</strong><br/>';
        }
        return resp;
    },

    conditionRenderer: function (val) {
        return this.getView().conditionsMap[val].displayName;
    },

    editRuleWin: function (view, rowIndex, colIndex, item, e, record) {
        var me = this;
        Ext.widget('ung.cmp.ruleeditor', {
            conditions: me.getView().getConditions(),
            conditionsMap: me.getView().getConditionsMap(),
            viewModel: {
                data: {
                    rule: record
                },
                formulas: {
                    conditionsData: {
                        bind: '{rule.conditions.list}',
                        get: function (coll) {
                            return coll || [];
                        }
                    },
                },
            }
        });
    }

});