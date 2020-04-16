import React, {Component} from "react";
import Table from "react-bootstrap/Table";

/**
 * TODO: Add additional fields to an application for configuration
 *
 * This model is used to display the available applications that exist
 */
export default class DisplayAvailableApplication extends Component {

    /**
     * State:
     *
     * message: table to display
     * applications: from api which where queried above
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            message: [],
            applications: this.props.applications
        };
    }

    /**
     * Generate the table to display the available applications
     */
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


    /**
     * Updates the applications and remounts components when needed to refresh. A refresh can be caused from a state changing operation
     * @param props
     * @param nextContext
     */
    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh)
            this.setState({applications: props.applications}, () => this.componentDidMount());
    }


    /**
     * Shows the table generate by {@link #componentDidMount}
     */
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