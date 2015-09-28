Ext.define("Ung.GridEventLogReports", {
    extend: "Ung.GridEventLogBase",
    reportsDate: null,
    selectedApplication: null,
    sectionName: null,
    drilldownType: null,
    drilldownValue: null,
    numDays: null,
    eventQuery: null,
    hasTimestampFilter: false,
    hasAutoRefresh: false,
    hasSelectors: false,
    exportHandler: function() {
        Ext.MessageBox.wait(i18n._("Exporting Events..."), i18n._("Please wait"));
        var downloadForm = document.getElementById('downloadForm');
        downloadForm["type"].value="reportsEventLogExport";
        downloadForm["app"].value=this.selectedApplication;
        downloadForm["section"].value=this.sectionName;
        downloadForm["numDays"].value=this.numDays;
        downloadForm["date"].value=this.reportsDate.time;
        downloadForm["type"].value= this.drilldownType;
        downloadForm["value"].value= this.drilldownValue;
        downloadForm["colList"].value=this.getColumnList();
        downloadForm.submit();
        Ext.MessageBox.hide();
    },
    getColumnList: function() {
        var columnList = "";
        for (var i=0; i<this.columns.length ; i++) {
            if (!this.columns[i].hidden) {
                if (i !== 0) {
                    columnList += ",";
                }
                columnList += this.columns[i].dataIndex;
            }
        }
        return columnList;
    },
    refreshHandler: function (forceFlush) {
        this.refreshList();
    },
    refreshList: function() {
        this.setLoading(i18n._('Querying Database...'));
        rpc.reportsManager.getDetailDataResultSet(Ext.bind(this.refreshCallback, this), this.reportsDate, this.numDays,
            this.selectedApplication, this.sectionName, this.drilldownType, this.drilldownValue);
    }
});