import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {LoadingModal} from "../modules/LoadingModal";

/**
 * Loading modal context. This context gives access to displaying the loading modal
 */
export const LoadingModalContext = createContext<Partial<ModalInfo<undefined>>>({});

/**
 * Provider that is needed to wrap around any component planning on using the loading modal
 *
 * @param props
 * @constructor
 */
export const LoadingModalProvider: FunctionComponent = (props) =>
    <LoadingModalContext.Provider value={useModal<undefined>()}>
        <LoadingModal/>
        {props.children}
    </LoadingModalContext.Provider>;