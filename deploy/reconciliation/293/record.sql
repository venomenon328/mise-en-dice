-- Record the exact corridor only after all writes and constraints have succeeded.
INSERT INTO databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype,
    md5sum, description, comments, liquibase, deployment_id)
SELECT h.id,h.author,h.filename,current_timestamp,
       (SELECT max(orderexecuted) FROM databasechangelog) + h.position - 62,
       'MARK_RAN',NULL,'sql','Issue #293: equivalent forward reconciliation',
       (SELECT liquibase FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 1),
       (SELECT deployment_id FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 1)
FROM issue_293_history h WHERE h.position > 62 ORDER BY h.position;
INSERT INTO databasechangelog (id,author,filename,dateexecuted,orderexecuted,exectype,description,comments)
SELECT '293-editorial-upgrade-corridor','venomenon328','deploy/reconciliation/293',
       current_timestamp,max(orderexecuted)+1,'EXECUTED','sql','Issue #293 complete atomic corridor'
FROM databasechangelog;
DROP TABLE issue_293_history;
COMMIT;
