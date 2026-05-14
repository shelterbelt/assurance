// See the Electron documentation for details on how to use preload scripts:
// https://www.electronjs.org/docs/latest/tutorial/process-model#preload-scripts
import { contextBridge, ipcRenderer, IpcRendererEvent } from 'electron';

import type { MainMenuStateWire } from './main-menu/mainMenuState';

export interface EngineCallErrorWire {
    code: number;
    message: string;
    data?: unknown;
}

export type EngineCallEnvelope =
    | { ok: true; result: unknown }
    | { ok: false; error: EngineCallErrorWire };

export interface EngineNotificationEnvelope {
    method: string;
    params: unknown;
}

export type EngineNotificationListener = (notification: EngineNotificationEnvelope) => void;

contextBridge.exposeInMainWorld('assuranceapi', {
    selectPath: (options: unknown, callback: (result: unknown) => void) => {
        ipcRenderer.invoke('selectPath', options).then((result) => {
            callback(result);
        });
    },
    /**
     * Invokes a JSON-RPC method on the embedded Java engine. Returns a
     * discriminated-union envelope so the renderer-side wrapper can rebuild a
     * typed Error without losing the JSON-RPC `code`/`data` fields.
     */
    engineCall: (method: string, params: unknown): Promise<EngineCallEnvelope> => {
        return ipcRenderer.invoke('engineCall', method, params);
    },
    /**
     * Subscribes to server-initiated JSON-RPC notifications the main process
     * has elected to forward. The listener is invoked once per notification
     * with `{ method, params }`. Returns an unsubscribe function. Multiple
     * subscribers are permitted.
     */
    onEngineNotification: (listener: EngineNotificationListener): (() => void) => {
        const wrapped = (_event: IpcRendererEvent, envelope: EngineNotificationEnvelope) => listener(envelope);
        ipcRenderer.on('engineNotification', wrapped);
        return () => ipcRenderer.removeListener('engineNotification', wrapped);
    },
    /**
     * Subscribes to application-level main menu commands. The Electron main
     * process sends one string command per menu item click.
     */
    onMainMenuCommand: (listener: (command: string) => void): (() => void) => {
        const wrapped = (_event: IpcRendererEvent, command: string) => listener(command);
        ipcRenderer.on('mainMenuCommand', wrapped);
        return () => ipcRenderer.removeListener('mainMenuCommand', wrapped);
    },
    /**
     * Sends the current UI enablement state to the Electron main process so
     * menu items can be enabled/disabled consistently with the focused
     * targets (selected scan definition / selected result row).
     */
    setMainMenuState: (state: MainMenuStateWire): void => {
        ipcRenderer.send('mainMenuState', state);
    },
});
