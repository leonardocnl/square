# Roadmap de Execução — square

Este documento estabelece o plano sequencial de desenvolvimento para a modernização do **square**. Cada marco é pequeno, incremental, possui critérios de aceitação objetivos e verificáveis, e prioriza a estabilidade estrutural e a experiência visual antes de avançar para subsistemas mais complexos.

---

## Visão Geral das Fases

```mermaid
flowchart LR
    M0["M0: Estabilidade e Build"] --> M1["M1: FlatLaf e Dark Mode"]
    M1 --> M2["M2: Dock Flutuante"]
    M2 --> M3["M3: Inspetores em Tempo Real"]
    M3 --> M4["M4: Paleta de Comandos"]
    M4 --> M5["M5: Guias Inteligentes"]
    M5 --> M6["M6: Salvamento e Undo/Redo"]
    M6 --> M7["M7: Motor 3D Moderno"]
```

---

## Marco 0: Fundação, Estabilidade do Ambiente e Baseline

**Objetivo:** Consolidar os scripts de compilação, assegurar reprodutibilidade total do build no JDK 25 e estabelecer a suíte básica de testes de fumaça sem quebras.

- [x] Validar compilação do núcleo sem intermediários (`./scripts/verify-core.sh`).
- [x] Validar compilação dos módulos Gradle (`./gradlew :sh3d-core:jar` e `./gradlew :app:classes`).
- [x] Criar teste de integração automatizado para leitura e escrita de projetos `.sh3d` e importação de catálogo `.sh3f`.
- [x] Estabelecer baseline de documentação viva (`AGENTS.md`, `docs/VISION.md`, `docs/ARCHITECTURE.md`, `docs/CODING_STANDARDS.md`, `docs/VERIFICATION.md`).

**Critério de Aceite:**
- Execução limpa com código de saída `0` em `./scripts/verify-core.sh` e `./gradlew :sh3d-core:jar`.
- Zero advertências críticas de sistema ou dependências nativas ausentes.

---

## Marco 1: Look & Feel Moderno (FlatLaf) e Suporte a Dark Mode

**Objetivo:** Substituir o visual datado do Swing por uma base estética contemporânea, nítida em HiDPI e com alternância suave entre temas Claro e Escuro.

- [x] Integrar a biblioteca **FlatLaf** como Look & Feel padrão no bootstrap da aplicação.
- [x] Criar a classe `ThemeManager` para alternar dinamicamente entre `FlatLightLaf` e `FlatDarkLaf`.
- [x] Definir tokens de cores semânticos para a interface e para a área de desenho da planta 2D (`theme.plan.background`, `theme.plan.grid`, `theme.plan.wall`).
- [x] Adaptar o `PlanComponent` para renderizar réguas, grade, paredes e seleções com contraste ideal no modo escuro.
- [x] Estilizar os divisores do layout (`JSplitPane`) para linhas limpas e minimalistas.

**Critério de Aceite:**
- O usuário consegue alternar entre modo Claro e Escuro através do menu ou atalho.
- Todos os 4 quadrantes (Catálogo, Tabela, Planta 2D e View 3D) atualizam suas cores instantaneamente sem falhas gráficas ou perda de legibilidade no desenho 2D.

---

## Marco 2: Dock Flutuante de Ferramentas (Estilo Figma)

**Objetivo:** Desacoplar a barra de ferramentas superior fixa, liberando espaço vertical e adotando um dock flutuante ergonômico sobreposto ao canvas.

- [ ] Implementar o componente `FloatingDockComponent` em Swing com bordas arredondadas, fundo translúcido e sombra suave.
- [ ] Posicionar o dock na parte superior central da área de trabalho utilizando `JLayeredPane`.
- [ ] Conectar os botões do dock diretamente às ações existentes no `HomeController` (`ActionType.SELECT`, `CREATE_WALLS`, `CREATE_ROOMS`, etc.).
- [ ] Organizar os botões em grupos visuais coesos (Seleção/Navegação, Criação Arquitetural, Elementos Auxiliares e Modos 3D).
- [ ] Adicionar microinterações visuais de hover, estado ativo e tooltips elegantes.
- [ ] Remover a antiga `JToolBar` fixa de `BorderLayout.NORTH` em `HomePane.java`.

**Critério de Aceite:**
- A barra cinza estática superior é eliminada.
- O dock flutua sobre o topo da área central, permitindo que a área da planta ganhe espaço vertical.
- 100% das ferramentas anteriores continuam funcionais e com estados selecionados sincronizados.

---

## Marco 3: Inspetor de Propriedades em Tempo Real (Substituição de Modais)

**Objetivo:** Eliminar diálogos modais bloqueantes para edição de elementos arquiteturais, substituindo-os por um painel lateral reativo.

