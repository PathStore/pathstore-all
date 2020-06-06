import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useState} from "react";
import {APIContext} from "../../contexts/APIContext";
import {Table} from "react-bootstrap";
import {ObjectRow} from "../../utilities/ObjectRow";
import {Application} from "../../utilities/ApiDeclarations";
import {LiveTransitionVisualModalContext} from "../../contexts/LiveTransitionVisualModalContext";

/**
 * This component is used to show the user what application are available on the network.
 * If no applications are installed then it will inform them that none are installed.
 * You can click on each table row to load the LVTM for that given application to visually
 * watch the network transition through the states of application installation
 * @constructor
 */
export const DisplayAvailableApplications: FunctionComponent = () => {

    // load applications from APIContext
    const {applications} = useContext(APIContext);

    // internal state for the table or the message to inform the user that nothing is available
    const [table, setTable] = useState<ReactElement | HTMLParagraphElement | null>(null);

    // LTVM context to load the modal on click of a given row
    const liveTransitionVisualModal = useContext(LiveTransitionVisualModalContext);

    /**
     * On click function to load the LTVM for that given application
     */
    const onClick = useCallback((application: Application) => {
        if (liveTransitionVisualModal.show)
            liveTransitionVisualModal.show(application);
    }, [liveTransitionVisualModal]);

    /**
     * This effect is used to update the table if the applications from the API context change i.e. someone loaded
     * another application
     */
    useEffect(() => {

        let value: ReactElement | HTMLParagraphElement;

        if (applications && applications.length > 0) {

            let tbody = [];

            for (let i = 0; i < applications.length; i++) {
                const current = applications[i];
                tbody.push(
                    <ObjectRow<Application> key={i} value={current} handleClick={onClick}>
                        <td>{current.keyspace_name}</td>
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
        } else {
            value = (
                <p>No application are installed on the network</p>
            );
        }

        setTable(value)
    }, [applications, setTable, onClick]);

    // display the table
    return (
        <>
            <p>Available applications</p>
            {table}
        </>
    );
};