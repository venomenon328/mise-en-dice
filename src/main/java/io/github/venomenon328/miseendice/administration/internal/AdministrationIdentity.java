package io.github.venomenon328.miseendice.administration.internal;

/** A non-domain identity that can be attributed to catalog administration changes. */
public record AdministrationIdentity(String actorKey, String displayName) {
}
