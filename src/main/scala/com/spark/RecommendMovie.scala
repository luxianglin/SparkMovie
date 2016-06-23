package com.spark

import java.util.Properties

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.mllib.recommendation.{ALS, MatrixFactorizationModel, Rating}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SQLContext}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.Map

/**
  * Created by luxianglin on 16/6/22.
  */


case class MovieRatings(userID: String, movieID: Int, rating: Double) extends scala.Serializable

case class Movies(MovieID: String, MovieName: String) extends scala.Serializable

object RecommendMovie {
  var sqlContext: SQLContext = _
  var BMovieAndName: Broadcast[Map[String, String]] = _
  var userIDToInt: RDD[(String, Long)] = _
  var model: MatrixFactorizationModel = _

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf().setAppName("RecommendMovies")
    val sc = new SparkContext(conf)
    sqlContext = new SQLContext(sc)
    val BasePath = "/Users/luxianglin/Desktop/data"
    val HotMovies = sc.textFile(BasePath + "/hot_movies.csv")
    val UserMovies = sc.textFile(BasePath + "/user_movies.csv")
    MovieModel(sc, HotMovies, UserMovies, BasePath)
    val username = if (args.length > 0) args(0) else "iRayc"
    val userIDMap: Map[String, Int] = userIDToInt.collectAsMap().map { case (s, l) => (s, l.toInt) }
    RecommendByName(username, UserMovies, BMovieAndName, userIDMap, model)
    sc.stop()
  }


  def BuildMovies(HotMovies: RDD[String]): RDD[Movies] = {
    HotMovies.map { line => val Array(movieID, pref, movieName) = line.split(',') //movieID,pref,movieName首字母必须小写,否则会报错
      if (movieID.isEmpty) {
        null
      }
      else {
        Movies(movieID, movieName)
      }
    }
  }

  def BuildRating(UserMovies: RDD[String]): RDD[MovieRatings] = {
    UserMovies.map {
      line => val Array(userID, moviesID, countStr) = line.split(',').map(_.trim)
        var count = countStr.toInt
        count = if (count == -1) 3 else count
        MovieRatings(userID, moviesID.toInt, count)
    }
  }

  def MovieModel(sc: SparkContext, HotMovies: RDD[String], UserMovies: RDD[String], BasePath: String): Unit = {
    val MovieAndName = BuildMovies(HotMovies)
    BMovieAndName = sc.broadcast(MovieAndName.map { movie => (movie.MovieID, movie.MovieName) }.collectAsMap())
    val MovieRatingResult = BuildRating(UserMovies)
    userIDToInt = MovieRatingResult.map(_.userID).distinct().zipWithUniqueId() //将RDD中元素和一个唯一ID组合成键/值对,该键值对为(String,Long)
    val reverseUserID: RDD[(Int, String)] = userIDToInt.map { case (s, l) => (l.toInt, s) } //将键值对反转以形成(Int,String)
    val userIDMap: Map[String, Int] = userIDToInt.collectAsMap().map { case (s, l) => (s, l.toInt) } //将键值对(String,Long)映射成(String,Int)
    val BUserIDMap = sc.broadcast(userIDMap)
    val BreverseUserID = sc.broadcast(reverseUserID.collectAsMap())
    val rating: RDD[Rating] = MovieRatingResult.map { line => Rating(BUserIDMap.value.get(line.userID).get, line.movieID, line.rating) }.persist(StorageLevel.MEMORY_AND_DISK)
    model = ALS.train(rating, 50, 10, 0.0001)
    rating.unpersist()
    //model.save(sc, BasePath + "model")
    val allRecommends = model.recommendProductsForUsers(5).map {
      case (userid, recommends) => {
        var recommendStr = ""
        for (r <- recommends) {
          recommendStr += r.product + ":" + BMovieAndName.value.getOrElse(r.product.toString, "") + ","
        }
        if (recommendStr.endsWith(",")) {
          recommendStr = recommendStr.substring(0, recommendStr.length - 1)
        }
        (BreverseUserID.value.get(userid).get, recommendStr)
      }
    }
    allRecommends.saveAsTextFile(BasePath + "/result.csv")



    HistoryMovies(sc, BasePath, HotMovies, UserMovies)



    model.userFeatures.unpersist()
    model.productFeatures.unpersist()
    val resultdata = sc.textFile(BasePath + "/result.csv").map(_.split(","))
    val schema = StructType(
      List(
        StructField("userID", StringType, false),
        StructField("movie01", StringType, false),
        StructField("movie02", StringType, false),
        StructField("movie03", StringType, false),
        StructField("movie04", StringType, false),
        StructField("movie05", StringType, false)
      )
    )
    val rows = resultdata.map(line => Row(line(0).substring(1).toString().trim, line(1).toString.trim, line(2).toString.trim, line(3).toString.trim, line(4).toString.trim, line(5).toString.trim))
    val dataRDD = sqlContext.createDataFrame(rows, schema)
    val prop = new Properties()
    prop.put("user", "root")
    prop.put("password", "123")
    dataRDD.write.mode("overwrite").jdbc("jdbc:mysql://localhost:3306/spark?characterEncoding=utf8", "recommend", prop)
  }

  def RecommendByName(UserName: String, UserMovies: RDD[String], BMovieAndName: Broadcast[Map[String, String]], userIDMap: Map[String, Int], model: MatrixFactorizationModel): Unit = {
    //val username = reverseUserID.lookup(UserID).head
    val UserID = userIDMap.filter {
      case (username, userid) => username == UserName
    }.values.head
    val recommendations = model.recommendProducts(UserID, 5)
    val RecommendMovieResult = recommendations.map(_.product).toSet
    val UserSeen = UserMovies.map(_.split(",")).filter { case Array(user, _, _) => user.trim == UserName }
    val MovieSeen = UserSeen.map { case Array(_, movieid, _) => movieid.toInt }.collect().toSet
    println("用户" + UserName + "点播过的电影名")
    BMovieAndName.value.filter { case (id, name) => MovieSeen.contains(id.toInt) }.values.foreach(println)
    println("推荐给用户" + UserName + "的电影名")
    BMovieAndName.value.filter { case (id, name) => RecommendMovieResult.contains(id.toInt) }.values.foreach(println)
  }


  def HistoryMovies(sc: SparkContext, BasePath: String, HotMovies: RDD[String], UserMovies: RDD[String]): Unit = {

    val UserHaveSeen: RDD[(String, String)] = UserMovies.map(_.split(",")).map { case Array(userid, movieid, _) => (userid.toString, movieid.toString) }
    val UserSeen = UserHaveSeen.reduceByKey((x, y) => x + "," + y)
    val reverse = UserHaveSeen.map { case (u, m) => (m, u) }
    val UserAndName: RDD[(String, String)] = HotMovies.map(_.split(",")).map { case Array(movieid, pref, moviename) => (movieid.toString, moviename.toString) }
    val resultRDD = reverse.join(UserAndName).map(_._2).reduceByKey((x, y) => x + "," + y)
    //val resultRDD = sc.parallelize(result)
    resultRDD.saveAsTextFile(BasePath + "/history.csv")
    val schema = StructType(
      List(
        StructField("userid", StringType, false),
        StructField("historymovies", StringType, false)

      )
    )
    val rows = resultRDD.map { case (key, values) => Row(key.toString.trim, values.toString.trim) }
    val dataRDD = sqlContext.createDataFrame(rows, schema)
    val prop = new Properties()
    prop.put("user", "root")
    prop.put("password", "123")
    dataRDD.write.mode("overwrite").jdbc("jdbc:mysql://localhost:3306/spark?characterEncoding=utf8", "history", prop)

  }
}
