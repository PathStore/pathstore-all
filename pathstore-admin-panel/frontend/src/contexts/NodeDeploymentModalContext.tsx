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

import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {NodeDeploymentModal} from "../modules/nodeDeployment/NodeDeploymentModal";
import {NodeDeploymentModalData, useNodeDeploymentModalData} from "../hooks/useNodeDeploymentModalData";
import {LoadingModalProvider} from "./LoadingModalContext";
import {ErrorModalProvider} from "./ErrorModalContext";
import {SubmissionErrorModalProvider} from "./SubmissionErrorModalContext";

/**
 * This context is used to store additional data not known at creation of the modal. This context
 * is used throughout all the components associated with the node deployment modal
 */
export const NodeDeploymentModalDataContext = createContext<Partial<NodeDeploymentModalData>>({});

/**
 * Provider for the above context. This is private as it is only used around the node deployment modal provider
 * context defined below.
 * @param props
 * @constructor
 */
const NodeDeploymentModalDataProvider: FunctionComponent = (props) =>
    <NodeDeploymentModalDataContext.Provider value={useNodeDeploymentModalData()}>
        {props.children}
    </NodeDeploymentModalDataContext.Provider>;

/**
 * Context used for the node deployment modal information.
 */
export const NodeDeploymentModalContext = createContext<Partial<ModalInfo<undefined>>>({});

/**
 * Provider for the above context, it uses loading, error, and hypothetical modal providers. It also uses the above
 * data context provider
 * @param props
 * @constructor
 */
export const NodeDeploymentModalProvider: FunctionComponent = (props) =>
    <NodeDeploymentModalContext.Provider value={useModal<undefined>()}>
        <LoadingModalProvider>
            <ErrorModalProvider>
                <SubmissionErrorModalProvider>
                    <NodeDeploymentModalDataProvider>
                            <NodeDeploymentModal/>
                    </NodeDeploymentModalDataProvider>
                </SubmissionErrorModalProvider>
            </ErrorModalProvider>
        </LoadingModalProvider>
        {props.children}
    </NodeDeploymentModalContext.Provider>;