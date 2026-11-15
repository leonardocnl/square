# Protocolo de Verificação Operacional — square

Este documento estabelece o protocolo mandatório de verificação de integridade a ser executado após qualquer modificação de código ou configuração no repositório **square**. Nenhuma tarefa pode ser marcada como concluída sem o cumprimento integral deste checklist.

---

## 1. Comandos Obrigatórios de Compilação e Integridade

Execute a sequência na raiz do projeto na ordem indicada:

### Passo 1: Verificação Rápida do Núcleo Headless
Valida compilação do core e ciclo de escrita/releitura de `.sh3d` diretamente no JDK (sem intermediários):
```bash
./scripts/verify-core.sh
```
- **Critério de Aceitação:** Saída terminando em:
  `walls=2 rooms=1`
  `joined=true`
  `area=120000.0`
  `OK — núcleo compilado em /tmp/.../classes`
- **Código de Saída Esperado:** `0`.

### Passo 2: Empacotamento do Core via Gradle
Gera o artefato JAR do módulo `:sh3d-core`:
```bash
./gradlew :sh3d-core:jar
```
- **Critério de Aceitação:** `BUILD SUCCESSFUL` sem erros de compilação.

### Passo 3: Compilação das Classes e Testes
Valida a árvore completa de compilação do projeto:
```bash
./gradlew :app:classes :app:testClasses
```
- **Critério de Aceitação:** `BUILD SUCCESSFUL`.

---

## 2. Checklist de Fumaça (Smoke Test) da Interface

Após a compilação, execute o aplicativo localmente para conferência interativa e visual:

```bash
./gradlew :app:run
```

Execute manualmente o checklist de 7 pontos:

| Item | Área | Ação do Teste | Comportamento Esperado | Status |
|:---:|---|---|---|:---:|
| 1 | **Inicialização** | Iniciar `./gradlew :app:run` | A janela principal abre em menos de 3s sem exceções fatais (`NullPointerException`, `UnsatisfiedLinkError`) no terminal. | [ ] |
| 2 | **Quatro Painéis** | Inspeção visual inicial | Os 4 quadrantes (Catálogo, Tabela, Planta 2D e View 3D) estão visíveis e ajustáveis via divisores. | [ ] |
| 3 | **Criação 2D** | Selecionar ferramenta de Parede e traçar 2 paredes no canvas | As paredes são desenhadas com linhas nítidas; cotas e ângulos aparecem durante o arrasto. | [ ] |
| 4 | **Navegação** | Zoom com roda do mouse e Pan com botão do meio ou Espaço+Arraste | O canvas move e amplia suavemente mantendo a fidelidade das coordenadas. | [ ] |
| 5 | **Tema / Dark Mode** | Alternar o tema do sistema / opção no menu | Todos os componentes adaptam fundo, bordas e fontes, mantendo alto contraste no canvas 2D. | [ ] |
| 6 | **Inspetor de Propriedades** | Clicar em uma parede traçada | O inspetor exibe instantaneamente espessura, comprimento, altura e cores sem disparar modal bloqueante. | [ ] |
| 7 | **Undo / Redo** | Pressionar `Ctrl+Z` e `Ctrl+Y` | A última ação é revertida e reaplicada de forma limpa no canvas e na árvore do modelo. | [ ] |

---

## 3. Validação Obrigatória de Compatibilidade de I/O

A integridade do formato de arquivo do Sweet Home 3D é um compromisso inegociável do projeto.

### 3.1 Teste de Gravação e Releitura de `.sh3d`
Gere um arquivo de teste e valide a abertura:
```bash
# Executa teste automatizado de I/O em modo headless
java -Djava.awt.headless=true -cp "vendor/sh3d/src:scripts" scripts/CoreSmoke.java /tmp/square-io-smoke.sh3d
```
Verifique se o arquivo `/tmp/square-io-smoke.sh3d` foi gerado e confira sua estrutura interna de ZIP:
```bash
unzip -l /tmp/square-io-smoke.sh3d
```
- **Critério de Aceitação:** O arquivo deve conter `Home.xml` (ou entradas serializadas equivalentes) compatíveis com a especificação do Sweet Home 3D 7.5.

### 3.2 Teste de Carregamento de Biblioteca de Móveis (`.sh3f`)
Valide que a importação de catálogo não lança exceções no `FileUserPreferences`:
1. No aplicativo em execução, abra o menu **Móveis > Importar biblioteca de móveis...** (ou use a paleta de comandos `Ctrl+K > Importar biblioteca`).
2. Selecione um arquivo `.sh3f` de teste.
3. **Critério de Aceitação:** As categorias e peças contidas no arquivo devem aparecer imediatamente na árvore do Catálogo de Móveis, com seus respectivos ícones renderizados e disponíveis para inserção na planta.

---

## 4. O que Fazer em Caso de Falha de Verificação

Se qualquer etapa deste protocolo falhar:
1. **Interrompa a tarefa imediatamente.** Não tente avançar para a próxima etapa ou comitar mudanças.
2. Analise o log de erro exato. Se for falha de compilação, verifique referências de tipos e codificação Latin-1/UTF-8.
3. Se o erro for regressão em cálculos geométricos ou I/O, compare o diff com o código original em `vendor/sh3d/src`.
4. Reverta a alteração causadora via Git (`git checkout` ou `git restore`) e formule uma abordagem corrigida mantendo as invariantes do sistema.

