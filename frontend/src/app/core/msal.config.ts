import { Configuration } from '@azure/msal-browser';
import { environment } from '../../environments/environment';

export const msalConfig: Configuration = {
  auth: {
    clientId: environment.msal.clientId,
    authority: environment.msal.authority,
    redirectUri: window.location.origin,
  },
  cache: {
    cacheLocation: 'sessionStorage',
  },
};

export const loginRequest = {
  scopes: [environment.msal.apiScope],
};