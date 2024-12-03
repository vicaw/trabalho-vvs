# Plano de Testes - Tasty

## Ferramentas Utilizadas

- **Mockito**: Para criação de mocks e testes unitários.
- **JUnit 5**: Framework para execução de testes unitários.
- **REST-Assured**: Para testes de componente.
- **SonarLint**: Análise estática de código.
- **GitHub Actions**: Configuração de pipeline de CI/CD.
- **H2**: Banco de dados em memória para testes.

## Procedimentos

- **Commits:**

  - Os commits são realizados diretamente na branch `master`.
  - Cada commit possui mensagens descritivas no seguinte formato:
    - Exemplo: `"Corrige mensagem de erro da exceção UserNotFoundException"`.

- **Controle de Qualidade:**
  - Durante o commit, o pipeline do **GitHub Actions** executa os testes automatizados.
  - Análise estática de código é feita com **SonarLint** no **VS Code**.

## Requisitos, Restrições e Configurações

- **Ambiente de Execução**:

  - O projeto foi desenvolvido utilizando o **Quarkus**, com o **JDK 11**
  - O build do projeto e a execução dos testes são realizados via **Maven**.

- **Banco de Dados**:

  - O banco de dados utilizado durante os testes é o **H2**.
  - Configuração no `application.properties` para o banco em memória:
    - `quarkus.datasource.db-kind = h2`
    - `quarkus.datasource.jdbc.url = jdbc:h2:mem:testdb;MODE=MySQL;`

- **Análise Estática**:
  - O **SonarLint** está configurado utilizando as regras padrão.
- **Ambiente de Integração**:
  - O pipeline de CI/CD é configurado no **GitHub Actions** e é acionado a cada `commit` para a branch `master`.
  - O pipeline configura o ambiente de execução com o **JDK 11** e executa os testes com **Maven**.
  - Para a realização dos testes que utilizam JWT, as **chaves públicas e privadas** para assinar e validar os tokens são armazenadas como **secrets** no GitHub Actions.

## Especificação de Casos de Teste

### Auth

#### Testes Unitários - `AuthService`

| **Cenário**                            | **Objetivo**                                               | **Método**                    | **Entrada**                                    | **Saída Esperada**                                                           |
| -------------------------------------- | ---------------------------------------------------------- | ----------------------------- | ---------------------------------------------- | ---------------------------------------------------------------------------- |
| Autenticar usuário não encontrado      | Lançar exceção ao tentar autenticar um e-mail inexistente. | authenticate(UserAuthRequest) | `UserAuthRequest` com e-mail não cadastrado.   | Exceção `ApiException` com mensagem "Seu usuário ou senha estão incorretos." |
| Autenticar usuário com senha incorreta | Lançar exceção ao autenticar com senha errada.             | authenticate(UserAuthRequest) | `UserAuthRequest` com senha incorreta.         | Exceção `ApiException` com mensagem "Seu usuário ou senha estão incorretos." |
| Autenticar usuário com sucesso         | Autenticar corretamente um usuário válido.                 | authenticate(UserAuthRequest) | `UserAuthRequest` com e-mail e senha corretos. | Objeto `UserAuthResponse` com token e informações do usuário.                |

#### Testes de Componente - `AuthResource`

| **Cenário**                                | **Objetivo**                                              | **Método HTTP** | **Endpoint**    | **Status Esperado** | **Entradas**                                            | **Saída Esperada**                                            |
| ------------------------------------------ | --------------------------------------------------------- | --------------- | --------------- | ------------------- | ------------------------------------------------------- | ------------------------------------------------------------- |
| Autenticar usuário com credenciais válidas | Autenticar um usuário existente com credenciais corretas. | POST            | /api/auth/login | 200 (OK)            | `UserAuthRequest` com e-mail e senha corretos.          | Objeto `UserAuthResponse` com token e informações do usuário. |
| Autenticar usuário com senha incorreta     | Bloquear autenticação com senha incorreta.                | POST            | /api/auth/login | 401 (Unauthorized)  | `UserAuthRequest` com e-mail correto e senha incorreta. | Mensagem de erro "Seu usuário ou senha estão incorretos."     |

---

### User

#### Testes Unitários - `UserService`

