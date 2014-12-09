package org.apache.spark.sql.catalyst.checker

import scala.collection
import scala.collection.JavaConverters._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.Map
import scala.collection.mutable.Set
import org.apache.spark.Logging
import org.apache.spark.sql.catalyst.analysis.Catalog
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import edu.thu.ss.spec.global.MetaManager
import edu.thu.ss.spec.lang.parser.PolicyParser
import edu.thu.ss.spec.lang.pojo.Action
import edu.thu.ss.spec.lang.pojo.DataCategory
import edu.thu.ss.spec.lang.pojo.DataRef
import edu.thu.ss.spec.lang.pojo.Desensitization
import edu.thu.ss.spec.lang.pojo.DesensitizeOperation
import edu.thu.ss.spec.lang.pojo.ExpandedRule
import edu.thu.ss.spec.lang.pojo.Policy
import edu.thu.ss.spec.lang.pojo.UserCategory
import edu.thu.ss.spec.meta.MetaRegistry
import edu.thu.ss.spec.meta.xml.XMLMetaRegistryParser
import org.apache.spark.sql.catalyst.analysis.Catalog

class SparkChecker extends PrivacyChecker {

  /**
   * path is essential a list of desensitize operations
   */
  case class Path(val ops: Seq[DesensitizeOperation]);

  var projectionPaths: Map[Policy, Map[DataCategory, Set[Path]]] = null;
  var conditionPaths: Map[Policy, Map[DataCategory, Set[Path]]] = null;
  lazy val user = MetaManager.currentUser();
  var policies: collection.Set[Policy] = null;

  def check(projections: collection.Set[Label], conditions: collection.Set[Label], policies: collection.Set[Policy]): Unit = {
    if (policies.size == 0) {
      return ;
    }
    this.policies = policies;
    projectionPaths = new HashMap;
    conditionPaths = new HashMap;
    //first calculate all paths for data categories
    projections.foreach(buildPath(_, projectionPaths));
    conditions.foreach(buildPath(_, conditionPaths));

    printPaths(projections, conditions);

    //check each rule
    policies.foreach(p => p.getExpandedRules().asScala.foreach(checkRule(_, p)));
  }

  /**
   * should be turn off in production
   */
  private def printPaths(projections: collection.Set[Label], conditions: collection.Set[Label]) {

    println("projections:");
    projections.foreach(t => {
      println(s"$t");
    });
    println();
    println("conditions:")
    conditions.foreach(t => {
      println(t);
    });
    println();
    println("\nprojection paths:");
    projectionPaths.foreach(p => {
      p._2.foreach(
        t => t._2.foreach(path => println(s"${t._1}\t$path")))
    })

    println("\ncondition paths:");

    conditionPaths.foreach(p => {
      p._2.foreach(
        t => t._2.foreach(path => println(s"${t._1}\t$path")))
    })

  }

  /**
   * build paths for each data category recursively.
   */
  private def buildPath(label: Label, paths: Map[Policy, Map[DataCategory, Set[Path]]], list: ListBuffer[String] = new ListBuffer): Unit = {
    label match {
      case data: DataLabel => {
        addPath(data.data, data, list, paths);
      }
      case cond: ConditionalLabel => {
        cond.fulfilled.foreach(data => {
          addPath(data, cond, list, paths);
        });
      }
      case func: Function => {
        list.prepend(func.udf);
        func.children.foreach(buildPath(_, paths, list));
        list.remove(0);
      }
      case pred: Predicate => {
        pred.children.foreach(buildPath(_, paths, list));
      }
      case _ =>
    }
  }

  /**
   * first map udfs to desensitize operations, then add the path to paths.
   */
  private def addPath(data: DataCategory, label: ColumnLabel, udfs: ListBuffer[String], paths: Map[Policy, Map[DataCategory, Set[Path]]]): Unit = {
    val meta = MetaManager.get(label.database, label.table);
    val ops = udfs.map(meta.lookup(data, _, label.database, label.table, label.attr.name)).filter(_ != null);

    policies.foreach(p => {
      if (MetaManager.applicable(p, label.database, label.table)) {
        val map = paths.getOrElseUpdate(p, new HashMap[DataCategory, Set[Path]]);
        map.getOrElseUpdate(data, new HashSet[Path]).add(Path(ops));
        return ;
      }
    })
  }

