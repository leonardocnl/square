# Arquitetura do Sistema — square

Este documento detalha o mapa da arquitetura atual do repositório, diagnostica os pontos críticos de acoplamento entre UI, estado e renderizadores, e apresenta a arquitetura alvo com uma estratégia de desacoplamento gradual e segura.

---

## 1. Mapa da Arquitetura Atual

O **square** herda a arquitetura Model-View-Controller (MVC) com separação via fábrica abstrata de visões (`ViewFactory`), conforme implementado na base de código do Sweet Home 3D 7.5 em `vendor/sh3d/src`.

```mermaid
flowchart TD
    subgraph Model ["Camada de Domínio / Modelo (com.eteks.sweethome3d.model)"]
        Home["Home (Raiz de Agregação)"]
        Wall["Wall, Room, Level"]
        Furniture["HomePieceOfFurniture, Catalog"]
        Prefs["UserPreferences"]
    end

    subgraph Controllers ["Camada de Controle e Casos de Uso (com.eteks.sweethome3d.viewcontroller)"]
        HomeController["HomeController"]
        PlanController["PlanController"]
        Controller3D["HomeController3D"]
        SubControllers["WallController, HomeFurnitureController, etc."]
        ViewFactory["ViewFactory (Interface)"]
    end

    subgraph IO ["Camada de Persistência e I/O (com.eteks.sweethome3d.io)"]
        FileRecorder["HomeFileRecorder (.sh3d)"]
        FilePrefs["FileUserPreferences (.sh3f, .sh3t)"]
        AutoRecovery["AutoRecoveryManager"]
    end

    subgraph SwingUI ["Camada de Apresentação Legada (com.eteks.sweethome3d.swing)"]
        SwingViewFactory["SwingViewFactory"]
        HomePane["HomePane (4 Painéis + JToolBar)"]
        PlanComponent["PlanComponent (Graphics2D 2D Plan)"]
        Modals["WallPanel, RoomPanel, HomeFurniturePanel (JDialogs)"]
    end

    subgraph Engine3D ["Motor 3D Legado (com.eteks.sweethome3d.j3d)"]
        Component3D["HomeComponent3D (Canvas3D / Java3D 1.6)"]
        ObjectBranches["Wall3D, Room3D, Furniture3D"]
    end

    Home --> Wall
    Home --> Furniture
    Controllers --> Home
    Controllers --> Prefs
    Controllers --> ViewFactory
    SwingViewFactory ..|> ViewFactory
    SwingViewFactory --> HomePane
    SwingViewFactory --> PlanComponent
    SwingViewFactory --> Modals
    SwingViewFactory --> Component3D
    HomePane --> PlanComponent
    HomePane --> Component3D
    PlanComponent -.-> PlanController
    Component3D -.-> Controller3D
    Component3D --> ObjectBranches
    FileRecorder --> Home
    FilePrefs --> Prefs
```

### 1.1 Pacotes e Classes Reais do Repositório

| Pacote | Classes Chave | Responsabilidade Principal | Acoplamento Externo |
|---|---|---|---|
| `com.eteks.sweethome3d.model` | `Home`, `Wall`, `Room`, `HomePieceOfFurniture`, `Level`, `Camera`, `UserPreferences` | Estado de domínio, entidades geométricas e listeners de mutação. | **Zero**. Apenas tipos padrão do JDK (`java.awt.geom`, `java.beans`, `javax.swing.undo`). |
| `com.eteks.sweethome3d.io` | `HomeFileRecorder`, `FileUserPreferences`, `AutoRecoveryManager` | Leitura e escrita do formato `.sh3d` (ZIP contendo XML e recursos binários) e carregamento de catálogos `.sh3f`. | Usa `model` e utilitários de compressão do JDK. |
| `com.eteks.sweethome3d.tools` | `OperatingSystem`, `ResourceURLContent`, `ExtensionsClassLoader` | Deteção de SO, caminhos de configuração e carregamento de recursos. | Mínimo; `OperatingSystem` possui chamada opcional a `AppleJavaExtensions.jar` para macOS. |
| `com.eteks.sweethome3d.viewcontroller` | `HomeController`, `PlanController`, `HomeController3D`, `ViewFactory`, `View`, `PlanView`, `DialogView` | Lógica de interação, máquinas de estado de desenho 2D, cálculo de magnetismo e orquestração de diálogos. | Independente de UI gráfica; comunica-se com a apresentação exclusivamente por interfaces. |
| `com.eteks.sweethome3d.swing` | `SwingViewFactory`, `HomePane`, `PlanComponent`, `WallPanel`, `RoomPanel`, `HomeFurniturePanel` | Implementação visual baseada em componentes Swing, layout em 4 quadrantes e diálogos modais de edição. | Depende fortemente de `javax.swing.*`, `java.awt.*` e do pacote `j3d`. |
| `com.eteks.sweethome3d.j3d` | `Component3DManager`, `Object3DBranch`, `Wall3D`, `Room3D` | Construção de grafo de cena tridimensional, malhas e renderização. | Fortemente acoplado a `javax.media.j3d.*` e JOGL legado. |
| Raiz (`com.eteks.sweethome3d`) | `SweetHome3D`, `HomeFrameController`, `HomeFramePane` | Ponto de entrada (bootstrap), configuração de janelas (`JFrame`) e ciclo de vida do processo. | Agrega `swing`, `io` e `model`. |

