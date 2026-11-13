# Portaria

Gerenciador de eventos com inscrições e check-in presencial.

Projeto Integrador I — IFSC Gaspar.

**Documentação**

- [Manual do Usuário](docs/manual-do-usuario.md) · [versão em PDF](docs/manual-do-usuario.pdf)
  — como usar o sistema, tela a tela, para organizador, colaborador e participante.
- [Arquitetura](docs/arquitetura.md) — diagramas do fluxo de check-in, do modelo de
  dados, das dependências entre pacotes e dos perfis de execução.
- Este README — instalação, decisões técnicas e o que ficou para o PI-II.

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

## Stacks Utilizadas

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java (Temurin) | **25** |
| Framework | Spring Boot | **4.1.2** |
| Telas | Spring MVC + Thymeleaf (gera HTML5) | BOM |
| Estilo | CSS próprio, sem framework | — |
| Scripts | JavaScript padrão, sem framework nem build | — |
| Interatividade | htmx (WebJar servido pela aplicação) | **1.9.12** |
| Segurança | Spring Security | BOM |
| Persistência | Spring Data JPA + Hibernate | BOM |
| Migrações | Flyway + `flyway-database-postgresql` | BOM |
| Banco (dev) | H2 em arquivo | **2.3.232** |
| Banco (teste) | H2 em memória | **2.3.232** |
| Banco (demonstração e produção) | PostgreSQL | **18** *(driver pela BOM)* |
| Build | Maven, via wrapper (`mvnw`) | **3.9.9** |
| Banco em contêiner *(alternativa ao cluster)* | Docker, imagem `postgres` | **18** |

O Docker é a única linha que não é dependência da aplicação: ele aparece como
forma alternativa de prover o PostgreSQL durante o desenvolvimento, no lugar de
instalar o banco na máquina. **A aplicação não é distribuída como contêiner** —
não há `Dockerfile` no projeto e o artefato continua sendo o jar do Spring Boot.
Containerizar a própria aplicação é assunto de PI-II.

Os números em negrito são os que o `pom.xml` fixa. **BOM** quer dizer que a
versão vem do `spring-boot-starter-parent` 4.1.2, que a resolve e a mantém
coerente com todo o resto; `mvn dependency:tree` mostra as resolvidas.

Elas não estão escritas aqui de propósito. Número copiado à mão para dentro de
um texto envelhece em silêncio: a dependência sobe, o README continua afirmando
a versão antiga, e ninguém percebe porque nada quebra. Onde existe uma fonte
única da verdade, o lugar do número é lá.

Não há framework de CSS nem de JavaScript, e não há etapa de build de
*frontend*: são 12 templates Thymeleaf, uma folha de estilo e três scripts,
escritos à mão. A identidade visual descrita adiante depende disso — ela não
sobreviveria por cima de um tema pronto.

Nenhum recurso é buscado da internet em tempo de execução: as fontes (com as
licenças no repositório), o htmx e o CSS são servidos pela própria aplicação,
porque o sistema precisa funcionar sem rede no dia do evento.

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

Para compilar e rodar os testes, requisito único: **JDK 25**. Não precisa
instalar Maven. O perfil `postgres`, que é o padrão, pede também um PostgreSQL
em `localhost:5432`, fornecido pelo cluster da máquina ou por um contêiner, como
os passos adiante mostram. Com o JDK sozinho, o caminho que roda de ponta a
ponta é o perfil `dev`.

O `mvnw` é o *Maven Wrapper*. Na primeira execução ele baixa o Maven 3.9.9 (uma
vez, para fora do projeto) e passa a usá-lo sempre. A versão do Maven fica assim
registrada no repositório, em `.mvn/wrapper/maven-wrapper.properties`, em vez de
depender do que cada máquina tem instalado. Quem já tem Maven pode chamar `mvn`
direto — funciona igual, só sem essa garantia. Nos exemplos abaixo, `mvnw.cmd` é
a forma do Windows; em Linux e macOS, `./mvnw`.

São três perfis, e cada um tem o seu modo de execução. Antes dos passos, vale
separar dois eixos que é fácil embaralhar: **o perfil diz o que a aplicação
procura; quem fornece aquilo é outra coisa.**

