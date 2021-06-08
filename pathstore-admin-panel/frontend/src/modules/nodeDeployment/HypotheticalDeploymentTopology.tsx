/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import React, {FunctionComponent, useCallback, useContext} from "react";
import {AlignedDivs, Left, Right} from "../../utilities/AlignedDivs";
import {getDeploymentObjects} from "./NodeDeploymentModal";
import {Deployment, DEPLOYMENT_STATE} from "../../utilities/ApiDeclarations";
import {HypotheticalDeploymentInfoModalContext} from "../../contexts/HypotheticalDeploymentInfoModalContext";
import {NodeDeploymentModalDataContext} from "../../contexts/NodeDeploymentModalContext";
import {PathStoreTopology} from "../PathStoreTopology";

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
        if (!additionNodeIdSet || !deletionNodeIdSet) return 'd_current_line_node';
        else {

            if (additionNodeIdSet.has(object.new_node_id)) return 'd_orange_node';
            else if (deletionNodeIdSet.has(object.new_node_id)) return 'd_red_node';
            else {
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
                        return 'd_current_line_node'
                }
            }
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
            <Left width='45%'>
                <h2>Topology Legend</h2>
                <br/>
                <p>Hypothetical additions are in <span className={'d_orange'}>orange</span></p>
                <p>Hypothetical deletions are in <span className={'d_red'}>red</span></p>
                <br/>
                <p>Processing Removing and Removing are in <span className={'d_purple'}>purple</span></p>
                <p>Waiting Removal nodes are in <span className={'d_pink'}>pink</span></p>
                <br/>
                <p>Deployed nodes are in <span className={'d_green'}>green</span></p>
                <p>Processing Deploying and Deploying are in <span className={'d_cyan'}>cyan</span></p>
                <p>Waiting deployment nodes are in <span className={'d_yellow'}>yellow</span></p>
                <br/>
                <p>Failed nodes are in <span className={'d_current_line'}>grey</span></p>
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