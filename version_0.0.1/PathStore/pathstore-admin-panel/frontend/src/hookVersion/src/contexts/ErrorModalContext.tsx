import React, {createContext, FunctionComponent} from "react";
import {Error} from "../../../utilities/ApiDeclarations";
import {ModalInfo, useModal} from "../hooks/useModal";
import {ErrorResponseModal} from "../modules/ErrorResponseModal";

/**
 * Error Modal context. This is used by any component who needs to render a error modal
 */
export const ErrorModalContext = createContext<Partial<ModalInfo<Error[]>>>({});

/**
 * Provider for the api context. This should only be wrapped around components who wish to display an error modal
 * at some point in time
 *
 * @param props
 * @constructor
 */
export const ErrorModalProvider: FunctionComponent = (props) =>
    <ErrorModalContext.Provider value={useModal<Error[]>()}>
        <ErrorResponseModal/>
        {props.children}
    </ErrorModalContext.Provider>;