| Perfil | O que a aplicação procura | Quem fornece |
|---|---|---|
| `postgres` | PostgreSQL em `localhost:5432/portaria` | o cluster próprio **ou** um contêiner Docker — à escolha |
| `dev` | H2 em arquivo `./data/portaria` | a própria aplicação; não exige nada instalado |
| `test` | H2 em memória | a própria aplicação, durante os testes |

O perfil `postgres` **não tem como saber quem atendeu**. O
`application-postgres.properties` pede um PostgreSQL naquele endereço, com banco,
usuário e senha `portaria`, e pronto. Foi exatamente isso que o teste de 29/10
mostrou: mesmo perfil, mesmo comando, mesma configuração — e do outro lado um
contêiner no lugar do cluster. A aplicação não percebeu diferença, e é esse o
ponto.

Os dois disputam a porta 5432, então vale um por vez. O critério prático é o que
acontece com os dados: o **cluster** os mantém entre sessões, e é o que você quer
quando está desenvolvendo ao longo de dias; o **contêiner** os descarta no
`docker rm`, e é o que você quer quando precisa de um banco limpo.

### Perfil `postgres` — o principal

É o banco que a aplicação tem em produção, e é nele que o sistema deve ser visto
funcionando. O cluster não sobe junto com o Windows (ver "Ambiente"), então a
rotina começa iniciando o banco.

**Na primeira vez**, o cluster e o banco precisam ser criados, ver "Subir o
PostgreSQL local", adiante. Depois disso, só os passos abaixo. Pelo contêiner
não existe primeira vez: o `docker run` do passo 1 cria banco, usuário e senha
a partir das variáveis que recebe, e o cluster nem precisa estar instalado.

**1. Subir o banco** — pelo cluster próprio:

```powershell
& "C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe" -D "$env:USERPROFILE\pgdata-portaria" -o "-p 5432" -l "$env:USERPROFILE\pgdata-portaria\servidor.log" start
```

Ou, se preferir o contêiner — mesma porta, mesmas credenciais, banco limpo:

```powershell
docker run -d --name portaria-db -p 5432:5432 -e POSTGRES_DB=portaria -e POSTGRES_USER=portaria -e POSTGRES_PASSWORD=portaria postgres:18
```

Um ou outro, nunca os dois: com o cluster no ar, o `docker run` falha com
`port is already allocated`. Detalhes em "Caminhos alternativos", adiante.

**2. Conferir que ele aceita conexão**

```powershell
& "C:\Program Files\PostgreSQL\18\bin\pg_isready.exe" -h localhost -p 5432
```

Esperado: `localhost:5432 - aceitando conexões`. O passo parece dispensável e não
é: o `pg_ctl` devolve o prompt antes de o servidor estar pronto, e subir a
aplicação nesse intervalo dá um erro de conexão que parece problema de
configuração sem ser.

Se vier `nenhuma resposta`, o passo 1 não funcionou — não adianta seguir.

**3. Rodar os testes**

```powershell
.\mvnw.cmd clean test
```

Esperado: `Tests run: 34, Failures: 0, Errors: 0`.

**4. Subir a aplicação**

```powershell
.\mvnw.cmd spring-boot:run
```

Sem parâmetro: `postgres` é o perfil padrão desde a Sprint 5, declarado em
`application.properties`. Assim não há como demonstrar o sistema em H2 por
engano — para isso seria preciso pedir o perfil `dev` explicitamente.

Se precisar informar o perfil mesmo assim, as aspas em volta do `-D...` são
obrigatórias no PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Sem elas o PowerShell parte o argumento no ponto, e o Maven recebe
`.run.profiles=postgres` como se fosse uma fase do ciclo de vida — o erro é
`Unknown lifecycle phase`, que não diz nada sobre aspas.

**5. Usar** — <http://localhost:8080>. No log, a linha
`The following 1 profile is active: "postgres"` confirma que você está no banco
certo, e não no H2.

**6. Encerrar a aplicação** — `Ctrl+C` no terminal onde ela está rodando.

**7. Parar o cluster**

```powershell
& "C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe" -D "$env:USERPROFILE\pgdata-portaria" stop
```

Com o contêiner, o equivalente é `docker stop portaria-db` — e `docker rm
portaria-db` se quiser descartar os dados junto.

Não é obrigatório, mas banco no ar à toa ocupa memória e segura a porta 5432 —
e esquecer disso é o que faz a próxima tentativa de usar outra instalação de
PostgreSQL falhar sem explicação.

