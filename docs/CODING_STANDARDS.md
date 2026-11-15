# Padrões de Código e Diretrizes de Engenharia — square

Este documento define os padrões obrigatórios de desenvolvimento, design e boas práticas para todo código novo ou refatorado no projeto **square**.

---

## 1. Princípios Gerais

1. **Rigor e Simplicidade:** Evite abstrações prematuras. Resolva o problema real de forma direta e limpa.
2. **Imutabilidade e Previsibilidade:** Favoreça estruturas de dados imutáveis sempre que possível; isole mutações de estado no modelo de domínio (`com.eteks.sweethome3d.model`).
3. **Mudanças Incrementais:** Trabalhe sempre em commits atômicos, testáveis e reversíveis.

---

## 2. Formatação e Estilo

- **Indentação:** Estritamente **4 espaços** por nível de indentação.
  - Proibido o uso de caracteres de tabulação (`\t`).
  - Aplica-se a todos os tipos de arquivo: Java, Kotlin, scripts Shell, arquivos Gradle (`.gradle.kts`), JSON, XML e Markdown.
- **Limite de Linha:** Recomenda-se limite de 120 caracteres por linha.
- **Chaves (`{}`):** Abertura de chaves na mesma linha da declaração da classe/método/condicional (estilo 1TBS / K&R):
  ```java
  public class PropertyInspectorPanel extends JPanel {
      public void updateSelection(Selectable item) {
          if (item != null) {
              refreshFields(item);
          }
      }
  }
  ```
- **Codificação de Arquivos:** Novos arquivos criados para o projeto **square** devem ser salvos em **UTF-8**. Os arquivos legados em `vendor/sh3d/src/` estão em **ISO-8859-1** e mantêm sua codificação original.

---

## 3. Convenções de Nomenclatura

- **Classes e Interfaces:** `PascalCase`. Nomes devem ser substantivos substantivos ou qualificadores claros (ex.: `FloatingDockComponent`, `SmartGuidesRenderer`, `CommandPaletteDialog`).
- **Interfaces:** Não use prefixo `I` (use `PlanLayerRenderer`, não `IPlanLayerRenderer`). Use sufixos indicativos de capacidade (`Drawable`, `Transformable`) quando apropriado.
- **Métodos:** `camelCase`. Nomes devem iniciar com verbos indicando a ação (ex.: `renderGuides`, `applyTransform`, `calculateDistance`).
- **Variáveis e Parâmetros:** `camelCase`. Nomes autodocumentados (evite variáveis de uma única letra fora de laços matemáticos triviais como `x`, `y`, `i`).
- **Constantes:** `UPPER_SNAKE_CASE` (ex.: `DEFAULT_DOCK_ELEVATION`, `SNAP_TOLERANCE_PIXELS`).
- **Pacotes:** Minúsculos contínuos, sem sublinhados (ex.: `br.com.square.ui.inspector`, `com.eteks.sweethome3d.model`).

---

## 4. Aplicação de Clean Architecture e SOLID

### 4.1 Separação de Camadas (Clean Architecture)
- **Domínio (`model`):** Contém entidades e regras puras do espaço de design. Não deve depender de nenhuma classe gráfica do Swing ou AWT (`JComponent`, `Graphics`, `Color` do Swing).
- **Casos de Uso / Controllers (`viewcontroller`):** Coordenam o fluxo de dados entre o modelo e a apresentação através de interfaces abstratas (`View`, `PlanView`). Não manipulam widgets gráficos diretamente.
- **Apresentação (`ui` / `swing`):** Widgets gráficos, Look & Feel, renderizadores 2D/3D. Dependem das interfaces do `viewcontroller`, nunca o contrário.

### 4.2 Princípios SOLID na Prática
1. **Single Responsibility Principle (SRP):**
   - Classes visuais devem renderizar ou gerenciar interação, nunca calcular regras de geometria analítica do domínio.
   - Refatore componentes monolíticos extraindo classes especialistas (ex.: extrair o cálculo e desenho de guias magnéticas do `PlanComponent` para `SmartGuidesRenderer`).
2. **Open/Closed Principle (OCP):**
   - Novas ferramentas ou novos estilos de inspetor devem ser adicionados através de novas implementações de interfaces ou extensões da `ViewFactory`, sem alterar cirurgicamente classes centrais fechadas.
3. **Liskov Substitution Principle (LSP):**
   - Qualquer implementação personalizada de view (como `SquarePlanView` ou `ModernHomePane`) deve ser 100% substituível onde a interface original (`PlanView`, `HomeView`) for esperada pelos controllers existentes.
4. **Interface Segregation Principle (ISP):**
   - Prefira interfaces pequenas e focadas. Em vez de uma interface gráfica gigante, divida em contratos para seleção, desenho de camadas e eventos de teclado.
5. **Dependency Inversion Principle (DIP):**
   - Detalhes de implementação gráfica (como FlatLaf ou APIs de desenho 2D) devem depender de abstrações de design semântico e tokens de cores.

---

## 5. Tratamento de Erros e Exceções

- **Sem Silenciamento de Erros:** Proibido capturar exceções sem tratá-las ou sem registrá-las adequadamente:
  ```java
  // PROIBIDO:
  try {
      fileRecorder.writeHome(home, path);
  } catch (Exception e) {}

  // CORRETO:
  try {
      fileRecorder.writeHome(home, path);
  } catch (RecorderException ex) {
      logger.log(Level.SEVERE, "Falha ao gravar arquivo de projeto: " + path, ex);
      displayUserFeedbackError("Não foi possível salvar o projeto.", ex);
  }
  ```
- **Validação Antecipada (Fail Fast):** Verifique argumentos nulos e estados inválidos no início dos métodos públicos (`Objects.requireNonNull(home, "home cannot be null")`).

---

## 6. Estratégia de Testes

1. **Testes Unitários:** Para qualquer lógica utilitária, matemática de projeção, transformações de coordenadas e parsing. Devem executar em poucos milissegundos e sem necessidade de display gráfico (`headless`).
2. **Testes de Integração de I/O:** Validação contínua do ciclo completo de escrita e leitura de `.sh3d` (`scripts/verify-core.sh` e testes em `app/src/test/`).
3. **Smoke Tests Visuais:** Para componentes de renderização, utilize testes offscreen que capturam frames em `BufferedImage` e salvam em `build/screenshots/` para conferência de regressão visual.

---

## 7. Padrão de Mensagens de Commit

Utilize o padrão **Conventional Commits**:
`<tipo>(<escopo opcional>): <descrição no imperativo e em minúsculas>`

### Tipos Permitidos:
- `feat:` Nova funcionalidade para o usuário ou para a UI.
- `fix:` Correção de um bug ou de uma regressão visual/comportamental.
- `refactor:` Refatoração de código sem alteração de comportamento externo.
- `style:` Ajustes puramente visuais, formatação ou indentação de 4 espaços.
- `docs:` Criação ou atualização de documentação técnica.
- `test:` Adição ou correção de testes automatizados.
- `chore:` Ajustes em scripts de build, dependências ou tarefas operacionais.

### Exemplo de Mensagem:
```text
feat(dock): criar componente flutuante sobreposto a area de desenho

Implementa o FloatingDockComponent substituindo a barra de ferramentas fixa
superior. O dock agora flutua na parte superior central do canvas utilizando
JLayeredPane, mantendo as acoes do HomeController intactas.
```

