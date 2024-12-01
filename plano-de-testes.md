# Plano de Testes - Tasty

## Ferramentas Utilizadas

- **Mockito**: Para criação de mocks e testes unitários.
- **JUnit 5**: Framework para execução de testes unitários.
- **REST-Assured**: Para testes de componente.
- **SonarLint**: Análise estática de código.
- **GitHub Actions**: Configuração de pipeline de CI/CD.
- **H2**: Banco de dados em memória para testes.

## Procedimentos

(...)

## Requisitos, Restrições e Configurações

(...)

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

| **Cenário**                                           | **Objetivo**                                                                           | **Método**                                     | **Entrada**                                         | **Saída Esperada**                                         |
| ----------------------------------------------------- | -------------------------------------------------------------------------------------- | ---------------------------------------------- | --------------------------------------------------- | ---------------------------------------------------------- |
| Buscar usuário por ID válido com token correspondente | Retornar um usuário válido e incluir o e-mail se o token corresponde ao usuário.       | getById(Long id)                               | ID de usuário válido (`1L`) e token correspondente. | Objeto `UserResponse` com ID, nome, foto e e-mail.         |
| Buscar usuário por ID válido com token diferente      | Retornar um usuário válido sem incluir o e-mail se o token não corresponde ao usuário. | getById(Long id)                               | ID de usuário válido (`1L`) e token diferente.      | Objeto `UserResponse` com ID, nome e foto, mas sem e-mail. |
| Buscar usuário por ID inexistente                     | Lançar exceção ao buscar usuário inexistente.                                          | getById(Long id)                               | ID inexistente (`-1L`).                             | Exceção `UserNotFoundException`.                           |
| Criar usuário com e-mail duplicado                    | Bloquear criação de usuário com e-mail duplicado.                                      | create(MultipartBody, UserCreateRequest)       | `UserCreateRequest` com e-mail existente.           | Exceção `ApiException` com status 409.                     |
| Criar usuário com dados válidos                       | Criar usuário e retornar autenticação com sucesso.                                     | create(MultipartBody, UserCreateRequest)       | `UserCreateRequest` com dados válidos.              | Objeto `UserAuthResponse`.                                 |
| Atualizar nome do usuário                             | Atualizar nome do usuário com dados válidos.                                           | update(Long, MultipartBody, UserUpdateRequest) | ID de usuário válido e novo nome.                   | Objeto `UserResponse` com nome atualizado.                 |
| Atualizar senha com senha atual incorreta             | Bloquear atualização de senha com senha atual incorreta.                               | update(Long, MultipartBody, UserUpdateRequest) | Senha incorreta no `UserUpdateRequest`.             | Exceção `ApiException` com status 401.                     |

#### Testes de Componente - `UserResource`

| **Cenário**                               | **Objetivo**                                                            | **Método HTTP** | **Endpoint**        | **Status Esperado** | **Entradas**                                                                | **Saída Esperada**                                                          |
| ----------------------------------------- | ----------------------------------------------------------------------- | --------------- | ------------------- | ------------------- | --------------------------------------------------------------------------- | --------------------------------------------------------------------------- |
| Criar novo usuário com dados válidos      | Salvar um novo usuário com dados válidos.                               | POST            | /api/users          | 200 (OK)            | `MultipartBody` com JSON e imagem válidos.                                  | Objeto `UserAuthResponse` com token de autenticação.                        |
| Criar novo usuário com dados inválidos    | Bloquear a criação de usuário ao enviar dados que violam as validações. | POST            | /api/users          | 400 (Bad Request)   | `MultipartBody` com JSON inválido (nome curto, e-mail mal formatado, etc.). | Exceção `ConstraintViolationException` com detalhes dos erros de validação. |
| Buscar usuário por ID válido              | Retornar usuário existente pelo ID.                                     | GET             | /api/users/{userId} | 200 (OK)            | ID de usuário válido.                                                       | Objeto `UserResponse` com dados do usuário.                                 |
| Atualizar senha com senha atual incorreta | Bloquear atualização de senha com senha atual incorreta.                | PUT             | /api/users/{userId} | 401 (Unauthorized)  | Senha incorreta no `UserUpdateRequest`.                                     | Mensagem "Senha incorreta."                                                 |
| Atualizar usuário autenticado             | Atualizar dados de um usuário autenticado.                              | PUT             | /api/users/{userId} | 200 (OK)            | `MultipartBody` com JSON válido e token de usuário autenticado.             | Objeto `UserResponse` atualizado.                                           |
| Atualizar usuário sem autenticação        | Bloquear atualização sem autenticação.                                  | PUT             | /api/users/{userId} | 401 (Unauthorized)  | Nenhum token de autenticação.                                               | Status 401 sem corpo de resposta.                                           |
| Criar usuário com e-mail duplicado        | Bloquear criação com e-mail duplicado.                                  | POST            | /api/users          | 409 (Conflict)      | `MultipartBody` com e-mail já existente.                                    | Mensagem "O e-mail informado já está cadastrado."                           |
