Ext.define('Ung.reports.Theme', {
    alternateClassName: 'Theme',
    singleton: true,

    DEFAULT: {
        // colors: Util.defaultColors,
        chart: {
            backgroundColor: '#FFFFFF'
        },
        xAxis: {
            lineColor: '#C0D0E0',
            labels: {
                style: {
                    color: '#777'
                }
            }
        },
        yAxis: {
            lineColor: '#C0D0E0',
            gridLineColor: '#EEE',
            labels: {
                style: {
                    color: '#777'
                }
            },
            title: {
                style: {
                    color: '#777'
                }
            }
        },
        plotOptions: {
            pie: {
                borderColor: '#FFF'
            }
        },
        legend: {
            itemStyle: {
                color: '#333'
            },
            itemHoverStyle: {
                color: '#000'
            }
        },
        loading: {
            style: {
                backgroundColor: '#FFF',
                opacity: 0.5
            }
        }
    },

    DARK: {
        // colors: ['#2b908f', '#90ee7e', '#f45b5b', '#7798BF', '#aaeeee', '#ff0066', '#eeaaee', '#55BF3B', '#DF5353', '#7798BF', '#aaeeee'],
        chart: {
            backgroundColor: {
                linearGradient: { x1: 0, y1: 1, x2: 0, y2: 1 },
                stops: [
                   [0, '#2a2a2b'],
                   [1, '#3e3e40']
                ]
            }
        },
        xAxis: {
            lineColor: '#707073',
            labels: {
                style: {
                    color: '#CCC'
                }
            }
        },
        yAxis: {
            lineColor: '#707073',
            gridLineColor: '#555',
            labels: {
                style: {
                    color: '#CCC'
                }
            },
            title: {
                style: {
                    color: '#CCC'
                }
            }
        },
        plotOptions: {
            pie: {
                borderColor: 'transparent'
            }
        },
        tooltip: {
            headerFormat: '<p style="margin: 0 0 5px 0; color: #EEE;">{point.key}</p>',
            backgroundColor: 'rgba(0, 0, 0, 0.85)',
            style: {
                color: '#F0F0F0'
            }
        },
        legend: {
            itemStyle: {
                color: '#DDD'
            },
            itemHoverStyle: {
                color: '#FFF'
            }
        },
        loading: {
            style: {
                backgroundColor: '#000',
                opacity: 0.5
            }
        }
    },

    SAND: {
        // colors: Util.defaultColors,
        chart: {
            background: null
        },
        xAxis: {
            lineColor: '#C0D0E0',
            labels: {
                style: {
                    color: '#777'
                }
            }
        },
        yAxis: {
            lineColor: '#C0D0E0',
            gridLineColor: '#EEE',
            labels: {
                style: {
                    color: '#777'
                }
            },
            title: {
                style: {
                    color: '#777'
                }
            }
        },
        legend: {
            itemStyle: {
                color: '#333'
            },
            itemHoverStyle: {
                color: '#000'
            }
        },
        loading: {
            style: {
                backgroundColor: '#FFF',
                opacity: 0.5
            }
        }
    }
});
