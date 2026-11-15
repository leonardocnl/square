# Visão do Produto — square

## 1. Declaração de Visão

O **square** é um fork desktop moderno, elegante e de alto desempenho do Sweet Home 3D 7.5. O projeto une a confiabilidade de duas décadas de maturidade algorítmica do Sweet Home 3D a uma experiência visual e ergonômica de padrão internacional, comparável a ferramentas modernas de design como Figma, Blender e Sketch.

Nossa missão é transformar o software de planejamento de interiores open-source mais completo do mundo em uma ferramenta que arquitetos, designers de interiores e entusiastas sintam prazer e agilidade ao utilizar diariamente.

---

## 2. Princípios Norteadores

1. **Continuidade Operacional Sem Regressões:** O usuário tradicional do Sweet Home 3D deve conseguir abrir seus arquivos históricos `.sh3d`, acessar suas bibliotecas de catálogo `.sh3f` e executar qualquer operação de modelagem existente sem encontrar erros, perdas de dados ou funcionalidades removidas.
2. **Eficiência e Ergonomia Espacial:** Reduzir a fadiga visual, eliminar fricções cognitivas causadas por janelas modais invasivas e maximizar a área útil de trabalho do canvas 2D e do viewport 3D.
3. **Desempenho Responsivo:** Interações de planta, zoom, pan, seleção e rotação 3D devem operar de forma suave, sem bloqueios na thread de eventos (EDT - Event Dispatch Thread).
4. **Evolução Gradual com Clean Architecture:** Cada nova funcionalidade e refatoração deve pavimentar o caminho para a modularidade do código, isolando a lógica de negócio do framework visual.

---

## 3. Escopo da Fase 1

A Fase 1 foca na **modernização da fundação visual e ergonômica sobre Java/Swing**, mantendo a integridade total do modelo de domínio e do pipeline de dados.

### Itens Incluídos no Escopo da Fase 1:
- **Look & Feel Moderno com Suporte a Dark Mode:**
  - Adoção de uma base moderna (FlatLaf) compatível com High-DPI em Linux, macOS e Windows.
  - Implementação de temas Claro e Escuro, com paletas de cores refinadas para contraste adequado no canvas da planta 2D (`PlanComponent`).
- **Dock de Ferramentas Flutuante (Estilo Figma):**
  - Substituição da `JToolBar` rígida no topo por um dock flutuante, translúcido e contextual sobre a área de trabalho.
  - Organização visual de ferramentas por grupos semânticos (Seleção/Navegação, Desenho Arquitetural, Elementos Auxiliares, Modos de Visualização).
- **Inspetores em Tempo Real (Substituição de Modais Bloqueantes):**
  - Eliminação de `JDialog` modal para edição frequente de paredes (`WallPanel`), móveis (`HomeFurniturePanel`), cômodos (`RoomPanel`) e textos/linhas.
  - Introdução de um painel lateral/dock de propriedades reativo: ao selecionar um objeto na planta 2D ou na visualização 3D, suas propriedades são exibidas e editáveis instantaneamente, sem abrir popups ou congelar o restante da interface.
- **Estrutura dos Quatro Painéis Otimizada:**
  - Manutenção do layout consagrado de 4 quadrantes (Catálogo de Móveis, Lista de Móveis, Planta 2D e Vista 3D), porém com divisores minimalistas, colapsamento rápido e melhor aproveitamento de espaço.
- **Paleta de Comandos (`Ctrl+K`):**
  - Janela de busca unificada e rápida ativada via teclado (`Ctrl+K` ou `Cmd+K` no macOS) para pesquisa e inserção de móveis do catálogo, ativação de ferramentas e execução de ações do sistema.
- **Guias Inteligentes (Smart Guides) na Planta 2D:**
  - Auxiliares visuais em tempo real no `PlanComponent` para alinhamento automático de arestas, centros de objetos e indicação dinâmica de distâncias em relação a paredes e móveis vizinhos.
- **Salvamento Contínuo e Histórico Aprimorado:**
  - Salvamento contínuo em segundo plano (background auto-save) sem micro-travamentos na UI, mantendo snapshot imutável do `Home`.
  - Histórico de `undo/redo` mais claro para o usuário, com pilha de ações descritivas.
- **Foco em 3D em Tempo Real:**
  - Otimizações no loop de renderização 3D e preparação para importação de formatos modernos de malhas 3D (ex.: GLTF/GLB).

---

## 4. Fora de Escopo da Fase 1 (Explicitly Out of Scope)

Para assegurar foco, previsibilidade e estabilidade, os seguintes itens estão **explicitamente excluídos** da Fase 1:

- **Reescrita em Rust:** Nenhuma camada do núcleo ou do renderizador será reescrita em Rust nesta fase. A reescrita em Rust é uma hipótese futura que só será considerada quando o produto em Java estiver com sua UX, recursos e regras de negócio totalmente estabilizados.
- **Frameworks Web Desktop (Electron, Tauri, React, WebViews):** A aplicação permanecerá 100% nativa na JVM / Java Swing, evitando consumo excessivo de memória, lentidão de inicialização e camadas extras de abstração de UI.
- **Renderização Fotorrealista Offline (Raytracing):** O pipeline de geração de fotos e vídeos estáticos de alta precisão (motores Sunflow e YafaRay) será mantido como no Sweet Home 3D original, sem receber novos recursos nesta fase. O foco é exclusivamente 3D em tempo real.
- **Compatibilidade com Plugins Legados de Terceiros:** Não haverá esforço para garantir que plugins antigos do Sweet Home 3D continuem funcionando se interferirem na modernização da interface e dos novos inspetores.

---

## 5. Critérios de Sucesso da Fase 1

O sucesso da Fase 1 será avaliado pelos seguintes critérios verificáveis:

1. **Taxa de Regressão Zero em I/O:** 100% dos arquivos de teste `.sh3d` (simples e complexos com múltiplos níveis, texturas personalizadas e centenas de móveis) abrem e salvam de forma idêntica ao Sweet Home 3D 7.5.
2. **Tempo de Inicialização:** A inicialização do aplicativo em ambiente desktop moderno não deve exceder 3 segundos até o estado interativo.
3. **Fluidez de Edição Sem Modais:** Um usuário deve ser capaz de selecionar uma parede ou móvel, alterar dimensões, elevação ou cores no inspetor lateral e ver a alteração refletida simultaneamente na planta 2D e no 3D sem que nenhum diálogo modal seja disparado.
4. **Ergonomia e Dark Mode:** Todos os componentes da interface (árvore, tabela, botões, inspetores, canvas 2D) devem responder perfeitamente à troca de tema claro/escuro, mantendo contraste legível em cotas, linhas de parede e fundos de tela.
5. **Estabilidade de Build e Testes:** Build reproduzível localmente e no CI com execução limpa dos testes automatizados e do script de fumaça (`scripts/verify-core.sh`).