- [ ] Desenvolver o componente `PropertyInspectorPanel` na lateral direita da área de desenho.
- [ ] Desacoplar os formulários de edição de `WallPanel`, `RoomPanel` e `HomeFurniturePanel` de instâncias de `JDialog`.
- [ ] Implementar observadores de seleção no `Home`:
  - Seleção de Parede: exibe espessura, comprimento, altura inicial/final e texturas no inspetor.
  - Seleção de Móvel: exibe largura, profundidade, altura, elevação, rotação e cor.
  - Seleção de Cômodo: exibe área, nome, piso e teto.
  - Nenhuma seleção: exibe propriedades do nível ou do projeto.
- [ ] Vincular as alterações de inputs do usuário aos respectivos controllers (`WallController`, `HomeFurnitureController`) de forma assíncrona ou debounced.
- [ ] Garantir que cada alteração seja registrada adequadamente no `UndoManager` para suporte contínuo a `Ctrl+Z`.

**Critério de Aceite:**
- Clicar em qualquer parede ou móvel na planta 2D abre seus atributos no inspetor lateral instantaneamente.
- Alterar um valor numérico (ex.: espessura de 10 para 15 cm) reflete na planta 2D e na malha 3D em tempo real, sem exibir nenhum diálogo modal `JOptionPane` e sem travar a interface.

---

## Marco 4: Paleta de Comandos (`Ctrl+K`)

**Objetivo:** Permitir busca e inserção rápida de móveis e execução de ferramentas diretamente pelo teclado.

- [ ] Criar o componente visual da paleta de comandos (`CommandPaletteDialog` flutuante e centralizado).
- [ ] Registrar atalho global de teclado `Ctrl+K` (e `Cmd+K` no macOS) em toda a aplicação.
- [ ] Indexar em memória as peças do `FurnitureCatalog` e as ações do `HomeController`.
- [ ] Implementar busca incremental com filtragem rápida por palavras-chave e categorias.
- [ ] Ao selecionar um móvel na busca, colocá-lo no modo de inserção ou no centro da visualização da planta.

**Critério de Aceite:**
- Pressionar `Ctrl+K` abre o campo de busca em menos de 100 ms.
- Digitar termos como "cadeira", "porta" ou "salvar" exibe os resultados filtrados instantaneamente. Pressionar `Enter` ativa a ferramenta ou insere o móvel selecionado.

---

## Marco 5: Guias Inteligentes (Smart Guides) e Réguas Dinâmicas

**Objetivo:** Elevar a precisão e velocidade do desenho 2D com linhas guia magnéticas e medição automática de distância em relação a elementos adjacentes.

- [ ] Extrair a renderização de alinhamento do monolito `PlanComponent.java` para `SmartGuidesRenderer`.
- [ ] Implementar cálculo em tempo real de alinhamento de arestas (esquerda, direita, centro, topo, base) entre o elemento sendo manipulado e os elementos vizinhos mais próximos.
- [ ] Renderizar linhas guia sutis (guias magenta/azuis) que se acendem quando o elemento atinge o ponto de snap.
- [ ] Exibir balões com a cota numérica da distância em centímetros até a parede ou móvel adjacente mais próximo durante o arraste.

**Critério de Aceite:**
- Ao mover um móvel ou desenhar uma parede próxima a outra, linhas guia magnéticas surgem automaticamente com indicação precisa da distância em tempo real.
- O recurso pode ser ativado/desativado de forma fluida (tecla modificadora `Alt` ou preferência do usuário).

---

## Marco 6: Salvamento Contínuo e Histórico de Ações Aprimorado

**Objetivo:** Assegurar preservação total do trabalho do usuário com salvamento contínuo em segundo plano e histórico visual de undo/redo.

- [ ] Refatorar o `AutoRecoveryManager` para operar com snapshots imutáveis do modelo de dados (`Home`), gravando o estado em thread de background (I/O) sem interferir na taxa de quadros da UI.
- [ ] Implementar histórico visual de ações (`ActionHistoryPanel`), permitindo que o usuário visualize a lista de passos executados e retroceda para qualquer ponto anterior com um clique.

**Critério de Aceite:**
- O salvamento em background ocorre silenciosamente sem engasgos ou quedas de FPS no desenho ou rotação 3D.
- O histórico de `undo/redo` exibe os nomes descritivos das últimas 30 ações realizadas e permite restauração precisa.

---

## Marco 7: Modernização e Desempenho do Motor 3D em Tempo Real

**Objetivo:** Otimizar o loop de renderização 3D, permitir atualização incremental de geometria e adicionar suporte a formatos 3D modernos.

- [ ] Desacoplar a geração de malhas tridimensionais das dependências estritas do Java3D legado.
- [ ] Implementar atualização incremental do grafo de cena: atualizar apenas a malha da parede ou móvel modificado, em vez de reconstruir o cenário inteiro a cada evento.
- [ ] Adicionar suporte à importação de modelos no formato moderno GLTF/GLB (com texturas embutidas e materiais PBR), mantendo compatibilidade com arquivos `.obj` e `.3ds` legados.

**Critério de Aceite:**
- Navegação fluida em projetos grandes com mais de 500 objetos em 60 FPS contínuos.
- Capacidade de importar e renderizar arquivos `.gltf`/`.glb` com materiais e texturas corretas no catálogo e na visualização 3D.

