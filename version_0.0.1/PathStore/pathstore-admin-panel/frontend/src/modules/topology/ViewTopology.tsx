import React, {FunctionComponent, ReactElement, useCallback, useContext, useEffect, useState} from "react";
import {APIContext} from "../../contexts/APIContext";
import {PathStoreTopology} from "../PathStoreTopology";
import {Deployment, DEPLOYMENT_STATE} from "../../utilities/ApiDeclarations";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";
import {NodeInfoModalContext} from "../../contexts/NodeInfoModalContext";

/**
 * This component is used to display the current topology of the network
 *
 * Contexts:
 * @see NodeInfoModalContext
 */
export const ViewTopology: FunctionComponent = () => {

    /* api context for generating the node info modal data */
    const {deployment} = useContext(APIContext);

    /* Shows the info modal when the user clicks on a node in the topology */
    const nodeInfoModal = useContext(NodeInfoModalContext);

    // Store the tree in the state
    const [tree, updateTree] = useState<ReactElement | null>(null);

    /* Callback function used to handle the click of a node */
    const handleClick = useCallback((event: any, node: number) => {
        if (nodeInfoModal && nodeInfoModal.show)
            nodeInfoModal.show(node);

    }, [nodeInfoModal]);

    /**
     * When the deployment records update re-create the tree
     */
    useEffect(() => updateTree(
        deployment ?
            <PathStoreTopology width={1200}
                               height={500}
                               deployment={deployment}
                               get_colour={getClassName}
                               get_click={handleClick}
            />
            :
            null
    ), [deployment, handleClick]);

    /* Render the legend and the tree */
    return (
        <AlignedDivs>
            <Left width='25%'>
                <h2>Topology Legend</h2>
                <br/>
                <p>Processing Removing and Removing are in <span className={'d_purple'}>purple</span></p>
                <p>Waiting Removal nodes are in <span className={'d_pink'}>pink</span></p>
                <br/>
                <p>Deployed nodes are in <span className={'d_green'}>green</span></p>
                <p>Processing Deploying and Deploying are in <span className={'d_cyan'}>cyan</span></p>
                <p>Waiting deployment nodes are in <span className={'d_yellow'}>yellow</span></p>
                <br/>
                <p>Failed nodes are in <span className={'d_red'}>red</span></p>
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
        case DEPLOYMENT_STATE[DEPLOYMENT_STATE.PROCESSING_REMOVING]:
        case DEPLOYMENT_STATE[DEPLOYMENT_STATE.REMOVING]:
            return 'd_purple_node';
        case DEPLOYMENT_STATE[DEPLOYMENT_STATE.WAITING_REMOVAL]:
            return 'd_pink_node';
        case DEPLOYMENT_STATE[DEPLOYMENT_STATE.DEPLOYED]:
            return 'd_green_node';
        case DEPLOYMENT_STATE[DEPLOYMENT_STATE.PROCESSING_DEPLOYING]:
        case DEPLOYMENT_STATE[DEPLOYMENT_STATE.DEPLOYING]:
            return 'd_cyan_node';
        case DEPLOYMENT_STATE[DEPLOYMENT_STATE.WAITING_DEPLOYMENT]:
            return 'd_yellow_node';
        default:
            return 'd_red'
    }
};