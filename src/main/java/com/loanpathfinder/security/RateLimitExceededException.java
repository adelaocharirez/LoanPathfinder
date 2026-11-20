package com.loanpathfinder.security;

/** Thrown on the 21st submission from one IP address within an hour (NFR-005). */
public class RateLimitExceededException extends RuntimeException {
    public RateLimitExceededException() {
        super("Too many requests, please try again later");
    }
}
