# Cobertura da ViewFactory

Cada método abaixo é chamado pelos controllers originais do SH3D. O Square só
tem paridade completa quando todos estiverem "pronto".

| Método | Tipo devolvido | Status |
|---|---|---|
| `createFurnitureCatalogView` | `View` | pendente |
| `createFurnitureView` | `View` | pendente |
| `createPlanView` | `PlanView` | parcial |
| `createView3D` | `View` | parcial |
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

## Parcial: `createView3D` → `Square3DView`

Pronto: janela jME e modo offscreen, câmera do `Home` (HomeController3D original),
paredes, pisos, tetos, chão e céu (cores do `HomeEnvironment`), entrada de mouse/teclado.
Falta: portas, janelas, móveis, texturas, rodapés, luzes do modelo, sombra/SSAO,
modo integrado na janela principal, persistência da janela destacada.

Os demais 28 métodos da `SquareViewFactory` são `TODO()`.
