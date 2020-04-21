import React, {Component} from "react";
import Table from "react-bootstrap/Table";

/**
 * TODO: Add additional fields to an application for configuration
 *
 * This model is used to display the available applications that exist
 *
 * Props:
 *
 * applications: from api
 *
 */
export default class DisplayAvailableApplication extends Component {


    /**
     * Shows the table generate by {@link #componentDidMount}
     */
    render() {


        let response = [];

        if (this.props.applications.length > 0) {

            let tableContents = [];

            tableContents.push(
                <thead key={0}>
                <tr>
                    <th>Application Name</th>
                    <th>TODO</th>
                </tr>
                </thead>
            );

            let body = [];

            for (let i = 0; i < this.props.applications.length; i++) {
                body.push(
                    <tr>
                        <td>{this.props.applications[i].application}</td>
                    </tr>
                )
            }

            tableContents.push(
                <tbody key={1}>
                {body}
                </tbody>
            );

            response.push(
                <Table>
                    {tableContents}
                </Table>
            );

        } else response.push(<p>No applications are installed on the network</p>);

        return (
            <div>
                <p>Available applications</p>
                {response}
            </div>
        )
    }
}