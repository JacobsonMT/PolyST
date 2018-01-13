'use strict';

// tag::vars[]
const React = require('react');
const ReactDOM = require('react-dom');
const $ = require("jquery");

import Highcharts from "highcharts";
import HeatMap from "highcharts/modules/heatmap";
import Boost from "highcharts/modules/boost";
import Exporting from "highcharts/modules/exporting";
HeatMap(Highcharts);
Boost(Highcharts);
Exporting(Highcharts);

// end::vars[]


// /**
//  * Synchronize zooming through the setExtremes event handler.
//  */
// function syncExtremes(e) {
//     var thisChart = this.chart;
//
//     if (e.trigger !== 'syncExtremes') { // Prevent feedback loop
//         window.charts.forEach( function (chart) {
//             if (chart !== thisChart) {
//                 if (chart.xAxis[0].setExtremes) { // It is null while updating
//                     chart.xAxis[0].setExtremes(e.min, e.max, undefined, false, { trigger: 'syncExtremes' });
//                 }
//             }
//         });
//     }
// }

/**
 * Custom Axis extension to allow emulation of negative values on a logarithmic
 * Y axis. Note that the scale is not mathematically correct, as a true
 * logarithmic axis never reaches or crosses zero.
 */
(function (H) {
    // Pass error messages
    H.Axis.prototype.allowNegativeLog = true;

    // Override conversions
    H.Axis.prototype.log2lin = function (num) {
        var isNegative = num < 0,
            adjustedNum = Math.abs(num),
            result;
        if (adjustedNum < 10) {
            adjustedNum += (10 - adjustedNum) / 10;
        }
        result = Math.log(adjustedNum) / Math.LN10;
        return isNegative ? -result : result;
    };
    H.Axis.prototype.lin2log = function (num) {
        var isNegative = num < 0,
            absNum = Math.abs(num),
            result = Math.pow(10, absNum);
        if (result < 10) {
            result = (10 * (result - 1)) / (10 - 1);
        }
        return isNegative ? -result : result;
    };
}(Highcharts));

// tag::app[]
class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {protein: {sequence: []}};
        window.charts = [];
    }

    componentDidMount() {
        const self = this;
        $.ajax({
            url: "/api/proteins/" + accession
        }).then(function (data) {
            self.setState({protein: data});
        });
    }

    render() {

        let data = [];
        let categories = [];
        let depthData = [];
        let iupData = [];
        let conservationData = [];
        this.state.protein.sequence.forEach(function (base, x) {
            // if (x < 100) {
            categories.push(base.reference);
            depthData.push(base.depth);
            iupData.push(base.iupred);
            conservationData.push(base.conservation);
            if (base.pst.length === 0) {
                base.pst = new Array(20).fill(0);
            }
            base.pst.forEach(function (val, y) {
                // if (y < 5) {
                data.push([x, y, val]);
                // }
            });

            // }
        });


        let charts = [];
        if (data.length !== 0) {
            charts.push(<HeatMapChart
                key="heatmap"
                data={data}
                categories={categories}
            />)
        }

        // if (depthData.length !== 0) {
        //     charts.push(<Chart
        //         title="Depth"
        //         key="depth"
        //         data={depthData}
        //         xAxisVisible={false}
        //         enableCredit={false}
        //         yAxisType="logarithmic"
        //     />)
        // }

        if (iupData.length !== 0) {
            charts.push(<Chart
                title="IUPred"
                key="iupred"
                data={iupData}
                xAxisVisible={false}
                enableCredit={false}
                yAxisType="linear"
            />)
        }

        if (conservationData.length !== 0) {
            charts.push(<Chart
                title="Conservation"
                key="conservation"
                data={conservationData}
                xAxisVisible={true}
                enableCredit={true}
                yAxisType="linear"
            />)
        }

        if (charts.length === 0) {
            return null;
        }

        return (
            <div className="App">
                {charts}
            </div>
        );
    }
}
// end::app[]


