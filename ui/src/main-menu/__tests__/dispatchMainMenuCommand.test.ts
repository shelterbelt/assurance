import { dispatchMainMenuCommand, type DispatchMainMenuCommandContext } from '../dispatchMainMenuCommand';

function makeCtx(partial: Partial<DispatchMainMenuCommandContext> = {}): DispatchMainMenuCommandContext {
    return {
        selectedResultId: null,
        canSwitchTabs: true,
        setActiveTabKey: jest.fn(),
        activateDefinitionPanel: jest.fn(),
        setSettingsPanelOpen: jest.fn(),
        ...partial,
    };
}

function setSelectedResultDom(resultId: number) {
    document.body.innerHTML = `
      <div class="scan-result-item" data-result-id="${resultId}">
        <div class="source-result-item"></div>
        <div class="target-result-item"></div>
        <button class="result-merge-button merge-target">Replace</button>
        <button class="result-merge-button merge-source">Replace</button>
        <div class="expand-button-row"></div>
      </div>
      <div class="actions-panel-container">
        <button class="action-button">Scan</button>
        <button class="action-button">Scan and Merge</button>
      </div>
      <div class="scan-list-container">
        <button class="scan-manage-button">New</button>
        <button class="scan-manage-button">Delete</button>
      </div>
    `;
}

test('maps viewScan / viewHistory to TabContainer key updates', () => {
    const ctx = makeCtx({ canSwitchTabs: true });
    dispatchMainMenuCommand('viewScan', ctx);
    expect(ctx.setActiveTabKey).toHaveBeenCalledWith('ScanContent');

    dispatchMainMenuCommand('viewHistory', ctx);
    expect(ctx.setActiveTabKey).toHaveBeenCalledWith('HistoryContent');
});

test('does not switch tabs when canSwitchTabs is false', () => {
    const ctx = makeCtx({ canSwitchTabs: false });
    dispatchMainMenuCommand('viewHistory', ctx);
    expect(ctx.setActiveTabKey).not.toHaveBeenCalled();
});

test('newScanDefiniton calls activateDefinitionPanel', () => {
    const ctx = makeCtx();
    dispatchMainMenuCommand('newScanDefiniton', ctx);
    expect(ctx.activateDefinitionPanel).toHaveBeenCalled();
});

test('displaySettings opens settings panel', () => {
    const ctx = makeCtx();
    dispatchMainMenuCommand('displaySettings', ctx);
    expect(ctx.setSettingsPanelOpen).toHaveBeenCalledWith(true);
});

test('deleteScanDefiniton clicks Delete in the ScanList footer', () => {
    document.body.innerHTML = `
      <div class="scan-list-container">
        <button class="scan-manage-button" id="delete-btn">Delete</button>
      </div>
    `;
    const deleteBtn = document.getElementById('delete-btn') as HTMLButtonElement;
    const onClick = jest.fn();
    deleteBtn.addEventListener('click', onClick);

    const ctx = makeCtx();
    dispatchMainMenuCommand('deleteScanDefiniton', ctx);
    expect(onClick).toHaveBeenCalled();
});

test('scan and scanAndMerge click the matching ActionsPanel buttons', () => {
    document.body.innerHTML = `
      <div class="actions-panel-container">
        <button class="action-button" id="scan-btn">Scan</button>
        <button class="action-button" id="scan-merge-btn">Scan and Merge</button>
      </div>
    `;

    const scanBtn = document.getElementById('scan-btn') as HTMLButtonElement;
    const scanMergeBtn = document.getElementById('scan-merge-btn') as HTMLButtonElement;
    const scanOnClick = jest.fn();
    const scanMergeOnClick = jest.fn();
    scanBtn.addEventListener('click', scanOnClick);
    scanMergeBtn.addEventListener('click', scanMergeOnClick);

    const ctx = makeCtx();
    dispatchMainMenuCommand('scan', ctx);
    expect(scanOnClick).toHaveBeenCalled();
    dispatchMainMenuCommand('scanAndMerge', ctx);
    expect(scanMergeOnClick).toHaveBeenCalled();
});

test('replaceSource/replaceTarget click the correct merge buttons for the selected result', () => {
    setSelectedResultDom(123);
    const ctx = makeCtx({ selectedResultId: 123 });

    const mergeSourceBtn = document.querySelector<HTMLButtonElement>('.result-merge-button.merge-source');
    const mergeTargetBtn = document.querySelector<HTMLButtonElement>('.result-merge-button.merge-target');
    if (!mergeSourceBtn || !mergeTargetBtn) {
        throw new Error('Missing merge buttons in test DOM.');
    }
    const mergeSourceOnClick = jest.fn();
    const mergeTargetOnClick = jest.fn();
    mergeSourceBtn.addEventListener('click', mergeSourceOnClick);
    mergeTargetBtn.addEventListener('click', mergeTargetOnClick);

    dispatchMainMenuCommand('replaceSource', ctx);
    expect(mergeTargetOnClick).toHaveBeenCalled();

    dispatchMainMenuCommand('replaceTarget', ctx);
    expect(mergeSourceOnClick).toHaveBeenCalled();
});

test('fileAttributes expands the selected result when not already expanded', () => {
    const resultId = 555;
    setSelectedResultDom(resultId);
    const ctx = makeCtx({ selectedResultId: resultId });

    const expandButtonRow = document.querySelector<HTMLElement>('.expand-button-row');
    if (!expandButtonRow) {
        throw new Error('Missing expand button row in test DOM.');
    }
    const expandOnClick = jest.fn();
    expandButtonRow.addEventListener('click', expandOnClick);

    dispatchMainMenuCommand('fileAttributes', ctx);
    expect(expandOnClick).toHaveBeenCalled();
});

