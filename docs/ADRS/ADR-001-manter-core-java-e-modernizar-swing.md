# ADR-001: Manter o Core em Java e Modernizar a Interface Diretamente em Java/Swing

- **Status:** Aceito
- **Data:** 2026-11-15
- **Decisores:** Arquiteto de Software e Engenheiro Principal do Square

---

## Contexto

O Sweet Home 3D é um sistema de planejamento arquitetural residencial com mais de 20 anos de desenvolvimento contínuo, maturidade algorítmica comprovada e centenas de milhares de linhas de código testadas em produção. Ele gerencia regras complexas de domínio: junções poligonais de paredes em ângulos arbitrários, subdivisão de cômodos, elevações por nível (`Level`), projeções de telhados e escadas, e serialização de formato próprio.

Durante a exploração inicial de modernização do projeto **square**, foram avaliadas diferentes alternativas de tecnologia:
1. Reescrita completa em frameworks web empacotados (Electron, React, Tauri).
2. Substituição total da camada visual por Compose Multiplatform Desktop e jMonkeyEngine.
3. Reescrita do núcleo de processamento em Rust.

Os testes de integração revelaram que soluções intermediárias que realizavam cópia de framebuffer offscreen (como jME offscreen transferindo frames para o Skia/Compose) introduziram graves gargalos de consumo de memória nativa (esgotando a memória RAM do sistema) e degradaram a taxa de atualização (queda para 12 a 1,3 FPS em resoluções 1080p). Além disso, reimplementar as centenas de regras de interação e ferramentas em um novo framework de UI do zero introduziria alto risco de regressões e atrasaria a entrega de valor ao usuário.

Por outro lado, reescrever o core em Rust neste momento configuraria uma otimização prematura, desviando o foco da experiência do usuário, da ergonomia da aplicação e da estabilização da arquitetura.

## Decisão

1. **Manter o Núcleo em Java:** Os pacotes `com.eteks.sweethome3d.model`, `com.eteks.sweethome3d.io` e `com.eteks.sweethome3d.viewcontroller` permanecerão compilados em Java nativo na JVM (JDK 25).
2. **Modernizar Diretamente em Java/Swing:** A interface gráfica será refinada e modernizada diretamente sobre a pilha Java/Swing, utilizando bibliotecas contemporâneas de Look & Feel (FlatLaf), componentes desacoplados e técnicas avançadas de renderização 2D.
3. **Rejeição de Frameworks Web:** Não serão utilizados Electron, React, Tauri ou WebViews para a interface de edição da aplicação.
4. **Postergamento de Rust:** Nenhuma reescrita em Rust será iniciada durante a Fase 1. A adoção de Rust fica reservada exclusivamente para o futuro, caso surja uma demanda estrita de processamento numérico de geometria ou renderização que a JVM não consiga suprir após a UX e o produto estarem maduros.
5. **Desacoplamento Progressivo com Clean Architecture:** Os novos componentes visuais e inspetores serão isolados das regras de negócio através de interfaces puras, facilitando qualquer migração ou substituição pontual futura.

## Consequências

### Positivas
- **Paridade Funcional Imediata:** 100% dos recursos do Sweet Home 3D (ferramentas de desenho, magnetismo, dimensionamento, agrupamento, níveis e cálculo de área) continuam operacionais desde o primeiro dia.
- **Risco de Regressão Quase Nulo:** A lógica de negócio e os controllers existentes não são reescritos, evitando a reintrodução de bugs históricos já sanados na versão 7.5.
- **Desempenho Nativo e Baixo Consumo de Recursos:** Elimina overhead de bridges IPC, instâncias do Chromium ou pipelines de cópia de buffers offscreen do Skia.
- **Alta Compatibilidade:** Suporte direto ao ecossistema de bibliotecas e fontes existente no desktop Linux, Windows e macOS.

### Negativas / Desafios
- **Manutenção de Código Legado do Swing:** A equipe precisará refatorar arquivos extensos como `PlanComponent.java` (6400+ linhas) com extremo cuidado para extrair camadas sem afetar o comportamento.
- **Complexidade no Gerenciamento da EDT:** A manipulação de novos componentes reativos e salvamento contínuo deve ser rigorosamente orquestrada fora da Event Dispatch Thread do Swing para evitar travamentos visuais.

