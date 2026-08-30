# ROADMAP — 45 minutos, arquivo por arquivo

Ordem de execução do teste. Cada bloco diz **qual arquivo abrir**, **o que escrever dentro
dele** e **em que minuto sair**.

Assume que existe scaffold. Se um arquivo já vier pronto, pula e ganha o tempo — está
marcado quais costumam vir.

**Duas regras que valem o tempo todo:**

1. Rodar teste é checkpoint, não interrupção. Nenhum bloco termina vermelho.
2. Salva a cada arquivo. A Ropes grava a timeline inteira, e progresso incremental lê muito
   melhor que um bloco gigante aparecendo no minuto 40.

---

## A ordem, em dez segundos

```
 1. recon                          0:00
 2. RejectionReason.java           0:03   (costuma vir pronto)
 3. AccountNotFoundException.java  0:04   (costuma vir pronto)
 4. EligibilityRequestDTO.java     0:05
 5. EligibilityResponseDTO.java    0:07
 6. CheckEligibilityUseCase.java   0:08   ← a peça de 30%
 7. EligibilityController.java     0:15
 8. ErrorCode.java                 0:18
 9. ErrorResponse.java             0:19
10. GlobalExceptionHandler.java    0:21   ← backend fecha aqui
11. api/types.ts                   0:26
12. api/client.ts                  0:29
13. App.tsx                        0:34
14. index.css                      0:42   (só se sobrar)
```

---

## 0:00 – 0:03 · Recon · nenhum arquivo aberto

```bash
mvn test          # ou ./mvnw test
```

Enquanto roda, olha a estrutura de pastas. Quando terminar:

1. **Lê os nomes dos testes que falharam.** É a especificação mais precisa que existe.
2. **Busca `TODO`** no projeto inteiro.
3. **Abre a entidade de domínio.** Só pra anotar mentalmente: os campos se chamam como? Tem
   `availableBalance()` ou você calcula? Os limites são nullable?

Não abre mais nada. Não renomeia nada. Não reorganiza pacote.

> **Se o scaffold usa nomes diferentes dos seus, os nomes dele vencem.** Sempre. Adaptar
> custa zero, renomear custa cinco minutos e nenhum ponto.

---

## Arquivo 2 · `eligibility/domain/RejectionReason.java` · 0:03 – 0:04

*Costuma vir pronto. Se vier, confirma a ordem das constantes e segue.*

```java
/**
 * Declaration order IS the order reasons appear in the API response.
 * Reordering these constants changes the contract.
 */
public enum RejectionReason {
    ACCOUNT_INACTIVE,
    KYC_NOT_VERIFIED,
    CURRENCY_MISMATCH,
    AMOUNT_BELOW_MINIMUM,
    AMOUNT_ABOVE_MAXIMUM,
    INSUFFICIENT_FUNDS,
    DAILY_LIMIT_EXCEEDED
}
```

**Por que primeiro:** o serviço inteiro depende dele. E esse comentário de duas linhas é o
sinal mais barato de senioridade do arquivo — mostra que você sabe que a ordem é contrato,
não acaso.

---

## Arquivo 3 · `shared/exception/AccountNotFoundException.java` · 0:04 – 0:05

*Costuma vir pronto.*

```java
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountId) {
        super("Account not found: " + accountId);
    }

}
```

**Armadilha:** o id pode ser `Long` no scaffold. Olha a entidade antes.

---

## Arquivo 4 · `eligibility/dto/EligibilityRequestDTO.java` · 0:05 – 0:07

```java
public record EligibilityRequestDTO(

        @NotBlank(message = "must not be blank")
        String accountId,

        @NotNull(message = "must not be null")
        @DecimalMin(value = "0.00", inclusive = false, message = "must be greater than 0")
        @Digits(integer = 12, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "must not be null")
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be 3 uppercase letters")
        String currency) {

}
```

**As três que sempre esquecem:**

- `inclusive = false` no `@DecimalMin` — sem isso, `0.00` passa
- `fraction = 2` no `@Digits` — é o que rejeita `250.500`
- `^[A-Z]{3}$` com âncoras — sem elas, `"usdX"` passa

**Por que aqui e não depois:** validação é o que transforma `"usd"` em `400 VALIDATION_ERROR`
em vez de `CURRENCY_MISMATCH`. Confundir esses dois é erro conceitual, e é testado.

---

