import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {HypotheticalInfoModal} from "../modules/nodeDeployment/HypotheticalInfoModal";
import {SubmissionErrorModalProvider} from "./SubmissionErrorModalContext";

/**
 * Definition of the hypothetical info modals show data
 */
interface HypotheticalInfoModalContextData {
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
export const HypotheticalInfoModalContext = createContext<Partial<ModalInfo<HypotheticalInfoModalContextData>>>({});

/**
 * Provider for the context, this needs to be wrapped around any component wishing to show this modal
 *
 * @param props
 * @constructor
 */
export const HypotheticalInfoModalProvider: FunctionComponent = (props) =>
    <HypotheticalInfoModalContext.Provider value={useModal<HypotheticalInfoModalContextData>()}>
        <SubmissionErrorModalProvider>
            <HypotheticalInfoModal/>
        </SubmissionErrorModalProvider>
        {props.children}
    </HypotheticalInfoModalContext.Provider>;