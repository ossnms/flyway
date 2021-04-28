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
package org.flywaydb.core.internal.util;

import org.flywaydb.core.internal.schemahistory.AppliedMigrationExtensions;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class AbbreviationUtilsTest {


    @Test
    void abbreviateSmall() {
        JSONObject obj = new JSONObject();
        assertTrue(AbbreviationUtils.abbreviateExtension(obj).length() <= 200);

    }

    @Test
    void abbreviateSmallWithDesc() {
        JSONObject obj = new JSONObject();
        final String foo = "foo";
        obj.putOpt(AppliedMigrationExtensions.DESCRIPTION.getKey(), foo);
        obj.putOpt(AppliedMigrationExtensions.IGNORE_PAST.getKey(), "true");

        final String res = AbbreviationUtils.abbreviateExtension(obj);
        assertTrue(res.length() <= 200);
        assertTrue(res.contains(foo));
    }

    @Test
    void abbreviateLongWithDesc() {
        JSONObject obj = new JSONObject();
        final String foo = "foo";
        obj.putOpt(AppliedMigrationExtensions.DESCRIPTION.getKey(), IntStream.range(0, 200).mapToObj(i -> "x").collect(Collectors.joining()));
        obj.putOpt(AppliedMigrationExtensions.IGNORE_PAST.getKey(), "true");
        final String res = AbbreviationUtils.abbreviateExtension(obj);
        assertTrue(res.length() <= 200,  () -> "extension contains " + res.length()  + " characters");

    }
}