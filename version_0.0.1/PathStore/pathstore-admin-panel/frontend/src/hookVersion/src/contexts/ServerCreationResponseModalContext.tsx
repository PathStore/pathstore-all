import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {Server} from "../utilities/ApiDeclarations";
import {ServerCreationResponseModal} from "../modules/nodeDeployment/ServerCreationResponseModal";

/**
 * This context is used for {@link ServerCreationResponseModal}
 */
export const ServerCreationResponseModalContext = createContext<Partial<ModalInfo<Server>>>({});

/**
 * This provider should be wrapped around any component wishing to use this modal
 *
 * @param props
 * @constructor
 */
export const ServerCreationResponseModalProvider: FunctionComponent = (props) =>
    <ServerCreationResponseModalContext.Provider value={useModal<Server>()}>
        <ServerCreationResponseModal/>
        {props.children}
    </ServerCreationResponseModalContext.Provider>;