### Perfil `dev` — clone e rode

Não exige nada instalado além do JDK, e funciona sem rede. É o caminho de quem
abre o projeto pela primeira vez, e a alternativa confiável quando o cluster não
sobe.

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Precisa ser pedido explicitamente: o padrão é `postgres`. O banco é um arquivo H2 em
`./data/portaria`, criado sozinho, e pode ser inspecionado ao vivo em
`/h2-console` (URL JDBC `jdbc:h2:file:./data/portaria`, usuário `sa`, senha em
branco). Esse console **só existe neste perfil**.

### Perfil `test` — só durante os testes

```powershell
.\mvnw.cmd clean test
```

Roda contra um H2 **em memória**, que nasce e morre com o processo. Não tem
endereço e não toca em nenhum dos outros bancos. Prova que o código está íntegro
— quem prova que o esquema do PostgreSQL está correto é a subida no perfil
`postgres`, onde o `ddl-auto=validate` derruba a aplicação se as entidades não
baterem com as migrações.

O `clean` está aí de propósito: sem ele, o Maven reaproveita o que está em
`target/` e pula a compilação, o que esconde erro de versão de JDK até o momento
mais inconveniente.

### Entrar no sistema

| | |
|---|---|
| E-mail | `admin@portaria.local` |
| Senha | `portaria123` |

O login é pelo **e-mail**, não por um apelido: para o Spring Security, o
identificador da conta é o endereço — o mesmo que a recuperação de senha usa
para achar a pessoa. Errar o identificador devolve a mesma mensagem que errar
a senha ("credenciais inválidas"), de propósito, para não revelar quais
e-mails têm conta.

Essa conta é semeada na primeira execução de cada banco, junto com um evento de
exemplo com quatro inscritos, para haver o que demonstrar imediatamente. Como o
sistema permite alterar e-mail e senha pelas próprias telas, **estes valores
valem enquanto ninguém os tiver trocado** — vale conferir antes de apresentar.

### Roteiro de demonstração (3 minutos)

1. Entrar em `/login`.
2. Abrir o evento de exemplo e **copiar o link público**.
3. Abrir o link em uma aba anônima e fazer uma inscrição — anote o código.
4. Voltar ao painel e clicar em **Abrir painel de portaria**.
5. Digitar o código e confirmar — botão **Confirmar entrada** ou Enter → entrada
   liberada, contador sobe.
6. **Digitar o mesmo código de novo** → "já registrado", e o contador *não* sobe.
   Este é o ponto técnico central da entrega.
7. Digitar um código inventado → não encontrado.

No perfil `postgres`, o banco é inspecionado com qualquer cliente — a extensão de
PostgreSQL do VS Code, o DBeaver ou o `psql` — em `localhost:5432`, banco
`portaria`, usuário `portaria`. No perfil `dev`, pelo `/h2-console` embutido.

---

## Decisões de arquitetura

> Os diagramas estão em **[docs/arquitetura.md](docs/arquitetura.md)**: fluxo do
> check-in ponta a ponta, modelo de dados, dependências entre pacotes, perfis e
> fornecedores de banco, e o fluxo da inscrição pública. Esta seção explica as
> decisões; lá elas estão desenhadas.

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
*é* a integração com o hardware — sem driver, sem SDK, sem dependência. O botão
"Confirmar entrada" existe ao lado dele para o celular, onde não há tecla Enter
evidente; os dois caminhos levam à mesma submissão.

**Migrações com Flyway, em SQL padrão.** O esquema é criado exclusivamente pelas
migrações e o Hibernate roda em `validate`. O SQL evita tipos proprietários, de
modo que a mesma migração roda nos dois — o que deixou de ser promessa em
29/10/2026, quando o esquema foi construído do zero num PostgreSQL em contêiner
sem uma linha de SQL ter sido reescrita.

---

## Identidade visual

O sistema tinha um tema escuro competente e genérico: cartões de canto
arredondado, cinza neutro, um âmbar qualquer. Funcionava e não dizia nada. A
identidade **"Cancela · noite fria"** existe para que cada decisão visual tenha
um motivo de domínio, e não de gosto.

