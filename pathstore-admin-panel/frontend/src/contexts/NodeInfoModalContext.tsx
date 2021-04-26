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
import {NodeInfoModal} from "../modules/infoModal/NodeInfoModal";
import {LoadingModalProvider} from "./LoadingModalContext";
import {ErrorModalProvider} from "./ErrorModalContext";

/**
 * Node info modal context. This is used by any component displaying the info modal or any child component of the node info modal
 */
export const NodeInfoModalContext = createContext<Partial<ModalInfo<number>>>({});

/**
 * Wraps the node info modal with the loading modal provider and the error modal provider as both of those modals
 * will need to be rendered. This provider should be wrapped around any component wishing to render a info modal
 *
 * @param props
 * @constructor
 * @see getNodeInfoModalData
 */
export const NodeInfoModalProvider: FunctionComponent = (props) =>
    <NodeInfoModalContext.Provider value={useModal<number>()}>
        <LoadingModalProvider>
            <ErrorModalProvider>
                <NodeInfoModal/>
            </ErrorModalProvider>
        </LoadingModalProvider>
        {props.children}
    </NodeInfoModalContext.Provider>;