import {FunctionComponent} from "react";
import {Application} from "../../utilities/ApiDeclarations";
import {Table} from "react-bootstrap";
import React from "react";

/**
 * Properties definition for {@link DisplayAvailableApplications}
 */
interface DisplayAvailableApplicationsProperties {
    /**
     * List of application objects from api
     */
    readonly applications: Application[]
}

/**
 * This component is used to display all available applications to the user
 *
 * @param props
 * @constructor
 */
export const DisplayAvailableApplications: FunctionComponent<DisplayAvailableApplicationsProperties> = (props) => {
    let response = [];

    if (props.applications.length > 0) {

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

        for (let i = 0; i < props.applications.length; i++) {
            body.push(
                <tr key={i}>
                    <td>{props.applications[i].keyspace_name}</td>
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
};