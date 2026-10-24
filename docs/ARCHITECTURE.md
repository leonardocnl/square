# Arquitetura

O SH3D segue o padrão MVC (Model-View-Controller):

```
model           dados: Home, Wall, Room, HomePieceOfFurniture, Level...
   ▲ listeners (o modelo avisa quando muda)
viewcontroller  regras de interação: PlanController, HomeController3D...
   │ pede views pela ViewFactory
   ▼
views           interface — no SH3D original: swing + j3d
                no Square:  SquareViewFactory (Compose + jMonkeyEngine)
io              abrir e salvar .sh3d (HomeFileRecorder)
```

## Módulos

| Módulo | Conteúdo | Estado |
|---|---|---|
| `vendor/sh3d` | Fonte original do SH3D 7.5, somente leitura | Importado |
| `sh3d-core` | Compila `model`, `viewcontroller`, `io`, `tools` do vendor | Compila (639 classes) |
| `app` | Square: Kotlin + Compose + jMonkeyEngine | Esqueleto |

## Como o Square se liga ao SH3D

1. O app cria `Home`, `UserPreferences` e o `HomeController` original,
   passando uma `SquareViewFactory`.
2. Os controllers chamam `viewFactory.createPlanView(...)`,
   `createView3D(...)` etc. Cada método devolve uma view do Square que
   implementa a interface esperada (`PlanView`, `View`, `DialogView`...).
3. Eventos do usuário (clique, arraste, tecla) vão da view para o controller
   original, que altera o modelo; o modelo notifica a view, que redesenha.

Assim, o comportamento (snapping, junções, undo/redo, detecção de cômodos)
é o do SH3D, sem reimplementação.

## Dependências do núcleo (verificadas)

`model`, `viewcontroller` e `io` usam apenas o JDK: `java.awt.geom` (geometria),
`javax.swing.undo` (undo/redo; faz parte do JDK, não exige interface Swing) e
`javax.xml`. Não dependem de Java3D nem de componentes Swing.
