/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.painless.node;

import org.elasticsearch.painless.AnalyzerCaster;
import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.Operation;
import org.elasticsearch.painless.Scope;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.ir.ComparisonNode;
import org.elasticsearch.painless.lookup.PainlessLookupUtility;
import org.elasticsearch.painless.lookup.def;
import org.elasticsearch.painless.symbol.ScriptRoot;

import java.util.Objects;

/**
 * Represents a comparison expression.
 */
public class EComp extends AExpression {

    protected final Operation operation;
    protected final AExpression left;
    protected final AExpression right;

    public EComp(Location location, Operation operation, AExpression left, AExpression right) {
        super(location);

        this.operation = Objects.requireNonNull(operation);
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
    }

    @Override
    Output analyze(ClassNode classNode, ScriptRoot scriptRoot, Scope scope, Input input) {
        Class<?> promotedType;

        Output output = new Output();

        Input leftInput = new Input();
        Output leftOutput = left.analyze(classNode, scriptRoot, scope, leftInput);

        Input rightInput = new Input();
        Output rightOutput = right.analyze(classNode, scriptRoot, scope, rightInput);

        if (operation == Operation.EQ || operation == Operation.EQR || operation == Operation.NE || operation == Operation.NER) {
            promotedType = AnalyzerCaster.promoteEquality(leftOutput.actual, rightOutput.actual);
        } else if (operation == Operation.GT || operation == Operation.GTE || operation == Operation.LT || operation == Operation.LTE) {
            promotedType = AnalyzerCaster.promoteNumeric(leftOutput.actual, rightOutput.actual, true);
        } else {
            throw createError(new IllegalStateException("unexpected binary operation [" + operation.name + "]"));
        }

        if (promotedType == null) {
            throw createError(new ClassCastException("cannot apply the " + operation.name + " operator " +
                    "[" + operation.symbol + "] to the types " +
                    "[" + PainlessLookupUtility.typeToCanonicalTypeName(leftOutput.actual) + "] and " +
                    "[" + PainlessLookupUtility.typeToCanonicalTypeName(rightOutput.actual) + "]"));
        }

        if (operation != Operation.EQR && operation != Operation.NER && promotedType == def.class) {
            leftInput.expected = leftOutput.actual;
            rightInput.expected = rightOutput.actual;
        } else {
            leftInput.expected = promotedType;
            rightInput.expected = promotedType;
        }

        if ((operation == Operation.EQ || operation == Operation.EQR || operation == Operation.NE || operation == Operation.NER)
                && left instanceof ENull && right instanceof ENull) {
            throw createError(new IllegalArgumentException("extraneous comparison of [null] constants"));
        }

        left.cast(leftInput, leftOutput);
        right.cast(rightInput, rightOutput);

        output.actual = boolean.class;

        ComparisonNode comparisonNode = new ComparisonNode();

        comparisonNode.setLeftNode(left.cast(leftOutput));
        comparisonNode.setRightNode(right.cast(rightOutput));

        comparisonNode.setLocation(location);
        comparisonNode.setExpressionType(output.actual);
        comparisonNode.setComparisonType(promotedType);
        comparisonNode.setOperation(operation);

        output.expressionNode = comparisonNode;

        return output;
    }

    @Override
    public String toString() {
        return singleLineToString(left, operation.symbol, right);
    }
}
