import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {NodeDeploymentModal} from "../modules/nodeDeployment/NodeDeploymentModal";
import {NodeDeploymentModalContextData, useNodeDeploymentModalData} from "../hooks/useNodeDeploymentModalData";
import {HypotheticalInfoModalProvider} from "./HypotheticalInfoModalContext";
import {LoadingModalProvider} from "./LoadingModalContext";
import {ErrorModalProvider} from "./ErrorModalContext";
import {SubmissionErrorModalProvider} from "./SubmissionErrorModalContext";

/**
 * This context is used to store additional data not known at creation of the modal. This context
 * is used throughout all the components associated with the node deployment modal
 */
export const NodeDeploymentModalData = createContext<Partial<NodeDeploymentModalContextData>>({});

/**
 * Provider for the above context. This is private as it is only used around the node deployment modal provider
 * context defined below.
 * @param props
 * @constructor
 */
const NodeDeploymentModalDataProvider: FunctionComponent = (props) =>
    <NodeDeploymentModalData.Provider value={useNodeDeploymentModalData()}>
        {props.children}
    </NodeDeploymentModalData.Provider>;

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
                        <HypotheticalInfoModalProvider>
                            <NodeDeploymentModal/>
                        </HypotheticalInfoModalProvider>
                    </NodeDeploymentModalDataProvider>
                </SubmissionErrorModalProvider>
            </ErrorModalProvider>
        </LoadingModalProvider>
        {props.children}
    </NodeDeploymentModalContext.Provider>;