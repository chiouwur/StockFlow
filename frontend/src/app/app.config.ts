import { APP_INITIALIZER, ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import {
  MSAL_GUARD_CONFIG, MSAL_INSTANCE, MSAL_INTERCEPTOR_CONFIG,
  MsalBroadcastService, MsalGuard, MsalInterceptor, MsalService
} from '@azure/msal-angular';
import { InteractionType, PublicClientApplication } from '@azure/msal-browser';
import { routes } from './app.routes';
import { environment } from '../environments/environment';
import { loginRequest, msalConfig } from './core/msal.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(withInterceptorsFromDi()),
    { provide: HTTP_INTERCEPTORS, useClass: MsalInterceptor, multi: true },
    { provide: MSAL_INSTANCE, useFactory: () => new PublicClientApplication(msalConfig) },
    { provide: MSAL_GUARD_CONFIG, useValue: { interactionType: InteractionType.Popup, authRequest: loginRequest } },
    {
      provide: MSAL_INTERCEPTOR_CONFIG,
      useValue: {
        interactionType: InteractionType.Popup,
        // Solo las llamadas a tu API Gateway llevan el Bearer token
        protectedResourceMap: new Map([[environment.apiBase, [environment.msal.apiScope]]])
      }
    },
    MsalService, MsalGuard, MsalBroadcastService,
    {
      provide: APP_INITIALIZER, multi: true, deps: [MsalService],
      useFactory: (msal: MsalService) => async () => {
        await firstValueFrom(msal.initialize());
        const i = msal.instance;
        if (!i.getActiveAccount() && i.getAllAccounts().length) i.setActiveAccount(i.getAllAccounts()[0]);
      }
    }
  ]
};
