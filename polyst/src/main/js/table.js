'use strict';

// tag::vars[]
const React = require('react');
const ReactDOM = require('react-dom');
const $ = require("jquery");
import ReactTable from "react-table";
import "react-table/react-table.css";
// end::vars[]

// tag::app[]
class App extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            loading: true,
            data: []
        };
    }

    componentDidMount() {
        const self = this;
        $.ajax({
            url: "/api/proteins"
        }).then(function (data) {
            self.setState({
                data: data,
                loading: false
            });
        });
    }

    render() {
        if (this.state.data.length === 0) {
            return null;
        }
        let data = this.state.data;

        const columns = [
            {
                Header: "Accession",
                accessor: "accession",
                filterMethod: (filter, row) => row[filter.id].toLowerCase().includes(filter.value.toLowerCase()),
                Cell: row => (
                    <div style={{display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'center'}}>
                        <a href={"//www.uniprot.org/uniprot/" + row.value} target="_blank" style={{margin: '0 1em'}}>
                            <img
                                src="//www.ebi.ac.uk/sites/ebi.ac.uk/files/documents/uniprot_rgb_320x146.png"
                                style={{width: `40px`, margin: "0"}}
                            />
                        </a>
                        {row.value}
                    </div>
                ),
                // maxWidth: 500,
            },
            {
                Header: "Size",
                accessor: "size",
                filterMethod: (filter, row) => row[filter.id] <= filter.value,
                Cell: row => (
                    <div style={{display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'center'}}>
                        {row.value}
                    </div>
                ),
                // maxWidth: 300,
            },
            {
                Header: "Results",
                sortable: false,
                filterable: false,
                Cell: cellInfo => (
                    <div style={{display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'center'}}>
                        <a href={"/proteins/" + cellInfo.row.accession} target="_blank">Matrix</a>
                        <a href={"/api/proteins/" + cellInfo.row.accession + "/download"} style={{margin: '0 1em'}}>Download</a>
                    </div>
                ),
                // maxWidth: 400
            }
        ];


        return (
            <div className="site">
                <div style={{display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'center'}}>
                    <h1>PolyST+</h1>
                </div>
                <ReactTable
                    filterable
                    resizable
                    data={data}
                    columns={columns}
                    defaultPageSize={15}
                    className="-striped -highlight"
                />
            </div>
        );
    }
}
// end::app[]

// tag::render[]
ReactDOM.render(
    <App />,
    document.getElementById('react')
);
// end::render[]