import play.api._
import play.api.templates._

import models._
import anorm._

object Global extends GlobalSettings {  
  override def onStart(app: Application) {
    InitialData.insert()
  }  
}

/**
 * Initial set of data to be imported 
 * in the sample application.
 */
object InitialData {
  
  def date(str: String) = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(str)
  
  def insert() = {    
    if(User.findAll.isEmpty) {
      var ausers = new collection.mutable.ArrayBuffer[User];
      Array(
          User(NotAssigned, "sebastian@theophil.net", "Sebastian", "secret", Role.Admin),
          User(NotAssigned, "test@test.net", "Tester", "secret", Role.Editor),
          User(NotAssigned, "test2@test.net", "Tester2", "secret", Role.Editor)
      ).foreach(u => {
        val id = User.create(u)
        ausers += User(id, u.email, u.name, u.password, u.role)
      });
      
      var acategories = new collection.mutable.ArrayBuffer[Category];
      Array( 
        Category(NotAssigned, "Wirtschaft", 1),
        Category(NotAssigned, "Inneres", 2),
        Category(NotAssigned, "Verteidigung", 3),
        Category(NotAssigned, "Landwirtschaft", 4)
      ).foreach(c => {
        val id  = Category.create(c)
        acategories += Category(id, c.name, c.order)
      })
      
      val aentries = List(
    	  Entry(NotAssigned, NotAssigned, "<strong>This is a super important update.</strong>", date("2013-05-10"), ausers(0)),
    	  Entry(NotAssigned, NotAssigned, "<p>Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.</p>", date("2013-05-12"), ausers(1)), 
    	  Entry(NotAssigned, NotAssigned, "<i>Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua.</i>", date("2013-05-09"), ausers(2)),
    	  Entry(NotAssigned, NotAssigned, "<h1>This is a super important update</h1><p>Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata</p>", date("2013-05-20"), ausers(1)),
    	  Entry(NotAssigned, NotAssigned, "<blockquote>\"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore\"</blockquote>", date("2013-05-21"), ausers(0))
      )
      
      Seq(
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(0), aentries.slice(0, 2), Rating.PromiseKept),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(0), aentries.slice(1, 3), Rating.PromiseKept),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(0), aentries, Rating.PromiseBroken),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(0), aentries.slice(0, 4), Rating.Unrated),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(1), List[Entry](), Rating.Compromise),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(1), List[Entry](), Rating.PromiseBroken),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(1), List[Entry](), Rating.InTheWorks),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(1), List[Entry](), Rating.PromiseKept),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(1), List[Entry](), Rating.PromiseBroken),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(2), List[Entry](), Rating.PromiseKept),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(2), List[Entry](), Rating.Stalled),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(3), List[Entry](), Rating.Stalled),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(3), List[Entry](), Rating.PromiseKept),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(3), List[Entry](), Rating.PromiseKept),
        Statement(NotAssigned, "Lorem ipsum dfhdk djhfd kdkjhdfk jd dd", acategories(3), List[Entry](), Rating.PromiseKept)
      ).foreach(Statement.create)
    }
    
  }
  
}