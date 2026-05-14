export interface MainMenuStateWire {
    // View / tab navigation
    canViewScan: boolean;
    canViewHistory: boolean;
    canDisplaySettings: boolean;

    // Scan definition actions (left Scan tab)
    canNewScanDefiniton: boolean;
    canDeleteScanDefiniton: boolean;
    canScan: boolean;
    canScanAndMerge: boolean;

    // Result actions (Results panel)
    canReplaceSource: boolean;
    canReplaceTarget: boolean;
    canSourceAttributes: boolean;
    canTargetAttributes: boolean;
}

