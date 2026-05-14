import { dialog } from "electron";

export default {
    "selectPath": (options: Electron.OpenDialogOptions) => {return showPathSelectorDialog(options);},
}

function showPathSelectorDialog(options: Electron.OpenDialogOptions): Promise<Electron.OpenDialogReturnValue> {
    return dialog.showOpenDialog(options);
}
