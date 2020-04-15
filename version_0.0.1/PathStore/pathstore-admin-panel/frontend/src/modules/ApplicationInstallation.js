import React, {Component} from "react";
import DeployApplication from "./DeployApplication";

export default class ApplicationInstallation extends Component {

    constructor(props) {
        super(props);
        this.state = {
            topology: props.topology,
            applications: props.application,
            childRefresh: false
        };
    }

    componentWillReceiveProps(props, nextContext) {
        if (this.props.refresh !== props.refresh)
            this.setState({
                applications: props.applications,
                topology: props.topology,
                childRefresh: !this.state.childRefresh
            });

    }


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