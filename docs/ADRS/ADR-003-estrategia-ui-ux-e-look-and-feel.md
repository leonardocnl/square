# ADR-003: Estratégia de UI/UX, Look & Feel e Eliminação de Modais Bloqueantes

- **Status:** Aceito
- **Data:** 2026-11-15
- **Decisores:** Arquiteto de Software e Engenheiro Principal do Square

---

## Contexto

A experiência do usuário no Sweet Home 3D sofre com padrões visuais e ergonômicos da década de 2000:
1. **Visual Datado e Ausência de Dark Mode:** O uso de Look & Feels padrão do Swing (`Metal`, `Nimbus` ou `System`) resulta em interfaces visualmente desatualizadas, sem suporte adequado a modo escuro e com problemas frequentes de renderização em monitores modernos de alta densidade de pixels (High-DPI / 4K).
2. **Desperdício de Espaço Útil:** A barra de ferramentas (`JToolBar`) fixada horizontalmente no topo em `HomePane.java` (BorderLayout.NORTH) consome área vertical contínua, mesmo quando o usuário está focado exclusivamente no desenho.
3. **Fricção Cognitiva com Diálogos Modais:** A edição de paredes (`WallPanel`), móveis (`HomeFurniturePanel`), cômodos (`RoomPanel`) e níveis é feita via `JOptionPane.createDialog(...)`, abrindo janelas pop-up modais síncronas que bloqueiam a aplicação, obstruem a área de desenho e impedem a comparação visual em tempo real.
4. **Localização Lenta de Ferramentas e Móveis:** A navegação pelo catálogo de móveis exige cliques repetitivos em pastas em árvore ou listas longas, na ausência de uma busca universal rápida por atalho de teclado.
5. **Falta de Alinhamento Magnético Visual Moderno:** No `PlanComponent.java`, o alinhamento 2D limita-se a eixos ortogonais simples, carecendo de guias inteligentes que exibam distâncias em tempo real entre objetos e arestas adjacentes.

## Decisão

1. **Adoção do FlatLaf como Base de Look & Feel:**
   - Padronizar a camada visual do Swing sobre a biblioteca **FlatLaf** (versão contemporânea).
   - O FlatLaf fornece renderização vetorial nítida em HiDPI, cantos arredondados nativos, componentes limpos e suporte robusto a temas Claro e Escuro sem depender de bibliotecas nativas de terceiros.
   - Criar um `ThemeManager` para gerenciar a troca dinâmica de temas (`FlatLightLaf` e `FlatDarkLaf`) e propagar tokens de cores customizados para o `PlanComponent`.
2. **Dock Flutuante de Ferramentas (Estilo Figma):**
   - Remover a barra de ferramentas fixa do `BorderLayout.NORTH` do `HomePane`.
   - Implementar o `FloatingDockComponent`: uma barra de ferramentas suspensa, horizontal, translúcida, com cantos arredondados e sombra sutil, renderizada através de um `JLayeredPane` centralizado no topo da área de desenho.
3. **Inspetor de Propriedades em Tempo Real (Substituição de Modais):**
   - Converter os formulários de edição de `WallPanel`, `RoomPanel`, `HomeFurniturePanel` em painéis embutíveis desacoplados de diálogos.
   - Criar o `PropertyInspectorPanel` na lateral direita da janela principal:
     - Quando nenhum objeto estiver selecionado, exibe propriedades gerais do projeto/nível atual.
     - Quando uma parede, móvel ou cômodo for selecionado, o inspetor é preenchido instantaneamente com seus atributos editáveis (dimensões, rotação, textura, cor, elevação).
     - Alterações nos inputs disparam atualizações no controller correspondente (`WallController`, `HomeFurnitureController`) e propagam as alterações imediatamente para o modelo e para a visualização 3D, sem bloquear a thread nem exigir confirmação de diálogo.
4. **Paleta de Comandos (`Ctrl+K`):**
   - Implementar um diálogo de busca instantânea flutuante ativado por `Ctrl+K` (ou `Cmd+K` no macOS).
   - O componente indexa em memória o catálogo completo de móveis e a lista de ações da aplicação, permitindo que o usuário digite o nome de um objeto (ex.: "Mesa", "Sofá") ou ferramenta (ex.: "Criar Paredes") e execute a ação sem tirar as mãos do teclado.
5. **Guias Inteligentes (Smart Guides) na Planta 2D:**
   - Desacoplar a lógica de guias do `PlanComponent` para uma camada dedicada (`SmartGuidesRenderer`).
   - Implementar feedback visual de linhas guia magnéticas (centros, arestas externas) e indicadores flutuantes com o valor numérico da distância relativa em centímetros entre o objeto sendo arrastado e os vizinhos mais próximos.

## Consequências

### Positivas
- **Elevação Radical da Experiência do Usuário:** O software adquire estética e ergonomia de nível profissional comparável a ferramentas líderes de mercado.
- **Maior Velocidade de Modelagem:** A edição por inspetor em tempo real e a paleta de comandos (`Ctrl+K`) reduzem em mais de 50% o número de cliques necessários para configurar ambientes.
- **Legibilidade Superior:** Dark Mode reduz drasticamente a fadiga visual em longas sessões de trabalho.

### Negativas / Desafios
- **Adaptação do Sistema de Undo/Redo nos Inspetores:** No modelo antigo com modal, o usuário podia cancelar o diálogo e descartar as alterações inteiras de uma vez. No inspetor em tempo real, cada mutação deve ser registrada de forma atômica ou agrupada no `UndoManager` para que o usuário possa desfazer (`Ctrl+Z`) de maneira natural.
- **Risco de Performance no Redesenho:** O feedback contínuo em tempo real (digitar um número no campo de espessura de parede e atualizar a malha 3D instantaneamente) exige que o evento seja despachado de forma assíncrona ou com debounce para não engargalar o pipeline de renderização.