---

## 2. Diagnóstico dos Principais Gargalos de Acoplamento

A análise minuciosa do código revela quatro pontos críticos de acoplamento que limitam a modernização da experiência do usuário:

### Gargalo 1: Modais Bloqueantes na Edição de Propriedades
- **Localização:** `com.eteks.sweethome3d.swing.WallPanel`, `RoomPanel`, `HomeFurniturePanel`.
- **Problema:** Cada um desses painéis estende `JPanel` e implementa `com.eteks.sweethome3d.viewcontroller.DialogView`. O método `displayView(View parentView)` chama diretamente `JOptionPane.createDialog(...)`, instanciando um `JDialog` modal síncrono.
- **Impacto:** O modal bloqueia a Event Dispatch Thread (EDT) da janela principal, obstrui a visão da planta e do 3D, impede que o usuário compare o resultado enquanto edita valores e quebra o fluxo de trabalho ágil.

### Gargalo 2: Monolitismo e Cores Fixas no `PlanComponent`
- **Localização:** `com.eteks.sweethome3d.swing.PlanComponent` (mais de 6.400 linhas).
- **Problema:** Uma única classe acumula:
  1. Tratamento de eventos de entrada brutos de mouse e teclado.
  2. Pintura com `Graphics2D` de todos os elementos (paredes, pisos, portas, réguas, cotas).
  3. Paleta de cores fixas (tons de cinza claro, bege e preto hardcoded), sem abstração de tokens de tema para Dark Mode.
  4. Lógica de snapping e magnetismo misturada com o feedback visual de desenho.
- **Impacto:** Qualquer modificação visual para Dark Mode ou adição de guias inteligentes exige alterar um arquivo gigantesco e com alto risco de regressão nos cálculos de projeção 2D.

### Gargalo 3: Barra de Ferramentas Rígida no `HomePane`
- **Localização:** `com.eteks.sweethome3d.swing.HomePane` (método `createToolBar` e adição em `contentPane.add(..., BorderLayout.NORTH)`).
- **Problema:** A `JToolBar` é instanciada diretamente no topo do layout da janela como um componente estático.
- **Impacto:** Impede a utilização de um dock flutuante contemporâneo (estilo Figma/Canva) posicionado sobre o canvas sem desperdiçar linhas inteiras de espaço vertical de tela.

### Gargalo 4: Acoplamento do 3D ao Java3D 1.6 Legado
- **Localização:** `com.eteks.sweethome3d.j3d.Component3DManager` e `HomeComponent3D`.
- **Problema:** Dependência estrita da API `javax.media.j3d` e de bibliotecas nativas de JOGL empacotadas para sistemas legados.
- **Impacto:** Dificuldade de suporte a formatos tridimensionais modernos (como GLTF/GLB) e limitações no controle de shaders e renderização moderna na GPU.

---

## 3. Arquitetura Alvo e Estratégia de Desacoplamento Gradual

