
'use strict';

// tag::vars[]
const React = require('react');
const ReactDOM = require('react-dom');
// end::vars[]

// tag::app[]
class App extends React.Component {

    constructor(props) {
        super(props);
    }

    render() {
        return (
            <div >
                <h1>Welcome to PolyST!</h1>
            </div>
    )
    }
}
// end::app[]

// tag::render[]
ReactDOM.render(
    <App />,
    document.getElementById('react')
)
// end::render[]