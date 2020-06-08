import React, {createContext, FunctionComponent} from "react";
import {ModalInfo, useModal} from "../hooks/useModal";
import {NodeInfoModal} from "../modules/infoModal/NodeInfoModal";
import {LoadingModalProvider} from "./LoadingModalContext";
import {ErrorModalProvider} from "./ErrorModalContext";
import {ApplicationStatus, AvailableLogDates, Deployment, Server} from "../utilities/ApiDeclarations";
import {APIContextType} from "../hooks/useAPIContext";

/**
 * This data is used to be passed into the node info modal on {@link ModalInfo#show}
 *
 * @see getNodeInfoModalData
 */
export interface NodeInfoModalContextData {
    /**
     * node number of the node that was clicked
     */
    readonly node: number;

    /**
     * deployment object set. By default this is the entire deployment object set from the api context
     */
    readonly deployment: Deployment[] | undefined;

    /**
     * server object set. By default this is the entire server object set from the api context
     */
    readonly servers: Server[] | undefined;

    /**
     * application status set. By default this is the entire application status set from the api context
     */
    readonly applicationStatus: ApplicationStatus[] | undefined;

    /**
     * available log dates set. By default this is the entire available log dates set from the api context
     */
    readonly availableLogDates: AvailableLogDates[] | undefined;

    /**
     * Force refresh function from the api context to allow children of the node info modal to use
     */
    readonly forceRefresh: (() => void) | undefined;
}

/**
 * Function to generate {@link NodeInfoModalContextData} to start the node info modal. All that is required is the node number and the api context
 *
 * @param apiContext api context to get data if the its not passed
 * @param node {@link NodeInfoModalContextData#node}
 * @param deployment {@link NodeInfoModalContextData#deployment}
 * @param servers {@link NodeInfoModalContextData#servers}
 * @param applicationStatus {@link NodeInfoModalContextData#applicationStatus}
 * @param availableLogDates {@link NodeInfoModalContextData#availableLogDates}
 */
export function getNodeInfoModalData(apiContext: Partial<APIContextType>, node: number, deployment?: Deployment[], servers?: Server[], applicationStatus?: ApplicationStatus[], availableLogDates?: AvailableLogDates[]): NodeInfoModalContextData {
    return {
        node: node,
        deployment: deployment ? deployment : apiContext.deployment,
        servers: servers ? servers : apiContext.servers,
        applicationStatus: applicationStatus ? applicationStatus : apiContext.applicationStatus,
        availableLogDates: availableLogDates ? availableLogDates : apiContext.availableLogDates,
        forceRefresh: apiContext.forceRefresh
    }
}

/**
 * Node info modal context. This is used by any component displaying the info modal or any child component of the node info modal
 */
export const NodeInfoModalContext = createContext<Partial<ModalInfo<NodeInfoModalContextData>>>({});

/**
 * Wraps the node info modal with the loading modal provider and the error modal provider as both of those modals
 * will need to be rendered. This provider should be wrapped around any component wishing to render a info modal
 *
 * @param props
 * @constructor
 * @see getNodeInfoModalData
 */
export const NodeInfoModalProvider: FunctionComponent = (props) =>
    <NodeInfoModalContext.Provider value={useModal<NodeInfoModalContextData>()}>
        <LoadingModalProvider>
            <ErrorModalProvider>
                <NodeInfoModal/>
            </ErrorModalProvider>
        </LoadingModalProvider>
        {props.children}
    </NodeInfoModalContext.Provider>;