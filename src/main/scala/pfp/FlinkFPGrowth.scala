

package pfp


import helper.IOHelperFlink
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.api.scala.ExecutionEnvironment

object FlinkFPGrowth {

  def main(args: Array[String]) {

    println("STARTING FPGROWTH IN FLINK")

    //Global variables for Flink and parameter parser
    val parameter = ParameterTool.fromArgs(args)
    val env = ExecutionEnvironment.getExecutionEnvironment
    val itemDelimiter = " "

    //Parse input parameter
    val input = parameter.get("input")
    val minSupport = parameter.get("support")
    val numGroup = parameter.get("group")
    val output = parameter.get("output")

    println("input: " + input + " support: " + minSupport + " numGroup: " + numGroup + " output: " + output)

    if (input == null || input == "" || minSupport == null) {
      println("Please indicate input file and support: --input inputFile --support minSupport")
      return
    }

    val starTime = System.currentTimeMillis()

    val pfp = new PFPGrowth(env, minSupport.toDouble)

    if (numGroup != null && numGroup.toInt >=0 ) {
      pfp.numPartition = numGroup.toInt
    }

    if (output != null) {
      pfp.output = output
    }

    //Read dataset
    val data = IOHelperFlink.readInput(env, input, itemDelimiter)
    //Run the PFPGrowth and get list of frequent itemsets
    val frequentItemsets = pfp.run(data)

    env.execute("FLINK FPGROWTH")

    println("TIME: " + (System.currentTimeMillis() - starTime) / 1000.0)
    println("FLINK FPGROWTH: " + frequentItemsets.size)
  }
}