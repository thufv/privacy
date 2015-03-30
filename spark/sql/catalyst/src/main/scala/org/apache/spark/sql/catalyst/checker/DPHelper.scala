package org.apache.spark.sql.catalyst.checker

import scala.math.BigDecimal
import scala.util.Random
import org.apache.spark.Logging
import org.apache.spark.sql.catalyst.expressions.AggregateExpression
import org.apache.spark.sql.catalyst.expressions.Sum
import org.apache.spark.sql.catalyst.expressions.SumDistinct
import org.apache.spark.sql.catalyst.expressions.CountDistinct
import org.apache.spark.sql.catalyst.expressions.Min
import org.apache.spark.sql.catalyst.expressions.Max
import org.apache.spark.sql.catalyst.expressions.Average
import org.apache.spark.sql.catalyst.expressions.CountDistinct
import org.apache.spark.sql.catalyst.expressions.Count

object DPHelper extends Logging {

  val Conf_Epsilon = "epsilon";

  val Default_Epsilon = "0.5";

  def laplace(stddev: Double): Double = {
    val uniform = Random.nextDouble() - 0.5;
    return stddev * Math.signum(uniform) * Math.log(1 - 2.0 * Math.abs(uniform));
  }

  def uniform(low: Double, high: Double): Double = {
    return low + (high - low) * Random.nextDouble();
  }

  def lapNoise(epsilon: Double, sensitivity: Double): Double = {
    return laplace(sensitivity / epsilon);
  }

  def supported(agg: AggregateExpression): Boolean = {
    agg match {
      case _: Sum => true;
      case _: SumDistinct => true;
      case _: Count => true;
      case _: CountDistinct => true;
      case _: Min => true;
      case _: Max => true;
      case _: Average => true;
      case _ => false;
    }
  }

  def toDouble(value: Any): Double = {
    value match {
      case long: Long => long.toDouble;
      case int: Int => int.toDouble;
      case double: Double => double.toDouble;
      case float: Float => float.toDouble;
      case short: Short => short.toDouble;
      case big: BigDecimal => big.toDouble;
      case str: String => str.toDouble;
      case null => null.asInstanceOf[Double];
      case _ => throw new RuntimeException(s"invalid argument: $value.");
    }
  }

  def min(v1: Any, v2: Any): Any = {
    val d1 = DPHelper.toDouble(v1);
    val d2 = DPHelper.toDouble(v2);
    if (d1 <= d2) {
      return v1;
    } else {
      return v2;
    }
  }

  def max(v1: Any, v2: Any): Any = {
    val d1 = DPHelper.toDouble(v1);
    val d2 = DPHelper.toDouble(v2);
    if (d1 <= d2) {
      return v2;
    } else {
      return v1;
    }
  }

  def lessThan(v1: Any, v2: Any): Boolean = {
    val d1 = DPHelper.toDouble(v1);
    val d2 = DPHelper.toDouble(v2);
    return d1 < d2;
  }

  def checkUtility(result: Any, epsilon: Double, sensitivity: Double): Boolean = {
    val value = toDouble(result);
    val stddev = sensitivity / epsilon;
    val noise = -stddev * Math.log(1 - SparkChecker.accuracyProb);

    logWarning(s"estimated noise range [-$noise, $noise] with probability ${SparkChecker.accuracyProb}");

    if (result == null || noise / value > SparkChecker.accurcayNoise) {
      //SparkChecker.rollback;
      //throw new PrivacyException("accuracy bound violated, please revise your query");
      return false;
    } else {
      return true;
    }
  }

  def calibrateNoise(result: Any, epsilon: Double, sensitivity: Double): Any = {
    logWarning(s"calibrating noise, epsilon:$epsilon, sensitivity:$sensitivity");
    if (!checkUtility(result, epsilon, sensitivity)) {
      logWarning(s"utility accuracy unsatisfied, suppress aggregate result: ${result}");
      return null;
    }
    val noise = lapNoise(epsilon, sensitivity);
    result match {
      case long: Long => long + noise.toLong;
      case int: Int => int + noise.toInt;
      case double: Double => double + noise.toDouble;
      case float: Float => float + noise.toFloat;
      case short: Short => short + noise.toShort;
      case big: BigDecimal => big + BigDecimal(noise);
      case null => null;
      case _ => throw new PrivacyException(s"invalid argument: $result.");
    }
  }

}