# Portaria

Gerenciador de eventos com inscrições e check-in presencial.

Projeto Integrador I — IFSC Gaspar.

---

## O que o sistema faz

O núcleo é uma afirmação verificável: **esta pessoa esteve fisicamente aqui**.

1. O organizador cria um evento e recebe um **link público de inscrição**.
2. O participante se inscreve sem login e recebe um **código de 6 caracteres**.
3. Na entrada, o operador digita o código no **painel de portaria**.
4. O sistema responde de uma entre três formas — liberado, já registrado, ou não
   encontrado — e o contador de presentes atualiza na hora.

Tudo o que vem depois (certificado com carga horária, retirada de kit, sorteio,
prestação de contas) é consequência dessa presença confirmada, e não altera
este núcleo.

---

## Convenção de idioma

O código é escrito em **português brasileiro**. O inglês aparece apenas onde é
vocabulário reservado de alguma tecnologia, e nesses casos traduzir quebraria o
funcionamento:

| Onde | Exemplo | Por quê |
|---|---|---|
| Anotações e tipos do Spring / JPA | `@Entity`, `SecurityFilterChain` | Nomes definidos pelo framework |
| Prefixos de consulta derivada | `findByCodigo`, `countByEventoId` | O Spring Data monta o SQL a partir do prefixo |
| Convenção JavaBean | `getNome()`, `setNome()` | JPA e Thymeleaf leem propriedades por ela |
| Sufixo de teste | `ServicoDeCheckInTest` | O Surefire só encontra classes terminadas em `Test` |
| Chaves de configuração | `spring.datasource.url` | Definidas pelo Spring Boot |
| Palavras da linguagem | `class`, `public`, `SELECT` | Java e SQL |

Onde a consulta é escrita à mão com `@Query`, o nome do método é livre e está em
português — por exemplo `buscarPorCodigoComEvento`.

**check-in** foi mantido por ser o termo que o cliente real usa: organizador de
evento fala "fazer o check-in", não "registrar a presença".

---

## Como rodar

Requisito único: **JDK 21**. Não precisa instalar banco de dados.

```bash
mvn spring-boot:run
```

Ou executar a classe `AplicacaoPortaria` pela IDE.

Depois, abrir <http://localhost:8080>.

| | |
|---|---|
| Usuário | `admin` |
| Senha | `portaria123` |

A aplicação cria sozinha um evento de exemplo com quatro inscritos na primeira
execução, para haver o que demonstrar imediatamente.

### Roteiro de demonstração (3 minutos)

1. Entrar em `/login`.
2. Abrir o evento de exemplo e **copiar o link público**.
3. Abrir o link em uma aba anônima e fazer uma inscrição — anote o código.
4. Voltar ao painel e clicar em **Abrir painel de portaria**.
5. Digitar o código e pressionar Enter → entrada liberada, contador sobe.
6. **Digitar o mesmo código de novo** → "já registrado", e o contador *não* sobe.
   Este é o ponto técnico central da entrega.
7. Digitar um código inventado → não encontrado.

O banco pode ser inspecionado ao vivo em `/h2-console`
(URL JDBC `jdbc:h2:file:./data/portaria`, usuário `sa`, senha em branco).

---

## Decisões de arquitetura

**Monolito modular, um pacote por domínio.** `evento`, `inscricao`, `checkin`.
Microsserviços foram descartados: não há problema de escala nem de times
independentes que os justifique, e o custo operacional destruiria o prazo.

**Renderização no servidor (Thymeleaf) com htmx, sem SPA.** Uma SPA exigiria
projeto separado, CORS, autenticação por token, build próprio e estado
duplicado — cada tela viraria duas tarefas. O htmx entrega a interatividade
necessária (o painel de portaria se atualiza sem recarregar) mantendo um único
artefato de implantação.

**Idempotência garantida no banco, não só no código.** A tabela `check_in` tem
`UNIQUE (inscricao_id)`. Uma consulta prévia cobre o caso comum; a restrição
cobre a corrida entre dois operadores escaneando em portões diferentes. Só a
consulta prévia seria um *check-then-act*.

**A gravação vive em transação própria** (`RegistroDePresenca`). Quando a
restrição dispara, a transação corrente fica marcada como *rollback-only*;
isolando a gravação, o serviço captura a violação já fora da transação morta e
continua atendendo a fila.

**Alfabeto do código sem caracteres ambíguos** (sem `0`/`O`, sem `1`/`I`/`L`).
Sob chuva, no escuro e com fila andando, um caractere ambíguo custa mais tempo
que um código um pouco maior.

**Leitor USB sai de graça.** Leitores de código de barras se comportam como
teclado (HID): digitam o código e pressionam Enter. O campo de digitação manual
*é* a integração com o hardware — sem driver, sem SDK, sem dependência.

**Migrações com Flyway, em SQL padrão.** O esquema é criado exclusivamente pelas
migrações e o Hibernate roda em `validate`. O SQL evita tipos proprietários, de
modo que a mesma migração roda no H2 hoje e no PostgreSQL depois.

---

## Trocar H2 por PostgreSQL

Não exige mudança de código. Em `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/portaria
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.username=portaria
spring.datasource.password=portaria
```

E no `pom.xml`, trocar a dependência `com.h2database:h2` por
`org.postgresql:postgresql`.

---

## Backlog priorizado

Deliberadamente fora desta entrega. Cada item tem justificativa de domínio — não
é lista de desejos:

| Item | Por quê |
|---|---|
| **Cadastro na portaria** | Metade do público de encontro de motoclube chega sem inscrição prévia. |
| **Check-in coletivo** | Caravana chega com 12 pessoas; a fila não pode parar. |
| **Fila offline** (IndexedDB + código assinado por HMAC) | Encontro acontece em pátio e área rural, onde não há sinal confiável. |
| **QR code** | Camada sobre o código curto. Exige HTTPS: a câmera do navegador só funciona em contexto seguro. |
| **Capacidade sob concorrência** | Hoje é *check-then-act*; duas requisições podem disputar a última vaga. |
| **Comprovante conforme o tipo de evento** | Certificado com carga horária (acadêmico), comprovante simples (cultural), nenhum. Variação por polimorfismo, não por condicional. |
| **Papéis e múltiplos operadores** | Hoje há um único usuário mantido em memória. |
| **Retenção de dados / LGPD** | Definir base legal e prazo de descarte dos dados pessoais coletados. |
