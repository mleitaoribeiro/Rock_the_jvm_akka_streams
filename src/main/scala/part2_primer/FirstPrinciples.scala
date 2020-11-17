package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples extends App {

  implicit val system = ActorSystem("FirstPrinciples")
  implicit val materializer = ActorMaterializer() // allows the running of akka streams components

  // sources
  val source = Source(1 to 10)
  // sinks
  val sink = Sink.foreach[Int](println)

  val graph = source.to(sink)
  // graph.run()

  // flows transform elements
  val flow = Flow[Int].map(x => x + 1)
  val sourceWithFlow = source.via(flow)
  val flowwithsink = flow.to(sink)

  //  sourceWithFlow.to(sink).run()
  //  source.to(flowwithsink).run()
  //  source.via(flow).to(sink).run()

  // nulls are NOT allowed
  // val illegalSource = Source.single[String](null)
  // illegalSource.to(Sink.foreach(println)).run()
  // use OPTIONS to overcome the null limitation

  // various kinds of sources
  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source(List(1,2,3))
  val emptySource = Source.empty[Int]
  val infiniteSource = Source(Stream.from(1)) // do not confuse an Akka Stream with a "collection Stream"

  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.fromFuture(Future(42))

  // sinks
  val theMostBoringSink = Sink.ignore // does nothing with the elements it receives
  val foreachSink = Sink.foreach[String](println)
  val headSink = Sink.head[Int] // retrieves head and then closes the stream, may return a value
  val foldSink = Sink.fold[Int, Int](0)((a,b) => a+b) // compute values after the elements it receives

  // flows - usually mapped to collection operators
  val mapFlow = Flow[Int].map(x => 2 * x)
  val takeFlow = Flow[Int].take(5) // only takes the first five numbers, transforms into an finite flow
  // drop. filter
  // NOT have flatMap

  // source -> flow -> flow -> .. -> sink
  val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink)
  // doubleFlowGraph.run()

  // syntactic sugars
  val mapSource = Source(1 to 10).map(x => x * 2) // Source(1 to 10).via(Flow[Int].map(x => x * 2))
  // run streams directly
  // mapSource.runForeach(println) // mapSource.to(Sink.foreach[Int](println)).run()

  // OPERATORS = components

  /**
   * Exercise: create a stream that takes the names of persons, then you will keep the first two names with length > 5
   * characters and print it to the console
   */

  val listNames = List[String]("Rita", "John", "Joe", "Marianne", "Daniel", "Severus")

  // First Attempt
     val sourceNames = Source(listNames)
     val flowNames = Flow[String].filter(_.length > 5)
     val takeNames = Flow[String].take(2)
     val sinkNames = Sink.foreach[String](println)
  // sourceNames.via(flowNames).via(takeNames).to(sinkNames).run()

  // syntactic sugar
  Source(listNames).filter(_.length > 5).take(2).runForeach(println)

}
