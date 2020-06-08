import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {NodeDeploymentModal} from "../modules/nodeDeployment/NodeDeploymentModal";
import {NodeDeploymentModalData, useNodeDeploymentModalData} from "../hooks/useNodeDeploymentModalData";
import {LoadingModalProvider} from "./LoadingModalContext";
import {ErrorModalProvider} from "./ErrorModalContext";
import {SubmissionErrorModalProvider} from "./SubmissionErrorModalContext";

/**
 * This context is used to store additional data not known at creation of the modal. This context
 * is used throughout all the components associated with the node deployment modal
 */
export const NodeDeploymentModalDataContext = createContext<Partial<NodeDeploymentModalData>>({});

/**
 * Provider for the above context. This is private as it is only used around the node deployment modal provider
 * context defined below.
 * @param props
 * @constructor
 */
const NodeDeploymentModalDataProvider: FunctionComponent = (props) =>
    <NodeDeploymentModalDataContext.Provider value={useNodeDeploymentModalData()}>
        {props.children}
    </NodeDeploymentModalDataContext.Provider>;

/**
 * Context used for the node deployment modal information.
 */
export const NodeDeploymentModalContext = createContext<Partial<ModalInfo<undefined>>>({});

/**
 * Provider for the above context, it uses loading, error, and hypothetical modal providers. It also uses the above
 * data context provider
 * @param props
 * @constructor
 */
export const NodeDeploymentModalProvider: FunctionComponent = (props) =>
    <NodeDeploymentModalContext.Provider value={useModal<undefined>()}>
        <LoadingModalProvider>
            <ErrorModalProvider>
                <SubmissionErrorModalProvider>
                    <NodeDeploymentModalDataProvider>
                            <NodeDeploymentModal/>
                    </NodeDeploymentModalDataProvider>
                </SubmissionErrorModalProvider>
            </ErrorModalProvider>
        </LoadingModalProvider>
        {props.children}
    </NodeDeploymentModalContext.Provider>;