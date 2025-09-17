# Reactive Programming with Spring WebFlux and Project Reactor

This guide explains reactive programming in the context of Spring WebFlux and Project Reactor, contrasts reactive (Netty) vs classic servlet (Tomcat) stacks, and provides practical Mono/Flux examples.

## 1) What is Reactive?

Reactive systems are designed for non-blocking I/O, backpressure, and high concurrency with fewer threads. Instead of dedicating a thread per request, reactive stacks use event loops and callbacks to continue work when data arrives, improving resource efficiency under load.

- Non-blocking I/O: Threads are not blocked waiting for data; they schedule continuation when data is available.
- Backpressure: Consumers can signal demand (how much they can handle) to producers to avoid overload.
- Compositional pipelines: Transformations are chained via operators.

Spring WebFlux is Spring’s reactive web framework built on Project Reactor (Reactive Streams implementation). It runs on servers like Netty (reactor-netty), Undertow, or even Tomcat (but then still using reactive adapters).

## 2) Tomcat (Servlet) vs Netty (Reactive) – How they work

### Servlet (Tomcat) model
- Thread per request model (or a small pool that blocks on I/O).
- Typical pattern: `HttpServlet` or Spring MVC controllers.
- When your code performs I/O (DB calls, HTTP calls), the thread is blocked until data returns.
- Under high latency or spikes, threads can be exhausted, causing timeouts and degraded throughput.

### Reactive (Netty) model
- Event loop model; a few non-blocking threads handle many connections.
- I/O returns immediately; you register a continuation (callback) to resume work when data arrives.
- Works best when the whole call chain is non-blocking (HTTP clients, DB drivers, etc.).
- Backpressure propagates demand to avoid overwhelming downstream services.

### When to choose what
- Choose WebFlux/Netty when:
  - You have many I/O-bound calls (HTTP, DB) and want efficient resource use.
  - You need streaming and backpressure.
  - You’re aggregating multiple sources (fan-out/fan-in) with high concurrency.
- Choose MVC/Tomcat when:
  - You rely heavily on blocking libraries/drivers.
  - Simpler synchronous code path is preferable and load is moderate.

## 3) Threading and Backpressure

- Reactor schedulers manage threads: `Schedulers.boundedElastic()` for blocking tasks, `Schedulers.parallel()` for CPU-bound tasks.
- Backpressure is supported by Reactive Streams; `Flux` can signal how many elements a subscriber can handle.

## 4) Core Reactor Types

- `Mono<T>`: 0..1 element async publisher (think of a single result or empty)
- `Flux<T>`: 0..N elements async stream (finite or infinite)

Operators are chainable: `map`, `flatMap`, `filter`, `zip`, `concat`, `merge`, `switchIfEmpty`, error handling operators, etc.

## 5) Sample Code – Mono and Flux

> The snippets are illustrative and can be adapted into your services.

### 5.1 Mono basics

```java
Mono<String> mono = Mono.just("hello")
    .map(String::toUpperCase)           // transform
    .doOnNext(v -> log.info("value={}", v))
    .flatMap(v -> Mono.delay(Duration.ofMillis(50)).thenReturn(v + "!"));

mono.subscribe(v -> log.info("result={}", v));
```

### 5.2 Flux basics

```java
Flux<Integer> flux = Flux.range(1, 5)
    .filter(i -> i % 2 == 1)           // 1,3,5
    .map(i -> i * 10);                  // 10,30,50

flux.subscribe(v -> log.info("v={}", v));
```

### 5.3 Error handling

```java
Mono<String> risky = Mono.fromCallable(() -> {
    if (Math.random() > 0.5) throw new IllegalStateException("boom");
    return "ok";
});

risky
    .onErrorResume(ex -> Mono.just("fallback"))
    .doOnError(ex -> log.error("error", ex))
    .subscribe(v -> log.info("v={}", v));
```

### 5.4 Timeouts and retries

```java
Mono<String> call = webClient.get().uri("/slow").retrieve().bodyToMono(String.class);

call
    .timeout(Duration.ofSeconds(2))
    .retryWhen(Retry.fixedDelay(3, Duration.ofMillis(200)))
    .subscribe();
```

### 5.5 Parallelization (CPU-bound)

```java
Flux<Integer> data = Flux.range(1, 20);

Flux<Integer> processed = data
    .parallel()
    .runOn(Schedulers.parallel())
    .map(i -> i * i)
    .sequential();
```

### 5.6 Offloading blocking work

```java
Mono<String> blockingCall = Mono.fromCallable(() -> blockingIo())
    .subscribeOn(Schedulers.boundedElastic());
```

### 5.7 Combining publishers

```java
Mono<String> a = webClient.get().uri("/a").retrieve().bodyToMono(String.class);
Mono<String> b = webClient.get().uri("/b").retrieve().bodyToMono(String.class);

Mono<String> combined = Mono.zip(a, b)
    .map(tuple -> tuple.getT1() + ":" + tuple.getT2());
```

### 5.8 Hot vs Cold publishers

- Cold: Each subscription triggers a fresh data sequence (default for Flux/Mono).
- Hot: Emissions occur regardless of subscribers; new subscribers may miss past data (use `Sinks`, `ConnectableFlux`).

```java
Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
Flux<String> hot = sink.asFlux();

sink.tryEmitNext("event1");
hot.subscribe(e -> log.info("sub1={}", e));
sink.tryEmitNext("event2");
```

### 5.9 Backpressure control

```java
Flux<Integer> source = Flux.range(1, 1000)
    .onBackpressureBuffer(100, dropped -> log.warn("dropped={}", dropped));

source.subscribe(new BaseSubscriber<>() {
    @Override
    protected void hookOnSubscribe(Subscription subscription) {
        request(10); // initial demand
    }
    @Override
    protected void hookOnNext(Integer value) {
        // process
        request(1); // request next
    }
});
```

### 5.10 Streaming responses (Server-Sent Events)

```java
@GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> stream() {
    return Flux.interval(Duration.ofSeconds(1))
               .map(i -> "tick-" + i);
}
```

## 6) WebFlux vs MVC at a glance

| Aspect | Spring MVC (Tomcat) | Spring WebFlux (Netty) |
|---|---|---|
| Concurrency model | Thread-per-request (blocking) | Event loop (non-blocking) |
| I/O | Blocking drivers | Non-blocking drivers |
| Throughput under I/O wait | Lower (threads occupied) | Higher (few threads, many requests) |
| Backpressure | Not built-in | First-class via Reactive Streams |
| Learning curve | Lower | Higher |
| Libraries | Massive (blocking) | Fewer mature non-blocking drivers (but growing) |

## 7) Practical guidance

- Go all-in reactive: Avoid mixing blocking libraries in the reactive path. If unavoidable, isolate with `boundedElastic()` and document it.
- Instrument everything: Use metrics and tracing (Micrometer + OpenTelemetry) to validate thread usage and latency.
- Tune Netty: Connection pools, timeouts, max in-memory sizes. Avoid huge in-memory buffers.
- Test with realistic latency: Reactive shines when there’s I/O wait.

## 8) References

- Reactive Streams: https://www.reactive-streams.org/
- Project Reactor: https://projectreactor.io/
- Spring WebFlux: https://docs.spring.io/spring-framework/reference/web/webflux.html
- Reactor reference guide: https://projectreactor.io/docs/core/release/reference/