class HeatMapChart extends React.Component {
    componentDidMount() {
        const staggerEnabled = (this.props.data.length <= 350 * 20);
        const staggerLines = staggerEnabled ? Math.ceil(this.props.data.length / (120 * 20)) : -1;
        const options = {

            chart: {
                type: 'heatmap',
                zoomType: 'x',
                resetZoomButton: {
                    position: {
                        // align: 'right', // by default
                        // verticalAlign: 'top', // by default
                        x: 0,
                        y: -40
                    }
                },
                height: 300,
                marginLeft: 40,
                marginRight: 75,
                // marginTop: 40,
                // marginBottom: 80,
                plotBorderWidth: 1
            },

            boost: {
                useGPUTranslations: true
            },

            credits: false,


            title: {
                text: accession
            },

            plotOptions: {
                pointStart: 1,
                series: {
                    point: {
                        events: {
                            mouseOver: function (e) {
                                var p = this;
                                window.charts.forEach(function (chart) {
                                    try {
                                        chart.xAxis[0].removePlotLine('plot-line-sync');
                                        chart.xAxis[0].addPlotLine({
                                            value: p.x + 1,
                                            color: "#cccccc",
                                            width: 1,
                                            zIndex: 5,
                                            id: 'plot-line-sync'
                                        });
                                    } catch (e) {
                                        console.log(e);
                                    }

                                });
                            }
                        }
                    }
                }
            },

            xAxis: {
                categories: this.props.categories,
                minPadding: 0,
                maxPadding: 0,
                startOnTick: true,
                endOnTick: true,
                tickWidth: 0,
                tickInterval: 1,
                padding: 1,
                labels: {
                    enabled: staggerEnabled,
                    staggerLines: staggerLines,
                    style: {
                        fontSize: '8px',
                    },
                },
                step: 1
            },

            yAxis: {
                categories: ['A', 'R', 'N', 'D', 'C', 'Q', 'E', 'G', 'H', 'I', 'L', 'K', 'M', 'F', 'P', 'S', 'T', 'W', 'Y', 'V'],
                title: null,
                tickInterval: 1,
                startOnTick: true,
                endOnTick: false,
                min: 0,
                max: 19,
                padding: 1,
                step: 1,
                tickWidth: 0,
                labels: {
                    align: 'center',
                    step: 1,
                    style: {
                        fontSize: '9px',
                    },
                },
            },

            colorAxis: {
                min: 0,
                max: 1,
                stops: [[0, '#ffffcc'],
                    [0.6, '#a1dab4'],
                    [0.8, '#41b6c4'],
                    [0.9, '#2c7fb8'],
                    [0.95, '#253494']],
                // minColor: '#FFFFFF',
                // maxColor: '#000099' //Highcharts.getOptions().colors[0]
            },

            legend: {
                align: 'right',
                layout: 'vertical',
                margin: 0,
                verticalAlign: 'top',
                y: 28,
                symbolHeight: 233 - 9 * staggerLines,
                navigation: {
                    enabled: false,
                    arrowSize: 0,
                }
            },

            tooltip: {
                formatter: function () {
                    return '<b>Mutation:</b> ' + this.series.xAxis.categories[this.point.x] + ' ' + (this.point.x + 1) + ' ' +
                        this.series.yAxis.categories[this.point.y] + '<br><b>Effect:</b> ' + this.point.value;
                }
            },

            series: [{
                boostThreshold: 100,
                name: 'LIST',
                borderWidth: 0,
                data: this.props.data,
                dataLabels: {
                    enabled: false,
                    color: '#000000'
                },
                turboThreshold: 1000,
            }]

        };

        this.chart = new Highcharts[this.props.type || 'Chart'](
            this.chartEl,
            options
        );

    }

    componentWillUnmount() {
        this.chart.destroy();
    }

    render() {
        return <div ref={el => (this.chartEl = el)}/>;
    }
}

class Chart extends React.Component {
    componentDidMount() {
        const options = {

            title: {
                text: this.props.title,
                align: 'left',
                margin: 0,
                x: 40
            },

            credits: this.props.enableCredit,

            chart: {
                height: 175,
                marginLeft: 40,
                marginRight: 75,
                spacingTop: 20,
                spacingBottom: 20
            },

            boost: {
                usePreallocated: true
            },

            plotOptions: {
                series: {
                    pointStart: 1,
                    point: {
                        events: {
                            mouseOver: function (e) {
                                var p = this;
                                window.charts.forEach(function (chart) {
                                    try {
                                        chart.xAxis[0].removePlotLine('plot-line-sync');
                                        chart.xAxis[0].addPlotLine({
                                            value: p.x,
                                            color: "#cccccc",
                                            width: 1,
                                            zIndex: 5,
                                            id: 'plot-line-sync'
                                        });
                                    } catch (e) {
                                        console.log(e);
                                    }
                                });
                            }
                        }
                    }
                }
            },

            xAxis: {
                min: 0.5,
                max: this.props.data.length + 0.5,
                crosshair: true,
                minPadding: 0,
                maxPadding: 0,
                startOnTick: false,
                endOnTick: false,
                visible: this.props.xAxisVisible
            },

            yAxis: {
                title: null,
                type: this.props.yAxisType,
                maxPadding: 0,
                minPadding:0,
            },

            legend: {
                enabled: false,
            },

            tooltip: {
                formatter: function () {
                    return '<b>Position:</b> ' + this.x + '<br>' + '<b>' + this.series.name + ':</b> ' + this.y;
                }
            },

            series: [{
                boostThreshold: 1000,
                name: this.props.title,
                type: 'area',
                data: this.props.data,
            }]
        };

        this.chart = new Highcharts[this.props.type || 'Chart'](
            this.chartEl,
            options
        );

        window.charts.push(this.chart);
    }

    componentWillUnmount() {
        this.chart.destroy();
    }

    render() {
        return <div ref={el => (this.chartEl = el)}/>;
    }
}

// tag::render[]
ReactDOM.render(
    <App />,
    document.getElementById('react')
);
// end::render[]