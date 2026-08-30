# A lógica da solução

Documento de estudo, em português. Tudo o mais no repositório é inglês.

---

## 1. O problema em uma frase

Dada uma conta e um valor, responder **se o pagamento pode acontecer** e, se não puder,
**todos os motivos**. A checagem não move dinheiro: ela só responde uma pergunta.

Isso muda tudo no design. Como nada é gravado, não existe transação, não existe estado, e
`eligibility` não tem repositório. A decisão é uma função pura de `(conta, valor, moeda)`.

---

## 2. O caminho de um request

```
POST /api/payments/eligibility
  → EligibilityController        valida o corpo (@Valid) e delega
  → CheckEligibilityUseCase      lê a conta uma vez, aplica as 7 regras
  → EligibilityResponseDTO       eligible + reasons + amount + availableBalance,
                                 com os valores já como string "250.50"
```

Se qualquer coisa der errado no meio, `GlobalExceptionHandler` intercepta e devolve **um
único envelope de erro**. O cliente nunca vê stack trace, nunca vê dois formatos de erro.

---

## 3. As 7 regras

Estão em `evaluate()`, um `if` por regra, na ordem em que aparecem na resposta:

| # | Motivo | Rejeita quando |
|---|---|---|
| 1 | `ACCOUNT_INACTIVE` | status != ACTIVE |
| 2 | `KYC_NOT_VERIFIED` | kycVerified é false |
| 3 | `CURRENCY_MISMATCH` | moeda do request != moeda da conta |
| 4 | `AMOUNT_BELOW_MINIMUM` | valor < minTransaction |
| 5 | `AMOUNT_ABOVE_MAXIMUM` | valor > maxTransaction |
| 6 | `INSUFFICIENT_FUNDS` | valor > availableBalance() |
| 7 | `DAILY_LIMIT_EXCEEDED` | dailySpent + valor > dailyLimit |

Três coisas fazem essa lista funcionar:

**Acumula, não para no primeiro.** A lista é construída inteira e só no fim se pergunta se
ela está vazia. Parar no primeiro motivo obrigaria o usuário a descobrir os problemas um
por request.

**A ordem é o `enum`.** `RejectionReason` declara os motivos na ordem do contrato, e a lista
é preenchida na mesma ordem. Não existe ordenação em lugar nenhum — reordenar as constantes
do enum muda a resposta da API. Está escrito lá em cima do enum, porque isso não é óbvio.

**`availableBalance()` não é `balance`.** Saldo não é o que dá pra gastar: parte dele está
retida por transações pendentes (`heldAmount`). O método vive no `Account` porque é uma
regra do domínio, não do caso de uso.

---

## 4. As três decisões difíceis

### CLOSED é terminal

Uma conta fechada devolve **só** `ACCOUNT_INACTIVE`, mesmo quebrando outras quatro regras
ao mesmo tempo. No código é um `return` antes de tudo:

```java
if (account.status() == AccountStatus.CLOSED) {
    return List.of(RejectionReason.ACCOUNT_INACTIVE);
}
```

O porquê: listar "saldo insuficiente" numa conta fechada sugere que depositar resolveria.
Não resolve. A lista de motivos é uma lista de coisas que o usuário poderia consertar, e
numa conta fechada não existe nada consertável.

`SUSPENDED` **não** é terminal — suspensão é reversível, então acumula com todo o resto.

### Regras de valor rodam mesmo com a moeda errada

Essa é a única decisão que a especificação deixa em aberto de propósito. Se a conta é em
EUR e o request vem em USD, comparar `250 USD` com um limite em EUR é tecnicamente sem
sentido.

Escolhi **rodar mesmo assim**, e o comentário no código diz por quê: o usuário vai corrigir
a moeda e reenviar, e é melhor ele receber todos os problemas numa rodada do que descobrir
o limite estourado só na segunda tentativa.

A decisão oposta é igualmente defensável. O que não é defensável é não ter decidido.

### Escala só é aplicada na saída

As sete regras comparam o valor **exatamente como chegou**. A conversão pra `"250.50"`
acontece num único lugar, o método `money()`, chamado só na hora de montar a resposta — ou
seja, depois de toda comparação já ter sido feita.

