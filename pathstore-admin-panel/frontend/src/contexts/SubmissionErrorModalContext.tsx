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
import {SubmissionErrorModal} from "../modules/SubmissionErrorModal";

/**
 * This context is used to work with the submission error modal
 */
export const SubmissionErrorModalContext = createContext<Partial<ModalInfo<string>>>({});

/**
 * Wrap this provider around a component that needs to display a submission error
 * @param props
 * @constructor
 */
export const SubmissionErrorModalProvider: FunctionComponent = (props) =>
    <SubmissionErrorModalContext.Provider value={useModal<string>()}>
        <SubmissionErrorModal/>
        {props.children}
    </SubmissionErrorModalContext.Provider>;