A transição arquitetural do **square** adota os princípios de **Clean Architecture** e **Inversão de Dependência (DIP)** para desacoplar a apresentação sem reescrever ou quebrar a lógica de negócio madura.

```mermaid
flowchart TD
    subgraph DomainCore ["Núcleo de Domínio (Intocado)"]
        HomeCore["Home, Wall, Room, Preferences"]
        IOCore["HomeFileRecorder, FileUserPreferences"]
    end

    subgraph ApplicationCore ["Casos de Uso / Controllers (Preservados)"]
        HomeControllerApp["HomeController, PlanController, Controller3D"]
        SubControllersApp["WallController, HomeFurnitureController"]
        ViewFactoryDef["ViewFactory (Interface Expandida/Decorada)"]
    end

    subgraph ModernPresentation ["Camada de Apresentação Modernizada (square.ui)"]
        SquareViewFactory["SquareSwingViewFactory"]
        ThemeManager["ThemeManager (FlatLaf Light/Dark Engine)"]
        FloatingDock["FloatingDockComponent (Figma Style)"]
        PropertyInspector["PropertyInspectorPanel (Dock Lateral Reativo)"]
        CommandPalette["CommandPaletteDialog (Ctrl+K Engine)"]
        PlanRendererEngine["PlanRenderingEngine (2D Decoupled Layers)"]
        Engine3DProxy["Modern3DViewport (Abstração de Renderização)"]
    end

    ApplicationCore --> DomainCore
    IOCore --> DomainCore
    ModernPresentation --> ApplicationCore
    SquareViewFactory ..|> ViewFactoryDef
    SquareViewFactory --> FloatingDock
    SquareViewFactory --> PropertyInspector
    SquareViewFactory --> PlanRendererEngine
    SquareViewFactory --> Engine3DProxy
    ThemeManager --> ModernPresentation
```

### 3.1 Estratégia de Transição em 4 Passos

#### Passo 1: Injeção do Tema Global (FlatLaf & Dark Mode)
- Configurar o motor de Look & Feel do **FlatLaf** na inicialização da aplicação (`SweetHome3D` / ponto de entrada do Square).
- Criar a classe `ThemeManager` para alternar dinamicamente entre tema Claro e Escuro (`FlatLightLaf.setup()` e `FlatDarkLaf.setup()`).
- Expor tokens de cor semânticos (`theme.plan.background`, `theme.wall.stroke`, `theme.grid.color`) para consumo no desenho da planta 2D.

#### Passo 2: Implementação do Dock Flutuante e Desacoplamento da Barra de Ferramentas
- Manter a geração de ações (`ActionType`) e listeners que já existem no `HomeController`.
- Substituir a `JToolBar` rígida por um componente Swing estilizado com bordas arredondadas e sombra suave (`FloatingDockComponent`), montado no `JLayeredPane` da janela principal sobre a área central.

#### Passo 3: Inspetor em Tempo Real Substituindo os Diálogos Modais
- Em vez de chamar `JOptionPane.createDialog()` quando um item é selecionado:
  - Os sub-controllers (`WallController`, `HomeFurnitureController`, `RoomController`) já operam sobre instâncias do modelo com suporte a binds de propriedades.
  - Implementar um `PropertyInspectorPanel` em um painel lateral retrátil (à direita da planta 2D ou acoplado à tabela).
  - Quando a seleção do `Home` muda (`SelectionListener`), o inspetor detecta o tipo de objeto selecionado, vincula o respectivo controller e exibe os campos de edição diretamente no painel lateral.
  - Qualquer alteração nos campos atualiza o controller e reflete instantaneamente no modelo, na planta 2D e na vista 3D.

#### Passo 4: Modularização do `PlanComponent` e Guias Inteligentes
- Introduzir a interface `PlanLayerRenderer`:
  - `GridAndRulerRenderer`: desenho de réguas e grade com cores adaptadas ao tema.
  - `WallsAndRoomsRenderer`: desenho das paredes e cômodos.
  - `SmartGuidesRenderer`: novo componente responsável pelo cálculo e desenho de guias de alinhamento magnetizado e cotas dinâmicas de distância em tempo real.
- O `PlanComponent` atua apenas como coordenador de viewport e despachador de eventos para o `PlanController`.