Isso não é firula: se o arredondamento acontecesse antes, a formatação da saída poderia
mudar a decisão. E o tipo do DTO é `String`, não `BigDecimal`, então a fronteira está
visível na assinatura: dali pra frente é texto, não número.

---

## 5. Dinheiro

Duas armadilhas, uma de cada lado do fio.

**No Java: `BigDecimal`, comparado com `compareTo`.** `double` não representa `0.1`
exatamente. E mesmo com `BigDecimal`, `equals` compara *escala* junto com valor —
`new BigDecimal("1.0").equals(new BigDecimal("1.00"))` é `false`. Só `compareTo` responde a
pergunta certa. Todas as sete regras usam `compareTo`, e existe um teste só pra isso.

**No JSON: string, nunca número.** Número em JSON é IEEE-754 double por especificação —
o `JSON.parse` do browser perderia precisão. Por isso `"amount": "250.50"` entre aspas, e
por isso o teste afirma `jsonPath("$.amount").isString()`.

**Null significa ilimitado, não zero.** `dailyLimit` e `maxTransaction` são os únicos campos
que aceitam null, e as regras 5 e 7 simplesmente não rodam quando o limite é null. Tratar
null como `BigDecimal.ZERO` inverteria o significado: de "sem teto" para "teto zero".

---

## 6. Erros

### Rejeição é 200

O ponto conceitual da solução inteira. `POST /eligibility` pergunta *"esse pagamento pode
acontecer?"*. **"Não, por estes quatro motivos"** é uma resposta bem-sucedida a essa
pergunta. É `200`.

`4xx` fica reservado pra quando a *pergunta* está malformada: valor negativo, moeda com duas
letras, conta que não existe. Devolver `422` numa rejeição de negócio confunde erro de
protocolo com resposta de domínio — e obriga todo cliente a tratar rejeição no caminho de
exceção.

### Um envelope só

Todo erro sai no mesmo formato: `timestamp`, `status`, `code`, `message`, e `fieldErrors`
apenas em `VALIDATION_ERROR` (o `@JsonInclude(NON_NULL)` some com o campo nos outros casos).

Quatro condições, quatro códigos:

| Condição | Status | code |
|---|---|---|
| Bean Validation falhou | 400 | `VALIDATION_ERROR` |
| JSON quebrado ou tipo errado | 400 | `MALFORMED_REQUEST` |
| Conta inexistente | 404 | `ACCOUNT_NOT_FOUND` |
| Qualquer outra coisa | 500 | `INTERNAL_ERROR` |

O handler de `Exception` no fim é o que garante que **nenhuma stack trace chega no cliente**.
É o handler mais importante do arquivo e o mais fácil de esquecer.

`{"amount": "abc"}` merece atenção: o Jackson falha antes da validação rodar, então cai em
`HttpMessageNotReadableException`. Sem esse handler específico, viraria `500` — e "digitei
letra no campo valor" não é erro de servidor.

---

## 7. O frontend

### O estado é um valor só

```ts
type Outcome =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "decided"; response }
  | { kind: "invalid"; fieldErrors }
  | { kind: "failed"; message }
```

Com quatro booleanos (`loading`, `error`, `result`, `fieldErrors`) existem 16 combinações,
e a maioria é inválida — "carregando e com resultado", "erro e sucesso". Com uma união
discriminada, **essas combinações não têm onde existir**. Não é preferência de estilo: é a
diferença entre garantir a regra e lembrar dela.

### `res.ok` é a linha mais importante

`fetch` só rejeita a promise em falha de rede. Um `400` chega como promise **resolvida**.
Quem escreve `const data = await res.json()` direto trata rejeição como sucesso e mostra
`undefined` na tela. É o bug número um em teste técnico com React.

### Rede quebrada ≠ pagamento rejeitado

`client.ts` traduz o que aconteceu em dois tipos:

- `invalid` — a API respondeu com o envelope, e dá pra dizer qual campo está errado
- `failed` — a API não respondeu, ou respondeu algo que não é o envelope

O segundo caso pega mais do que parece: com o backend derrubado, o proxy do Vite devolve
`502` com corpo vazio, o `res.json()` estoura, e o `catch` transforma isso em "serviço
indisponível". Sem esse tratamento, o form giraria pra sempre.

