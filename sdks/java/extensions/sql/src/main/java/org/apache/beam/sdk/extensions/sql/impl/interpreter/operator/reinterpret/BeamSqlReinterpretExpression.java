/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.sdk.extensions.sql.impl.interpreter.operator.reinterpret;

import java.util.List;
import org.apache.beam.sdk.extensions.sql.impl.interpreter.BeamSqlExpressionEnvironment;
import org.apache.beam.sdk.extensions.sql.impl.interpreter.operator.BeamSqlExpression;
import org.apache.beam.sdk.extensions.sql.impl.interpreter.operator.BeamSqlPrimitive;
import org.apache.beam.sdk.transforms.windowing.BoundedWindow;
import org.apache.beam.sdk.values.Row;
import org.apache.calcite.sql.type.SqlTypeFamily;
import org.apache.calcite.sql.type.SqlTypeName;

/**
 * {@code BeamSqlExpression} for Reinterpret call.
 *
 * <p>Currently supported conversions: - {@link SqlTypeName#DATETIME_TYPES} to {@code BIGINT}; -
 * {@link SqlTypeName#INTEGER} to {@code BIGINT};
 */
public class BeamSqlReinterpretExpression extends BeamSqlExpression {

  private static final Reinterpreter REINTERPRETER =
      Reinterpreter.builder()
          .withConversion(DatetimeReinterpretConversions.TIME_TO_BIGINT)
          .withConversion(DatetimeReinterpretConversions.DATE_TYPES_TO_BIGINT)
          .withConversion(IntegerReinterpretConversions.INTEGER_TYPES_TO_BIGINT)
          .build();

  public BeamSqlReinterpretExpression(List<BeamSqlExpression> operands, SqlTypeName outputType) {
    super(operands, outputType);
  }

  @Override
  public boolean accept() {
    if (getOperands().size() != 1) {
      return false;
    }

    // Interval types will be already converted into BIGINT after evaluation.
    SqlTypeFamily opTypeFamily = opType(0).getFamily();
    if (opTypeFamily.equals(SqlTypeFamily.INTERVAL_DAY_TIME)
        || opTypeFamily.equals(SqlTypeFamily.INTERVAL_YEAR_MONTH)) {
      return true;
    }

    return REINTERPRETER.canConvert(opType(0), SqlTypeName.BIGINT);
  }

  @Override
  public BeamSqlPrimitive evaluate(
      Row inputRow, BoundedWindow window, BeamSqlExpressionEnvironment env) {
    return REINTERPRETER.convert(
        SqlTypeName.BIGINT, operands.get(0).evaluate(inputRow, window, env));
  }
}
