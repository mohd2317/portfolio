package com.portfolio.exception;

/** Thrown when a requested entity does not exist; mapped to HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String what, Long id) {
        super(what + " " + id + " not found");
    }
}
