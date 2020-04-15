import React, {Component} from "react";
import Table from "react-bootstrap/Table";

export default class DisplayAvailableApplication extends Component {

    constructor(props) {
        super(props);
        this.state = {
            message: [],
            applications: this.props.applications
        };
    }

    componentDidMount() {
        let response = [];

        response.push(
            <thead key={0}>
            <tr>
                <th>Application Name</th>
                <th>TODO</th>
            </tr>
            </thead>
        );

        let body = [];

        for (let i = 0; i < this.state.applications.length; i++) {
            body.push(
                <tr>
                    <td>{this.state.applications[i].application}</td>
                </tr>
            )
        }

        response.push(
            <tbody key={1}>
            {body}
            </tbody>
        );

        this.setState({message: response});
    }


    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh)
            this.setState({applications: props.applications}, () => this.componentDidMount());

    }


    render() {
        return (
            <div>
                <p>Available applications</p>
                <Table>
                    {this.state.message}
                </Table>
            </div>
        )
    }
}