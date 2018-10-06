package com.danilenko.service

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import com.danilenko.service.TablesService.{CreateTableRequest, Table}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

trait TablesService {
  def get: Future[Seq[Table]]
  def add(table: CreateTableRequest): Future[Table]
  def update(table: Table): Future[Option[Table]]
  def remove(id: Int): Future[Option[Table]]
}

class TablesServiceImpl(implicit executionContext: ExecutionContext) extends TablesService {

  override def get: Future[Seq[Table]] =
    Future(storeMap.values().asScala.toSeq)

  override def add(table: CreateTableRequest): Future[Table] =
    Future {
      val tableId = atomicTableIdGenerator.incrementAndGet()
      val newTable = Table(id = tableId, name = table.name, participants = table.participants)
      storeMap.put(tableId, newTable)
      newTable
    }

  override def update(table: Table): Future[Option[Table]] =
    Future(Option(storeMap.replace(table.id, table)).map(_ => table))

  override def remove(id: Int): Future[Option[Table]] =
    Future(Option(storeMap.remove(id)))

  // internal
  private val atomicTableIdGenerator = new AtomicInteger(1)
  private val storeMap = new ConcurrentHashMap[Int, Table]()

}

object TablesService {

  case class Table(id: Int, name: String, participants: Int)
  case class CreateTableRequest(name: String, participants: Int)

}
