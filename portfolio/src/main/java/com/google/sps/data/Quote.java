package com.google.sps.data;

/** Class that models a quote. */
public final class Quote {

  private final long id;
  private final String text;
  private final long timestampMillis;
  private final String userEmail;  
  private final String nickname;

  public Quote(long id, String text, long timestampMillis, String userEmail, String nickname) {
    this.id = id;
    this.text = text;
    this.timestampMillis = timestampMillis;
    this.userEmail = userEmail; 
    this.nickname = nickname; 
  }
}
