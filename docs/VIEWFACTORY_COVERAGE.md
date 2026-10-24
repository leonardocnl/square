# Cobertura da ViewFactory

Cada método abaixo é chamado pelos controllers originais do SH3D. O Square só
tem paridade completa quando todos estiverem "pronto".

| Método | Tipo devolvido | Status |
|---|---|---|
| `createFurnitureCatalogView` | `View` | pendente |
| `createFurnitureView` | `View` | pendente |
| `createPlanView` | `PlanView` | parcial |
| `createView3D` | `View` | pendente |
| `createHomeView` | `HomeView` | pendente |
| `createWizardView` | `DialogView` | pendente |
| `createBackgroundImageWizardStepsView` | `View` | pendente |
| `createImportedFurnitureWizardStepsView` | `ImportedFurnitureWizardStepsView` | pendente |
| `createImportedTextureWizardStepsView` | `View` | pendente |
| `createThreadedTaskView` | `ThreadedTaskView` | pendente |
| `createUserPreferencesView` | `DialogView` | pendente |
| `createLevelView` | `DialogView` | pendente |
| `createHomeFurnitureView` | `DialogView` | pendente |
| `createWallView` | `DialogView` | pendente |
| `createRoomView` | `DialogView` | pendente |
| `createPolylineView` | `DialogView` | pendente |
| `createDimensionLineView` | `DialogView` | pendente |
| `createLabelView` | `DialogView` | pendente |
| `createCompassView` | `DialogView` | pendente |
| `createObserverCameraView` | `DialogView` | pendente |
| `createHome3DAttributesView` | `DialogView` | pendente |
| `createTextureChoiceView` | `TextureChoiceView` | pendente |
| `createBaseboardChoiceView` | `View` | pendente |
| `createModelMaterialsView` | `View` | pendente |
| `createPageSetupView` | `DialogView` | pendente |
| `createPrintPreviewView` | `DialogView` | pendente |
| `createPhotoView` | `DialogView` | pendente |
| `createPhotosView` | `DialogView` | pendente |
| `createVideoView` | `DialogView` | pendente |
| `createHelpView` | `HelpView` | pendente |

## Parcial: `createPlanView` → `SquarePlanView`

Pronto: escala (`getScale`/`setScale`, limites e zoom pelo `PlanController`
original), conversões pixel/modelo, `getPixelLength`, `moveView`, enquadrar.

Ainda sem implementação (no-op com `// TODO`, não afetam o pan/zoom):
feedbacks de edição (retângulo, tooltips, alinhamento, ângulo, arraste,
cotas), cursores, `makeSelectionVisible`/`makePointVisible`.
Falham alto com `TODO()` se chamados: `getTextBounds`, réguas,
`getPrintPreferredScale`, `getPieceOfFurnitureSizeInPlan`, `createTransferData`,
`exportData`.

Os demais 29 métodos da `SquareViewFactory` são `TODO()`.
