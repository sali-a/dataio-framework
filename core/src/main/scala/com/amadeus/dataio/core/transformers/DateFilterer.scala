package com.amadeus.dataio.core.transformers

import com.amadeus.dataio.core.Logging
import com.amadeus.dataio.core.time.DateRange
import org.apache.spark.sql.{Column, Dataset}
import java.sql.Date
import java.time.LocalTime


trait DateFilterer extends Logging {
  val dateRange: Option[DateRange]
  val dateColumn: Option[Column]

  def applyDateFilter[T](ds: Dataset[T]): Dataset[T] = {
    (dateRange, dateColumn) match {
      case (Some(range), Some(column)) =>
        val dateFrom: Date  = Date.valueOf(range.from.toLocalDate)

        // if we have a time after midnight, include the right extreme of the DateRange
        val dateUntil =
          if (range.until.toLocalTime.isAfter(LocalTime.of(0, 0))) Date.valueOf(range.until.plusDays(1).toLocalDate)
          else Date.valueOf(range.until.toLocalDate)

        logger.info(s"DataRange: $dateFrom to $dateUntil, using column $dateColumn.")
        ds.filter(column >= dateFrom and column < dateUntil)
      case (Some(range), None) => throw new Exception("Can not filter by date without a date column.")
      case (_, _) =>
        logger.info(s"DataRange: $dateRange, using column: $dateColumn.")
        ds
    }
  }
}

object DateFilterer {
  def apply[T](range: Option[DateRange], column: Option[Column]): Dataset[T] => Dataset[T] = {
    new DateFilterer {
      override val dateRange: Option[DateRange] = range
      override val dateColumn: Option[Column]   = column
    }.applyDateFilter
  }
}
