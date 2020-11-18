package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

object GraphBasics extends App {

  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer()

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(x => x + 1) // hard computation
  val multiplier = Flow[Int].map(x => x * 10) // hard computation
  val output = Sink.foreach[(Int, Int)](println)

  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._
      // brings some nice operators into scope

      // step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator because broadcast 1 input and makes available 2 outputs
      val zip = builder.add(Zip[Int, Int]) // fan-in operator, 2 inputs, 1 output

      // step 3 - tying up the components
      input ~> broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1

      zip.out ~> output

      //step 4 - return a closed shape
      ClosedShape // FREEZE the builder's shape => IMMUTABLE
      // it produces a shape
    } // it produces a static graph
  ) // it produces a runnable graph

  // graph.run() // run the graph and materialize it

  /**
   * Exercise 1: feed the source into 2 sinks at the same time (hint: use a broadcast)
   */
  val firstSink = Sink.foreach[Int](x => println(s"first: ${x}"))
  val secondSink = Sink.foreach[Int](x => println(s"second: ${x}"))

  val graph2 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      input ~>  broadcast ~> firstSink // implicit port numbering
                broadcast ~> secondSink

//      broadcast.out(0) ~> output2
//      broadcast.out(1) ~> output3

      ClosedShape
    }
  )

  // graph2.run()

  /**
   * Exercise 2: feed 2 source into a merge then a balance into 2 sinks at the same time
   */
    import scala.concurrent.duration._
  val fastSource = input.throttle(5, 1 second)
  val slowSource = input.throttle(2, 1 second)

  val sink1 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink 1 number os elements: $count")
    count + 1
  })
  val sink2 = Sink.fold[Int, Int](0)((count, _) => {
    println(s"Sink 2 number os elements: $count")
    count + 1
  })

  val graph3 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      fastSource ~> merge
      slowSource ~> merge

      merge ~> balance

      balance ~> sink1
      balance ~> sink2

      ClosedShape
    }
  )

  graph3.run()

  // what happens in here is with two different source rates, when we merge the 2 sources and then BALANCE them,
  // they are printed in the respectively sink almost at the same rate

}