  /**
   * check rule
   *
   * @throws PrivacyException
   */
  private def checkRule(rule: ExpandedRule, policy: Policy): Unit = {
    // check user
    if (!rule.contains(user)) {
      return ;
    }
    var error = false;
    if (rule.isSingle()) {
      //single rule
      val access = new HashSet[DataCategory];
      val dataRef = rule.getDataRef();
      //collect all applicable data categories
      collectDatas(dataRef, access, policy);
      error = checkRestriction(rule, access, policy);
    } else {
      //association rule
      val association = rule.getAssociation();
      val accesses = Array.fill(association.getDimension())(new HashSet[DataCategory]);
      val dataRefs = association.getDataRefs();
      //collect all applicable data categories
      for (i <- 0 to dataRefs.size - 1) {
        collectDatas(dataRefs.get(i), accesses(i), policy);
      }
      error = checkRestrictions(rule, accesses, policy);
    }
    if (error) {
      throw new PrivacyException(s"The SQL query violates the rule: #${rule.getRuleId()}.");
    }
  }

  /**
   * collect all applicable data categories into access.
   * note that data ref/ association is essentially treated as n (>=1) buckets,
   * and we fill these buckets based on data categories accessed by the query.
   */
  private def collectDatas(ref: DataRef, access: Set[DataCategory], policy: Policy) {
    ref.getAction() match {
      case Action.All => {
        collectDatas(ref, projectionPaths, access, policy);
        collectDatas(ref, conditionPaths, access, policy);
      }
      case Action.Projection => collectDatas(ref, projectionPaths, access, policy);
      case Action.Condition => collectDatas(ref, conditionPaths, access, policy);
    }
  }

  private def collectDatas(ref: DataRef, paths: Map[Policy, Map[DataCategory, Set[Path]]], access: Set[DataCategory], policy: Policy): Unit = {
    if (ref.isGlobal()) {
      // all data categories are applicable
      paths.values.foreach(_.keys.foreach(data => {
        if (ref.contains(data)) {
          access.add(data);
        }
      }));
    } else {
      //only collect data categories that for current policy
      val map = paths.getOrElse(policy, null);
      if (map != null) {
        ref.getMaterialized().asScala.foreach(data => {
          if (map.contains(data)) {
            access.add(data);
          }
        });
      }
    }
  }

  /**
   * check restriction for single rule
   * only 1 restriction, and only 1 desensitization
   */
  private def checkRestriction(rule: ExpandedRule, access: HashSet[DataCategory], policy: Policy): Boolean = {
    if (access.size == 0) {
      return false;
    }

    val restriction = rule.getRestriction();
    if (restriction.isForbid()) {
      return true;
    }
    val de = restriction.getDesensitization();
    val ref = rule.getDataRef();
    for (data <- access) {
      if (checkDesensitization(ref, data, de, policy, ref.isGlobal())) {
        return true;
      }
    }

    false;
  }

  /**
   * check restriction for association rule
   */
  private def checkRestrictions(rule: ExpandedRule, accesses: Array[HashSet[DataCategory]], policy: Policy): Boolean = {
    //if any bucket is empty, then return.
    if (accesses.exists(_.size == 0)) {
      return false;
    }
    if (rule.getRestriction().isForbid()) {
      return true;
    }
    val array = new Array[DataCategory](accesses.length);
    return checkRestrictions(array, 0, rule, accesses, policy);
  }

