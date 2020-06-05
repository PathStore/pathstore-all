import React from 'react';
import ReactDOM from 'react-dom';
import './stylesheets/bootstrap-dracula.scss'
import './stylesheets/node_colours.scss'
import {APIContextProvider} from "./hookVersion/src/contexts/APIContext";
import {PathStoreControlPanel} from "./hookVersion/src/PathStoreControlPanel";

ReactDOM.render(
  <React.StrictMode>
      <APIContextProvider>
          <PathStoreControlPanel/>
      </APIContextProvider>
  </React.StrictMode>,
  document.getElementById('root')
);
