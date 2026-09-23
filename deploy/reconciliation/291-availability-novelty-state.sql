SELECT CASE
    WHEN (SELECT count(*) FROM databasechangelog
          WHERE id = '033-availability-novelty-final-review') = 1
     AND (SELECT count(*) FROM databasechangelog
          WHERE id = '033-availability-novelty-final-review'
            AND author = 'venomenon328'
            AND filename = 'db/changelog/catalog/033-availability-novelty-final-review.sql'
            AND exectype IN ('EXECUTED', 'MARK_RAN')) = 1
        THEN 'APPLIED'
    WHEN (SELECT count(*) FROM databasechangelog
          WHERE id = '033-availability-novelty-final-review') = 0
     AND (SELECT count(*) FROM databasechangelog
          WHERE (id, author, filename) IN (
              ('031-vietnam-curation', 'venomenon328', 'db/changelog/catalog/031-vietnam-curation.sql'),
              ('032-thailand-curation', 'venomenon328', 'db/changelog/catalog/032-thailand-curation.sql'),
              ('018-five-level-availability', 'venomenon328', 'db/changelog/schema/018-five-level-availability.sql'),
              ('019-availability-curator-note', 'venomenon328', 'db/changelog/schema/019-availability-curator-note.sql')
          )) = 4
     AND (SELECT orderexecuted FROM databasechangelog
          WHERE id = '019-availability-curator-note'
            AND author = 'venomenon328'
            AND filename = 'db/changelog/schema/019-availability-curator-note.sql')
         = (SELECT max(orderexecuted) FROM databasechangelog)
        THEN 'PENDING'
    ELSE 'INVALID'
END;
