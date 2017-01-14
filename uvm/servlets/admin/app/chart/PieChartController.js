Ext.define('Ung.chart.PieChartController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.piechart',
    styles: {
        'LINE': { styleType: 'spline' },
        'AREA': { styleType: 'areaspline' },
        'AREA_STACKED': {styleType: 'areaspline', stacking: true },
        'BAR': {styleType: 'column', grouping: true },
        'BAR_OVERLAPPED': {styleType: 'column', overlapped: true },
        'BAR_STACKED': {styleType: 'column', stacking: true }
    },
    init: function () {
        this.defaultColors = ['#00b000', '#3030ff', '#009090', '#00ffff', '#707070', '#b000b0', '#fff000', '#b00000', '#ff0000', '#ff6347', '#c0c0c0'];
    },

    onAfterRender: function (view) {
        var me = this;
        //console.log(cmp.viewModel.get('timeStyle'));
        this.entry = view.getViewModel().get('entry') || this.getView().getEntry();

        this.chart =  new Highcharts.Chart({
            chart: {
                type: me.entry.get('pieStyle').indexOf('COLUMN') >= 0 ? 'column' : 'pie',
                renderTo: view.lookupReference('piechart').getEl().dom,
                //margin: (entry.chartType === 'pie' && !forDashboard) ? [80, 20, 50, 20] : undefined,
                marginTop: 10,
                marginRight: 0,
                marginLeft: 0,
                //spacing: [10, 10, 20, 10],
                backgroundColor: 'transparent',
                style: {
                    fontFamily: 'Source Sans Pro', // default font
                    fontSize: '12px'
                },
                options3d: {
                    enabled: false,
                    alpha: 45,
                    beta: 5
                }
            },
            title: null,
            lang: {
                noData: ''
            },
            noData: {
                style: {
                    fontSize: '12px',
                    fontWeight: 'normal',
                    color: '#CCC'
                }
            },
            colors: (me.entry.get('colors') !== null && me.entry.get('colors') > 0) ? me.entry.get('colors') : this.defaultColors,
            credits: {
                enabled: false
            },
            navigation: {
                buttonOptions: {
                    enabled: false
                }
            },
            /*
            exporting: {
                chartOptions: {
                    title: {
                        text: entry.category + ' - ' + entry.title,
                        style: {
                            fontSize: '12px'
                        }
                    }
                },
                type: 'image/jpeg'
            },
            */
            xAxis: {
                type: 'category',
                crosshair: true,
                alternateGridColor: 'rgba(220, 220, 220, 0.1)',
                labels: {
                    style: {
                        fontSize: '11px'
                    }
                },
                lineColor: '#C0D0E0',
                lineWidth: 1,
                tickLength: 3,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                /*
                title: {
                    align: 'middle',
                    text: 'some name',
                    style: {
                        //fontSize: !forDashboard ? '14px' : '12px',
                        fontWeight: 'bold'
                    }
                },
                */
                maxPadding: 0,
                minPadding: 0
            },
            yAxis: {
                lineColor: '#C0D0E0',
                lineWidth: 1,
                gridLineWidth: 1,
                gridLineDashStyle: 'dash',
                gridLineColor: '#EEE',
                tickLength: 5,
                tickWidth: 1,
                tickPosition: 'inside',
                showFirstLabel: false,
                showLastLabel: true,
                endOnTick: true,
                labels: {
                    align: 'left',
                    useHTML: true,
                    padding: 0,
                    style: {
                        fontFamily: 'Source Sans Pro',
                        color: '#555',
                        fontSize: '11px',
                        fontWeight: 600,
                        background: 'rgba(255, 255, 255, 0.6)',
                        padding: '0 1px',
                        borderRadius: '2px',
                        //textShadow: '1px 1px 1px #000'
                        lineHeight: '11px'
                    },
                    x: 9,
                    y: 6,
                    formatter: function() {
                        if (this.isLast) {
                            return '<span style="color: #555; font-size: 12px;"><strong>' + this.value + '</strong> (' + me.entry.get('units') + ')</span>';
                        }
                        return this.value;
                    }
                }
            },
            tooltip: {
                headerFormat: '<span style="font-size: 14px; font-weight: bold;">aaa : {point.key}</span><br/>',
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
                    //depth: 0,
                    minSize: 150,
                    borderWidth: 1,
                    borderColor: '#EEE',
                    dataLabels: {
                        enabled: true,
                        distance: 5,
                        padding: 0,
                        reserveSpace: false,
                        style: {
                            fontSize: '11px',
                            color: '#777',
                            fontFamily: 'Source Sans Pro',
                            fontWeight: 400
                        },
                        formatter: function () {
                            if (this.point.percentage < 3) {
                                return null;
                            }
                            if (this.point.name.length > 25) {
                                return this.point.name.substring(0, 25) + '...';
                            }
                            return this.point.name;
                        }
                        //color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || '#555'
                    }
                },
                column: {
                    borderWidth: 0,
                    colorByPoint: true,
                    depth: 25,
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
                enabled: false,
                backgroundColor: '#EEE',
                borderRadius: 3,
                padding: 15,
                style: {
                    overflow: 'hidden'
                },
                title: {
                    text: 'aaa',
                    style: {
                        fontSize: '14px'
                    }
                },
                itemStyle: {
                    fontSize: '11px',
                    width: '120px',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                },
                //itemWidth: 120,
                useHTML: true,
                layout: 'vertical',
                align: 'left',
                verticalAlign: 'top',
                symbolHeight: 8,
                symbolWidth: 8,
                symbolRadius: 4
            }
        });

    },

    onResize: function () {
        this.chart.reflow();
    },

    onSetSeries: function (data) {
        this.getView().lookupReference('loader').hide();

        while(this.chart.series.length > 0) {
            this.chart.series[0].remove(true);
        }

        //var entry = this.getViewModel().get('entry');
        var _mainData = [], _otherCumulateVal = 0, i;

        for (i = 0; i < data.length; i += 1) {
            if (i < this.entry.get('pieNumSlices')) {
                _mainData.push({
                    name: data[i][this.entry.get('pieGroupColumn')] !== undefined ? data[i][this.entry.get('pieGroupColumn')] : 'None',
                    y: data[i].value
                });
            } else {
                _otherCumulateVal += data[i].value;
            }
        }

        if (_otherCumulateVal > 0) {
            _mainData.push({
                name: 'Other',
                color: '#DDD',
                y: _otherCumulateVal
            });
        }

        //this.chart.series[0].setData(_mainData, true, true);
        this.chart.addSeries({
            name: 'aaa',
            type: this.entry.get('pieStyle').indexOf('COLUMN') >= 0 ? 'column' : 'pie',
            colors: this.entry.get('colors') || this.defaultColors,
            innerSize: this.entry.get('pieStyle').indexOf('DONUT') >= 0 ? '50%' : 0,
            data: _mainData
        }, true, true);
    },

    onSetStyle: function () {
        var me = this,
            style = me.getViewModel().get('entry.pieStyle'),
            colors = me.getViewModel().get('entry.colors') || this.defaultColors;
        if (this.chart) {
            this.chart.series[0].update({
                type: style.indexOf('COLUMN') >= 0 ? 'column' : 'pie',
                colors: colors,
                innerSize: style.indexOf('DONUT') >= 0 ? '50%' : 0
            });
        }
    },

    onBeginFetchData: function() {
        this.getView().lookupReference('loader').show();
    }

});