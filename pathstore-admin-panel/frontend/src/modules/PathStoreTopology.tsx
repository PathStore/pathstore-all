import {Deployment} from "../utilities/ApiDeclarations";
import Tree from "react-tree-graph";
import React, {FunctionComponent} from "react";

/**
 * Properties definition for {@link PathStoreTopology} components
 */
interface PathStoreTopologyProps {
    /**
     * Width of the tree
     */
    readonly width: number

    /**
     * Height of the tree
     */
    readonly height: number

    /**
     * List of deployment objects from api
     */
    readonly deployment: Deployment[]

    /**
     * Function to get a css class for a specific node
     *
     * @param object deployment object
     */
    readonly get_colour: (object: Deployment) => string

    /**
     * Function to call when a click has occured
     *
     * @param event event
     * @param node node id
     */
    readonly get_click?: (event: any, node: number) => void
}

/**
 * This component is used to display a network topology for pathstore.
 *
 * @see {@link PathStoreTopologyProps}
 */
export const PathStoreTopology: FunctionComponent<PathStoreTopologyProps> = (props) =>
    <Tree data={createTree(props.deployment, -1, props.get_colour, props.get_click)}
          nodeRadius={15}
          margins={{top: 20, bottom: 10, left: 20, right: 200}}
          height={props.height}
          width={props.width}
    />;

/**
 * Creates interpretable json object for the Tree package
 *
 * @param array from parent
 * @param parentId -1 for initial call to look for the root node and work way down
 * @param get_colour
 * @param get_click
 * @returns {{}}
 */
function createTree(
    array: Deployment[],
    parentId: number,
    get_colour: (object: Deployment) => string,
    get_click?: (event: any, node: number) => void): {} | {}[] {

    if (array.length === 0) return {};

    let children = [];

    for (let d of array)
        if (parentId === -1) {
            if (d.parent_node_id === parentId) return createTreeObject(d, array, get_colour, get_click);
        } else {
            if (d.parent_node_id === parentId) children.push(createTreeObject(d, array, get_colour, get_click));
        }


    return children;
}

/**
 * Name is the node id, textProps is the location of the text associated with the node, children is a list of children
 *
 * @param object
 * @param array
 * @param get_colour
 * @param get_click
 * @returns {{textProps: {x: number, y: number}, children: ({textProps: {x: number, y: number}, children: *[], name: *}|*[]), name: *}}
 */
function createTreeObject(
    object: Deployment,
    array: Deployment[],
    get_colour: (object: Deployment) => string,
    get_click?: (event: any, node: number) => void): {} {

    return {
        name: object.new_node_id,
        textProps: {x: -20, y: 25},
        pathProps: {className: 'path'},
        gProps:
            get_click !== null ?
                {
                    className: get_colour(object),
                    onClick: get_click
                } :
                {
                    className: get_colour(object)
                },
        children: createTree(array, object.new_node_id, get_colour, get_click)
    }
}