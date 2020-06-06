import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useState} from "react";
import Modal from "react-bootstrap/Modal";
import {Button} from "react-bootstrap";
import {LiveTransitionVisualModalContext} from "../../contexts/LiveTransitionVisualModalContext";
import {useObjectAttachedSet} from "../../hooks/useObjectAttachedSet";
import {APPLICATION_STATE, ApplicationStatus, Deployment} from "../../utilities/ApiDeclarations";
import {APIContext} from "../../contexts/APIContext";
import {PathStoreTopology} from "../../../../modules/PathStoreTopology";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";
import {useReducedState} from "../../hooks/useReducedState";


// How to map an application status object to its node id
const mapApplicationStatusToNodeId = (applicationStatus: ApplicationStatus) => applicationStatus.node_id;

// Filter a given state by what their current process status is
const filterByState = (value: APPLICATION_STATE) => (applicationStatus: ApplicationStatus) => applicationStatus.process_status === APPLICATION_STATE[value];

// NOTE: All functions are statically defined rather then using callbacks.

// filter by waiting install
const waitingFilter = filterByState(APPLICATION_STATE.WAITING_INSTALL);

// filter by installing
const installingFilter = filterByState(APPLICATION_STATE.INSTALLING);

// filter by processing installing
const processingInstallingFilter = filterByState(APPLICATION_STATE.PROCESSING_INSTALLING);

// filter by installed
const installedFilter = filterByState(APPLICATION_STATE.INSTALLED);

/**
 * This component is used to show the user what the network is doing while it process the installation of an application
 * on a sub tree of the network. This component can display the responsiveness to the user of how the network performs
 * tasks
 * @constructor
 */
export const LiveTransitionVisualModal: FunctionComponent = () => {

    // Pull the application status and deployment records from the api
    const {applicationStatus, deployment} = useContext(APIContext);

    // load the modal information from the modal context
    const {visible, data, close} = useContext(LiveTransitionVisualModalContext);

    // Callback function to map the application status object to a specific keyspace
    const mapApplicationStatusToKeySpaceName = useCallback((applicationStatus: ApplicationStatus) => applicationStatus.keyspace_name === data?.keyspace_name, [data]);

    // Create a reduced application status set based on what keyspace was passed
    const reducedApplicationStatus = useReducedState<ApplicationStatus>(
        applicationStatus,
        mapApplicationStatusToKeySpaceName);

    // Keep an updated waiting deployment set to use too determining each node's colour
    const waiting: Set<number> = useObjectAttachedSet<ApplicationStatus, number>(
        reducedApplicationStatus,
        mapApplicationStatusToNodeId,
        waitingFilter);

    // Keep an updated installing deployment set to use to determine each node's colour
    const installing: Set<number> = useObjectAttachedSet<ApplicationStatus, number>(
        reducedApplicationStatus,
        mapApplicationStatusToNodeId,
        installingFilter);

    // Keep an updated installing deployment set to use to determine each node's colour
    const processingInstalling: Set<number> = useObjectAttachedSet<ApplicationStatus, number>(
        reducedApplicationStatus,
        mapApplicationStatusToNodeId,
        processingInstallingFilter);

    // Keep an updated installed deployment set to use to determine each node's colour
    const installed: Set<number> = useObjectAttachedSet<ApplicationStatus, number>(
        reducedApplicationStatus,
        mapApplicationStatusToNodeId,
        installedFilter);

    /**
     * This function is used to determine the css class for a given node based on which set the given deployment object
     * node id is apart of
     */
    const getColour = useCallback((object: Deployment): string => {
        const node = object.new_node_id;

        if (installed.has(node)) return 'installed_node';
        else if (processingInstalling.has(node)) return 'processing_node';
        else if (installing.has(node)) return 'installing_node';
        else if (waiting.has(node)) return 'waiting_node';
        else return 'not_set_node';

    }, [waiting, installing, processingInstalling, installed]);

    // Store an internal state for the tree as the deployment records may be null on startup
    const [tree, updateTree] = useState<ReactElement | null>(null);

    // update the tree when the deployment records change
    useEffect(() => {
        const value: ReactElement =
            deployment ?
                <PathStoreTopology width={700}
                                   height={500}
                                   deployment={deployment.filter(i => i.process_status === "DEPLOYED")}
                                   get_colour={getColour}/>
                : <p>Loading...</p>;
        updateTree(value);
    }, [deployment, getColour, updateTree]);

    return (
        <Modal show={visible}
               size={"xl"}
               centered
        >
            <Modal.Header>
                <Modal.Title>Live updates for {data?.keyspace_name}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <AlignedDivs>
                    <Left width='35%'>
                        <h2>Topology Legend</h2>
                        <p>Nodes installed are in <span className={'d_green'}>green</span></p>
                        <p>Nodes processing are in <span className={'d_orange'}>orange</span></p>
                        <p>Nodes installing are in <span className={'d_cyan'}>cyan</span></p>
                        <p>Nodes waiting are in <span className={'d_yellow'}>yellow</span></p>
                        <p>Nodes not set are <span className={'d_currentLine'}>dark grey</span></p>
                    </Left>
                    <Right>
                        <h2>Topology</h2>
                        {tree}
                    </Right>
                </AlignedDivs>
            </Modal.Body>
            <Modal.Footer>
                <Button onClick={close}>close</Button>
            </Modal.Footer>
        </Modal>
    );
};