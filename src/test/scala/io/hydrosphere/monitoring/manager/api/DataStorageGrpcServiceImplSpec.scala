//package io.hydrosphere.monitoring.manager.api
//
//import zio._
//import zio.test._
//import zio.test.Assertion._
//import zio.test.environment._
//import monitoring_manager.monitoring_manager.{GetObjectPathRequest, GetObjectPathResponse}
//import zio.test.DefaultRunnableSpec
//
//class DataStorageGrpcServiceImplSpec extends zio.test.junit.JUnitRunnableSpec {
//  def spec = suite("DataStorageGrpcServiceImplSpec") {
//    testM("should return") {
//      val impl    = DataStorageGrpcServiceImpl()
//      val request = GetObjectPathRequest.defaultInstance
//      impl
//        .getObjectPath(request)
//        .map(result => assert(result)(equalTo(GetObjectPathResponse.defaultInstance)))
//    }
//  }
//}
