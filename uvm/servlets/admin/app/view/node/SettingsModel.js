Ext.define('Ung.view.node.SettingsModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.nodesettings',

    data: {
        nodeInstance: null,
        nodeProps: null,
        metrics: null,
        nodeReports: null
    },

    formulas: {
        powerMessage: function (get) {
            if (get('nodeInstance.targetState') === 'RUNNING') {
                return Ext.String.format('{0} is enabled.'.t(), get('nodeProps.displayName'));
            }
            return Ext.String.format('{0} is disabled.'.t(), get('nodeProps.displayName'));
        },
        powerButton: function (get) {
            if (get('nodeInstance.targetState') === 'RUNNING') {
                return Util.iconTitle('Disable', 'power_settings_new-16');
            }
            return Util.iconTitle('Enable', 'power_settings_new-16');
        }
    },

    stores: {
        // holds the metrics for a specific node instance, which are rendered in Node Status
        nodeMetrics: {
            model: 'Ung.model.NodeMetric',
            data: '{metrics}'
        },
        areports: {
            model: 'Ung.model.Report',
            data: '{nodeReports}'
        }
    }
});