| **Cenário**                                           | **Objetivo**                                                                           | **Método**                                     | **Entrada**                                  | **Saída Esperada**                                                           |
| ----------------------------------------------------- | -------------------------------------------------------------------------------------- | ---------------------------------------------- | -------------------------------------------- | ---------------------------------------------------------------------------- |
| Buscar usuário por ID válido com token correspondente | Retornar um usuário válido e incluir o e-mail se o token corresponde ao usuário.       | getById(Long id)                               | ID de usuário válido e token correspondente. | Objeto `UserResponse` com ID, nome, foto e e-mail.                           |
| Buscar usuário por ID válido com token diferente      | Retornar um usuário válido sem incluir o e-mail se o token não corresponde ao usuário. | getById(Long id)                               | ID de usuário válido e token diferente.      | Objeto `UserResponse` com ID, nome e foto, mas sem e-mail.                   |
| Buscar usuário por ID inexistente                     | Lançar exceção ao buscar usuário inexistente.                                          | getById(Long id)                               | ID inexistente.                              | Exceção `UserNotFoundException`.                                             |
| Criar usuário com e-mail duplicado                    | Bloquear criação de usuário com e-mail duplicado.                                      | create(MultipartBody, UserCreateRequest)       | `UserCreateRequest` com e-mail existente.    | Exceção `ApiException` com mensagem "O e-mail informado já está cadastrado." |
| Criar usuário com dados válidos                       | Criar usuário e retornar autenticação com sucesso.                                     | create(MultipartBody, UserCreateRequest)       | `UserCreateRequest` com dados válidos.       | Objeto `UserAuthResponse`.                                                   |
| Atualizar nome do usuário                             | Atualizar nome do usuário com dados válidos.                                           | update(Long, MultipartBody, UserUpdateRequest) | ID de usuário válido e novo nome.            | Objeto `UserResponse` com nome atualizado.                                   |
| Atualizar senha com senha atual incorreta             | Bloquear atualização de senha com senha atual incorreta.                               | update(Long, MultipartBody, UserUpdateRequest) | Senha incorreta no `UserUpdateRequest`.      | Exceção `ApiException` com mensagem "Senha incorreta."                       |

#### Testes de Componente - `UserResource`

| **Cenário**                               | **Objetivo**                                                            | **Método HTTP** | **Endpoint**        | **Status Esperado** | **Entradas**                                                    | **Saída Esperada**                                   |
| ----------------------------------------- | ----------------------------------------------------------------------- | --------------- | ------------------- | ------------------- | --------------------------------------------------------------- | ---------------------------------------------------- |
| Criar novo usuário com dados válidos      | Salvar um novo usuário com dados válidos.                               | POST            | /api/users          | 200 (OK)            | `MultipartBody` com JSON e imagem válidos.                      | Objeto `UserAuthResponse` com token de autenticação. |
| Criar novo usuário com dados inválidos    | Bloquear a criação de usuário ao enviar dados que violam as validações. | POST            | /api/users          | 400 (Bad Request)   | `MultipartBody` com JSON com dados inválidos.                   | Mensagem com detalhes dos erros de validação.        |
| Buscar usuário por ID válido              | Retornar usuário existente pelo ID.                                     | GET             | /api/users/{userId} | 200 (OK)            | ID de usuário válido.                                           | Objeto `UserResponse` com dados do usuário.          |
| Atualizar senha com senha atual incorreta | Bloquear atualização de senha com senha atual incorreta.                | PUT             | /api/users/{userId} | 401 (Unauthorized)  | `MultipartBody` com senha incorreta no JSON.                    | Mensagem "Senha incorreta."                          |
| Atualizar usuário autenticado             | Atualizar dados de um usuário autenticado.                              | PUT             | /api/users/{userId} | 200 (OK)            | `MultipartBody` com JSON válido e token de usuário autenticado. | Objeto `UserResponse` atualizado.                    |
| Atualizar usuário sem autenticação        | Bloquear atualização sem autenticação.                                  | PUT             | /api/users/{userId} | 401 (Unauthorized)  | Nenhum token de autenticação.                                   | Status 401 sem corpo de resposta.                    |
| Criar usuário com e-mail duplicado        | Bloquear criação com e-mail duplicado.                                  | POST            | /api/users          | 409 (Conflict)      | `MultipartBody` com e-mail já existente no JSON.                | Mensagem "O e-mail informado já está cadastrado."    |

---

### Recipe

#### Testes Unitários - `RecipeService`

