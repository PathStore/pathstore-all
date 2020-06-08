import React, {FunctionComponent, useCallback, useContext} from "react";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";
import {PathStoreTopology} from "../../../../modules/PathStoreTopology";
import {getDeploymentObjects} from "./NodeDeploymentModal";
import {Deployment} from "../../utilities/ApiDeclarations";
import {HypotheticalDeploymentInfoModalContext} from "../../contexts/HypotheticalDeploymentInfoModalContext";
import {NodeDeploymentModalDataContext} from "../../contexts/NodeDeploymentModalContext";

/**
 * This component is used to manage the topology of the hypothetical network in the node deployment modal.
 *
 * If you click on a node it will bring up {@link HypotheticalDeploymentInfoModalContext} which allows the user
 * to see what server the given node is installed on and allows them to delete it if them chose.
 *
 * NOTE: choosing to delete a node will always delete all its children this will be visually shown to the user
 *
 * @param props
 * @constructor
 */
export const HypotheticalDeploymentTopology: FunctionComponent = (props) => {

    // load needed information from the node deployment modal data context
    const {deployment, additions, additionNodeIdSet, deletionNodeIdSet} = useContext(NodeDeploymentModalDataContext);

    // Allows the hypothetical info modal to be used
    const hypotheticalInfoModal = useContext(HypotheticalDeploymentInfoModalContext);

    /**
     * Graph colour function.
     *
     * If the node is in the addition set then the node is cyan
     * If the node is in the deletion set then the node is red
     * Else the node is grey
     */
    const getColour = useCallback((object: Deployment): string => {
        if (!additionNodeIdSet || !deletionNodeIdSet) return 'not_set_node';
        else {
            return additionNodeIdSet.has(object.new_node_id) ?
                'hypothetical'
                :
                deletionNodeIdSet.has(object.new_node_id) ?
                    'uninstalled_node'
                    :
                    object.process_status === "DEPLOYED" ?
                        'not_set_node'
                        : 'waiting_node';
        }
    }, [additionNodeIdSet, deletionNodeIdSet]);

    /**
     * If any node is clicked render a hypothetical info modal with their information
     */
    const handleClick = useCallback((event: any, node: number): void => {
        if (hypotheticalInfoModal.show && additionNodeIdSet)
            hypotheticalInfoModal.show({
                node: node,
                isHypothetical: additionNodeIdSet.has(node)
            });
    }, [additionNodeIdSet, hypotheticalInfoModal]);

    return (
        <AlignedDivs>
            <Left width='35%'>
                <h2>Topology Legend</h2>
                <br/>
                <p>Deployed Node: <span className={'d_currentLine'}>Light Grey</span></p>
                <p>Hypothetical Node: <span className={'d_cyan'}>Cyan</span></p>
                <p>Hypothetical Deletion: <span className={'d_red'}>Red</span></p>
            </Left>
            <Right>
                <h2>Hypothetical Topology</h2>
                <PathStoreTopology width={700}
                                   height={500}
                                   deployment={getDeploymentObjects(deployment, additions)}
                                   get_colour={getColour}
                                   get_click={handleClick}
                />
                {props.children}
            </Right>
        </AlignedDivs>
    );
};