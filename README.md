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

## Stacks Utilizadas

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java (Temurin) | **25** |
| Framework | Spring Boot | **4.1.1** |
| Telas | Spring MVC + Thymeleaf (gera HTML5) | BOM |
| Estilo | CSS próprio, sem framework | — |
| Scripts | JavaScript padrão, sem framework nem build | — |
| Interatividade | htmx (WebJar servido pela aplicação) | **1.9.12** |
| Segurança | Spring Security | BOM |
| Persistência | Spring Data JPA + Hibernate | BOM |
| Migrações | Flyway + `flyway-database-postgresql` | BOM |
| Banco (dev e teste) | H2 em arquivo | **2.3.232** |
| Banco (produção) | PostgreSQL | BOM (driver) |
| Build | Maven, via wrapper (`mvnw`) | **3.9.9** |

Os números em negrito são os que o `pom.xml` fixa. **BOM** quer dizer que a
versão vem do `spring-boot-starter-parent` 4.1.1, que a resolve e a mantém
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

Requisito único: **JDK 25**. Não precisa instalar banco de dados — nem Maven.

```bash
./mvnw spring-boot:run
```

No Windows: `mvnw.cmd spring-boot:run`.

O `mvnw` é o *Maven Wrapper*. Na primeira execução ele baixa o Maven 3.9.9 (uma
vez, para fora do projeto) e passa a usá-lo sempre. A versão do Maven fica assim
registrada no repositório, em `.mvn/wrapper/maven-wrapper.properties`, em vez de
depender do que cada máquina tem instalado. Quem já tem Maven pode chamar `mvn`
direto — funciona igual, só sem essa garantia.

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
- **Aviso sonoro** com três sinais distintos (dois tons subindo para liberado,
  dois iguais para já registrado, um grave longo para desconhecido), gerados pelo
  próprio navegador — sem arquivo de áudio e sem internet. No portão o operador
  olha para a pessoa, não para a tela.
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

## Banco de dados: H2 em desenvolvimento, PostgreSQL em produção

A troca é de **configuração, não de código**. Nenhuma das sete migrações foi
reescrita: elas estão em SQL padrão desde a primeira sprint, e esta seção é a
cobrança dessa aposta.

O projeto tem dois perfis, e o banco é a única diferença entre eles:

| Perfil | Banco | Quando |
|---|---|---|
| `dev` *(padrão)* | H2 em arquivo (`./data/portaria`) | Desenvolvimento e apresentação. Não exige nada instalado e roda sem rede. |
| `postgres` | PostgreSQL | Produção, e sempre que se quiser conferir o comportamento real. |

```sh
./mvnw spring-boot:run                                      # dev / H2
./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres  # PostgreSQL
```

### Subir o PostgreSQL local

Três caminhos. O primeiro é o usado no desenvolvimento deste projeto, e é o
único que não depende de serviço do Windows nem de Docker.

**1. Cluster próprio, iniciado à mão** *(recomendado)*

Cria um banco dentro da pasta do usuário, com os binários de uma instalação
PostgreSQL já existente. Não registra serviço, não exige privilégio de
administrador e não interfere em nenhuma outra instalação.

```powershell
& "C:\Program Files\PostgreSQL\18\bin\initdb.exe" -D "$env:USERPROFILE\pgdata-portaria" -U postgres -W --encoding=UTF8
```

```powershell
& "C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe" -D "$env:USERPROFILE\pgdata-portaria" -o "-p 5432" -l "$env:USERPROFILE\pgdata-portaria\servidor.log" start
```

```powershell
& "C:\Program Files\PostgreSQL\18\bin\psql.exe" -h localhost -p 5432 -U postgres -c "CREATE USER portaria WITH PASSWORD 'portaria';" -c "CREATE DATABASE portaria OWNER portaria;"
```

O `initdb` roda uma vez só. Nas próximas vezes, apenas o `pg_ctl ... start`.
Para parar, o mesmo comando com `stop` no lugar de `start`. O servidor **não**
volta sozinho depois de reiniciar o Windows.

**2. Docker**

```sh
docker run -d --name portaria-db -p 5432:5432 \
  -e POSTGRES_DB=portaria -e POSTGRES_USER=portaria \
  -e POSTGRES_PASSWORD=portaria postgres:18
```

**3. PostgreSQL instalado como serviço**

Com o serviço no ar, no `psql`:

```sql
CREATE USER portaria WITH PASSWORD 'portaria';
CREATE DATABASE portaria OWNER portaria;
```

Em qualquer um dos três, o banco pode subir vazio: o Flyway cria o esquema
inteiro da V1 à V7 na primeira execução.

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

---

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

## Pendências de ambiente

Não são do projeto: são da máquina de desenvolvimento. Ficam registradas aqui
porque atrapalharam uma sprint inteira e vão voltar a atrapalhar se ninguém
lembrar do contexto.

**O serviço `postgresql-x64-18` não inicia.** Roda com `NT AUTHORITY\NetworkService`,
e falha sem escrever nada no log do PostgreSQL nem no log de eventos do Windows
— ou seja, morre antes de o servidor chegar a subir. O diretório de dados está
íntegro (a última execução, em 10/10/2026, terminou com desligamento limpo).
Por isso o projeto passou a usar um cluster próprio iniciado à mão, descrito
acima. É contorno, não conserto.

**O Docker Desktop não sobe.** O motor não responde em
`npipe:////./pipe/dockerDesktopLinuxEngine`. Suspeita principal: conflito com o
VirtualBox instalado na mesma máquina, que disputa o subsistema de
virtualização.

**Cuidado ao consertar o segundo:** a correção usual para o Docker Desktop
passa por habilitar WSL2 ou Hyper-V, e isso pode inutilizar as VMs do
VirtualBox. É uma escolha, não um ajuste — vale decidir antes o que importa
mais naquela máquina.

Nenhum dos dois bloqueia nada: o sistema roda em H2 sem qualquer um deles, e em
PostgreSQL com o cluster próprio. Resolver ambos ficou agendado para **entre a
entrega do PI-I e a primeira sprint do PI-II**, nessa ordem — o serviço
primeiro, por ser o mais provável de ter conserto simples e por resolver
sozinho a comodidade de o banco subir com o Windows.

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
| **Papéis e múltiplos operadores** | Hoje há um único usuário mantido em memória. |
| **Retenção de dados / LGPD** | Definir base legal e prazo de descarte dos dados pessoais coletados. |
