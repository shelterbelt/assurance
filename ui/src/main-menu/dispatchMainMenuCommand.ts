export type MainMenuCommand =
    | 'newScanDefiniton'
    | 'deleteScanDefiniton'
    | 'scan'
    | 'scanAndMerge'
    | 'replaceSource'
    | 'replaceTarget'
    | 'fileAttributes'
    | 'viewScan'
    | 'viewHistory'
    | 'displaySettings';

export type MainMenuTabKey = 'ScanContent' | 'HistoryContent';

export interface DispatchMainMenuCommandContext {
    selectedResultId: number | null;
    canSwitchTabs: boolean;
    setActiveTabKey: (key: MainMenuTabKey) => void;
    activateDefinitionPanel: () => void;
    setSettingsPanelOpen: (open: boolean) => void;
}

function clickButtonByExactText(root: ParentNode, className: string, text: string): boolean {
    const buttons = Array.from(root.querySelectorAll(`button.${className}`));
    const match = buttons.find((b) => (b.textContent ?? '').trim() === text);
    if (!match || match instanceof HTMLButtonElement && match.disabled) {
        return false;
    }
    (match as HTMLButtonElement).click();
    return true;
}

function clickSelectedResultAction(selectedResultId: number, selectorWithinResult: string): boolean {
    const row = document.querySelector<HTMLElement>(`.scan-result-item[data-result-id="${selectedResultId}"]`);
    if (!row) return false;
    const target = row.querySelector<HTMLElement>(selectorWithinResult);
    if (!target) return false;
    // Avoid dispatching through disabled buttons.
    if (target instanceof HTMLButtonElement && target.disabled) return false;
    target.click();
    return true;
}

function ensureSelectedResultExpanded(selectedResultId: number): boolean {
    const row = document.querySelector<HTMLElement>(`.scan-result-item[data-result-id="${selectedResultId}"]`);
    if (!row) return false;

    // Expanded state is represented by `expanded` CSS class on the source/target details blocks.
    const alreadyExpanded = row.querySelector('.source-result-item.expanded, .target-result-item.expanded') !== null;
    if (alreadyExpanded) return true;

    const expandButtonRow = row.querySelector<HTMLElement>('.expand-button-row');
    if (!expandButtonRow) return false;
    expandButtonRow.click();
    return true;
}

export function dispatchMainMenuCommand(
    command: string,
    ctx: DispatchMainMenuCommandContext,
): void {
    switch (command as MainMenuCommand) {
        case 'viewScan': {
            if (!ctx.canSwitchTabs) return;
            ctx.setActiveTabKey('ScanContent');
            return;
        }
        case 'viewHistory': {
            if (!ctx.canSwitchTabs) return;
            ctx.setActiveTabKey('HistoryContent');
            return;
        }
        case 'newScanDefiniton': {
            if (!ctx.canSwitchTabs) return;
            ctx.setActiveTabKey('ScanContent');
            ctx.activateDefinitionPanel();
            return;
        }
        case 'deleteScanDefiniton': {
            if (!ctx.canSwitchTabs) return;
            ctx.setActiveTabKey('ScanContent');
            // Delete lives inside the ScanList footer; it is disabled when
            // no scan definition is selected.
            const scanList = document.querySelector<HTMLElement>('.scan-list-container');
            if (!scanList) return;
            clickButtonByExactText(scanList, 'scan-manage-button', 'Delete');
            return;
        }
        case 'scan': {
            if (!ctx.canSwitchTabs) return;
            ctx.setActiveTabKey('ScanContent');
            const actionsPanel = document.querySelector<HTMLElement>('.actions-panel-container');
            if (!actionsPanel) return;
            clickButtonByExactText(actionsPanel, 'action-button', 'Scan');
            return;
        }
        case 'scanAndMerge': {
            if (!ctx.canSwitchTabs) return;
            ctx.setActiveTabKey('ScanContent');
            const actionsPanel = document.querySelector<HTMLElement>('.actions-panel-container');
            if (!actionsPanel) return;
            clickButtonByExactText(actionsPanel, 'action-button', 'Scan and Merge');
            return;
        }
        case 'displaySettings': {
            ctx.setSettingsPanelOpen(true);
            return;
        }
        case 'replaceSource': {
            // Swing semantics:
            // - replaceSourceAction => mergeTargetToSource => engine strategy TARGET.
            // React mapping:
            // - mergeTarget button triggers merge.start(resultId, 'target').
            if (ctx.selectedResultId === null) return;
            clickSelectedResultAction(ctx.selectedResultId, '.result-merge-button.merge-target');
            return;
        }
        case 'replaceTarget': {
            // Swing replaceTargetAction => mergeSourceToTarget => engine strategy SOURCE.
            // React mapping:
            // - mergeSource button triggers merge.start(resultId, 'source').
            if (ctx.selectedResultId === null) return;
            clickSelectedResultAction(ctx.selectedResultId, '.result-merge-button.merge-source');
            return;
        }
        case 'fileAttributes': {
            if (ctx.selectedResultId === null) return;
            ensureSelectedResultExpanded(ctx.selectedResultId);
            return;
        }
        default:
            return;
    }
}

