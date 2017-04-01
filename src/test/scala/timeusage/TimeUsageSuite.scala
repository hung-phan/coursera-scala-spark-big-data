package timeusage

import org.apache.spark.sql.{DataFrame, Dataset}
import org.apache.spark.sql.types.{DoubleType, StringType}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterAll, FunSuite}

@RunWith(classOf[JUnitRunner])
class TimeUsageSuite extends FunSuite with BeforeAndAfterAll {
  lazy val (columns, initDf) = TimeUsage.read("/timeusage/atussum-10000.csv")
  lazy val (primaryNeedsColumns, workColumns, otherColumns) = TimeUsage.classifiedColumns(columns)
  lazy val summaryDf: DataFrame = TimeUsage.timeUsageSummary(primaryNeedsColumns, workColumns, otherColumns, initDf)
  lazy val finalDf: DataFrame = TimeUsage.timeUsageGrouped(summaryDf)
  lazy val sqlDf: DataFrame = TimeUsage.timeUsageGroupedSql(summaryDf)
  lazy val summaryDs: Dataset[TimeUsageRow] = TimeUsage.timeUsageSummaryTyped(summaryDf)
  lazy val finalDs: Dataset[TimeUsageRow] = TimeUsage.timeUsageGroupedTyped(summaryDs)

  test("dfSchema") {
    val testSchema = TimeUsage.dfSchema(List("fieldA", "fieldB"))

    assert(testSchema.fields(0).name === "fieldA")
    assert(testSchema.fields(0).dataType === StringType)
    assert(testSchema.fields(1).name === "fieldB")
    assert(testSchema.fields(1).dataType === DoubleType)
  }

  test("row") {
    val testRow = TimeUsage.row(List("fieldA", "0.3", "1"))

    assert(testRow(0).getClass.getName === "java.lang.String")
    assert(testRow(1).getClass.getName === "java.lang.Double")
    assert(testRow(2).getClass.getName === "java.lang.Double")
  }

  test("read") {
    assert(columns.size === 455)
    assert(initDf.count === 10000 - 1)
  }

  test("classifiedColumns") {
    val pnC = primaryNeedsColumns.map(_.toString)
    val wC = workColumns.map(_.toString)
    val oC = otherColumns.map(_.toString)

    assert(pnC.contains("t010199"))
    assert(pnC.contains("t030501"))
    assert(pnC.contains("t110101"))
    assert(pnC.contains("t180382"))
    assert(wC.contains("t050103"))
    assert(wC.contains("t180589"))
    assert(oC.contains("t020101"))
    assert(oC.contains("t180699"))
  }

  test("timeUsageSummary") {
    assert(summaryDf.columns.length === 6)
    assert(summaryDf.count === 6872)
    summaryDf.show()
  }

  test("timeUsageGrouped") {
    assert(finalDf.count === 2 * 2 * 3)
    assert(finalDf.head.getDouble(3) === 12.3)
    finalDf.show()
  }

  test("timeUsageGroupedSql") {
    assert(sqlDf.count === 2 * 2 * 3)
    assert(sqlDf.head.getDouble(3) === 12.3)
    sqlDf.show()
  }

  test("timeUsageSummaryTyped") {
    assert(summaryDs.head.getClass.getName === "timeusage.TimeUsageRow")
    assert(summaryDs.head.other === 8.75)
    assert(summaryDs.count === 6872)
    summaryDs.show()
  }

  test("timeUsageGroupedTyped") {
    assert(finalDs.count === 2 * 2 * 3)
    assert(finalDs.head.primaryNeeds === 12.3)
    assert(finalDs.head.primaryNeeds === 12.3)
  }
}
