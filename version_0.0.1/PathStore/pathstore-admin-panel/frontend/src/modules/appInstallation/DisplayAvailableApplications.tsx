import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useState} from "react";
import {APIContext} from "../../contexts/APIContext";
import {Table} from "react-bootstrap";
import {ObjectRow} from "../../utilities/ObjectRow";
import {Application} from "../../utilities/ApiDeclarations";
import {ApplicationDeploymentModalContext} from "../../contexts/ApplicationDeploymentModalContext";

/**
 * This component is used to show the user what application are available on the network.
 * If no applications are installed then it will inform them that none are installed.
 * You can click on each table row to load the ADM for that given application to visually
 * watch the network transition through the states of application installation
 * @constructor
 */
export const DisplayAvailableApplications: FunctionComponent = () => {

    // load applications from APIContext
    const {applications} = useContext(APIContext);

    // internal state for the table or the message to inform the user that nothing is available
    const [table, setTable] = useState<ReactElement | HTMLParagraphElement | null>(null);

    // ADM context to load the modal on click of a given row
    const applicationDeploymentModal = useContext(ApplicationDeploymentModalContext);

    /**
     * On click function to load the ADM for that given application
     */
    const onRowClick = useCallback((application: Application) => {
        if (applicationDeploymentModal.show)
            applicationDeploymentModal.show(application);
    }, [applicationDeploymentModal]);

    /**
     * This effect is used to update the table if the applications from the API context change i.e. someone loaded
     * another application
     */
    useEffect(() => {

        let value: ReactElement | HTMLParagraphElement;

        if (applications && applications.length > 0) {

            let tbody = [];

            for (let [index, application] of applications.entries()) {
                tbody.push(
                    <ObjectRow<Application> key={index} value={application} handleClick={onRowClick}>
                        <td>{application.keyspace_name}</td>
                    </ObjectRow>
                );
            }

            value = (
                <Table>
                    <thead>
                    <tr>
                        <th>Application Name</th>
                    </tr>
                    </thead>
                    <tbody>
                    {tbody}
                    </tbody>
                </Table>
            );
        } else
            value = (
                <p>No application are installed on the network</p>
            );

        setTable(value);
    }, [applications, setTable, onRowClick]);

    // display the table
    return (
        <>
            <p>Available applications</p>
            {table}
            <p>Click on a row to view / modify the installation of the given application</p>
        </>
    );
};