import React, {Component} from "react";
import Tree from "react-tree-graph";

/**
 * This class is used to render custom trees to display the pathstore topology in many different areas of the website
 *
 * Props:
 *
 * topology: topology to render (this does not have to be the entire network and could be a subset)
 * get_colour: function that is used to calculate what css class to give each node
 * get_click: function that is used to get the onclick function for each node
 */
export default class PathStoreTopology extends Component {
    render() {
        return (
            <Tree data={createTree(this.props.topology, -1, this.props.get_colour, this.props.get_click)}
                  nodeRadius={15}
                  margins={{top: 20, bottom: 10, left: 20, right: 200}}
                  height={1000}
                  width={1080}
            />
        );
    }
};

/**
 * Creates interpretable json object for the Tree package
 *
 * @param array from parent
 * @param parentId -1 for initial call to look for the root node and work way down
 * @param get_colour
 * @param get_click
 * @returns {{}}
 */
function createTree(array, parentId, get_colour, get_click) {

    if (array.length === 0) return {};

    let children = [];

    for (let i = 0; i < array.length; i++)
        if (parentId === -1) {
            if (array[i].parent_node_id === parentId) return createTreeObject(array[i], array, get_colour, get_click);
        } else {
            if (array[i].parent_node_id === parentId) children.push(createTreeObject(array[i], array, get_colour, get_click));
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
function createTreeObject(object, array, get_colour, get_click) {
    return {
        name: object.new_node_id,
        textProps: {x: -20, y: 25},
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