# ADR-002: Compatibilidade Estrita com Arquivos .sh3d e Bibliotecas de Catálogo

- **Status:** Aceito
- **Data:** 2026-11-15
- **Decisores:** Arquiteto de Software e Engenheiro Principal do Square

---

## Contexto

A comunidade e os usuários do Sweet Home 3D acumulam mais de duas décadas de projetos arquiteturais armazenados no formato `.sh3d`, além de vastos repositórios de bibliotecas de modelos 3D (`.sh3f`), texturas (`.sh3t`) e pacotes de internacionalização (`.sh3l`).

Um dos maiores riscos de falha na modernização de um software consolidado é a ruptura da compatibilidade retroativa: se o novo software for incapaz de carregar arquivos históricos ou gerar arquivos legíveis por versões anteriores, ele cria um silo fechado e gera desconfiança nos usuários.

No Sweet Home 3D, o formato `.sh3d` é implementado pela classe `com.eteks.sweethome3d.io.HomeFileRecorder`:
- Consiste em um contêiner ZIP contendo um descritor de estado estruturado (`Home.xml` ou fluxo de objetos serializados Java) e diretórios de recursos compactados (imagens de textura, modelos 3D em formato `.obj` e ícones).
- A classe `com.eteks.sweethome3d.io.FileUserPreferences` é responsável pelo parsing e indexação de bibliotecas externas `.sh3f`.

## Decisão

1. **Paridade Binária e Funcional com `.sh3d`:** O **square** manterá suporte inegociável à leitura e escrita de arquivos `.sh3d`, preservando a compatibilidade bidirecional com o Sweet Home 3D 7.5. Um projeto salvo no **square** continuará podendo ser aberto no Sweet Home 3D original e vice-versa.
2. **Suporte Nativo a Bibliotecas de Móveis e Texturas:** As bibliotecas `.sh3f`, `.sh3t` e `.sh3l` continuarão sendo carregadas através dos mecanismos canônicos do `FileUserPreferences` e `FurnitureCatalog`.
3. **Extensão de Metadados Não Intrusiva:** Quaisquer metadados novos específicos das inovações do **square** (como posição do dock flutuante, estado de visualização do inspetor lateral ou preferências de guias inteligentes) serão salvos utilizando a API de propriedades customizadas já existente em `Home.setProperty(String name, String value)` ou no armazenamento de preferências do usuário (`UserPreferences`), garantindo que o parser do SH3D padrão ignore essas chaves desconhecidas sem falhar.
4. **Despriorização de Plugins Legados:** O sistema de plugins legados de terceiros (`com.eteks.sweethome3d.plugin.PluginManager`) não é um requisito prioritário na Fase 1. Se componentes visuais de plugins antigos colidirem com os novos componentes do **square**, a modernização da interface prevalecerá.

## Consequências

### Positivas
- **Adoção Imediata sem Fricção:** Usuários atuais podem migrar seus projetos de trabalho para o **square** no primeiro dia de uso com garantia total contra perda de dados.
- **Interoperabilidade Contínua:** Escritórios e equipes podem utilizar o **square** simultaneamente com colegas que ainda utilizem o Sweet Home 3D padrão.
- **Acesso Direto ao Acervo Existente:** Dezenas de milhares de móveis 3D e texturas criados pela comunidade mundial estão imediatamente disponíveis.

### Negativas / Desafios
- **Restrição de Evolução do Esquema:** O formato estrutural do arquivo não pode ser arbitrariamente alterado ou convertido para JSON/SQLite sem que um conversor de compatibilidade seja rigorosamente mantido.
- **Necessidade de Testes Contínuos de Regressão:** Toda modificação em classes do pacote `com.eteks.sweethome3d.model` deve ser verificada com o teste de fumaça de I/O (`scripts/verify-core.sh`) para garantir que nenhuma propriedade seja corrompida.

