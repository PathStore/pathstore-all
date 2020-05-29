import {Deployment, Server} from "../../utilities/ApiDeclarations";
import React, {FunctionComponent} from "react";
import {Button} from "react-bootstrap";
import Modal from "react-bootstrap/Modal";
import {ServerInfo} from "./servers/ServerInfo";

/**
 * Properties definition for {@link HypotheticalInfoModal}
 */
interface HypotheticalInfoModalProperties {
    /**
     * Whether to show the modal or not
     */
    readonly show: boolean

    /**
     * If the node clicked is hypothetical
     */
    readonly hypothetical: boolean

    /**
     * Node id of the clicked node
     */
    readonly node: number

    /**
     * List of server objects from api
     */
    readonly servers: Server[]

    /**
     * List of deployment object from {@link NodeDeploymentModal}
     */
    readonly deployment: Deployment[]

    /**
     * Callback function to delete node if user requests it
     */
    readonly deleteNode: (event: any) => void

    /**
     * Callback function to close modal
     */
    readonly callback: () => void
}

/**
 * TODO: Allow removal of non-hypothetical nodes
 *
 * This function displays the server information for a a clicked node. It also allows the user to delete a node
 * iff they're a hypothetical node
 *
 * @param props
 * @constructor
 */
export const HypotheticalInfoModal: FunctionComponent<HypotheticalInfoModalProperties> = (props) =>
    <Modal show={props.show}
           size={"lg"}
           centered
    >
        <Modal.Header>
            <Modal.Title>Info Modal for node {props.node}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
            <ServerInfo deployment={props.deployment} servers={props.servers} node={props.node}/>
        </Modal.Body>
        <Modal.Footer>
            {props.hypothetical ? <Button onClick={props.deleteNode}>Delete</Button> : null}
            <Button onClick={props.callback}>Close</Button>
        </Modal.Footer>
    </Modal>;