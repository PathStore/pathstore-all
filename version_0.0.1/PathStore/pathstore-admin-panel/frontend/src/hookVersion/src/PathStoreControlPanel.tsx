import React, {FunctionComponent, useContext, useEffect, useState} from "react";
import {useInterval} from "./hooks/useInterval";
import {APIContext} from "./contexts/APIContext";
import {genericLoadFunctionWait} from "./utilities/Utils";
import {ApplicationStatus, AvailableLogDates, Deployment} from "./utilities/ApiDeclarations";
import {ViewTopology} from "./modules/topology/ViewTopology";
import {Center} from "./utilities/AlignedDivs";
import {NodeInfoModalProvider} from "./contexts/NodeInfoModalContext";
import {NodeDeployment} from "./modules/nodeDeployment/NodeDeployment";
import {NodeDeploymentModalProvider} from "./contexts/NodeDeploymentModalContext";

/**
 * This is the main component for the pathstore control panel website. It must have access to the {@link APIContext}
 * as this component will manage the setting of the data within that context on a timer.
 * @constructor
 */
export const PathStoreControlPanel: FunctionComponent = () => {
    // Grab needed values from the context
    const {setDeployment, setApplicationStatus, setAvailableLogDates, forceRefresh} = useContext(APIContext);

    // state to force the use effect to only be called on startup
    const [called, setCalled] = useState<boolean>(false);

    // Load all data on startup
    useEffect(() => {
        if (forceRefresh && !called) {
            forceRefresh();
            setCalled(true);
        }
    }, [forceRefresh, called]);

    // every 2 seconds reload the below endpoint
    useInterval(() => {
        genericLoadFunctionWait<Deployment>('/api/v1/deployment', setDeployment);
        genericLoadFunctionWait<ApplicationStatus>('/api/v1/application_management', setApplicationStatus);
        genericLoadFunctionWait<AvailableLogDates>('/api/v1/available_log_dates', setAvailableLogDates);
    }, 2000);

    return (
        <>
            <Center>
                <h1 style={{fontWeight: 700, textDecoration: 'underline'}}>PathStore Control Panel</h1>
            </Center>
            <NodeInfoModalProvider>
                <ViewTopology/>
            </NodeInfoModalProvider>
            <NodeDeploymentModalProvider>
                <NodeDeployment/>
            </NodeDeploymentModalProvider>
        </>
    );
};