import React, {Component} from "react";
import Table from "react-bootstrap/Table";

/**
 * This component is used to display all available applications to the user
 *
 * Props:
 * applications: list of applications objects from api
 */
export default class DisplayAvailableApplication extends Component {

    /**
     * Generate a table to display all applications installed on the system
     *
     * @returns {*}
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
                    <tr key={i}>
                        <td>{this.props.applications[i].keyspace_name}</td>
                    </tr>
                )
            }

            tableContents.push(
                <tbody key={1}>
                {body}
                </tbody>
            );

            response.push(
                <Table key={0}>
                    {tableContents}
                </Table>
            );

        } else response.push(<p key={0}>No applications are installed on the network</p>);

        return (
            <div>
                <p>Available applications</p>
                {response}
            </div>
        )
    }
}