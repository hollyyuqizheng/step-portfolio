package com.google.sps.data;

/** Class that models a quote. */
public final class Quote {

  private final long id;
  private final String text;
  private final long timestampMillis;

  public Quote(long id, String text, long timestampMillis) {
    this.id = id;
    this.text = text;
    this.timestampMillis = timestampMillis;
  }
}
