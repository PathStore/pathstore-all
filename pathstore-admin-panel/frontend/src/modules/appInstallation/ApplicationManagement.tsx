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

import React, {FunctionComponent} from "react";
import {DisplayAvailableApplications} from "./DisplayAvailableApplications";
import {ApplicationCreation} from "./ApplicationCreation";
import {LoadingModalProvider} from "../../contexts/LoadingModalContext";
import {ErrorModalProvider} from "../../contexts/ErrorModalContext";
import {SubmissionErrorModalProvider} from "../../contexts/SubmissionErrorModalContext";
import {ApplicationDeploymentModalProvider} from "../../contexts/ApplicationDeploymentModalContext";

/**
 * This is the parent component of {@link ApplicationCreation} and {@link DisplayAvailableApplications}. This component
 * is used to represent the entire creation process of applications on the pathstore network.
 *
 * This component only requires the {@link APIContext} to run.
 * @constructor
 */
export const ApplicationManagement: FunctionComponent = () =>
    <>
        <h2>Application Management</h2>
        <ApplicationDeploymentModalProvider>
            <DisplayAvailableApplications/>
        </ApplicationDeploymentModalProvider>
        <LoadingModalProvider>
            <ErrorModalProvider>
                <SubmissionErrorModalProvider>
                    <ApplicationCreation/>
                </SubmissionErrorModalProvider>
            </ErrorModalProvider>
        </LoadingModalProvider>
    </>;