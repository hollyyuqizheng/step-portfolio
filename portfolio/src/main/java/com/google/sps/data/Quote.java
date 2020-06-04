package com.google.sps.data;

/** Class that models a quote. */
public final class Quote {

  private final long id;
  private final String text;
  private final long timestamp;

  public Quote(long id, String text, long timestamp) {
    this.id = id;
    this.text = text;
    this.timestamp = timestamp;
  }
}
