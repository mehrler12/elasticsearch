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

import org.elasticsearch.painless.Location;
import org.elasticsearch.painless.Operation;
import org.elasticsearch.painless.Scope;
import org.elasticsearch.painless.ir.BooleanNode;
import org.elasticsearch.painless.ir.ClassNode;
import org.elasticsearch.painless.symbol.ScriptRoot;

import java.util.Objects;

/**
 * Represents a boolean expression.
 */
public class EBool extends AExpression {

    protected final Operation operation;
    protected final AExpression left;
    protected final AExpression right;

    public EBool(Location location, Operation operation, AExpression left, AExpression right) {
        super(location);

        this.operation = Objects.requireNonNull(operation);
        this.left = Objects.requireNonNull(left);
        this.right = Objects.requireNonNull(right);
    }

    @Override
    Output analyze(ClassNode classNode, ScriptRoot scriptRoot, Scope scope, Input input) {
        Output output = new Output();

        Input leftInput = new Input();
        leftInput.expected = boolean.class;
        Output leftOutput = left.analyze(classNode, scriptRoot, scope, leftInput);
        left.cast(leftInput, leftOutput);

        Input rightInput = new Input();
        rightInput.expected = boolean.class;
        Output rightOutput = right.analyze(classNode, scriptRoot, scope, rightInput);
        right.cast(rightInput, rightOutput);

        output.actual = boolean.class;

        BooleanNode booleanNode = new BooleanNode();

        booleanNode.setLeftNode(left.cast(leftOutput));
        booleanNode.setRightNode(right.cast(rightOutput));

        booleanNode.setLocation(location);
        booleanNode.setExpressionType(output.actual);
        booleanNode.setOperation(operation);

        output.expressionNode = booleanNode;

        return output;
    }

    @Override
    public String toString() {
        return singleLineToString(left, operation.symbol, right);
    }
}
