package com.pe.pivot.transaction.support;

import io.reactivex.rxjava3.functions.Predicate;

/**
 * Reusable predicates for reactive terminal error events.
 */
public final class TestAssertions {

  private TestAssertions() {
  }

  /**
   * Returns a predicate that checks the message of a reactive error event.
   *
   * @param expectedMessage the expected error message
   * @return a predicate that validates the received throwable message
   */
  public static Predicate<Throwable> hasErrorMessage(String expectedMessage) {
    return error -> expectedMessage.equals(error.getMessage());
  }
}
