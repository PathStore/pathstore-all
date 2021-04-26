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
import {Button} from "react-bootstrap";
import {NodeDeploymentModalContext} from "../../contexts/NodeDeploymentModalContext";

/**
 * This component is used to render the button to display the node deployment modal
 * @constructor
 * @see NodeDeploymentModal
 */
export const NodeDeployment: FunctionComponent = () => {
    const {show} = useContext(NodeDeploymentModalContext);

    // show the node deployment modal on click
    const onClick = useCallback(() => {
        if (show)
            show();
    }, [show]);

    return (
        <>
            <h2>Network Expansion</h2>
            <Button onClick={onClick}>Deploy Additional Nodes to the Network</Button>
        </>
    );
};