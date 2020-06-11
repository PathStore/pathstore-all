import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {NodeInfoModal} from "../modules/infoModal/NodeInfoModal";
import {LoadingModalProvider} from "./LoadingModalContext";
import {ErrorModalProvider} from "./ErrorModalContext";

/**
 * Node info modal context. This is used by any component displaying the info modal or any child component of the node info modal
 */
export const NodeInfoModalContext = createContext<Partial<ModalInfo<number>>>({});

/**
 * Wraps the node info modal with the loading modal provider and the error modal provider as both of those modals
 * will need to be rendered. This provider should be wrapped around any component wishing to render a info modal
 *
 * @param props
 * @constructor
 * @see getNodeInfoModalData
 */
export const NodeInfoModalProvider: FunctionComponent = (props) =>
    <NodeInfoModalContext.Provider value={useModal<number>()}>
        <LoadingModalProvider>
            <ErrorModalProvider>
                <NodeInfoModal/>
            </ErrorModalProvider>
        </LoadingModalProvider>
        {props.children}
    </NodeInfoModalContext.Provider>;