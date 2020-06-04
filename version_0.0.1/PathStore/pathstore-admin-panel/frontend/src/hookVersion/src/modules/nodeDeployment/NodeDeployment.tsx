import React, {FunctionComponent, useCallback, useContext} from "react";
import {Button} from "react-bootstrap";
import {NodeDeploymentModalContext} from "../../contexts/NodeDeploymentModalContext";

/**
 * This component is used to render the button to display the node deployment modal
 * @constructor
 * @see NodeDeploymentModal
 */
export const NodeDeployment: FunctionComponent = () => {
    const {show} = useContext(NodeDeploymentModalContext);

    // show the node deployment modal on click
    const onClick = useCallback(() => {
        if (show)
            show();
    }, [show]);

    return (
        <Button onClick={onClick}>Deploy Additional Nodes to the Network</Button>
    );
};