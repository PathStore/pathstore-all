import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {HypotheticalDeploymentInfoModal} from "../modules/nodeDeployment/HypotheticalDeploymentInfoModal";
import {SubmissionErrorModalProvider} from "./SubmissionErrorModalContext";

/**
 * Definition of the hypothetical info modals show data
 */
interface HypotheticalDeploymentInfoModalContextData {
    /**
     * What node id was clicked
     */
    readonly node: number;

    /**
     * Whether the node is hypothetical or not
     */
    readonly isHypothetical: boolean;
}

/**
 * Export the context to all components to use it
 */
export const HypotheticalDeploymentInfoModalContext = createContext<Partial<ModalInfo<HypotheticalDeploymentInfoModalContextData>>>({});

/**
 * Provider for the context, this needs to be wrapped around any component wishing to show this modal
 *
 * @param props
 * @constructor
 */
export const HypotheticalDeploymentInfoModalProvider: FunctionComponent = (props) =>
    <HypotheticalDeploymentInfoModalContext.Provider value={useModal<HypotheticalDeploymentInfoModalContextData>()}>
        <SubmissionErrorModalProvider>
            <HypotheticalDeploymentInfoModal/>
        </SubmissionErrorModalProvider>
        {props.children}
    </HypotheticalDeploymentInfoModalContext.Provider>;