import React, {FunctionComponent} from "react";
import {Log} from "../utilities/ApiDeclarations";

/**
 * Properties definition for {@link LogViewer}
 */
interface LogViewerProps {
    /**
     * Node id of node to show info of
     */
    readonly node: number

    /**
     * Optional List of logs for all node from api
     */
    readonly logs?: Log[]
}

/**
 * This component is used to display a nodes log to the user in a scrollable text viewer
 *
 * @param props
 * @constructor
 */
export const LogViewer: FunctionComponent<LogViewerProps> = (props) => {

    if (props.logs === undefined) return null;

    const log = props.logs.filter(i => i.node_id === props.node);

    if (log === undefined || log[0] === undefined) return null;

    let response = "";

    for (let i = 0; i < log[0].log.length; i++)
        response += log[0].log[i].log + "\n";

    return (
        <div className={"logViewer"}>
            <code id={"log"}>{response}</code>
        </div>
    );
};