## Arquivo 5 · `eligibility/dto/EligibilityResponseDTO.java` · 0:07 – 0:08

```java
/**
 * Monetary fields are String, not BigDecimal: a JSON number is an IEEE-754 double and the
 * browser would lose precision parsing it.
 */
public record EligibilityResponseDTO(
        boolean eligible,
        List<RejectionReason> reasons,
        String amount,
        String availableBalance) {

}
```

**A decisão que vale ponto:** `String` e não `BigDecimal`. Resolve dinheiro no JSON sem
serializer, sem annotation, sem nada pra lembrar — e o tipo documenta a fronteira sozinho.

Se o scaffold já define esse record com `BigDecimal`, **não muda**. Aí você formata com
`setScale(2, HALF_UP)` e deixa o Jackson serializar. Adapta.

---

## Arquivo 6 · `eligibility/usecase/CheckEligibilityUseCase.java` · 0:08 – 0:15

**A peça de 30%. Sete minutos.** Escreve nesta ordem, de cima pra baixo, sem pular:

### 6.1 · Esqueleto e construtor · 30s

```java
@Service
public class CheckEligibilityUseCase {

    private final AccountRepository accountRepository;

    public CheckEligibilityUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
```

Injeção por construtor, nunca `@Autowired` em campo.

### 6.2 · O `execute` · 1min

```java
    public EligibilityResponseDTO execute(EligibilityRequestDTO request) {
        // Read once, then evaluate every rule against that one snapshot.
        Account account = accountRepository.getById(request.accountId());

        List<RejectionReason> reasons = evaluate(account, request.amount(), request.currency());

        return new EligibilityResponseDTO(reasons.isEmpty(), reasons,
                money(request.amount()), money(account.availableBalance()));
    }
```

`reasons.isEmpty()` é o `eligible`. Não precisa de flag, não precisa de enum de decisão.

### 6.3 · O guard do CLOSED · 1min

```java
    private List<RejectionReason> evaluate(Account account, BigDecimal amount, String currency) {
        // A CLOSED account is terminal: reporting the other reasons would imply the payment
        // could be fixed, and it cannot.
        if (account.status() == AccountStatus.CLOSED) {
            return List.of(RejectionReason.ACCOUNT_INACTIVE);
        }

        List<RejectionReason> reasons = new ArrayList<>();
```

**Primeiro de tudo, com `return`.** E o comentário — é a pergunta número um da apresentação.

### 6.4 · Os sete `if` · 3min

Na ordem do enum, sempre:

```java
        if (account.status() != AccountStatus.ACTIVE)  → ACCOUNT_INACTIVE
        if (!account.kycVerified())                    → KYC_NOT_VERIFIED
        if (!account.currency().equals(currency))      → CURRENCY_MISMATCH

        // Ambiguity call: the amount rules still run on a currency mismatch. The caller will
        // fix the currency and resubmit, and reporting every problem in one round-trip beats
        // a second rejection for a limit we already knew was breached.
        if (amount.compareTo(account.minTransaction()) < 0)      → AMOUNT_BELOW_MINIMUM
        if (max != null && amount.compareTo(max) > 0)            → AMOUNT_ABOVE_MAXIMUM
        if (amount.compareTo(account.availableBalance()) > 0)    → INSUFFICIENT_FUNDS
        if (limit != null && spent.add(amount).compareTo(limit) > 0) → DAILY_LIMIT_EXCEEDED

        return reasons;
```

**As quatro armadilhas, todas testadas:**

| Armadilha | O certo |
|---|---|
| `equals` em `BigDecimal` | `compareTo` — `equals` compara escala junto |
| `balance` | `availableBalance()` — saldo retido não é gastável |
| `>=` no máximo | `>` — o limite é inclusivo |
| esquecer o `!= null` | null é ilimitado, não zero. **É o `NullPointerException` que vira 500** |

O comentário da ambiguidade é o único parágrafo do arquivo. É o que separa "implementou" de
"decidiu".

### 6.5 · O `money()` · 1min

```java
    /**
     * The only place scale is touched, and it runs after every rule has already compared the
     * exact value the caller sent. Formatting a response can never change a decision.
     */
    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
```

> ### ✅ CHECKPOINT · roda o teste
> Os testes de regra têm que ficar verdes. **Não passa daqui sem isso.** Se sobrar tempo no
> bloco, usa aqui — é o único lugar onde tempo extra compra nota.

