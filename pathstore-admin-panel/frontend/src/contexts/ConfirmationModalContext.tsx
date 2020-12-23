import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {ConfirmationModal} from "../modules/confirmation/ConfirmationModal";

/**
 * What data is needed for the modal
 */
export interface ConfirmationModalData {
    readonly onClick: () => void;
    readonly message: string;
}

// context declaration
export const ConfirmationModalContext = createContext<Partial<ModalInfo<ConfirmationModalData>>>({});

/**
 * Only used in {@link ConfirmationButton}
 */
export const ConfirmationModalProvider: FunctionComponent = (props) =>
    <ConfirmationModalContext.Provider value={useModal<ConfirmationModalData>()}>
        <ConfirmationModal/>
        {props.children}
    </ConfirmationModalContext.Provider>;