package com.pe.pivot.transaction.support;

import java.util.function.Function;
import org.springframework.cloud.client.circuitbreaker.ConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Pass-through circuit breaker factory used by output adapter tests.
 *
 * <p>The real resilience4j factory is replaced by a deterministic implementation that simply
 * delegates to the supplied fallback whenever the upstream publisher fails. This keeps the tests
 * fast and free of timing-based flakiness while still exercising the adapter fallback logic.
 */
public final class TestCircuitBreakerFactory {

  private TestCircuitBreakerFactory() {
  }

  /**
   * Creates a circuit breaker factory that invokes the fallback on every error.
   *
   * @return a deterministic pass-through circuit breaker factory
   */
  public static ReactiveCircuitBreakerFactory<Object, ConfigBuilder<Object>> passThrough() {
    return new ReactiveCircuitBreakerFactory<>() {
      @Override
      public ReactiveCircuitBreaker create(String id) {
        return new ReactiveCircuitBreaker() {
          @Override
          public <T> Mono<T> run(Mono<T> toRun, Function<Throwable, Mono<T>> fallback) {
            return toRun.onErrorResume(fallback);
          }

          @Override
          public <T> Flux<T> run(Flux<T> toRun, Function<Throwable, Flux<T>> fallback) {
            return toRun.onErrorResume(fallback);
          }
        };
      }

      @Override
      protected ConfigBuilder<Object> configBuilder(String id) {
        return Object::new;
      }

      @Override
      public void configureDefault(Function<String, Object> defaultConfiguration) {
        // No configuration is required for the deterministic test factory.
      }
    };
  }
}