**A marca é uma cancela erguida** — poste, braço inclinado, luz na ponta. Três
formas, que é o que sobrevive a 20 px na aba do navegador. A escolha foge de
caveira, guidão e chama: marcas assim prendem o sistema a um único tipo de
evento, e ele também atende encontro cultural, feira e festa de clube. É
desenhada em SVG dentro do HTML, sem biblioteca de ícones e sem requisição
extra, e puxa as cores da folha de estilo — então acompanha o tema sozinha.

**Três decisões se repetem em toda tela**, e é a repetição que separa um
projeto de um tema pronto:

1. **Faixa de risco.** A diagonal âmbar e preta é barreira — cancela, fita de
   isolamento, portão. Aparece no topo da barra superior, no cartão de login e
   no indicador que importa. Em nenhum outro lugar: motivo que aparece em tudo
   deixa de ser motivo e vira papel de parede.
2. **Cantos retos de 4 px.** Placa metálica, não cápsula. A única exceção são
   os itens da barra superior, redondos de propósito — cápsula lê como
   "estado/sessão", e nada mais na interface é estado.
3. **Tipografia de sinalização.** Oswald em caixa alta com entreletra aberta em
   títulos, rótulos de campo, cabeçalhos de tabela e botões. Inter só no texto
   corrido, onde ler importa mais que anunciar.

**Cada cor tem uma função só**, e é por isso que são quatro e não uma paleta:

| Cor | Papel |
|---|---|
| Âmbar `#FFB224` | A marca e a ação principal |
| Lima `#C9F23F` | **Só** liberação de entrada na portaria |
| Verde `#4FB477` | Confirmações comuns ("senha alterada"), discreto para não roubar o significado do lima |
| Vermelho `#F2564B` | Recusa e perigo |

**Três fontes embarcadas, 116 KB, nenhum CDN.** A portaria de um evento ao ar
livre é justamente onde falta sinal, e a apresentação também precisa rodar sem
internet. São arquivos variáveis com o subconjunto latino, que já cobre todos os
acentos do português. A escolha da monoespaçada é funcional, não estética: na
JetBrains Mono o zero é cortado, o `1` tem base e o `I` tem serifa, então `0/O` e
`1/l/I` não se confundem quando o operador digita o código lido do celular do
participante.

**Nenhum `style=` nos templates.** Havia 17 espalhados, incluindo a mesma caixa
verde de sucesso escrita à mão em sete arquivos. Estilo embutido no HTML não
aparece na folha, não pode ser reaproveitado e precisa ser corrigido arquivo por
arquivo quando o tema muda. O Thymeleaf monta o HTML; o visual está todo no CSS.

### Acessibilidade

Não é item de conformidade: é o que faz a tela funcionar no escuro, de luva e com
fila andando.

- **Cor nunca decide sozinha.** Lima e âmbar são próximos para quem tem
  daltonismo vermelho-verde, então todo resultado leva ícone (✓, !, ✕) e rótulo
  escrito. A cor reforça, nunca informa sozinha.
- **A área de retorno tem altura mínima fixa**, para o campo não subir e descer a
  cada leitura. Elemento que se mexe é elemento que se erra.
- **`aria-live="assertive"`** no retorno do check-in: o leitor de tela anuncia o
  resultado sem o operador procurar.
- **Aviso sonoro** com três sinais distintos (um sino para liberado, dois tons
  iguais para já registrado, um grave áspero para desconhecido), gerados pelo
  próprio navegador — sem arquivo de áudio e sem internet. No portão o operador
  olha para a pessoa, não para a tela.
- **Alvos grandes no celular.** O botão de confirmação cresce em telas estreitas,
  e a altura vem de espaçamento e não de medida fixa, para acompanhar quem
  aumenta o tamanho do texto no sistema.
- **Falha de rede aparece na tela.** Antes, se a requisição não chegasse, nada
  mudava e o operador achava que tinha registrado a entrada.
- **`color-scheme: dark`**, que faz o calendário nativo dos campos de data abrir
  escuro em vez de branco sobre a página escura.
- Foco visível igual em toda a aplicação, atalho "Ir para o conteúdo" e
  `prefers-reduced-motion` respeitado.

---

## Atualização para Spring Boot 4 e Java 25

O projeto nasceu em Spring Boot 3.5 com Java 21. O suporte OSS do 3.5 encerrou em
30/06/2026, e a entrega final vai até meados de 2027 — passar um ano sem correção
de segurança não se sustentava. A migração foi feita em 12/09/2026, para **Spring
Boot 4.1.1, Spring Framework 7 e Hibernate 7**.

