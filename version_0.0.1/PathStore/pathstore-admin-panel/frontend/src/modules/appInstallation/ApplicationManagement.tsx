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