Na tela isso vira um painel âmbar com `role="alert"`, visualmente diferente do painel
vermelho de rejeição, que é `role="status"`.

### Erro de campo vai no campo

Um `400 VALIDATION_ERROR` traz `fieldErrors: [{field: "amount", ...}]`. O `byField()` vira
isso num mapa e cada `<Field>` lê o seu. Banner genérico dizendo "erro de validação" obriga
o usuário a adivinhar qual dos três campos está errado.

`404 ACCOUNT_NOT_FOUND` também vira erro de campo, no `accountId` — quem errou foi o que
foi digitado ali.

### Duas submissões sobrepostas

O botão fica `disabled` durante a requisição, então pelo botão não dá pra disparar duas. Mas
o requisito é mais forte: **se duas se sobrepuserem, quem fica na tela é a mais recente, não
a que chegar por último**.

A solução são três linhas: um `useRef` guarda o `AbortController` em voo, cada submissão
aborta o anterior, e o `catch` ignora erro de request abortado. Sem isso, uma resposta lenta
de "aprovado" pode sobrescrever um "rejeitado" mais novo — e a tela passa a mentir.

---

## 8. Os testes

Dois arquivos, 16 testes, **zero mocks**.

**`CheckEligibilityUseCaseTest`** — as regras. O truque é que as 6 contas semeadas *são* a
fixture: cada uma existe pra exercitar uma parte diferente. `ACC-1003` tem quase todo o
saldo retido, `ACC-1004` quebra quatro regras de uma vez, `ACC-1005` é a fechada. Então o
teste é `new CheckEligibilityUseCase(new AccountRepository())` e cada caso vira duas linhas.
Sem Mockito, sem builder de conta em cada teste.

`ACC-1001` tem `maxTransaction` e `dailyLimit` os dois em `2500.00` de propósito: um único
request de `"2500.00"` prova que as duas regras são `>` e não `>=`.

**`EligibilityControllerTest`** — o contrato HTTP com o app inteiro de pé: 200 na rejeição,
dinheiro como string, os 400 com o campo certo, o 404 com envelope.

Nome de teste é frase, e diz o porquê quando o porquê não é óbvio:
`shouldReturnOnlyAccountInactiveWhenClosedBecauseClosedIsTerminal`.

---

## 9. O que eu tiraria primeiro se o tempo acabasse

Nesta ordem, e o motivo:

1. **CSS.** Nenhum critério de avaliação olha pra estilo.
2. **`EligibilityControllerTest`.** O teste de regras é o que prova a parte que mais vale.
3. **`AbortController`.** É o único requisito do front que ninguém sente falta em 45 minutos.

O que nunca sai: **200 na rejeição**, **`res.ok` no fetch**, **o envelope de erro**. Somados
são metade da nota e cabem em cem linhas.

---

## 10. O roteiro dos 5 minutos de apresentação

As falas estão em inglês porque a avaliação é em inglês. Se a conversa acabar sendo em
português, a estrutura é a mesma — só traduz.

Cinco minutos comportam **três ideias**. Escolhi as três que provam senioridade: a decisão
conceitual, a ambiguidade resolvida com consciência, e o rigor com dinheiro.

**Antes de começar:** deixe aberto na tela o arquivo do caso de uso. Um arquivo só. Passear
por diretórios queima um minuto e não mostra nada.

### 0:00 – 0:45 · O que é, e o caminho de um request

> "The service answers one question: can this account make this payment? It never moves
> money — it returns a decision and the reasons behind it.
>
> A request comes in with an account id, an amount and a currency. The controller validates
> the shape of the request. The use case loads the account once, runs seven rules against
> that single snapshot, and returns every reason that applies. On the frontend, a form calls
> that endpoint and renders either the approval, the list of reasons in plain language, or
> an error."

*Não liste tecnologias. Ninguém dá ponto por "usei Spring Boot".*

### 0:45 – 2:30 · A decisão conceitual: rejeição é 200

Essa é a ideia central. Fale devagar.

