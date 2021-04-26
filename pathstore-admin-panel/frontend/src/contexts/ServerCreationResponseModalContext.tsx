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
import {Server} from "../utilities/ApiDeclarations";
import {ServerCreationResponseModal} from "../modules/nodeDeployment/ServerCreationResponseModal";

/**
 * This context is used for {@link ServerCreationResponseModal}
 */
export const ServerCreationResponseModalContext = createContext<Partial<ModalInfo<Server>>>({});

/**
 * This provider should be wrapped around any component wishing to use this modal
 *
 * @param props
 * @constructor
 */
export const ServerCreationResponseModalProvider: FunctionComponent = (props) =>
    <ServerCreationResponseModalContext.Provider value={useModal<Server>()}>
        <ServerCreationResponseModal/>
        {props.children}
    </ServerCreationResponseModalContext.Provider>;