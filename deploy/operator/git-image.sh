#!/usr/bin/env bash

# Safe Git-ref resolution and commit-addressed application image builds.

resolve_ref() {
    local requested_ref=$1
    med_valid_ref_input "$requested_ref" || med_die "Ungültiger Git-Ref: $requested_ref"

    med_note 'Aktualisiere Remote-Refs ...'
    git -C "$REPOSITORY_ROOT" fetch --quiet --prune --tags origin '+refs/heads/*:refs/remotes/origin/*'

    local candidate sha
    if [[ $requested_ref =~ ^[0-9A-Fa-f]{7,40}$ ]]; then
        if sha=$(git -C "$REPOSITORY_ROOT" rev-parse --verify --quiet "${requested_ref}^{commit}"); then
            printf '%s' "$sha"
            return 0
        fi
        med_die "Commit nicht gefunden oder nicht eindeutig: $requested_ref"
    fi

    if [[ $requested_ref == refs/* ]]; then
        git -C "$REPOSITORY_ROOT" check-ref-format "$requested_ref" >/dev/null 2>&1 \
            || med_die "Ungültiger vollständiger Git-Ref: $requested_ref"
        candidate=$requested_ref
    elif [[ $requested_ref == origin/* ]]; then
        candidate="refs/remotes/$requested_ref"
    else
        git -C "$REPOSITORY_ROOT" check-ref-format "refs/heads/$requested_ref" >/dev/null 2>&1 \
            || med_die "Ungültiger Branch- oder Tag-Name: $requested_ref"
        candidate="refs/remotes/origin/$requested_ref"
    fi

    if sha=$(git -C "$REPOSITORY_ROOT" rev-parse --verify --quiet "${candidate}^{commit}"); then
        printf '%s' "$sha"
        return 0
    fi

    if [[ $requested_ref != refs/* && $requested_ref != origin/* ]] \
        && sha=$(git -C "$REPOSITORY_ROOT" rev-parse --verify --quiet "refs/tags/${requested_ref}^{commit}"); then
        printf '%s' "$sha"
        return 0
    fi

    med_die "Remote-Branch, Tag oder Commit nicht gefunden: $requested_ref"
}

cleanup_worktree() {
    local worktree=$1
    if [[ -d $worktree ]]; then
        git -C "$REPOSITORY_ROOT" worktree remove --force "$worktree" >/dev/null 2>&1 || rm -rf "$worktree"
    fi
    git -C "$REPOSITORY_ROOT" worktree prune >/dev/null 2>&1 || true
}

verify_image_user() {
    local image=$1
    local image_user
    image_user=$(docker image inspect --format '{{.Config.User}}' "$image")
    [[ -n $image_user && $image_user != 0 && $image_user != root && $image_user != 0:0 ]] \
        || med_die "Das Image läuft nicht als expliziter Nicht-Root-Benutzer: $image_user"
}

build_image() {
    local requested_ref=$1
    local sha image worktree
    sha=$(resolve_ref "$requested_ref")
    image="mise-en-dice:git-${sha:0:12}"

    if docker image inspect "$image" >/dev/null 2>&1; then
        verify_image_user "$image"
        med_note "Verwende bereits gebautes Image $image"
        printf '%s\t%s\n' "$sha" "$image"
        return 0
    fi

    worktree="$WORKTREES_DIR/build-${sha:0:12}-$$"
    cleanup_worktree "$worktree"
    med_note "Erzeuge Build-Worktree für ${sha:0:12} ..."
    git -C "$REPOSITORY_ROOT" worktree add --quiet --detach "$worktree" "$sha"

    local build_result=0
    med_note "Baue $image ..."
    DOCKER_BUILDKIT=1 docker build \
        --build-arg "VCS_REF=$sha" \
        --build-arg "BUILD_REF=$requested_ref" \
        --label "io.mise-en-dice.source-ref=$requested_ref" \
        --tag "$image" \
        "$worktree" >&2 || build_result=$?

    cleanup_worktree "$worktree"
    (( build_result == 0 )) || med_die "Docker-Build fehlgeschlagen für $requested_ref"
    verify_image_user "$image"

    printf '%s\t%s\n' "$sha" "$image"
}

