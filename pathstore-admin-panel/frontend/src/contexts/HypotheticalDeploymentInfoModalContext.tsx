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
import {HypotheticalDeploymentInfoModal} from "../modules/nodeDeployment/HypotheticalDeploymentInfoModal";
import {SubmissionErrorModalProvider} from "./SubmissionErrorModalContext";

/**
 * Definition of the hypothetical info modals show data
 */
interface HypotheticalDeploymentInfoModalContextData {
    /**
     * What node id was clicked
     */
    readonly node: number;

    /**
     * Whether the node is hypothetical or not
     */
    readonly isHypothetical: boolean;
}

/**
 * Export the context to all components to use it
 */
export const HypotheticalDeploymentInfoModalContext = createContext<Partial<ModalInfo<HypotheticalDeploymentInfoModalContextData>>>({});

/**
 * Provider for the context, this needs to be wrapped around any component wishing to show this modal
 *
 * @param props
 * @constructor
 */
export const HypotheticalDeploymentInfoModalProvider: FunctionComponent = (props) =>
    <HypotheticalDeploymentInfoModalContext.Provider value={useModal<HypotheticalDeploymentInfoModalContextData>()}>
        <SubmissionErrorModalProvider>
            <HypotheticalDeploymentInfoModal/>
        </SubmissionErrorModalProvider>
        {props.children}
    </HypotheticalDeploymentInfoModalContext.Provider>;