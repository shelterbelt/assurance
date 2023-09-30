import { dialog } from "electron";

export default {
    "selectPath": (options) => {return showPathSelectorDialog(options);},
}

function showPathSelectorDialog(options): Promise<Electron.OpenDialogReturnValue> {
    return dialog.showOpenDialog(options);
}
