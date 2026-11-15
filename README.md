# Square

**Square** é um fork desktop modernizado do [Sweet Home 3D](https://www.sweethome3d.com) (versão base 7.5), projetado para oferecer uma experiência profissional, fluida e contemporânea para design de interiores e arquitetura residencial, preservando 100% da compatibilidade funcional e comportamental do software original.

---

## Proposta e Diretrizes Centrais

O Sweet Home 3D consolidou-se como uma das ferramentas open-source mais estáveis e confiáveis para modelagem de plantas baixas e visualização 3D. No entanto, sua interface gráfica reflete convenções do início dos anos 2000 (modais bloqueantes, barras de ferramentas saturadas, ausência de modo escuro e rigidez visual).

O **Square** redefine essa experiência com foco em produtividade moderna, ergonomia visual e desempenho:

- **Compatibilidade Inegociável:** Paridade binária e comportamental total com arquivos de projeto `.sh3d` e bibliotecas de catálogo `.sh3f`, texturas `.sh3t` e idiomas `.sh3l`. Nenhum projeto legado deixa de abrir ou salvar.
- **Ecossistema Java Nativo:** Mantém o core de regras de negócio em Java com interface modernizada diretamente em **Java/Swing** com suporte nativo a aceleração de hardware e telas High-DPI.
- **Passos Incrementais e Reversíveis:** Mudanças modulares apoiadas por testes automatizados, evitando quebras acidentais ou refatorações destrutivas.

---

## Recursos e Evolução Visual

| Recurso | Sweet Home 3D Tradicional | Square |
| :--- | :--- | :--- |
| **Look & Feel** | Metal / Nimbus / GTK retrô | **FlatLaf Moderno** com tema Claro e Escuro dinâmico (`Ctrl+Shift+T`) |
| **Barra de Ferramentas** | Linha fixa de botões topo | **Dock Flutuante de Ferramentas** (estilo Figma / macOS) |
| **Edição de Propriedades** | Diálogos modais bloqueantes | **Inspetores contextuais em tempo real** no painel lateral |
| **Acesso a Comandos** | Menus tradicionais aninhados | **Paleta de Comandos Universal** (`Ctrl+K`) com busca difusa |
| **Auxílio de Desenho** | Grade estática com imã padrão | **Guias inteligentes** de alinhamento e distância em tempo real |
| **Persistência** | Salvamento sob demanda manual | **Salvamento contínuo em segundo plano** e histórico de undo/redo robusto |

---

## Arquitetura do Repositório

```text
square/
├── app/                  # Módulo principal da aplicação, Look & Feel e UI moderna
│   └── src/main/java/br/com/square/ui/
│       └── theme/        # ThemeManager, tokens de cores semânticos e transição
├── sh3d-core/            # Núcleo de domínio headless (model, viewcontroller, io, tools)
├── vendor/sh3d/          # Código canônico e recursos do Sweet Home 3D 7.5
├── archive/              # Protótipos anteriores arquivados com segurança
├── docs/                 # Documentação arquitetural viva, ADRs e Roadmap
│   ├── ADRS/             # Registros de Decisões Arquiteturais
│   ├── ARCHITECTURE.md   # Diagnóstico de acoplamento e arquitetura-alvo
│   ├── CODING_STANDARDS.md # Convenções de código (4 espaços, Clean Code, SOLID)
│   ├── ROADMAP.md        # Marcos pequenos sequenciais de entrega
│   ├── VERIFICATION.md   # Protocolo obrigatório de testes
│   └── VISION.md         # Escopo, metas e critérios de sucesso
└── scripts/
    └── verify-core.sh    # Verificação headless do núcleo sem dependências externas
```

---

## Como Compilar e Executar

### Pré-requisitos
- **Java Development Kit (JDK):** Versão 25 instalada (`java --version`).
- **Sistema Operacional:** Linux (Fedora, Ubuntu, Arch), macOS ou Windows.

### 1. Validação do Núcleo Headless (Sem Dependências de UI)
Valida a integridade do modelo de dados, cálculo de áreas, junção de paredes e serialização de arquivos `.sh3d`:
```bash
./scripts/verify-core.sh
```

### 2. Compilar Todos os Módulos
```bash
./gradlew :app:classes :app:testClasses
```

### 3. Executar o Square
```bash
./gradlew :app:run
```

*(Opcional: passe o caminho de um arquivo `.sh3d` diretamente como argumento: `./gradlew :app:run --args="caminho/do/projeto.sh3d"`)*

---

## Roadmap de Execução

- [x] **Marco 0:** Fundação, Estabilidade do Build no JDK 25 e Protocolo de Verificação.
- [x] **Marco 1:** Look & Feel Moderno (FlatLaf), Suporte a Dark Mode e Adaptação da Planta 2D.
- [x] **Marco 2:** Dock Flutuante de Ferramentas (Estilo Figma) sobreposto ao canvas.
- [x] **Marco 3:** Inspetores de Propriedades em Tempo Real (eliminação de modais bloqueantes).
- [x] **Marco 4:** Paleta de Comandos Universal (`Ctrl+K`).
- [ ] **Marco 5:** Guias Inteligentes e Réguas Dinâmicas de Distância na Planta 2D.
- [ ] **Marco 6:** Salvamento Contínuo e Histórico de Undo/Redo Granular.
- [ ] **Marco 7:** Motor de Renderização 3D em Tempo Real Modernizado.

Para detalhes completos de cada fase, consulte [`docs/ROADMAP.md`](docs/ROADMAP.md).

---

## Licença

Distribuído sob a licença **GNU General Public License v2.0** (ou posterior), em conformidade com o Sweet Home 3D. Consulte [`vendor/sh3d/COPYING.TXT`](vendor/sh3d/COPYING.TXT) para os termos completos.
