// package io.hydrosphere.monitoring.manager

// import org.scalatest.funspec.AsyncFunSpecLike
// import cats.effect.IO
// import cats.effect.unsafe.IORuntime.global
// import cats.effect.unsafe.IORuntime
// import java.time.Instant
// import doobie.util.transactor.Transactor
// import io.hydrosphere.monitoring.manager.database.initializeDb

// trait BaseTest extends AsyncFunSpecLike:
//   given runtime: IORuntime = global

// class S3IndexRepositorySpec extends BaseTest:
//   describe("S3IndexRepositoryImpl") {
//     it("should list all indices") {
//       val result = initializeDb[IO]
//         .use { tx =>
//           val db     = new S3IndexRepositoryImpl[IO]()
//           val result = db.list().compile.toList
//         }
//         .unsafeRunSync()
//       assert(result.nonEmpty, result)
//     }

//     // it("should insert an indicex") {
//     //   val index = S3Index(Instant.now())
//     //   val db    = new S3IndexRepositoryImpl[IO]()
//     //   db.insert(index).unsafeRunSync()
//     //   succeed
//     // }
//   }
