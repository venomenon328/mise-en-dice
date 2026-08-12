package io.github.venomenon328.miseendice.catalog.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkAction;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkOperation;
import io.github.venomenon328.miseendice.catalog.api.CatalogBulkCommands.BulkSelection;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class CatalogBulkCommandsTest {

    @Test
    void requiresBetweenOneAndTwoHundredUniqueExplicitConcepts() {
        assertThatThrownBy(() -> new BulkOperation(
                List.of(), BulkAction.ACTIVATE, null, null, false, "admin"))
                .isInstanceOf(CatalogCommandValidationException.class);

        List<BulkSelection> tooMany = LongStream.rangeClosed(1, 201)
                .mapToObj(id -> new BulkSelection(id, 0))
                .toList();
        assertThatThrownBy(() -> new BulkOperation(
                tooMany, BulkAction.ACTIVATE, null, null, false, "admin"))
                .isInstanceOf(CatalogCommandValidationException.class);

        assertThatThrownBy(() -> new BulkOperation(
                List.of(new BulkSelection(7, 0), new BulkSelection(7, 0)),
                BulkAction.ACTIVATE, null, null, false, "admin"))
                .isInstanceOf(CatalogCommandValidationException.class);
    }
}
