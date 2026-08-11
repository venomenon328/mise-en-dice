package io.github.venomenon328.miseendice.administration.internal;

import java.util.Optional;

/** Internal seam for replacing configuration-based administration identities later. */
public interface AdministrationIdentitySource {

    Optional<AdministrationIdentity> findByActorKey(String actorKey);
}
