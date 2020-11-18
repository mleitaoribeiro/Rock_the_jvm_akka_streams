package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}

object OpenGraphs extends App {

  implicit val system = ActorSystem("OpenGraphs")
  implicit val materializer = ActorMaterializer()

  /*
    A composite source that concatenates 2 sources
    - emits ALL elements from the first source
    - then All the elements from de second
   */

  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)

  // step 1
  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2: declaring components
      val concat = builder.add(Concat[Int](2))

      // step3: tying them together
      firstSource ~> concat
      secondSource ~> concat

      // step 4
      SourceShape(concat.out)
    }
  )

  // sourceGraph.to(Sink.foreach(println)).run()

  /*
    Complex sink
   */
  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1: ${x}"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2: ${x}"))

  // step 1
  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // step 2 - add a broadcast
      val broadcast = builder.add(Broadcast[Int](2))

      // step 3 - tie components together
      broadcast ~> sink1
      broadcast ~> sink2

      // step 4 - return the shape
      SinkShape(broadcast.in)
    }
  )

  // firstSource.to(sinkGraph).run()

  /**
   * Challenge - complex flow?
   * Write your own flow that's composed of two other flows
   * - one that adds 1 to a number
   * - one that does number * 10
   */

  val incrementer = Flow[Int].map(x => x + 1)
  val multiplier = Flow[Int].map(x => x * 10)

  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      // everything operates on SHAPES, not operators
      // step 2 - define auxiliary SHAPES
      val incrementerShape = builder.add(incrementer)
      val multiplierShape = builder.add(multiplier)

      // step 3 - connect the SHAPES
      incrementerShape ~> multiplierShape

      FlowShape(incrementerShape.in, multiplierShape.out)
    } // static graph
  ) // component or operator

  firstSource.via(flowGraph).to(Sink.foreach(println)).run()

  /**
    Exercise: flow from a sink and and a source?
   */
  def fromSinkAndSource[A, B](sink: Sink[A, _], source: Source[B, _]): Flow[A, B, _] =
  // step 1
    Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        // step 2: declare the SHAPES
        val sourceShape = builder.add(source)
        val sinkShape = builder.add(sink)

        // step 3
        // step 4 - return the shape
        FlowShape(sinkShape.in, sourceShape.out)
      }
    )

  // this method already exists
  val f1 = Flow.fromSinkAndSource(Sink.foreach[String](println), Source(1 to 10)) // in this case there's the danger that the stream never stops because there's no connection
                                                                                  // between the sink and the source
  // so there's this method
  val f2 = Flow.fromSinkAndSourceCoupled(Sink.foreach[String](println), Source(1 to 10))

}
