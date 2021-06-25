/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.guava;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class NoGuavaSetsNewConcurrentHashSet extends Recipe {
    private static final MethodMatcher NEW_HASH_SET = new MethodMatcher("com.google.common.collect.Sets newConcurrentHashSet()");

    @Override
    public String getDisplayName() {
        return "Construct a set from a `new ConcurrentHashMap<>()` instead of Guava";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new UsesMethod<>(NEW_HASH_SET);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate newConcurrentHashSet = JavaTemplate.builder(this::getCursor, "Collections.newSetFromMap(new ConcurrentHashMap<>())")
                    .imports("java.util.Collections")
                    .imports("java.util.concurrent.ConcurrentHashMap")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                if (NEW_HASH_SET.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Sets");
                    maybeAddImport("java.util.Collections");
                    maybeAddImport("java.util.concurrent.ConcurrentHashMap");
                    return method.withTemplate(newConcurrentHashSet, method.getCoordinates().replace());
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }
}