**Java 25, e não 26.** O 25 é LTS; o 26 é *feature release*, com cerca de seis
meses de vida, e seria substituído pelo 27 poucas semanas depois. O alvo foi
escolhido pelo horizonte do projeto, não pelo número maior.

Três mudanças exigiram intervenção, e nenhuma delas era evidente:

**A integração Thymeleaf + Spring Security deixou de ser um módulo à parte.** O
repositório `thymeleaf-extras-springsecurity` foi arquivado em abril de 2026 e o
código absorvido pelo Thymeleaf 3.2. A dependência sai do `pom.xml` e o
`${#authentication}` continua funcionando, agora vindo do
`spring-boot-starter-thymeleaf`.

**No Boot 4, o starter é o contrato.** As autoconfigurações foram retiradas do jar
monolítico `spring-boot-autoconfigure` e distribuídas em módulos por tecnologia.
Com apenas `flyway-core` no classpath, a biblioteca existe mas a ligação com o
Spring não: as migrações não rodam, sem log e sem erro — e o Hibernate falha
depois, reclamando de uma tabela ausente. A troca por `spring-boot-starter-flyway`
resolve. Módulo de autoconfiguração ausente falha igual a biblioteca ausente: em
silêncio.

**`AntPathRequestMatcher` foi removido no Spring Security 7**, substituído por
`PathPatternRequestMatcher`. O `ConfiguracaoSeguranca` já usa o novo, declarando
um `Builder` uma vez e reaproveitando nos três pontos de rota.

---

## Banco de dados: três perfis, dois bancos

A troca é de **configuração, não de código**. Nenhuma das sete migrações foi
reescrita: elas estão em SQL padrão desde a primeira sprint, e esta seção é a
cobrança dessa aposta.

O projeto tem três perfis. O banco é a diferença principal — o perfil `dev`
liga, além disso, o console do H2:

| Perfil | Banco | Papel |
|---|---|---|
| `dev` | H2 em arquivo (`./data/portaria`) | Porta de entrada e alternativa de emergência. Não exige nada instalado e funciona sem rede. |
| `test` | H2 em memória | Só durante `./mvnw test`. Nasce e morre com o processo; não tem endereço. |
| `postgres` *(padrão)* | PostgreSQL | **Perfil de demonstração e de produção.** É nele que o sistema deve ser visto funcionando, e é o que sobe quando nenhum perfil é informado. |

**A partir da Sprint 5, `postgres` é o perfil de trabalho.** Desenvolvimento
diário e demonstrações acontecem nele, porque é o banco que a aplicação terá em
produção e porque algumas diferenças de comportamento só aparecem ali — o
PostgreSQL aborta a transação inteira quando uma restrição estoura, enquanto o
H2 é mais tolerante. É exatamente por isso que a gravação da presença vive em
transação própria (ver "Decisões de arquitetura"); rodando em PostgreSQL, uma
regressão nesse ponto aparece no mesmo dia.

O `dev` **não** foi aposentado, e isso é deliberado. Ele é o caminho de quem
clona o projeto pela primeira vez, é o que permite demonstrar a portabilidade ao
vivo — a mesma migração, o mesmo código, outro banco — e é a alternativa
confiável se o cluster local não subir na hora de apresentar.

Perfil não é ambiente. Os três são a mesma aplicação, na mesma máquina, com
configurações diferentes: `dev` e `postgres` respondem no mesmo
`http://localhost:8080` e por isso sobem um de cada vez. Um ambiente de produção
de verdade seria a aplicação instalada num servidor, com `SPRING_PROFILES_ACTIVE`
e as credenciais vindo do ambiente — assunto de PI-II.

```sh
./mvnw spring-boot:run                                 # postgres (padrão)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  # H2 em arquivo
```

### Subir o PostgreSQL local

Esta seção cria o cluster e o banco — operação que acontece **uma vez** na vida
da máquina. A rotina de todo dia (iniciar, rodar, parar) está em "Como rodar", no
começo deste documento.