| **Cenário**                               | **Objetivo**                                                   | **Método**                                       | **Entrada**                                                          | **Saída Esperada**                                                     |
| ----------------------------------------- | -------------------------------------------------------------- | ------------------------------------------------ | -------------------------------------------------------------------- | ---------------------------------------------------------------------- |
| Listar receitas sem filtros               | Retornar todas as receitas sem aplicar filtros.                | list(Long, Integer, Integer, String)             | Nenhum filtro.                                                       | Objeto `RecipeListResponse` com todas as receitas.                     |
| Listar receitas com paginação e autor     | Retornar receitas de um autor específico com paginação.        | list(Long, Integer, Integer, String)             | ID de usuário válido com parâmetros de paginação.                    | Objeto `RecipeListResponse` com receitas do autor e paginação correta. |
| Buscar receita por ID existente           | Retornar uma receita específica pelo ID informado.             | getById(Long)                                    | ID de receita válido.                                                | Objeto `RecipeResponse` com detalhes da receita.                       |
| Buscar receita por ID inexistente         | Lançar exceção ao buscar receita com ID inexistente.           | getById(Long)                                    | ID de receita inválido.                                              | Exceção `RecipeNotFoundException`                                      |
| Criar receita com dados válidos           | Criar uma nova receita com informações válidas.                | create(MultipartBody, RecipeCreateRequest)       | `MultipartBody` com imagem e dados válidos no `RecipeCreateRequest`. | Objeto `RecipeResponse` com detalhes da nova receita.                  |
| Atualizar receita com dados válidos       | Atualizar uma receita existente com novas informações válidas. | update(Long, MultipartBody, RecipeUpdateRequest) | ID de receita válida e dados atualizados.                            | Objeto `RecipeResponse` com dados atualizados.                         |
| Deletar receita existente                 | Deletar uma receita existente com sucesso.                     | delete(Long)                                     | ID de receita válida.                                                | Nenhuma (ação bem-sucedida).                                           |
| Buscar receitas com termo de busca válido | Retornar receitas que correspondem ao termo de busca.          | searchRecipe(String, Integer, Integer, String)   | Parâmetros de pesquisa válidos.                                      | Objeto `RecipeListResponse` com receitas correspondentes.              |

#### Testes de Componente - `RecipeResource`

| **Cenário**                                 | **Objetivo**                                                                     | **Método HTTP** | **Endpoint**        | **Status Esperado** | **Entradas**                                                  | **Saída Esperada**                                                    |
| ------------------------------------------- | -------------------------------------------------------------------------------- | --------------- | ------------------- | ------------------- | ------------------------------------------------------------- | --------------------------------------------------------------------- |
| Listar todas as receitas                    | Retornar todas as receitas sem filtros.                                          | GET             | /api/recipes        | 200 (OK)            | Sem filtros.                                                  | Objeto `RecipeListResponse` com todas as receitas.                    |
| Listar receitas com filtros                 | Retornar receitas de um autor específico com paginação e ordenação.              | GET             | /api/recipes        | 200 (OK)            | ID de receita e Query Params válidos.                         | Objeto `RecipeListResponse` filtrado.                                 |
| Buscar receita por ID válido                | Retornar detalhes de uma receita pelo ID.                                        | GET             | /api/recipes/{id}   | 200 (OK)            | ID de receita válido.                                         | Objeto `RecipeResponse` com detalhes da receita.                      |
| Buscar receita por ID inexistente           | Lançar erro ao buscar receita inexistente.                                       | GET             | /api/recipes/{id}   | 404 (Not Found)     | ID de receita inválido.                                       | Mensagem de erro "Nenhuma receita foi encontrada com o ID informado." |
| Criar receita com dados válidos             | Criar uma nova receita com informações corretas.                                 | POST            | /api/recipes        | 200 (OK)            | `MultipartBody` com imagem e JSON com dados válidos .         | Objeto `RecipeResponse` com a nova receita.                           |
| Criar receita com dados inválidos           | Bloquear a criação de uma nova receita ao enviar dados que violam as validações. | POST            | /api/userecipes     | 400 (Bad Request)   | `MultipartBody` com JSON com dados inválidos.                 | Mensagem com detalhes dos erros de validação.                         |
| Atualizar receita com dados válidos         | Atualizar uma receita existente com sucesso.                                     | PUT             | /api/recipes/{id}   | 200 (OK)            | ID de receita válido e `MultipartBody` com dados atualizados. | Objeto `RecipeResponse` atualizado.                                   |
| Deletar receita existente                   | Deletar uma receita existente com sucesso.                                       | DELETE          | /api/recipes/{id}   | 200 (OK)            | ID de receita válido.                                         | Nenhuma (ação bem-sucedida).                                          |
| Deletar receita de outro usuário            | Bloquear exclusão de receita de outro usuário.                                   | DELETE          | /api/recipes/{id}   | 403 (Forbidden)     | ID de usuário válido, mas token de outro usuário.             | Mensagem de erro "Você não pode deletar receitas de outros usuários." |
| Buscar receitas com termo de busca válido   | Retornar receitas correspondentes ao termo de busca.                             | GET             | /api/recipes/search | 200 (OK)            | Query Params válidos.                                         | Objeto `RecipeListResponse` com receitas correspondentes.             |
| Buscar receitas com termo de busca inválido | Lançar erro ao buscar com termo inválido.                                        | GET             | /api/recipes/search | 400 (Bad Request)   | Query Params inválidos.                                       | Mensagem de erro sobre o termo de busca.                              |

