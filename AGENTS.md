# AGENTS.md — Regras de Operação e Governança para Agentes de IA

Este documento estabelece as regras mandatórias, padrões arquiteturais e protocolos operacionais para qualquer agente de IA ou desenvolvedor atuando no repositório **square**.

---

## 1. Resumo do Projeto

O **square** é um fork modernizado do **Sweet Home 3D** (versão base 7.5). Seu propósito é fornecer uma experiência desktop profissional, contemporânea e de alto desempenho para design de interiores e arquitetura residencial, preservando 100% das funcionalidades do software original sem introduzir regressões.

### Decisões Estratégicas Vigentes
- **Core e Interface em Java:** O núcleo de regras de negócio, modelo de domínio, importação/exportação e a interface de usuário permanecem em Java no ecossistema de desktop. A interface gráfica é modernizada diretamente em **Java/Swing** (rejeitando frameworks web empacotados como Electron, React ou Tauri).
- **Sem Reescrita Precipitada em Rust:** Uma futura reescrita de módulos de performance ou do core em Rust só será avaliada quando o produto, a UX e as abstrações de domínio estiverem totalmente consolidados e validados em produção.
- **Compatibilidade Inegociável:** Abertura e salvamento de projetos `.sh3d` e importação de bibliotecas de móveis (`.sh3f`), texturas (`.sh3t`) e idiomas (`.sh3l`) devem operar com paridade binária e comportamental absoluta.
- **Evolução Visual e Interativa:**
  - Substituição do visual retrô do Swing por um Look & Feel moderno com suporte nativo a Dark Mode e High-DPI (baseado em FlatLaf).
  - Barra de ferramentas superior transformada em um dock flutuante contextual (estilo Figma).
  - Eliminação de modais bloqueantes (diálogos de paredes, móveis, cômodos) em favor de inspetores de propriedades em tempo real (painéis contextuais).
  - Paleta de comandos universal (`Ctrl+K`) para ferramentas, catálogo e ações.
  - Guias inteligentes (smart guides) e réguas dinâmicas de distância em tempo real na planta 2D.
  - Salvamento contínuo em segundo plano e histórico de undo/redo robusto.

---

## 2. Restrições Inegociáveis (Guardrails)

1. **Proibição de Quebra de Compatibilidade:**
   - Nenhum arquivo `.sh3d` existente pode falhar ao ser carregado ou salvo.
   - Nenhuma biblioteca de catálogo existente (`.sh3f`) pode falhar ao ser importada.
   - Nenhuma funcionalidade de desenho, dimensionamento, agrupamento, níveis, cálculo de área ou exportação pode ser removida ou desabilitada.
2. **Isolamento de Código Legado:**
   - O código em `vendor/sh3d/src/` é a referência canônica do Sweet Home 3D 7.5. Nenhuma alteração pontual deve desvirtuar a compatibilidade das classes em `com.eteks.sweethome3d.model`, `com.eteks.sweethome3d.io` e `com.eteks.sweethome3d.viewcontroller`.
3. **Padrão de Indentação:**
   - Indentação estrita de **4 espaços** em todo e qualquer código modificado ou criado (Java, Kotlin, Gradle, scripts shell, JSON, XML). Proibido o uso de tabs.
4. **Passos Incrementais e Reversíveis:**
   - Proibido "big bang refactoring". Cada mudança deve focar em um único subsistema, ser pequena, conter testes ou passos de verificação automatizados e ser imediatamente reversível via Git.
5. **Protocolo de Verificação Obrigatório:**
   - Nenhum agente pode dar uma tarefa por concluída sem executar o protocolo de verificação descrito em `docs/VERIFICATION.md` e validar os critérios de aceitação.

---

## 3. Padrões de Engenharia de Software

- **Clean Architecture & SOLID:**
  - Manter o desacoplamento estrito entre Domínio (`model`), Casos de Uso/Apresentadores (`viewcontroller`), I/O (`io`) e Camada de Apresentação (`swing` / UI).
  - Os componentes Swing devem implementar interfaces puras (como as definidas em `com.eteks.sweethome3d.viewcontroller.View`, `PlanView`, `HomeView`).
  - Nenhuma regra de negócio ou mutação de cálculo geométrico deve residir dentro de ouvintes de eventos da UI (`ActionListener`, `MouseListener`, `paintComponent`).
- **Clean Code:**
  - Nomes de classes, métodos e variáveis autodocumentados em inglês ou alinhados ao vocabulário do domínio do Sweet Home 3D.
  - Métodos curtos com responsabilidade única.
  - Tratamento explícito de exceções (evitar `catch (Exception e) {}` silenciosos).

---

## 4. Comandos Canônicos de Compilação e Execução

### 4.1 Validação do Núcleo Headless (Sem Dependências Externas)
Compila o core (`model`, `viewcontroller`, `io`, `tools`) e valida ida e volta de criação e gravação de `.sh3d`:
```bash
./scripts/verify-core.sh
```

### 4.2 Compilação do Módulo Core via Gradle
Gera o JAR do núcleo do domínio:
```bash
./gradlew :sh3d-core:jar
```

### 4.3 Compilação Completa das Classes e Testes
Valida a compilação de todos os módulos do projeto:
```bash
./gradlew :app:classes :app:testClasses
```

### 4.4 Execução do Projeto
```bash
./gradlew :app:run
```

---

## 5. Protocolo Obrigatório de Conclusão de Tarefa

Antes de reportar a conclusão de qualquer alteração no código:
1. Executar `./scripts/verify-core.sh` e assegurar que não haja regressão no modelo e serialização.
2. Executar `./gradlew build -x test` (ou o comando de compilação aplicável ao escopo da tarefa) e garantir zero erros de compilação.
3. Se a alteração envolver I/O, executar o teste de abertura/salvamento de arquivo `.sh3d`.
4. Registrar nos artefatos ou no sumário da tarefa os comandos exatos executados e seus respectivos códigos de saída.