O cluster **não sobe junto com o Windows** — é decisão, e o porquê está em
"Ambiente", adiante. E vale guardar um ponto que confunde: DBeaver, a extensão de
PostgreSQL do VS Code e o `psql` **não iniciam** o banco. Eles abrem conexão com
um servidor que já precisa estar no ar; sem ninguém rodando, o que vem é
`conexão recusada` — nas duas pilhas, IPv6 e IPv4. Quando só uma das duas falha,
aí sim o problema costuma ser de configuração de escuta.

#### Só na primeira vez

```powershell
& "C:\Program Files\PostgreSQL\18\bin\initdb.exe" -D "$env:USERPROFILE\pgdata-portaria" -U postgres -W --encoding=UTF8
```

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -h localhost -p 5432 -U postgres -c "CREATE USER portaria WITH PASSWORD 'portaria';" -c "CREATE DATABASE portaria OWNER portaria;"
```

O `psql` acima precisa do cluster já iniciado — ou seja, o `pg_ctl ... start`
(passo 1 de "Como rodar") vem entre os dois comandos. O `CREATE USER` e o
`CREATE DATABASE` existem porque as credenciais do
`application-postgres.properties` são `portaria`/`portaria`, e não as do
superusuário.

O banco pode ficar vazio: o Flyway cria o esquema inteiro, da V1 à V7, na
primeira execução da aplicação.

#### Caminhos alternativos

**Docker** — um comando no lugar de toda a criação do cluster. Exige o Docker
Desktop aberto e com o motor no ar (ver "Ambiente"), e a porta 5432 livre: o
contêiner e o cluster próprio disputam a mesma, e com o cluster no ar o
`docker run` falha com `port is already allocated`.

No PowerShell, em **uma linha só** — a barra invertida de continuação é sintaxe
de shell Unix e não funciona lá:

```powershell
docker run -d --name portaria-db -p 5432:5432 -e POSTGRES_DB=portaria -e POSTGRES_USER=portaria -e POSTGRES_PASSWORD=portaria postgres:18
```

Em Linux ou macOS:

```sh
docker run -d --name portaria-db -p 5432:5432 \
  -e POSTGRES_DB=portaria -e POSTGRES_USER=portaria \
  -e POSTGRES_PASSWORD=portaria postgres:18
