import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {Application} from "../utilities/ApiDeclarations";
import {LiveTransitionVisualModal} from "../modules/appDeployment/LiveTransitionVisualModal";

// LTVM context
export const LiveTransitionVisualModalContext = createContext<Partial<ModalInfo<Application>>>({});

/**
 * Wrap this around any component wishing to use the LVTM only the api context is needed
 *
 * @param props
 * @constructor
 */
export const LiveTransitionVisualModalProvider: FunctionComponent = (props) =>
    <LiveTransitionVisualModalContext.Provider value={useModal<Application>()}>
        <LiveTransitionVisualModal/>
        {props.children}
    </LiveTransitionVisualModalContext.Provider>;