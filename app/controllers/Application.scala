package controllers

import scala.concurrent.duration._
import java.util.Date

import play.api._
import play.api.cache._
import play.api.data._
import play.api.templates._
import play.api.mvc._
import play.api.data.Forms._
import play.api.Play.current

import play.api.libs.ws._

import models._
import models.Rating._
import views._

/** Controller that handles all requests accessible from the main 
	page like the main page itself, the search, the list of most
	recently changed items, the list of all items etc. They all return
	possibly filtered lists of [[models.Statement]] instances.
*/
object Application extends Controller with Cached {
	override val cachePrefix = "app"

	// Index page
	def index = CachedAction("index") { implicit request =>
		val optuser = user(request)

		val oauthor = Author.loadTopLevel
		Ok(views.html.index(
			Author.loadAll, 
			Statement.statistics, 
			Statement.byEntryDate(oauthor, Some(5)), 
			RelatedUrl.loadRecentGroups(7, Some(10)), 
			user(request)
		))
	}
	
	def recent = CachedAction("recent") { implicit request => 		
		val mapstmtByAuthor = Statement.byEntryDate(None, None).groupBy(_.author)
		Ok(views.html.viewStatementList(
			"Alle Wahlversprechen nach letzter Aktualisierung", 
			mapstmtByAuthor, 
			false, 
			user(request),
			Some(routes.Application.updatesAsFeed.url)
		))
	}

	def top = CachedAction("top") { implicit request =>  
		Ok(views.html.viewStatementList("Die wichtigsten Wahlversprechen nach Ressorts", Statement.byImportantTag(None, None).groupBy(_.author), true, user(request)))
	}

	def all = CachedAction("all") { implicit request => 
		Ok(views.html.viewStatementList("Alle Wahlversprechen nach Ressorts", Statement.all(), true, user(request)))
	}

	def media = CachedAction("media") { implicit request => 
		Ok(views.html.media(user(request)))
	}

	def tag(tag: String) = CachedAction("tag." + tag) { implicit request =>
		Ok(views.html.viewStatementList("Wahlversprechen mit Tag '"+tag+"'", Statement.byTag(tag, None, None).groupBy(_.author), false, user(request)))		
	}

	def category(category: String) = CachedAction("category."+category) { implicit request => 
		Ok(views.html.viewStatementList("Wahlversprechen aus dem Ressort '"+category+"'", Statement.byCategory(category, None, None).groupBy(_.author), false, user(request)))		
	}

	def rating(ratingId: Int) = CachedAction("rating."+ratingId) { implicit request => 
		if(0<= ratingId && ratingId < Rating.maxId ) {
			val rating = Rating(ratingId)
			Ok(views.html.viewStatementList("Wahlversprechen mit Bewertung '"+Formatter.name(rating)+"'", Statement.byRating(rating, None, None).groupBy(_.author), true, user(request)))		
		} else {
			NotFound
		}
	}

	def search(query: String) = CachedAction("search."+query) { implicit request => 
		val mapstmtByAuthor = Statement.find(query)
		Ok(views.html.viewStatementList("Suchergebnisse für '" + query + "'", mapstmtByAuthor, false, user(request) ))
	}

	import play.api.libs.json._
	def itemsByAuthorAsJSON(authorName: String) = CachedAction("items.json." + authorName) { implicit request => 
		Author.load(authorName) match {
			case None => NotFound
			case Some(author) => {
				Ok( Json.toJson( Statement.filter(author = Some(author)) ) )
			}
		}
	}

	def authorsAsJSON() = CachedAction("authors.json") { implicit request => 
		Ok(Json.toJson( Author.loadAll() ))
	}

	def tagsAsJSON() = CachedAction("tags.json") { implicit request =>
		Ok( Json.toJson( Tag.loadAll() ) )
	}

	def categoriesAsJSON() = CachedAction("categories.json") { implicit request =>
		Ok( Json.toJson( Category.loadAll() ) )
	}

	def relatedUrlsAsJSON(from: Date, to: Option[Date]) = CachedAction("relatedurls.json."+from+"."+to) 
	{	implicit request =>
		Ok( Json.toJson( RelatedUrl.loadRecent(from, to)))
	}

	def updatesAsFeed = CachedAction("updatesAsFeed") { implicit request =>
		Ok(views.xml.entryList("wahlversprechen2013.de: Alle Aktualisierungen", routes.Application.recent.absoluteURL(false), Entry.loadRecent(10)))			
	}

	def mostActiveCommenters = Action.async { implicit request =>
		implicit val context = scala.concurrent.ExecutionContext.Implicits.global
		Play.configuration.getString("application.disqus_api_key") match {
			case None => 
				scala.concurrent.Future { 
					BadRequest("Disqus API key not configured")
				}				
			case Some(apikey) => 
				val forum = Formatter.disqus_shortname
				val cache = "mostActiveCommenters"
				Cache.get( cache ) match {
					case None => {
						WS.url("http://disqus.com/api/3.0/forums/listMostActiveUsers.json").withQueryString(
							"api_secret" -> apikey, 
							"forum" -> forum,
							"limit" -> "100" 
						).get().map { response =>
							Logger.info("Store " + cache + " in cache.")
							Cache.set( cache, response.json, 2.hours )
						    Ok(response.json)
						}
					}
					case Some(json) => {
						scala.concurrent.Future { 
							Logger.info("Return cached " + cache)
							Ok(json.toString)
						}	
					}
				}				
		}
	}
	
