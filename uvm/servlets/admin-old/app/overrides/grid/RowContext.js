Ext.define('Ung.overrides.grid.RowContext', {
    override: 'Ext.grid.RowContext',

    free: function () {
        var viewModel = this.viewModel;
        if (viewModel && viewModel.destroyed) {
            this.viewModel = null;
        }

        return this.callParent(arguments);
    }
});