---

### Rating

#### Testes Unitários - `RatingService`

| **Cenário**                                   | **Objetivo**                                                              | **Método**                                | **Entrada**                                                         | **Saída Esperada**                                                            |
| --------------------------------------------- | ------------------------------------------------------------------------- | ----------------------------------------- | ------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| Listar avaliações de uma receita              | Retornar todas as avaliações de uma receita com paginação e ordenação.    | listRecipeRatings(Long, int, int, String) | ID de receita e parâmetros de pesquisa válidos.                     | Objeto `RecipeRatingsResponse` com as avaliações e metadados.                 |
| Postar avaliação com dados válidos            | Criar uma nova avaliação com informações válidas.                         | postRating(Long, RatingCreateRequest)     | ID de receita válido e `RatingCreateRequest` com comentário e nota. | Objeto `RatingResponse` com detalhes da nova avaliação.                       |
| Postar avaliação duplicada                    | Bloquear criação de avaliação se o usuário já avaliou a receita.          | postRating(Long, RatingCreateRequest)     | ID de receita válido e `RatingCreateRequest` com comentário e nota. | Exceção `ApiException` com mensagem "Este usuário já comentou nesta receita." |
| Obter metadados das avaliações de uma receita | Retornar média e contagem de avaliações de uma receita.                   | getRatingInfo(Long)                       | ID de receita válido.                                               | Objeto `RatingInfoResponse` com média e contagem.                             |
| Obter informações de receita inexistente      | Lançar exceção ao tentar obter informações de uma receita não encontrada. | getRatingInfo(Long)                       | ID de receita inválido.                                             | Exceção `RecipeNotFoundException`.                                            |
| Obter avaliação específica de um usuário      | Retornar a avaliação de um usuário para uma receita.                      | getUserRating(Long, Long)                 | IDs de usuário e de receita válidos.                                | Objeto `RatingResponse` com detalhes da avaliação.                            |

#### Testes de Componente - `RatingResource`

| **Cenário**                                   | **Objetivo**                                                                  | **Método HTTP** | **Endpoint**                     | **Status Esperado** | **Entradas**                                      | **Saída Esperada**                                         |
| --------------------------------------------- | ----------------------------------------------------------------------------- | --------------- | -------------------------------- | ------------------- | ------------------------------------------------- | ---------------------------------------------------------- |
| Listar avaliações de uma receita              | Retornar todas as avaliações de uma receita com paginação e ordenação.        | GET             | /api/ratings/{recipeId}          | 200 (OK)            | ID de receita válido com os Query Params válidos. | Objeto `RecipeRatingsResponse` com avaliações e metadados. |
| Postar nova avaliação                         | Criar uma nova avaliação com informações corretas.                            | POST            | /api/ratings/{recipeId}          | 200 (OK)            | JSON com comentário e nota válidos.               | Objeto `RatingResponse` com detalhes da avaliação.         |
| Postar nova avaliação com dados inválidos     | Bloquear a criação de uma avaliação ao enviar dados que violam as validações. | POST            | /api/ratings/{recipeId}          | 400 (Bad Request)   | JSON com dados inválidos.                         | Mensagem com detalhes dos erros de validação.              |
| Obter metadados das avaliações de uma receita | Retornar média e contagem de avaliações de uma receita.                       | GET             | /api/ratings/{recipeId}/info     | 200 (OK)            | ID de receita válido.                             | Objeto `RatingInfoResponse` com média e contagem.          |
| Obter avaliação de um usuário                 | Retornar a avaliação específica de um usuário para uma receita.               | GET             | /api/ratings/{recipeId}/{userId} | 200 (OK)            | IDs de usuário e de receita válidos.              | Objeto `RatingResponse` com detalhes da avaliação.         |