```

O `docker run` devolve o identificador do contêiner assim que ele inicia, o que
não significa que o banco já aceite conexão — o PostgreSQL ainda inicializa o
diretório de dados por dentro. Vale o mesmo `pg_isready` do passo 2 de "Como
rodar" antes de subir a aplicação.

Daí em diante, a rotina é a de "Como rodar" a partir do passo 3 — o contêiner
substitui apenas os passos 1 e 2. Para parar e remover:

```powershell
docker stop portaria-db
docker rm portaria-db
```

`docker rm` **apaga os dados**: o contêiner guarda o banco num volume anônimo que
morre com ele. É o que se quer de um banco descartável de desenvolvimento, e é a
diferença em relação ao cluster próprio, que sobrevive entre sessões. Para só
pausar sem perder nada, use `docker stop` e depois `docker start portaria-db`.

**PostgreSQL como serviço do Windows** — com o serviço no ar, basta criar o
usuário e o banco, no `psql`:

```sql
CREATE USER portaria WITH PASSWORD 'portaria';
CREATE DATABASE portaria OWNER portaria;
```

### Por que as migrações atravessaram sem mudança

Não foi sorte; foram decisões tomadas antes de serem necessárias:

- **`GENERATED BY DEFAULT AS IDENTITY`**, que é SQL padrão, em vez de
  `AUTO_INCREMENT` ou `IDENTITY` no dialeto do H2. As entidades usam
  `GenerationType.IDENTITY`, que casa com coluna de identidade nos dois bancos.
- **Nenhum identificador entre aspas.** Sem aspas, o PostgreSQL rebaixa para
  minúsculas e o H2 eleva para maiúsculas; qualquer nome citado literalmente
  passaria a existir só em um dos dois.
- **Nenhuma palavra reservada como nome.** `local_evento` em vez de `local`,
  `usuario` em vez de `user`.
- **Apenas tipos que os dois têm**: `BIGINT`, `INTEGER`, `VARCHAR(n)`,
  `TIMESTAMP`, `BOOLEAN`.

O `spring.jpa.hibernate.ddl-auto=validate` é o juiz disso tudo: se o esquema
criado pelo Flyway no PostgreSQL não batesse com as entidades, a aplicação não
subiria. Subir já é a prova.

### O Flyway precisa do módulo do banco

Uma pedra no caminho que não estava documentada em lugar nenhum e custou duas
tentativas: a partir do Flyway 10, **o suporte a cada banco vive num módulo
separado**. O `flyway-core` sozinho não reconhece PostgreSQL — ele conecta,
pergunta a versão, não encontra nenhum tipo de banco registrado que a atenda e
recusa com:

```
FlywayException: Unsupported Database: PostgreSQL 18.6
```

A mensagem sugere versão velha, e não é: subir o Flyway para a versão mais
recente dá exatamente o mesmo erro. O que falta é a dependência:

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

O H2 nunca precisou disso porque o suporte a ele já vem embutido no núcleo —
o que torna o problema invisível até o dia em que o segundo banco entra.

### Credenciais

As do arquivo `application-postgres.properties` são do banco **local**, e por
isso podem ficar no repositório. Em produção nada delas se aplica: o provedor
injeta `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` e
`SPRING_DATASOURCE_PASSWORD` no ambiente, e os valores do arquivo nem chegam a
ser lidos. Senha de produção em arquivo versionado é vazamento, não
configuração.

### O console do H2 não vaza para produção

O console exige duas exceções de segurança — ignorar CSRF na rota dele e
afrouxar `X-Frame-Options` para `SAMEORIGIN`, porque roda dentro de um frame.
Essas exceções estão amarradas à propriedade `spring.h2.console.enabled`, e
**não** ao nome do perfil.

A diferença importa: amarrar ao perfil deixaria as exceções valendo em qualquer
ambiente que por descuido subisse como `dev`, mesmo sem console nenhum.
Amarradas à propriedade, elas existem exatamente enquanto existe aquilo que as
justifica. No perfil `postgres` o console não sobe, e a aplicação roda com CSRF
íntegro e `X-Frame-Options` em `DENY`.

### Os testes continuam no H2

De propósito. Apontá-los ao PostgreSQL os deixaria lentos e dependentes de um
serviço rodando — e eles precisam rodar em qualquer máquina, offline, em
segundos.

O custo honesto disso é que a suíte não prova compatibilidade com o PostgreSQL;
quem prova é subir a aplicação no perfil `postgres` com `validate` ligado. Para
fechar essa lacuna de vez bastaria **um** teste de integração com Testcontainers,
ao preço de o build passar a exigir Docker. Fica anotado para o PI-II.

---

## Levantado com o domínio

Registro de uma observação feita por quem organiza e frequenta encontros de
motoclube, e que corrige uma premissa do projeto:

> Encontros de motoclube costumam flexibilizar bastante as inscrições tardias —
> para o dia do evento, ou para o segundo dia. Varia com a organização, mas é
> normal.

**Consequência 1.** O encerramento de inscrições não é um estado final. É um
interruptor de rotina: fecha-se na véspera para fechar a contagem do kit e
reabre-se no sábado quando aparece gente no portão. Por isso `encerrar` e
`reabrir` são simétricos, e o evento encerrado continua editável.

**Consequência 2.** Inscrever alguém no próprio portão deixa de ser recurso
secundário e passa a ser caminho principal. Foi promovido ao topo do backlog.

**Consequência 3.** Em evento de dois dias, uma presença por inscrição não
basta: interessa saber quem esteve no sábado e quem esteve no domingo. Isso
reabre a discussão de check-in por dia ou por atividade, e não apenas por evento.

---

## Ambiente: duas decisões

Nenhuma das duas é do projeto — são da máquina de desenvolvimento. Ficam
registradas aqui porque custaram tempo e vão voltar a custar se ninguém lembrar
do contexto.

As duas seguem o mesmo princípio: **serviço de desenvolvimento sobe sob demanda,
não junto com o sistema operacional.** E as duas esbarraram no mesmo
mal-entendido — ferramenta cliente não inicia servidor, ela só conecta.

### Decidido: o PostgreSQL sobe sob demanda, não como serviço

O serviço `postgresql-x64-18` não inicia. Roda com `NT AUTHORITY\NetworkService`
e falha sem escrever nada no log do PostgreSQL nem no log de eventos do Windows
— ou seja, morre antes de o servidor chegar a subir. O diretório de dados está
íntegro (a última execução, em 10/10/2026, terminou com desligamento limpo).

**Não vamos consertar, e isso é escolha, não desistência.** Banco de
desenvolvimento não precisa subir junto com o sistema operacional: fica ocupando
memória e porta o dia inteiro para ser usado em uma fração dele. O projeto usa um
cluster próprio, iniciado com `pg_ctl start` quando se vai trabalhar com
PostgreSQL, e parado depois.

Vale saber o que isso implica, porque é fácil se enganar: DBeaver, a extensão do
VS Code e o `psql` **não iniciam o banco**. Eles abrem uma conexão para um
servidor que já precisa estar no ar — com nenhum rodando, o que se recebe é
`conexão recusada`. O `pg_ctl start` vem antes, sempre.

### Decidido: o Docker também sobe sob demanda

O Docker Desktop funciona nesta máquina. O erro `failed to connect to the docker
API` em `npipe:////./pipe/dockerDesktopLinuxEngine`, registrado antes como
defeito, era outra coisa: o motor não estava no ar quando o comando foi dado.
Mesmo mal-entendido do PostgreSQL — `docker ps` e `psql` conectam, não iniciam.