  /**
   * check restrictions recursively.
   * for buckets with m1, m2, ..., mn elements, we need to check m1 * m2 *... mn combinations.
   */
  private def checkRestrictions(array: Array[DataCategory], i: Int, rule: ExpandedRule, accesses: Array[HashSet[DataCategory]], policy: Policy): Boolean = {
    if (i == accesses.length) {
      val restrictions = rule.getRestrictions();
      val association = rule.getAssociation();
      //a combination of element, and check whether exist a satisfied restriction
      return restrictions.exists(res => {
        if (res.isForbid()) {
          return false;
        }
        res.getDesensitizations().asScala.forall(de => {
          for (index <- de.getDataIndex()) {
            val ref = association.get(index);
            val data = array(index);
            val global = ref.isGlobal();
            checkDesensitization(ref, data, de, policy, global);
          }
          return true;
        });
      });
    } else {
      val access = accesses(i);
      for (data <- access) {
        array(i) = data;
        if (!checkRestrictions(array, i + 1, rule, accesses, policy)) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * check whether a desensitization is satisfied
   */
  private def checkDesensitization(ref: DataRef, data: DataCategory, de: Desensitization, policy: Policy, global: Boolean): Boolean = {
    ref.getAction() match {
      case Action.All => if (checkOperations(de, data, projectionPaths, policy, global) || checkOperations(de, data, conditionPaths, policy, global)) {
        return true;
      }
      case Action.Projection => if (checkOperations(de, data, projectionPaths, policy, global)) {
        return true;
      }
      case Action.Condition => if (checkOperations(de, data, conditionPaths, policy, global)) {
        return true;
      }
    }
    false;
  }

  /**
   * check all paths for a data category is desensitized with one of the operations
   */
  private def checkOperations(de: Desensitization, data: DataCategory, paths: Map[Policy, Map[DataCategory, Set[Path]]], policy: Policy, global: Boolean): Boolean = {
    if (global) {
      for (map <- paths.values) {
        if (checkOperations(de, data, map)) {
          return true;
        }
      }
    } else {
      val map = paths.getOrElse(policy, null);
      if (map != null) {
        return checkOperations(de, data, map);
      }
    }
    return false;

  }

  private def checkOperations(de: Desensitization, data: DataCategory, paths: Map[DataCategory, Set[Path]]): Boolean = {
    val set = paths.getOrElse(data, null);
    if (set == null) {
      return false;
    }
    var ops = de.getOperations();
    if (ops == null) {
      //fall back to default desensitize operations
      ops = data.getOperations();
    }
    return !set.forall(path => path.ops.exists(ops.contains(_)));
  }

}

object SparkChecker extends Logging {

  val checker = new SparkChecker();

  /**
   * load policy and meta during startup.
   * may need to be modified in a pluggable way.
   */
  def init(catalog: Catalog, policyPath: String, metaPath: String): Unit = {
    loadPolicy(policyPath);
    loadMeta(metaPath, catalog);
  }

  def loadPolicy(path: String): Unit = {
    val parser = new PolicyParser;
    try {
      parser.parse(path, true);
    } catch {
      case e: Exception => logError(e.getMessage, e);
    }
  }

  def loadMeta(path: String, catalog: Catalog): Unit = {
    val parser = new XMLMetaRegistryParser;
    try {
      val meta = parser.parse(path);
      checkMeta(meta, catalog);
    } catch {
      case e: Exception => logError(e.getMessage, e);
    }
  }

  /**
   * wrap of spark checker
   */
  def apply(plan: LogicalPlan): Unit = {
    val begin = System.currentTimeMillis();
    val propagator = new LabelPropagator;
    val policies = propagator(plan);
    val checker = new SparkChecker;
    checker.check(plan.projections.values.toSet, plan.conditions, policies);
    val end = System.currentTimeMillis();
    val time = end - begin;
    println(s"privacy checking finished in $time ms");
  }

  /**
   * check whether column is properly labeled
   */
  private def checkMeta(meta: MetaRegistry, catalog: Catalog): Unit = {
    val databases = meta.getDatabases();
    for (db <- databases.asScala) {
      val dbName = Some(db._1); ;
      val tables = db._2.getTables().asScala;
      for (t <- tables) {
        val relation = lookupRelation(catalog, dbName, t._1);
        if (relation == null) {
          logError(s"Error in MetaRegistry, table: ${t._1} not found in database: ${db._1}.");
        } else {
          relation.checkMeta(t._2.getAllColumns().asScala);
          val conds = t._2.getAllConditions().asScala;
          val condColumns = new HashSet[String];
          conds.foreach(join => {
            val list = join.getJoinColumns().asScala;
            list.foreach(e => condColumns.add(e.column));
            val name = join.getJoinTable();
            val relation = lookupRelation(catalog, dbName, name);
            if (relation == null) {
              logError(s"Error in MetaRegistry, table: $name (joined with ${t._1}) not found in database: ${db._1}.");
            } else {
              val cols = list.map(_.target);
              relation.checkMeta(cols);
            }
          });
          relation.checkMeta(condColumns.toList);
        }
      }
    }
  }

  private def lookupRelation(catalog: Catalog, database: Option[String], table: String): LogicalPlan = {
    try {
      catalog.lookupRelation(database, table);
    } catch {
      case _: Throwable => null;
    }
  }
}
