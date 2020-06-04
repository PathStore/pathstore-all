import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useState} from "react";
import {APIContext} from "../../contexts/APIContext";
import {PathStoreTopology} from "../PathStoreTopology";
import {Deployment} from "../../utilities/ApiDeclarations";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";
import {getNodeInfoModalData, NodeInfoModalContext} from "../../contexts/NodeInfoModalContext";

/**
 * This component is used to display the current topology of the network
 *
 * Contexts:
 * @see NodeInfoModalContext
 */
export const ViewTopology: FunctionComponent = () => {

    /* api context for generating the node info modal data */
    const apiContext = useContext(APIContext);

    /* Shows the info modal when the user clicks on a node in the topology */
    const nodeInfoModal = useContext(NodeInfoModalContext);

    // Store the tree in the state
    const [tree, updateTree] = useState<ReactElement | null>(null);

    /* Callback function used to handle the click of a node */
    const handleClick = useCallback((event: any, node: number) => {
        if (nodeInfoModal && nodeInfoModal.show)
            nodeInfoModal.show(getNodeInfoModalData(apiContext, node));

    }, [nodeInfoModal, apiContext]);

    /**
     * When the deployment records update re-create the tree
     */
    useEffect(() => updateTree(
        apiContext && apiContext.deployment ?
            <PathStoreTopology width={1200}
                               height={500}
                               deployment={apiContext.deployment}
                               get_colour={getClassName}
                               get_click={handleClick}
            />
            :
            null
    ), [apiContext, handleClick]);

    /* Render the legend and the tree */
    return (
        <AlignedDivs>
            <Left width='25%'>
                <h2>Topology Legend</h2>
                <br/>
                <p>Waiting Deployment: <span className={'d_yellow'}>Yellow</span></p>
                <p>Deploying / Processing Deploying: <span className={'d_cyan'}>Cyan</span></p>
                <p>Deployed: <span className={'d_green'}>Green</span></p>
                <p>Failed: <span className={'d_red'}>Red</span></p>
                <br/>
                <p>Waiting Removal: <span className={'d_pink'}>Pink</span></p>
                <p>Removing / Processing Removing: <span className={'d_purple'}>Purple</span></p>
            </Left>
            <Right>
                <h2>Topology</h2>
                <p>Click on a node to view its current applications</p>
                {tree}
            </Right>
        </AlignedDivs>
    );
};

/**
 * Pathstore topology colour function. This function colours nodes based on their deployment status
 *
 * Waiting is orange (waiting_node)
 * Installing is blue (installing_node)
 * Failed is red (uninstalled_node)
 * Deployed is green (installed_node)
 *
 * @param object
 * @returns {string}
 */
const getClassName = (object: Deployment): string => {
    switch (object.process_status) {
        case "WAITING_DEPLOYMENT":
            return 'waiting_node';
        case "DEPLOYING":
        case "PROCESSING_DEPLOYING":
            return 'installing_node';
        case "FAILED":
            return 'uninstalled_node';
        case "WAITING_REMOVAL":
            return 'waiting_removal_node';
        case "REMOVING":
        case "PROCESSING_REMOVING":
            return 'removing_node';
        default:
            return 'installed_node';
    }
};