A rotina é a mesma: abrir o Docker Desktop, esperar o indicador do canto inferior
esquerdo ficar verde em `Engine running`, usar, e fechar ao terminar. O
`wsl -l -v` confirma de fora: com o motor no ar, a distribuição `docker-desktop`
aparece como `Running`.

O WSL2 já está habilitado nesta máquina — todas as distribuições aparecem em
`VERSION 2` —, então o alerta antigo sobre conflito com o VirtualBox não se
aplica: não há nada a habilitar, e a convivência entre os dois já é a de hoje.

O caminho de contêiner foi exercitado de ponta a ponta em 29/10/2026, e o
resultado vale como evidência da portabilidade. Num `postgres:18` criado minutos
antes, vazio, a aplicação subiu no perfil `postgres` e o Flyway construiu o
esquema inteiro — as sete migrações registradas com `success = t`, nenhuma linha
de SQL escrita para este banco. O `ddl-auto=validate` passou, o `DadosIniciais`
semeou um administrador, um evento e quatro inscrições, e uma segunda subida
contra o mesmo banco não repetiu nada: o Flyway reconheceu a versão 7 e seguiu.

O check-in também foi exercitado ali, e esse teste importa mais no PostgreSQL do
que no H2: o mesmo código lido duas vezes devolveu "já registrado", o contador
não subiu e a tela continuou atendendo. É a restrição `UNIQUE (inscricao_id)`
disparando e a transação própria do `RegistroDePresenca` absorvendo a violação —
no banco que aborta a transação inteira quando uma restrição estoura, e onde o H2
seria tolerante demais para expor um erro.

Com isso, o teste de integração com Testcontainers deixa de ter impedimento de
ambiente para o PI-II.

---

## Backlog priorizado

Deliberadamente fora desta entrega. Cada item tem justificativa de domínio — não
é lista de desejos:

| Item | Por quê |
|---|---|
| **Cadastro na portaria** *(prioridade máxima)* | Inscrição tardia no dia do evento é prática comum, não exceção. Hoje quem chega sem inscrição não entra no sistema. |
| **Check-in por dia de evento** | Encontro de dois dias precisa distinguir quem veio no sábado de quem veio no domingo. Uma presença por inscrição não representa isso. |
| **Check-in coletivo** | Caravana chega com 12 pessoas; a fila não pode parar. |
| **Fila offline** (IndexedDB + código assinado por HMAC) | Encontro acontece em pátio e área rural, onde não há sinal confiável. |
| **QR code** | Camada sobre o código curto. Exige HTTPS: a câmera do navegador só funciona em contexto seguro. |
| **Capacidade sob concorrência** | Hoje é *check-then-act*; duas requisições podem disputar a última vaga. |
| **Comprovante conforme o tipo de evento** | Certificado com carga horária (acadêmico), comprovante simples (cultural), nenhum. Variação por polimorfismo, não por condicional. |
| **Mostrar quem registrou cada entrada** | A coluna `registrado_por` de `check_in` é gravada a cada leitura e nunca exibida. Com vários operadores no portão, é o dado que responde "quem liberou esta pessoa?". |
| **Retenção de dados / LGPD** | Definir base legal e prazo de descarte dos dados pessoais coletados. |
