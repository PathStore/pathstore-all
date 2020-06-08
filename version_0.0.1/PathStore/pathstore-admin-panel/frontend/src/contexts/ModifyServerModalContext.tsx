import React, {createContext, FunctionComponent} from "react";
import {Server} from "../utilities/ApiDeclarations";
import {ModalInfo, useModal} from "../hooks/useModal";
import {ModifyServerModal} from "../modules/nodeDeployment/ModifyServerModal";
import {LoadingModalProvider} from "./LoadingModalContext";
import {ErrorModalProvider} from "./ErrorModalContext";

/**
 * This context is used to allow consumers ofr the modify server modal to display it
 */
export const ModifyServerModalContext = createContext<Partial<ModalInfo<Server>>>({});

/**
 * Provider to be wrapped around any component using this modal
 *
 * @param props
 * @constructor
 */
export const ModifyServerModalProvider: FunctionComponent = (props) =>
    <ModifyServerModalContext.Provider value={useModal<Server>()}>
        <LoadingModalProvider>
            <ErrorModalProvider>
                <ModifyServerModal/>
            </ErrorModalProvider>
        </LoadingModalProvider>
        {props.children}
    </ModifyServerModalContext.Provider>;