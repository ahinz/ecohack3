package carbon

import javax.servlet.http.HttpServletRequest
import javax.ws.rs.core.Response
import javax.ws.rs.{GET, POST, Path, DefaultValue, QueryParam}
import javax.ws.rs._
import javax.ws.rs.core.{Response, Context, MediaType, MultivaluedMap}
import geotrellis._
import geotrellis.feature.Polygon
import geotrellis.process._
import geotrellis.raster.op._
import geotrellis.statistics.op._ //stat.TiledPolygonalZonalCount
import geotrellis.raster._
import geotrellis.data._
import geotrellis.data.arg._
import geotrellis.feature.rasterize.PolygonRasterizer

case class S(species: String, year: String, climate: String) {
  private val pathBase = "/var/geotrellis/eco/ecodata/%s/%s_climate_%s/%s" format (species, year, climate, species)
  private val threshPath = pathBase + "_threshold.txt"

  val path = pathBase + ".arg"
  val cutoff = (scala.io.Source.fromFile(threshPath).mkString.toDouble * 100.0).toInt
}

object Species {
  val server = Server("name")

  val yearclimates = Seq(
      ("1950","forest"),
      ("1950","only"),
      ("2000","forest"),
      ("2000","only"),
      ("2080","only"),
      ("2080a","forest"),
      ("2080b","forest"))
  var spcs = Seq("Adetomyrma_MG01",
	         "Adetomyrma_MG02",
	         "Amblyopone_MG01",
	         "Amblyopone_MG04",
	         "Amblyopone_MGm02",
	         "Amblyopone_MGm03",
	         "Amblyoponine_genus1_MG01",
	         "Anochetus_goodmani",
	         "Anochetus_grandidieri",
	         "Appias_sabina",
	         "Avahi_laniger",
	         "Belenois_grandidieri",
	         "Colotis_amata",
	         "Gideona_lucasi",
	         "Leptogenys_falcigera",
	         "Papilio_dardanus",
	         "Ravavy_miafina")

      val species = spcs.flatMap(sp =>
          yearclimates.map {
            case (yr,cl) =>
              S(sp, yr, cl)
          })

      println(Species.species.map(a => a.path))

      val rasters:Map[String,Raster] = Species.species.map ( sp => sp.path -> server.run(io.LoadFile(sp.path))).toMap

  def main(args:Array[String]) {
    for ( (yr, cl) <- yearclimates ) {
      println("processing %s, %s".format(yr,cl))
      val ss = spcs.map( sp => S(sp, yr, cl) )
      val rs = ss.map ( s => rasters(s.path).map ( z => if (z > s.cutoff) 1 else 0 )).map( Literal(_) ).toSeq
      val totalArg = server.run(local.AddRasters(rs:_*))  
      val name = "%s_%s".format(yr,cl)
      ArgWriter(TypeInt).write("/tmp/" + name + ".arg", totalArg, name)
      println("processed %s, %s".format(yr,cl))
    }
  }
}

@Path("/speciesPolygon/")
class SpeciesPolygon {
  def selectSpeciesPolygon(s: S, p:Polygon[Unit] ) = 
    logic.applicative.Fmap( Species.rasters(s.path) )(
      r => { 
       var max = Int.MinValue
       val f = (col:Int, row:Int, p:Polygon[Unit] ) => {
          val z = r.get(col,row)
          if (z > max) max = z
       }
       PolygonRasterizer.foreachCellByPolygon(p,r.rasterExtent, f)  
       s -> max
     })
  @GET
  def getSpecies(
    @QueryParam("polygon")
    polygon:String
    ) = {

    val p = Species.server.run( io.LoadPolygonGeoJson(polygon) )
 
    val s = logic.Collect(Species.species.map(a => selectSpeciesPolygon(a,p)))
    val sr = Species.server.run(s).groupBy(_._1.species) map {
        case (k,v) => k -> v.groupBy(_._1.year).map {
          case (k,v) => k -> v.groupBy(_._1.climate).map {
            case (k,v) => k -> v.map(t => Map("value" -> t._2,
                                              "cutoff" -> t._1.cutoff,
                                              "valid" -> (t._2 > t._1.cutoff)))
          }
        }
      }


     toJSON(sr)
  } 
  def toJSON(a: Any):String = a match {
      case m: Map[String,Any] => "{ %s }" format (m.map(kv => "\"%s\": %s" format (kv._1, toJSON(kv._2)))).mkString(",")
      case m: String => """"%s"""" format m
      case i: Int => i.toString
      case l: Seq[Any] => "[ %s ]" format (l.map(z => toJSON(z)).mkString(","))
      case b: Boolean => b.toString
      case a => error("Can't handle this!!!! => " + a)
    }
}

@Path("/species")
class Species {

  def selectSpecies(s: S, x: Double, y: Double) =
    logic.applicative.Fmap( Species.rasters(s.path) )(
      r => {
        val (cc,rr) = r.rasterExtent.mapToGrid(x,y)
        s -> r.get(cc,rr)
      })

  @GET
  def getSpecies(
    @QueryParam("x")
    xs: String,

    @QueryParam("y")
    ys: String) = {

    val x = xs.toDouble
    val y = ys.toDouble

    val s = logic.Collect(Species.species.map(a => selectSpecies(a,x,y)))
    val sr = Species.server.run(s).groupBy(_._1.species) map {
        case (k,v) => k -> v.groupBy(_._1.year).map {
          case (k,v) => k -> v.groupBy(_._1.climate).map {
            case (k,v) => k -> v.map(t => Map("value" -> t._2,
                                              "cutoff" -> t._1.cutoff,
                                              "valid" -> (t._2 > t._1.cutoff)))
          }
        }
      }

    
    toJSON(sr)
  }

  def toJSON(a: Any):String = a match {
      case m: Map[String,Any] => "{ %s }" format (m.map(kv => "\"%s\": %s" format (kv._1, toJSON(kv._2)))).mkString(",")
      case m: String => """"%s"""" format m
      case i: Int => i.toString
      case l: Seq[Any] => "[ %s ]" format (l.map(z => toJSON(z)).mkString(","))
      case b: Boolean => b.toString
      case a => error("Can't handle this!!!! => " + a)
    }
}
