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