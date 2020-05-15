import {ApplicationStatus} from "../utilities/ApiDeclarations";
import React, {FunctionComponent} from "react";
import {Table} from "react-bootstrap";

/**
 * Properties definition for {@link ApplicationStatusViewer}
 */
interface ApplicationStatusViewerProps {
    /**
     * List of application statues. This can be given as the full api set or a subset of that
     */
    readonly applicationStatus: ApplicationStatus[]
}

/**
 * This component is used to render a table to inform the user the current application statues for a desired set of nodes.
 *
 * In practice this is used in the Node Info Modal where the status information in the props is filter based on the node
 * id of the node info modal displayed
 *
 * @param props
 * @constructor
 */
export const ApplicationStatusViewer: FunctionComponent<ApplicationStatusViewerProps> = (props) => {

    // If there are no application status's in the dataset inform the user about this instead of rendering an empty table
    if (props.applicationStatus.length === 0)
        return (
            <div>
                <h2>Application Status Viewer</h2>
                <p>There are no application status records</p>
            </div>
        );

    let body = [];

    // table body from the given data set
    for (let i = 0; i < props.applicationStatus.length; i++) {

        let currentObject = props.applicationStatus[i];

        body.push(
            <tr>
                <td>{currentObject.nodeid}</td>
                <td>{currentObject.keyspace_name}</td>
                <td>{currentObject.process_status}</td>
                <td>{currentObject.wait_for}</td>
                <td>{currentObject.process_uuid}</td>
            </tr>)
    }

    return (
        <div>
            <h2>Application Status Viewer</h2>
            <Table>
                <thead>
                <tr>
                    <th>Nodeid</th>
                    <th>Application</th>
                    <th>Status</th>
                    <th>Waiting</th>
                    <th>Job UUID</th>
                </tr>
                </thead>
                <tbody>
                    {body}
                </tbody>
            </Table>
        </div>
    );
};