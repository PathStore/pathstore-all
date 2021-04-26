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
import {Error} from "../utilities/ApiDeclarations";
import {ErrorResponseModal} from "../modules/ErrorResponseModal";

/**
 * Error Modal context. This is used by any component who needs to render a error modal
 */
export const ErrorModalContext = createContext<Partial<ModalInfo<Error[]>>>({});

/**
 * Provider for the api context. This should only be wrapped around components who wish to display an error modal
 * at some point in time
 *
 * @param props
 * @constructor
 */
export const ErrorModalProvider: FunctionComponent = (props) =>
    <ErrorModalContext.Provider value={useModal<Error[]>()}>
        <ErrorResponseModal/>
        {props.children}
    </ErrorModalContext.Provider>;