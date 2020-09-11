import React from 'react';
import ReactDOM from 'react-dom';
import './stylesheets/bootstrap-dracula.scss'
import './stylesheets/node_colours.scss'
import {APIContextProvider} from "./contexts/APIContext";
import {PathStoreControlPanel} from "./PathStoreControlPanel";

ReactDOM.render(
  <React.StrictMode>
      <APIContextProvider>
          <PathStoreControlPanel/>
      </APIContextProvider>
  </React.StrictMode>,
  document.getElementById('root')
);
