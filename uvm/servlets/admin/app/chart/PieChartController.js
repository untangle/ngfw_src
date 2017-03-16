Ext.define('Ung.chart.PieChartController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.piechart',

    onAfterRender: function (view) {
        var me = this, vm = this.getViewModel();

        // fetch data first
        vm.bind('{entry}', me.fetchData, me);
        vm.bind('{entry.pieNumSlices}', me.setSeries, me);
        vm.bind('{entry.pieStyle}', me.setStyles, me);
        vm.bind('{entry.colors}', me.setStyles, me);
        vm.bind('{entry.approximation}', me.setStyles, me);
    },


    /**
     * fetches report data via rpc
     */
    fetchData: function () {
        var me = this, vm = this.getViewModel();
        if (!vm.get('entry')) { return; }
        me.getView().setLoading(true);
        Rpc.asyncData('rpc.reportsManager.getDataForReportEntry',
                        vm.get('entry').getData(),
                        vm.get('startDate'),
                        vm.get('tillNow') ? null : vm.get('endDate'), -1)
            .then(function(result) {
                me.getView().setLoading(false);
                me.data = result.list;
                me.setSeries();

                if (me.getView().up('reports-entry')) {
                    me.getView().up('reports-entry').getController().setCurrentData(result.list);
                }
            });
    },

    /**
     * creates (normalize) series definition based on entry and data
     */
    setSeries: function () {
        if (!this.data) { return; }
        // this.chart = null;

        var me = this,
            vm = this.getViewModel(),
            serie = {
                name: vm.get('entry.units').t()
            },
            _slicesData = [],
            _othersValue = 0,
            i;

        for (i = 0; i < me.data.length; i += 1) {
            if (i < vm.get('entry.pieNumSlices')) {
                _slicesData.push({
                    name: me.data[i][vm.get('entry.pieGroupColumn')] !== undefined ? me.data[i][vm.get('entry.pieGroupColumn')] : 'None',
                    y: me.data[i].value
                });
            } else {
                _othersValue += me.data[i].value;
            }
        }

        if (_othersValue > 0) {
            _slicesData.push({
                name: 'Other',
                color: '#DDD',
                y: _othersValue
            });
        }

        serie.data = _slicesData;

        // create or update the chart
        if (!this.chart) {
            this.buildChart(serie);
        } else {
            while(this.chart.series.length > 0) {
                this.chart.series[0].remove(true);
            }
            this.chart.addSeries(serie, true, true);
        }
    },

    /**
     * updates the styles of the chart based on report entry conditions
     */
    setStyles: function () {
        if (!this.chart) { return ;}
        var entry = this.getViewModel().get('entry'),
            isColumn = entry.get('pieStyle').indexOf('COLUMN') >= 0,
            isDonut = entry.get('pieStyle').indexOf('DONUT') >= 0;

        this.chart.update({
            chart: {
                type: isColumn ? 'column' : 'pie',
                margin: !isColumn ? [15, 25, 25, 25] : undefined,
                options3d: {
                    enabled: entry.get('pieStyle').indexOf('3D') > 0,
                    alpha: isColumn ? 30 : 50,
                    beta: isColumn ? 5 : 0
                },
            },
            colors: entry.get('colors') || Util.defaultColors,
            plotOptions: {
                pie: {
                    innerSize: isDonut ? '40%' : 0
                }
            },
            xAxis: {
                visible: isColumn
            },
            yAxis: {
                visible: isColumn
            },
            legend: {
                enabled: !isColumn
            }
        });
    },

    buildChart: function (serie) {
        var me = this, entry = me.getViewModel().get('entry');

        me.chart = new Highcharts.Chart({
            chart: {
                type: entry.get('pieStyle').indexOf('COLUMN') >= 0 ? 'column' : 'pie',
                renderTo: me.getView().lookupReference('piechart').getEl().dom,
                animation: false,
                backgroundColor: 'transparent',
                style: {
                    fontFamily: 'Source Sans Pro', // default font
                    fontSize: '12px'
                }
            },
            title: null,
            lang: {
                noData: '<p style="text-align: center;"><i class="fa fa-info-circle fa-2x"></i><br/>' + 'No data!' + '<p>'
            },
            noData: {
                style: {
                    padding: 0,
                    fontSize: '12px',
                    fontWeight: 'normal',
                    color: '#999'
                },
                useHTML: true
            },
            // colors: (entry.get('colors') !== null && entry.get('colors') > 0) ? entry.get('colors') : this.defaultColors,
            credits: {
                enabled: false
            },
            navigation: {
                buttonOptions: {
                    enabled: false
                }
            },
            xAxis: {
                type: 'category',
                crosshair: true,
                alternateGridColor: 'rgba(220, 220, 220, 0.1)',
                labels: {
                    style: {
                        fontSize: '11px',
                        color: '#333',
                        fontWeight: 600
                    }
                },
                lineColor: '#C0D0E0',
                lineWidth: 1,
                tickLength: 3,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                maxPadding: 0,
                minPadding: 0
            },
            yAxis: {
                lineColor: '#C0D0E0',
                lineWidth: 1,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                showFirstLabel: false,
                showLastLabel: true,
                title: {
                    text: entry.get('units').t()
                },
                endOnTick: true
            },
            tooltip: {
                headerFormat: '<span style="font-size: 14px; font-weight: bold;">{point.key}</span><br/>',
                hideDelay: 0
                //pointFormat: '{series.name}: <b>{point.y}</b>' + (entry.pieStyle.indexOf('COLUMN') < 0 ? ' ({point.percentage:.1f}%)' : '')
            },
            plotOptions: {
                pie: {
                    allowPointSelect: true,
                    cursor: 'pointer',
                    center: ['50%', '50%'],
                    showInLegend: true,
                    colorByPoint: true,

                    depth: 35,

                    minSize: 150,
                    borderWidth: 1,
                    borderColor: '#FFF',
                    dataLabels: {
                        enabled: true,
                        distance: 5,
                        padding: 0,
                        reserveSpace: false,
                        style: {
                            fontSize: '11px',
                            color: '#333',
                            // fontFamily: 'Source Sans Pro',
                            fontWeight: 600
                        },
                        formatter: function () {
                            if (this.point.percentage < 2) {
                                return null;
                            }
                            if (this.point.name.length > 25) {
                                return this.point.name.substring(0, 25) + '...';
                            }
                            return this.point.y + ' (' + this.point.percentage.toFixed(2) + '%)';
                        }
                    }
                },
                column: {
                    borderWidth: 0,
                    colorByPoint: true,
                    depth: 25,
                    shadow: true,
                    dataLabels: {
                        enabled: false,
                        align: 'center'
                    }
                },
                series: {
                    animation: false
                }
            },
            legend: {
                title: {
                    text: 'Some title ... tbd'
                },
                style: {
                    overflow: 'hidden'
                },
                itemStyle: {
                    fontSize: '11px',
                    fontWeight: 600,
                    width: '120px',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                },
                useHTML: true,
                layout: 'vertical',
                align: 'left',
                verticalAlign: 'top',
                symbolHeight: 8,
                symbolWidth: 8,
                symbolRadius: 4
            },
            series: [serie]
        });

        // apply the chart styles after creation
        me.setStyles();
    },

    onResize: function () {
        if (this.chart) {
            this.chart.reflow();
        }
    }
});
