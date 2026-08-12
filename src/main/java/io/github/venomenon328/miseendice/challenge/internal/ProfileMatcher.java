package io.github.venomenon328.miseendice.challenge.internal;

import io.github.venomenon328.miseendice.challenge.api.GeneratorConfiguration;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.CandidateProfile;
import io.github.venomenon328.miseendice.challenge.api.GeneratorModel.ProfileSlot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Deterministic bipartite matching for multi-role requirements and profile slots. */
final class ProfileMatcher {
    private ProfileMatcher() {
    }

    static Match match(
            CandidateProfile profile,
            List<RoleRequirement> requirements,
            GeneratorConfiguration configuration
    ) {
        List<ProfileSlot> slots = configuration.profiles().get(profile).requiredSlots().stream()
                .sorted(Comparator.comparing(Enum::name)).toList();
        List<RoleRequirement> ordered = requirements.stream().sorted(RoleRequirement.CANONICAL_ORDER).toList();
        Search search = new Search(slots, ordered, configuration);
        search.visit(0, new HashSet<>(), new ArrayList<>());
        return new Match(search.best.size() == slots.size(), List.copyOf(search.best),
                slots.stream().filter(slot -> search.best.stream().noneMatch(item -> item.slot() == slot)).toList());
    }

    static boolean supports(ProfileSlot slot, Set<String> roles, GeneratorConfiguration configuration) {
        return switch (slot) {
            case ANCHOR_1, ANCHOR_2, ANCHOR_3 -> intersects(roles, configuration.anchorRoles());
            case PRODUCE_1, PRODUCE_2 -> roles.contains("VEGETABLE") || roles.contains("FRUIT");
            case PROTEIN -> roles.contains("ANIMAL_PROTEIN") || roles.contains("PLANT_PROTEIN");
            case PROTEIN_OR_PRODUCE -> roles.contains("ANIMAL_PROTEIN") || roles.contains("PLANT_PROTEIN")
                    || roles.contains("VEGETABLE") || roles.contains("FRUIT");
            case STARCH -> roles.contains("STARCH");
        };
    }

    private static boolean intersects(Set<String> left, Set<String> right) {
        return left.stream().anyMatch(right::contains);
    }

    record RoleRequirement(String code, long id, Set<String> roles) {
        private static final Comparator<RoleRequirement> CANONICAL_ORDER =
                Comparator.comparing(RoleRequirement::code).thenComparingLong(RoleRequirement::id);

        RoleRequirement {
            roles = Set.copyOf(roles);
        }
    }

    record SlotAssignment(ProfileSlot slot, RoleRequirement requirement) {
        String diagnostic() {
            return slot.name() + "=" + requirement.code();
        }
    }

    record Match(boolean complete, List<SlotAssignment> assignments, List<ProfileSlot> unfilledSlots) {
    }

    private static final class Search {
        private final List<ProfileSlot> slots;
        private final List<RoleRequirement> requirements;
        private final GeneratorConfiguration configuration;
        private List<SlotAssignment> best = List.of();

        private Search(List<ProfileSlot> slots, List<RoleRequirement> requirements,
                       GeneratorConfiguration configuration) {
            this.slots = slots;
            this.requirements = requirements;
            this.configuration = configuration;
        }

        private void visit(int slotIndex, Set<Integer> used, List<SlotAssignment> assignments) {
            if (slotIndex == slots.size()) {
                keep(assignments);
                return;
            }
            ProfileSlot slot = slots.get(slotIndex);
            for (int requirementIndex = 0; requirementIndex < requirements.size(); requirementIndex++) {
                RoleRequirement requirement = requirements.get(requirementIndex);
                if (!used.contains(requirementIndex) && supports(slot, requirement.roles(), configuration)) {
                    used.add(requirementIndex);
                    assignments.add(new SlotAssignment(slot, requirement));
                    visit(slotIndex + 1, used, assignments);
                    assignments.removeLast();
                    used.remove(requirementIndex);
                }
            }
            visit(slotIndex + 1, used, assignments);
        }

        private void keep(List<SlotAssignment> candidate) {
            if (candidate.size() > best.size()) {
                best = List.copyOf(candidate);
            }
        }
    }
}