---

## Arquivo 7 · `eligibility/controller/EligibilityController.java` · 0:15 – 0:18

```java
@RestController
@RequestMapping("/api/payments")
public class EligibilityController {

    private final CheckEligibilityUseCase checkEligibilityUseCase;

    public EligibilityController(CheckEligibilityUseCase checkEligibilityUseCase) {
        this.checkEligibilityUseCase = checkEligibilityUseCase;
    }

    /** A rejection is a successful answer: 200 with the reasons, never a 4xx. */
    @PostMapping("/eligibility")
    public EligibilityResponseDTO checkEligibility(@Valid @RequestBody EligibilityRequestDTO request) {
        return checkEligibilityUseCase.execute(request);
    }

}
```

**Três linhas, três decisões:**

- **`@Valid`** — sem ele as annotations do DTO não fazem nada. É o esquecimento mais comum
  da prova inteira.
- **Devolve o DTO direto**, sem `ResponseEntity`. O 200 sai de graça, e é o ponto conceitual
  central: rejeição é resposta bem-sucedida.
- **O comentário.** Quem revisa vai procurar exatamente isso.

> ### ✅ CHECKPOINT · roda o teste

---

## Arquivo 8 · `shared/exception/ErrorCode.java` · 0:18 – 0:19

```java
public enum ErrorCode {
    VALIDATION_ERROR,
    MALFORMED_REQUEST,
    ACCOUNT_NOT_FOUND,
    INTERNAL_ERROR
}
```

---

## Arquivo 9 · `shared/exception/ErrorResponse.java` · 0:19 – 0:21

```java
/**
 * Single error envelope for every failure the API can return. fieldErrors is populated only
 * for VALIDATION_ERROR and left out of the JSON entirely otherwise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        ErrorCode code,
        String message,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

}
```

`@JsonInclude(NON_NULL)` é o que faz `fieldErrors` **sumir** do JSON nos outros três casos,
em vez de aparecer como `null`. Tem teste pra isso.

---

## Arquivo 10 · `shared/exception/GlobalExceptionHandler.java` · 0:21 – 0:26

**Ordem interna importa.** Escreve assim:

### 10.1 · O helper primeiro · 1min

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<ErrorResponse> build(HttpStatus status, ErrorCode code, String message,
            List<ErrorResponse.FieldError> fieldErrors) {

        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now().truncatedTo(ChronoUnit.SECONDS), status.value(), code, message, fieldErrors));
    }
```

Escrever o `build` antes evita repetir a construção do envelope quatro vezes.

### 10.2 · Validação · 1min30

```java
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldError> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();

        return build(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Request validation failed", fieldErrors);
    }