> "The decision I'd most like to defend is the status code. When a payment is rejected, the
> API returns two hundred, not four twenty-two, not four hundred.
>
> The endpoint asks a question: can this payment happen? 'No, and here are the four reasons'
> is a successful answer to that question. It's the service working correctly. A four-x-x is
> reserved for when the *question itself* is malformed — a negative amount, a currency with
> two letters, an account that doesn't exist.
>
> The practical consequence is on the client. If a business rejection came back as an error
> status, every consumer would have to handle a normal, expected outcome in its exception
> path. That's where bugs live.
>
> The related trap is on the frontend, and I handled it explicitly: fetch only rejects on a
> network failure. A four hundred arrives as a *resolved* promise. So checking `res.ok` is
> the single line that keeps a rejected request from being read as a successful decision."

*Se sobrar tempo aqui, mencione o envelope: um formato de erro só, `fieldErrors` apenas em
validação, e um handler final de `Exception` pra garantir que nenhuma stack trace vaze.*

### 2:30 – 3:45 · A ambiguidade que resolvi de propósito

Aqui é onde você mostra julgamento em vez de execução.

> "There's one case the specification doesn't settle, and I want to name it rather than let
> it look accidental.
>
> When the requested currency doesn't match the account's, the amount is denominated in a
> different currency from every limit we'd compare it against. So: do the amount rules still
> run, or is comparing across currencies meaningless?
>
> I decided they still run, and I left a comment in the code saying why. The caller is going
> to fix the currency and resubmit. Reporting every problem in one round trip beats sending
> them back a second time for a limit we already knew was breached.
>
> The opposite decision is just as defensible — you could argue those comparisons are
> nonsense and should be suppressed. What isn't defensible is not having decided."

*Se perguntarem qual você escolheria num sistema real de pagamentos: converteria as duas
pontas pra mesma moeda antes de comparar, e aí a ambiguidade desaparece.*

### 3:45 – 4:30 · Dinheiro

> "Three things about money. First, every comparison is `BigDecimal.compareTo`, never
> `equals` — `equals` compares scale as well as value, so one point zero and one point zero
> zero come back as different numbers.
>
> Second, monetary values leave the API as JSON strings, not numbers. A JSON number is an
> IEEE double, and the browser would lose precision parsing it.
>
> Third, and this is the part I like: the rules compare the exact amount the caller sent, and
> the scale is only applied when the response is built — after every decision has already been
> made. The response type is a String, so you can see that boundary in the signature. The way
> a response is formatted can never change a decision."

### 4:30 – 5:00 · O que faria em seguida

> "With more time, the first thing I'd add is idempotency — an optional key on the request so
> a retry replays the stored answer instead of re-evaluating. After that, currency conversion
> so the amount rules are always comparing like with like.
>
> What I deliberately didn't add: an interface with one implementation, or a persistence layer
> for a decision that is never stored."

*Terminar nomeando o que você **não** fez é forte. Mostra que você sabe onde fica a linha.*

---

### Perguntas prováveis, e a resposta curta

**"Por que a conta fechada devolve só um motivo?"**
> "Because the list of reasons is a list of things the caller could fix. On a closed account
> nothing is fixable, so listing insufficient funds would suggest that depositing would help.
> Suspended is different — it's reversible, so it accumulates with everything else."

**"Como você garantiu a ordem dos motivos?"**
> "The declaration order of the enum is the order in the response. There's no sorting anywhere
> — the rules are evaluated in that order and appended to a list. I put a comment on the enum
> saying that reordering those constants changes the API contract."

**"Por que não abortou na primeira regra que falhou?"**
> "Because the user would then discover their problems one request at a time. The exception is
> the closed account, for the reason I just gave."

**"E se o backend estiver fora do ar?"**
> "The client distinguishes two failures: the API answered with our error envelope, or it
> didn't answer at all. The second one renders as 'service unavailable', visually different
> from a rejected payment, and the loading state always clears. A form stuck spinning forever
> is the most common version of that bug."

**"Testou o quê?"**
> "The rules against the seeded accounts — each one exists to break a different rule, so the
> suite needs no mocking library at all — and the HTTP contract end to end: two hundred on
> rejection, monetary fields as strings, and the error envelope."

**"O que faltou?"**
> "Styling, and I'd say that was deliberate — none of the requirements are about how it looks.
> And I'd have liked a test around the overlapping-submissions case on the frontend."
