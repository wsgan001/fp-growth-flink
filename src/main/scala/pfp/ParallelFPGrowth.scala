package pfp

import java.lang.Iterable

import fpgrowth.{FPGrowth => FPGrowthLocal, Item}
import org.apache.flink.api.common.functions.{FlatMapFunction, GroupReduceFunction}
import org.apache.flink.util.Collector

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object ParallelFPGrowth {

  /**
    * Mapper in step4. The idea is to generate independent conditional based itemset. Each itemset has its own order based on frequency
    * Item with highest frequency has order 0, Item with second frequency has order 1 .....
    * From now on, item is represented by their order(called ItemId). An itemset is a list of itemId
    *
    * => Sorting by frequency is sorting itemset itemId
    * @param gList
    */
  class ParallelFPGrowthFlatMap(val idToGroupMap: mutable.HashMap[Int, Int], val order: Map[Item, Int]) extends FlatMapFunction[ListBuffer[Item], (Int, ListBuffer[Int])] {

    override def flatMap(itemset: ListBuffer[Item], collector: Collector[(Int, ListBuffer[Int])]): Unit = {

      //Check if the current group has been processed
      var outputGroup = Set[Int]()
      //Extract itemId from itemset and sort frequency in increasing order
      val itemIds = itemset.flatMap(order.get).sortWith( _ > _)

      for(j <- (itemIds.size - 1) to (0, -1)) {
        val itemId = itemIds(j)
        val groupId = idToGroupMap(itemId)

        if (!outputGroup.contains(groupId)) {
          outputGroup += groupId
          collector.collect(groupId, itemIds.slice(0, j + 1))
        }
      }
    }
  }

  class ParallelFPGrowthGroupReduce(val idToGroupMap: mutable.HashMap[Int, Int], val minCount: Long) extends GroupReduceFunction[(Int, ListBuffer[Int]), (ListBuffer[Int], Int)] {
    override def reduce(iterable: Iterable[(Int, ListBuffer[Int])], collector: Collector[(ListBuffer[Int], Int)]): Unit = {
      var groupId: Long = 0
      val fpGrowthLocal: FPGrowthLocal = new FPGrowthLocal(null, minCount, false)

      iterable.foreach(
        tuple => {
          groupId = tuple._1
          fpGrowthLocal.fptree.addTransaction(tuple._2, 1)
        }
      )

      //Extract now group
      val nowGroup = {
        idToGroupMap.filter(_._2 == groupId).keys
      }

      nowGroup.foreach(
        item => {
          //Extract the frequentId itemset
          val frequentIdSets = fpGrowthLocal.extractPattern(fpGrowthLocal.fptree, null, item)
          frequentIdSets.foreach(collector.collect(_))
        }
      )
    }
  }

}