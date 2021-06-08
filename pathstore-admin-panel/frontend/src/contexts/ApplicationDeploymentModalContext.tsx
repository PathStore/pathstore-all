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
import {Application} from "../utilities/ApiDeclarations";
import {ApplicationDeploymentModal} from "../modules/appDeployment/ApplicationDeploymentModal";
import {
    ApplicationDeploymentModalData,
    useApplicationDeploymentModalData
} from "../hooks/useApplicationDeploymentModalData";
import {SubmissionErrorModalProvider} from "./SubmissionErrorModalContext";
import {LoadingModalProvider} from "./LoadingModalContext";
import {ErrorModalProvider} from "./ErrorModalContext";

// Data context for ApplicationDeployment Modal and children
export const ApplicationDeploymentModalDataContext = createContext<Partial<ApplicationDeploymentModalData>>({});

// Application deployment modal context to be used by the modal
export const ApplicationDeploymentModalContext = createContext<Partial<ModalInfo<Application>>>({});

/**
 * Wrap this around any component wishing to use the application deployment modal only the api context is needed
 *
 * @param props
 * @constructor
 */
export const ApplicationDeploymentModalProvider: FunctionComponent = (props) => {
    const data = useModal<Application>();

    return (
        <ApplicationDeploymentModalContext.Provider value={data}>
            <ErrorModalProvider>
                <LoadingModalProvider>
                    <SubmissionErrorModalProvider>
                        <ApplicationDeploymentModalDataContext.Provider
                            value={useApplicationDeploymentModalData(data.data)}>
                            <ApplicationDeploymentModal/>
                        </ApplicationDeploymentModalDataContext.Provider>
                    </SubmissionErrorModalProvider>
                </LoadingModalProvider>
            </ErrorModalProvider>
            {props.children}
        </ApplicationDeploymentModalContext.Provider>
    );
};