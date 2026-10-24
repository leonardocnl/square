package br.com.square

import com.eteks.sweethome3d.model.*
import com.eteks.sweethome3d.viewcontroller.*

// Só createPlanView existe por enquanto; o resto falha alto com TODO() até ser
// implementado (status em docs/VIEWFACTORY_COVERAGE.md).
class SquareViewFactory : ViewFactory {
    override fun createFurnitureCatalogView(catalog: FurnitureCatalog, preferences: UserPreferences, furnitureCatalogController: FurnitureCatalogController): View = TODO("ViewFactory.createFurnitureCatalogView")

    override fun createFurnitureView(home: Home, preferences: UserPreferences, furnitureController: FurnitureController): View = TODO("ViewFactory.createFurnitureView")

    override fun createPlanView(home: Home, preferences: UserPreferences, planController: PlanController): PlanView =
        SquarePlanView(home, planController)

    override fun createView3D(home: Home, preferences: UserPreferences, homeController3D: HomeController3D): View =
        Square3DView(home, homeController3D)

    override fun createHomeView(home: Home, preferences: UserPreferences, homeController: HomeController): HomeView = TODO("ViewFactory.createHomeView")

    override fun createWizardView(preferences: UserPreferences, wizardController: WizardController): DialogView = TODO("ViewFactory.createWizardView")

    override fun createBackgroundImageWizardStepsView(backgroundImage: BackgroundImage, preferences: UserPreferences, backgroundImageWizardController: BackgroundImageWizardController): View = TODO("ViewFactory.createBackgroundImageWizardStepsView")

    override fun createImportedFurnitureWizardStepsView(piece: CatalogPieceOfFurniture, modelName: String, importHomePiece: Boolean, preferences: UserPreferences, importedFurnitureWizardController: ImportedFurnitureWizardController): ImportedFurnitureWizardStepsView = TODO("ViewFactory.createImportedFurnitureWizardStepsView")

    override fun createImportedTextureWizardStepsView(texture: CatalogTexture, textureName: String, preferences: UserPreferences, importedTextureWizardController: ImportedTextureWizardController): View = TODO("ViewFactory.createImportedTextureWizardStepsView")

    override fun createThreadedTaskView(taskMessage: String, userPreferences: UserPreferences, threadedTaskController: ThreadedTaskController): ThreadedTaskView = TODO("ViewFactory.createThreadedTaskView")

    override fun createUserPreferencesView(preferences: UserPreferences, userPreferencesController: UserPreferencesController): DialogView = TODO("ViewFactory.createUserPreferencesView")

    override fun createLevelView(preferences: UserPreferences, levelController: LevelController): DialogView = TODO("ViewFactory.createLevelView")

    override fun createHomeFurnitureView(preferences: UserPreferences, homeFurnitureController: HomeFurnitureController): DialogView = TODO("ViewFactory.createHomeFurnitureView")

    override fun createWallView(preferences: UserPreferences, wallController: WallController): DialogView = TODO("ViewFactory.createWallView")

    override fun createRoomView(preferences: UserPreferences, roomController: RoomController): DialogView = TODO("ViewFactory.createRoomView")

    override fun createPolylineView(preferences: UserPreferences, polylineController: PolylineController): DialogView = TODO("ViewFactory.createPolylineView")

    override fun createDimensionLineView(modification: Boolean, preferences: UserPreferences, dimensionLineController: DimensionLineController): DialogView = TODO("ViewFactory.createDimensionLineView")

    override fun createLabelView(modification: Boolean, preferences: UserPreferences, labelController: LabelController): DialogView = TODO("ViewFactory.createLabelView")

    override fun createCompassView(preferences: UserPreferences, compassController: CompassController): DialogView = TODO("ViewFactory.createCompassView")

    override fun createObserverCameraView(preferences: UserPreferences, home3DAttributesController: ObserverCameraController): DialogView = TODO("ViewFactory.createObserverCameraView")

    override fun createHome3DAttributesView(preferences: UserPreferences, home3DAttributesController: Home3DAttributesController): DialogView = TODO("ViewFactory.createHome3DAttributesView")

    override fun createTextureChoiceView(preferences: UserPreferences, textureChoiceController: TextureChoiceController): TextureChoiceView = TODO("ViewFactory.createTextureChoiceView")

    override fun createBaseboardChoiceView(preferences: UserPreferences, baseboardChoiceController: BaseboardChoiceController): View = TODO("ViewFactory.createBaseboardChoiceView")

    override fun createModelMaterialsView(preferences: UserPreferences, modelMaterialsController: ModelMaterialsController): View = TODO("ViewFactory.createModelMaterialsView")

    override fun createPageSetupView(preferences: UserPreferences, pageSetupController: PageSetupController): DialogView = TODO("ViewFactory.createPageSetupView")

    override fun createPrintPreviewView(home: Home, preferences: UserPreferences, homeController: HomeController, printPreviewController: PrintPreviewController): DialogView = TODO("ViewFactory.createPrintPreviewView")

    override fun createPhotoView(home: Home, preferences: UserPreferences, photoController: PhotoController): DialogView = TODO("ViewFactory.createPhotoView")

    override fun createPhotosView(home: Home, preferences: UserPreferences, photosController: PhotosController): DialogView = TODO("ViewFactory.createPhotosView")

    override fun createVideoView(home: Home, preferences: UserPreferences, videoController: VideoController): DialogView = TODO("ViewFactory.createVideoView")

    override fun createHelpView(preferences: UserPreferences, helpController: HelpController): HelpView = TODO("ViewFactory.createHelpView")
}
