import React, {FunctionComponent} from "react";
import {DisplayAvailableApplications} from "./DisplayAvailableApplications";
import {ApplicationCreation} from "./ApplicationCreation";
import {LoadingModalProvider} from "../../contexts/LoadingModalContext";
import {ErrorModalProvider} from "../../contexts/ErrorModalContext";
import {SubmissionErrorModalProvider} from "../../contexts/SubmissionErrorModalContext";
import {LiveTransitionVisualModalProvider} from "../../contexts/LiveTransitionVisualModalContext";

/**
 * This is the parent component of {@link ApplicationCreation} and {@link DisplayAvailableApplications}. This component
 * is used to represent the entire creation process of applications on the pathstore network.
 *
 * This component only requires the {@link APIContext} to run.
 * @constructor
 */
export const ApplicationInstallation: FunctionComponent = () =>
    <>
        <h2>Application Creation</h2>
        <LiveTransitionVisualModalProvider>
            <DisplayAvailableApplications/>
        </LiveTransitionVisualModalProvider>
        <LoadingModalProvider>
            <ErrorModalProvider>
                <SubmissionErrorModalProvider>
                    <ApplicationCreation/>
                </SubmissionErrorModalProvider>
            </ErrorModalProvider>
        </LoadingModalProvider>
    </>;