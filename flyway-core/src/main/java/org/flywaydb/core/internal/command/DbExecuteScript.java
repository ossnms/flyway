/*
 * Copyright © Red Gate Software Ltd 2010-2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flywaydb.core.internal.command;

import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.executor.Context;
import org.flywaydb.core.api.logging.Log;
import org.flywaydb.core.api.logging.LogFactory;
import org.flywaydb.core.api.output.CommandResultFactory;
import org.flywaydb.core.api.output.ExecuteScriptResult;
import org.flywaydb.core.api.resolver.MigrationResolver;
import org.flywaydb.core.api.resolver.ResolvedMigration;
import org.flywaydb.core.internal.database.base.Connection;
import org.flywaydb.core.internal.database.base.Database;
import org.flywaydb.core.internal.database.base.Schema;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DbExecuteScript {

    private static final Log LOG = LogFactory.getLog(DbExecuteScript.class);

    private final Database database;
    private final Schema schema;
    private final MigrationResolver migrationResolver;
    private final Configuration configuration;
    private final Predicate<ResolvedMigration> filter;
    private final Connection connectionUserObjects;

    public DbExecuteScript(Database database,
                           Schema schema,
                           MigrationResolver migrationResolver,
                           Configuration configuration,
                           Predicate<ResolvedMigration> filter) {
        this.database = database;
        this.schema = schema;
        this.connectionUserObjects = database.getMigrationConnection();
        this.migrationResolver = migrationResolver;
        this.configuration = configuration;
        this.filter = filter;
    }


    public ExecuteScriptResult executeScript() throws FlywayException {

        final Stream<ResolvedMigration> resolvedMigrations = migrationResolver.resolveMigrations(() -> configuration).stream()
                .filter(filter);

        Context context = new Context() {
            @Override
            public Configuration getConfiguration() {
                return configuration;
            }

            @Override
            public java.sql.Connection getConnection() {
                return connectionUserObjects.getJdbcConnection();
            }
        };


        resolvedMigrations
                .sorted(Comparator.comparing(ResolvedMigration::getVersion))
                .forEachOrdered(migration -> {
                    LOG.info("Executing " + migration.getScript());
                    connectionUserObjects.restoreOriginalState();
                    connectionUserObjects.changeCurrentSchemaTo(schema);
                    try {
                        migration.getExecutor().execute(context);
                        LOG.info("Executed " + migration.getScript());
                    } catch (Exception throwables) {
                        throw new FlywayException("Failed to execute " + migration, throwables);
                    }
                });


        return CommandResultFactory.createExecuteScriptResult(database.getCatalog(), configuration);

    }
}
