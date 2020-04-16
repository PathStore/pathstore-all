import React, {Component} from "react";
import DeployApplication from "./DeployApplication";

/**
 * Parent model to load a submodel
 *
 * @see DeployApplication
 */
export default class ApplicationInstallation extends Component {

    /**
     * State:
     *
     * topology: api topology data
     * application: api application data
     * childRefresh: on change the children will refresh
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {
            topology: props.topology,
            applications: props.application,
            childRefresh: false
        };
    }

    /**
     * On props change refresh all data in state and reload children
     *
     * @param props
     * @param nextContext
     */
    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh)
            this.setState({
                applications: props.applications,
                topology: props.topology,
                childRefresh: !this.state.childRefresh
            });

    }


    /**
     * Render header and submodels
     *
     * @returns {*}
     */
    render() {
        return (
            <div>
                <h2>Application Deployment</h2>
                <DeployApplication topology={this.state.topology} applications={this.state.applications}
                                   refresh={this.state.childRefresh}/>
            </div>
        )
    }

}