	def loader_io = Action {
		Ok("loaderio-a8c18c9612671703651ea5e79d55623e")
	}

	def preflight(all: String) = CachedAction("preflight", 30.days) { implicit request => 
		Logger.debug("OPTIONS preflight request: " + request.path)
		Ok("").withHeaders("Access-Control-Allow-Origin" -> "*",
	      "Allow" -> "*",
	      "Access-Control-Allow-Methods" -> "POST, GET, PUT, DELETE, OPTIONS",
	      "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept, Referer, User-Agent");
	}

	import com.amazonaws._
	import com.amazonaws.auth.profile._	
	import com.amazonaws.auth._
	import com.amazonaws.regions._
	import com.amazonaws.services.s3._
	import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
	def signs3put(s3_object_name: String, s3_object_type: String) = Action { implicit request => 
		val S3_BUCKET_NAME = "wahlversprechenimages"
		val S3_REGION = "eu-central-1"

		val awsCreds = new DefaultAWSCredentialsProviderChain()
		val s3client = Region.getRegion(Regions.fromName(S3_REGION)).createClient(classOf[AmazonS3Client], awsCreds, null)
		
		val expiration = new java.util.Date()
		val msec = expiration.getTime() + 1000 * 60 * 60 // 1 hour
		expiration.setTime(msec)
		             
		val generatePresignedUrlRequest = 
		              new GeneratePresignedUrlRequest(S3_BUCKET_NAME, s3_object_name)
		generatePresignedUrlRequest.setMethod(HttpMethod.PUT)
		generatePresignedUrlRequest.setExpiration(expiration)
		generatePresignedUrlRequest.setContentType(s3_object_type)
		             
		Ok(s"""{ 
			"signed_request": "${s3client.generatePresignedUrl(generatePresignedUrlRequest).toString()}",
			"url": "https://s3.$S3_REGION.amazonaws.com/$S3_BUCKET_NAME/$s3_object_name"
			}""")
	}

	def javascriptRoutes = CachedAction("javascriptRoutes", 7.days) { implicit request => 
	    import routes.javascript._
	    Ok(
	      Routes.javascriptRouter("jsroutes")(
	        controllers.routes.javascript.Application.itemsByAuthorAsJSON,
	        controllers.routes.javascript.Application.tagsAsJSON,
	        controllers.routes.javascript.Application.categoriesAsJSON,
	        controllers.routes.javascript.Application.authorsAsJSON,
	        controllers.routes.javascript.Application.relatedUrlsAsJSON,
			controllers.routes.javascript.Application.tag,
			controllers.routes.javascript.Application.category,
			controllers.routes.javascript.Application.search,
	        controllers.routes.javascript.DetailViewController.viewAsJSON,
	        controllers.routes.javascript.DetailViewController.relatedUrlsAsJSON,
	        controllers.routes.javascript.DetailViewController.update,
	        controllers.routes.javascript.DetailViewController.addEntry,
			controllers.routes.javascript.DetailViewController.delete,
			controllers.routes.javascript.DetailViewController.addTag,
			controllers.routes.javascript.DetailViewController.deleteTag,
			controllers.routes.javascript.DetailViewController.getEntry,
			controllers.routes.javascript.DetailViewController.updateEntry,
			controllers.routes.javascript.DetailViewController.deleteEntry,
			controllers.routes.javascript.Admin.doImport,
			controllers.routes.javascript.Admin.newUser,
			controllers.routes.javascript.Admin.editUser,
			controllers.routes.javascript.Admin.deleteUser,
			controllers.routes.javascript.Admin.newAuthor,
			controllers.routes.javascript.Admin.editAuthor,
			controllers.routes.javascript.Admin.updateTag,
			controllers.routes.javascript.Admin.deleteTag
	      )
	    ).as("text/javascript")
	}
}

trait ControllerBase {
	def username(request: RequestHeader) = request.session.get("email")
	def user(request: RequestHeader) = username(request) flatMap { User.load(_) }
}

object CacheFormat extends Enumeration {
	type CacheFormat = Value
	val HTML, JSON, RSS = Value
};
import CacheFormat._

trait Cached extends ControllerBase {
	def cachePrefix: String

	private def cacheId(id: Any, format: CacheFormat) = cachePrefix + "." + format + "." + id

	def invalidateCaches(id: Any) {
		for( f <- 0 until CacheFormat.maxId ) Cache.remove( cacheId(id, CacheFormat(f)) )
	}

	def CachedEternal(id: Any, format: CacheFormat)(f: Request[AnyContent] => Result): Action[AnyContent] = {	
		CachedAction(cacheId(id, format), 0.seconds)(f)
	}

	def CachedAction(key: String, duration: Duration = 30.minutes)(f: Request[AnyContent] => Result): Action[AnyContent] = {
	  Action { request =>
	    username(request) match {
	    	case Some(user) => {
	    		Logger.debug("Uncached request. Key = " + key)
	    		f(request)
	    	}
	    	case None => {
	    		Logger.debug("Cached request. Key = " + key + " duration = " + duration.toSeconds);
	    		Cache.getOrElse( key, duration.toSeconds.toInt ) { 
	    			Logger.debug("Added request to cache. Key = " + key);
	    			f(request) 
	    		}
	    	}
	    }
	  }
	}
}
