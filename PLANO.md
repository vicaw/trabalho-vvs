# Plano de Testes - Tasty

## Ferramentas Utilizadas

- **Mockito**: Para criação de mocks e testes unitários.
- **JUnit 5**: Framework para execução de testes unitários.
- **REST-Assured**: Para testes de componente.
- **SonarLint**: Análise estática de código.
- **GitHub Actions**: Configuração de pipeline de CI/CD.
- **H2**: Banco de dados em memória para testes.

---

## Procedimentos

(...)

---

## Requisitos, Restrições e Configurações

(...)

---

## Matriz de Funcionalidades versus Testes

### Testes Unitários

| **Funcionalidade**         | **Objetivo do Teste**                                                       | **Cenário Testado**                                 | **Saída Esperada**                                                |
| -------------------------- | --------------------------------------------------------------------------- | --------------------------------------------------- | ----------------------------------------------------------------- |
| **Cadastro de Usuário**    | Validar a lógica de criação de usuários no nível de serviço e persistência. | Dados de entrada: `displayName` e `photoUrl`.       | Usuário criado corretamente no banco de dados com `id` gerado.    |
| **Atualização de Usuário** | Testar a atualização dos dados do usuário no serviço e repositório.         | Atualização de `displayName` e/ou `photoUrl`.       | Dados atualizados corretamente no banco e retorno do novo modelo. |
| **Login**                  | Verificar se o serviço gera um JWT válido para credenciais corretas.        | Email e senha válidos.                              | JWT gerado corretamente.                                          |
| **Criação de Receita**     | Validar a lógica de criação de receitas, associando o autor via JWT.        | Dados: ingredientes, modo de preparo, autor do JWT. | Receita persistida no banco com `id` e timestamp.                 |
| **Atualização de Receita** | Testar a atualização dos dados da receita no serviço.                       | Dados atualizados da receita.                       | Receita modificada e persistida no banco corretamente.            |
| **Avaliação de Receita**   | Validar a criação de avaliações com nota e comentário.                      | Dados: `recipeId`, nota e comentário.               | Avaliação registrada corretamente no banco.                       |

### Testes de Componentes (API)

| **Funcionalidade**                                                          | **Objetivo do Teste**                                                          | **Cenário Testado**                                              | **Saída Esperada**                                                          |
| --------------------------------------------------------------------------- | ------------------------------------------------------------------------------ | ---------------------------------------------------------------- | --------------------------------------------------------------------------- |
| **Cadastro de Usuário (POST /api/users)**                                   | Testar o endpoint para criação de usuários com validação de dados.             | JSON com `displayName` e `photoUrl`.                             | Retorno 201 (Created) e JSON com `id`, `displayName` e `photoUrl`.          |
| **Consulta de Usuário (GET /api/users/{id})**                               | Testar a recuperação de usuários pelo endpoint.                                | Requisição GET com `userId`.                                     | Retorno 200 (OK) e JSON do usuário encontrado.                              |
| **Atualização de Usuário (PUT /api/users/{id})**                            | Verificar se o endpoint atualiza corretamente os dados do usuário autenticado. | Envio de JWT e JSON com novos dados (`displayName`, `photoUrl`). | Retorno 200 (OK) e JSON do usuário atualizado.                              |
| **Login (POST /api/authenticate)**                                          | Validar a autenticação e retorno de JWT.                                       | JSON com `email` e `password`.                                   | Retorno 200 (OK) e JSON com o token JWT.                                    |
| **Criação de Receita (POST /api/recipes)**                                  | Testar a criação de receitas pelo endpoint autenticado.                        | JWT e JSON com ingredientes e modo de preparo.                   | Retorno 201 (Created) e JSON da receita criada.                             |
| **Consulta de Receita (GET /api/recipes/{id})**                             | Verificar a recuperação de receitas específicas pelo endpoint.                 | GET com `recipeId`.                                              | Retorno 200 (OK) e JSON da receita.                                         |
| **Atualização de Receita (PUT /api/recipes/{id})**                          | Testar se o endpoint atualiza receitas corretamente.                           | JWT e JSON com dados atualizados.                                | Retorno 200 (OK) e JSON da receita atualizada.                              |
| **Avaliação de Receita (POST /api/ratings)**                                | Validar o registro de avaliações via endpoint autenticado.                     | JWT e JSON com `recipeId`, nota e comentário.                    | Retorno 201 (Created) e JSON da avaliação registrada.                       |
| **Listagem de Avaliações (GET /api/ratings/recipes/{id})**                  | Testar a listagem de avaliações para uma receita.                              | GET com `recipeId` e paginação opcional.                         | Retorno 200 (OK) com JSON das avaliações e metadados de paginação.          |
| **Consulta de Média de Avaliações (GET /api/ratings/recipes/{id}/average)** | Verificar se o endpoint calcula e retorna a média corretamente.                | GET para média de uma receita.                                   | Retorno 200 (OK) com média e total de avaliações.                           |
| **Busca de Receitas (GET /api/search?q={query})**                           | Testar a busca com validação de parâmetros e paginação.                        | Query params: `q`, `pagesize`, `page`, `orderBy`.                | Retorno 200 (OK) com JSON contendo lista de receitas e indicador `hasMore`. |
