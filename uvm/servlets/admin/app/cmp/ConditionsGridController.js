Ext.define('Ung.cmp.ConditionsGridController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.conditionsgrid',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            // beforerender: 'onBeforeRender',
            // close: 'onClose',
        },
        '#addConditionBtn': {
            // click: 'addCondition'
        }
    },

    onAfterRender: function (view) {

        var menuConditions = [], i;

        console.log(view.getViewModel().get('ruleJavaClass'));

        view.getViewModel().bind({
            bindTo: '{record}',
        }, this.setMenuConditions);

        for (i = 0; i < view.conditions.length; i += 1) {
            menuConditions.push({
                text: view.conditions[i].displayName,
                conditionType: view.conditions[i].name,
            });
        }

        view.down('#addConditionBtn').setMenu({
            showSeparator: false,
            plain: true,
            items: menuConditions,
            mouseLeaveDelay: 0,
            listeners: {
                click: 'addCondition'
            }
        });

        // console.log(view.down('grid').getStore());

        // view.down('grid').getStore().on({
        //     update: function () {
        //         console.log('update');
        //     },
        //     scope: this
        // });
    },

    setMenuConditions: function (conds) {
        var menu = this.getView().down('#addConditionBtn').getMenu(),
            store = this.getView().getStore();
        menu.items.each(function (item) {
            item.setDisabled(store.findRecord('conditionType', item.conditionType) ? true : false);
        });
    },

    onWidgetAttach: function (column, container, record) {
        // if widget aklready attached do nothing
        if (container.items.length >= 1) {
            return;
        }
        // console.log('widget attach');
        // container.removeAll(true);

        var condition = this.getView().conditionsMap[record.get('conditionType')], i, ckItems = [];

        switch (condition.type) {
        case 'boolean':
            container.add({
                xtype: 'component',
                padding: 3,
                html: 'True'.t()
            });
            break;
        case 'textfield':
            container.add({
                xtype: 'textfield',
                bind: {
                    value: '{record.value}'
                },
                vtype: condition.vtype
            });
            break;
        case 'checkboxgroup':
            // console.log(condition.values);
            // var values_arr = (cond.value !== null && cond.value.length > 0) ? cond.value.split(',') : [], i, ckItems = [];
            for (i = 0; i < condition.values.length; i += 1) {
                ckItems.push({
                    inputValue: condition.values[i][0],
                    boxLabel: condition.values[i][1]
                });
            }
            container.add({
                xtype: 'checkboxgroup',
                bind: {
                    value: '{record.value}'
                },
                columns: 3,
                vertical: true,
                defaults: {
                    padding: '0 10 0 0'
                },
                items: ckItems
            });
        }
    },

    addCondition: function (menu, item) {
        // console.log('add');
        item.setDisabled(true);
        var newCond = {
            conditionType: item.conditionType,
            invert: false,
            javaClass: this.getViewModel().get('ruleJavaClass'),
            value: ''
        };
        this.getView().getStore().add(newCond);
        // this.getView().ruleConditions.push(newCond);
        // this.addRowView(newCond);
    },

    onDataChanged: function () {
        console.log('datachanged');
    },

    conditionRenderer: function (val) {
        return this.getView().conditionsMap[val].displayName;
        // return [val].displayName;
    },

    removeCondition: function (view, rowIndex, colIndex, item, e, record) {
        this.getView().getStore().remove(record);
        this.setMenuConditions();
    }

});