```

**A linha mais difícil de digitar de cabeça da prova inteira** é
`getBindingResult().getFieldErrors()`. Se você decorar uma coisa só deste arquivo, decore
essa. É o que faz o erro chegar no campo certo lá no front.

### 10.3 · Corpo ilegível · 45s

```java
    /** Body that Jackson cannot parse: broken JSON, or "abc" where a number is expected. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformed(HttpMessageNotReadableException exception) {
        return build(HttpStatus.BAD_REQUEST, ErrorCode.MALFORMED_REQUEST, "Malformed request body", null);
    }
```

`{"amount": "abc"}` estoura no Jackson **antes** da validação rodar. Sem esse handler vira
500 — e "digitei letra no campo valor" não é erro de servidor. Tem teste.

### 10.4 · Conta inexistente · 30s

```java
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, ErrorCode.ACCOUNT_NOT_FOUND, exception.getMessage(), null);
    }
```

### 10.5 · O catch-all · **por último** · 30s

```java
    /** Last resort: no stack trace ever reaches the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "Unexpected error", null);
    }
```

O Spring escolhe sempre o handler mais específico, então a ordem no arquivo não muda o
comportamento — mas escrever por último te obriga a lembrar que ele existe. É o que garante
que nenhuma stack trace vaza, e o mais fácil de esquecer.

> ### ✅ CHECKPOINT · roda o teste. **Backend fechado às 0:26.**
> Esse é o seu piso: regras + API + erro = 80% dos pontos possíveis. Se o relógio explodir
> daqui pra frente, você já entregou a maior parte.

---

## Arquivo 11 · `src/api/types.ts` · 0:26 – 0:29

```ts
export type RejectionReason =
  | "ACCOUNT_INACTIVE" | "KYC_NOT_VERIFIED" | "CURRENCY_MISMATCH"
  | "AMOUNT_BELOW_MINIMUM" | "AMOUNT_ABOVE_MAXIMUM"
  | "INSUFFICIENT_FUNDS" | "DAILY_LIMIT_EXCEEDED";

export interface EligibilityRequest { accountId: string; amount: string; currency: string }

export interface EligibilityResponse {
  eligible: boolean;
  reasons: RejectionReason[];
  amount: string;
  availableBalance: string;
}

export type ErrorCode =
  | "VALIDATION_ERROR" | "MALFORMED_REQUEST" | "ACCOUNT_NOT_FOUND" | "INTERNAL_ERROR";

export interface FieldError { field: string; message: string }

export interface ErrorResponse {
  code: ErrorCode;
  message: string;
  fieldErrors?: FieldError[];
}
```

**A união literal do `RejectionReason` não é enfeite:** é ela que faz o TypeScript exigir os
sete labels no mapa lá do `App.tsx`. Esquecer um vira erro de compilação em vez de
`undefined` na tela.

`amount` é `string` dos dois lados. Se vier `number`, você perdeu precisão antes de começar.

---

## Arquivo 12 · `src/api/client.ts` · 0:29 – 0:34

Caminho feliz primeiro, erro depois.

### 12.1 · O tipo do resultado · 1min

```ts
export type CheckResult =
  | { kind: "decided"; response: EligibilityResponse }
  | { kind: "invalid"; fieldErrors: Record<string, string> }
  | { kind: "failed"; message: string }
  | { kind: "aborted" };
```

Devolver união em vez de lançar exceção mata o `try/catch` do componente inteiro.

### 12.2 · O fetch · 1min30

```ts
export async function checkEligibility(
  request: EligibilityRequest,
  signal?: AbortSignal,
): Promise<CheckResult> {
  let response: Response;

  try {
    response = await fetch("/api/payments/eligibility", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
      signal,
    });
  } catch {
    if (signal?.aborted) return { kind: "aborted" };

    return { kind: "failed", message: "Could not reach the eligibility service." };
  }
```

**`headers` com `Content-Type` é obrigatório.** Sem ele o Spring devolve 415 e você perde
minutos procurando no lugar errado.

### 12.3 · O `res.ok` · 30s

```ts
  // fetch only rejects on a network failure, so a 400 arrives here as a resolved promise.
  if (response.ok) {
    return { kind: "decided", response: (await response.json()) as EligibilityResponse };
  }
```

**A linha mais importante do frontend inteiro.** `fetch` só rejeita em falha de rede — um
400 chega como promise *resolvida*. Quem faz `const data = await res.json()` direto trata
rejeição como sucesso.

### 12.4 · O envelope · 2min

```ts
  let envelope: ErrorResponse;

  try {
    envelope = (await response.json()) as ErrorResponse;
  } catch {
    return { kind: "failed", message: "The eligibility service is not responding correctly." };
  }

  if (envelope.code === "VALIDATION_ERROR" && envelope.fieldErrors?.length) {
    return {
      kind: "invalid",
      fieldErrors: Object.fromEntries(envelope.fieldErrors.map((e) => [e.field, e.message])),
    };
  }

  // An unknown account is a problem with what was typed, so it belongs to that field.
  if (envelope.code === "ACCOUNT_NOT_FOUND") {
    return { kind: "invalid", fieldErrors: { accountId: "No account with this id" } };
  }

  return { kind: "failed", message: envelope.message };
}
```

O `catch` do parse é o que pega **backend fora do ar**: o proxy devolve 502 de corpo vazio,
o `.json()` estoura, e vira "serviço indisponível" em vez de o form girar pra sempre.

---

## Arquivo 13 · `src/App.tsx` · 0:34 – 0:42

Oito minutos. Ordem interna:

### 13.1 · O mapa de labels · 1min

```tsx
const REASON_LABELS: Record<RejectionReason, string> = {
  ACCOUNT_INACTIVE: "This account is not active",
  KYC_NOT_VERIFIED: "KYC verification is pending",
  CURRENCY_MISMATCH: "The account does not hold this currency",
  AMOUNT_BELOW_MINIMUM: "Amount is below the minimum for this account",
  AMOUNT_ABOVE_MAXIMUM: "Amount is above the per-transaction limit",
  INSUFFICIENT_FUNDS: "Available balance is not enough",
  DAILY_LIMIT_EXCEEDED: "This would exceed the daily limit",
};
```

Requisito explícito do enunciado: código nunca aparece cru.

### 13.2 · O estado · 1min

```tsx
type Outcome = { kind: "idle" } | { kind: "loading" } | CheckResult;

const [accountId, setAccountId] = useState("ACC-1001");
const [amount, setAmount] = useState("250.00");
const [currency, setCurrency] = useState("USD");
const [outcome, setOutcome] = useState<Outcome>({ kind: "idle" });

const inFlight = useRef<AbortController | null>(null);
```

**Uma união, não quatro booleanos.** Com `loading` + `error` + `result` + `fieldErrors` você
tem 16 combinações e a maioria é inválida. Com união, "carregando e com resultado" não tem
onde existir.

Valores iniciais preenchidos economizam digitação a cada teste manual.

### 13.3 · O submit · 2min

```tsx
async function submit(event: FormEvent) {
  event.preventDefault();

  // Whatever is still in flight belongs to an older submission.
  inFlight.current?.abort();
  const controller = new AbortController();
  inFlight.current = controller;

  setOutcome({ kind: "loading" });

  const result = await checkEligibility({ accountId, amount, currency }, controller.signal);

  if (result.kind === "aborted") return;

  setOutcome(result);
}
```

`event.preventDefault()` primeiro — sem ele a página recarrega e nada funciona.

Sem `try/catch`: o client já devolve tudo como resultado.

### 13.4 · O JSX · 3min

```tsx
<form onSubmit={submit} noValidate>
  <Field name="accountId" label="Account" value={accountId} onChange={setAccountId} error={errors.accountId} />
  <Field name="amount"    label="Amount"  value={amount}    onChange={setAmount}    error={errors.amount} />
  <Field name="currency"  label="Currency" value={currency} onChange={setCurrency}  error={errors.currency} />

  <button type="submit" disabled={loading}>
    {loading ? "Checking…" : "Check eligibility"}
  </button>
</form>
```

com

```tsx
const loading = outcome.kind === "loading";
const errors = outcome.kind === "invalid" ? outcome.fieldErrors : {};
```

`noValidate` desliga a validação do browser — você quer que o request chegue no backend e
volte com o envelope, que é o que está sendo avaliado.

### 13.5 · O `Field` e o resultado · 1min

`Field` é label + input + `{error && <span>}`, com `aria-invalid={Boolean(error)}`.

O painel: título Approved/Rejected, `<ul>` com os labels dos motivos, e — separado, com cor
e `role="alert"` — o painel de falha, que **nunca** pode parecer rejeição.

> ### ✅ CHECKPOINT · abre o browser e submete uma vez
> Um caso aprovado e um rejeitado. Se os dois renderizam, acabou.

---

## Arquivo 14 · `src/index.css` · 0:42 – 0:45 · **só se sobrar**

Vinte linhas no máximo: largura da coluna, vermelho no erro, borda no painel. Nenhum critério
de avaliação olha pra estilo.

**Se não sobrar tempo, não escreve.** HTML sem estilo com os campos certos pontua igual.

---

## 0:42 – 0:45 · Buffer · não comece nada novo

1. Roda a suíte inteira uma última vez
2. Se estiver vermelho, conserta
3. Se estiver verde, escreve **um** `TODO` curto dizendo o que faria em seguida e por quê

Plano declarado pontua mais que silêncio. Começar uma feature nova no minuto 42 pontua menos
que os dois.

---

## Tabela de corte

| Se às… | você ainda não terminou | corta |
|---|---|---|
| 0:15 | as regras | o CSS, definitivamente |
| 0:26 | o backend | o `AbortController` e o `Field` extraído |
| 0:36 | o form | o painel de resumo e todo enfeite |

**Nunca corta:** o 200 na rejeição · o `res.ok` · o envelope de erro.
São metade da nota e cabem em cem linhas.

---

## Se não vier scaffold nenhum

Mesma ordem de arquivos. Sem os checkpoints de teste — sobe o backend e testa com curl ou
pelo browser. Só escreve teste no fim, se sobrar: dois, nas regras. Em 45 minutos do zero,
suíte completa é luxo que custa a metade que vale mais.
