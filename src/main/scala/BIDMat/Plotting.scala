package BIDMat
import ptolemy.plot._
import java.awt.image.BufferedImage
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import scala.collection.mutable.ListBuffer;

class MyPlot extends Plot {
  var fut:Future[_] = null;
  var frame:PlotFrame = null;
  var done:Boolean = false;
}

class MyHistogram extends Histogram {
  var fut:Future[_] = null;
  var frame:PlotFrame = null;
  var done:Boolean = false;
}

object Plotting { 
  var ifigure:Int = 0;
  val marksmat = Array("points","dots","various");
  
  def _plot(mats0:Array[Mat])(xlog:Boolean=false, ylog:Boolean=false, isconnected:Boolean=true, bars:Boolean=false, marks:Int = 0) = {
    var p:Plot = new Plot;
    val mats = mats0.map(MatFunctions.cpu);
    p.setXLog(xlog);
    p.setYLog(ylog);
    p.setBars(bars);
    p.setConnected(isconnected);
    p.setMarksStyle(marksmat(marks));
    val dataset = 0
    _replot(mats, p, dataset, isconnected)
    ifigure += 1;
    showGraphics(p);
  }
  
  def _liveplot(fn:()=>Array[Mat], interval:Float)(xlog:Boolean=false, ylog:Boolean=false, isconnected:Boolean=true, bars:Boolean=false, marks:Int = 0) = {
    var p:MyPlot = new MyPlot;
    p.setXLog(xlog);
    p.setYLog(ylog);
    p.setBars(bars);
    p.setConnected(isconnected);
    p.setMarksStyle(marksmat(marks));
    val dataset = 0;
    val mats = fn().map(MatFunctions.cpu);
    _replot(mats, p, dataset, isconnected);
    p.frame = new PlotFrame("Figure "+ifigure, p);
  	p.frame.setVisible(true);
  	p.done = false;
    val runme = new Runnable {
      override def run() = {
        while (!p.done) {
        	Thread.sleep((1000*interval).toLong);
        	val mats = fn().map(MatFunctions.cpu);
        	for (i <- 0 until mats.length) p.clear(i);
        	_replot(mats, p, dataset, isconnected);
        }
      }
    }
    ifigure += 1;
    p.fut = Image.getService.submit(runme);
    p;
  }
  
  def history(fn:()=>Mat):()=>ListBuffer[Mat] = {
  	val listbuff = new ListBuffer[Mat]();
  	def histfun() = {
  		listbuff.append(fn());
  		listbuff;
  	}
  	histfun _;
  }
  
  def _liveplot2(fn:()=>Mat, interval:Float)(xlog:Boolean=false, ylog:Boolean=false, isconnected:Boolean=true, bars:Boolean=false, marks:Int = 0) = {
    var p:MyPlot = new MyPlot;
    p.setXLog(xlog);
    p.setYLog(ylog);
    p.setBars(bars);
    p.setConnected(isconnected);
    p.setMarksStyle(marksmat(marks));
    val dataset = 0;
    val mat = MatFunctions.cpu(fn());
    _replot(Array(mat), p, dataset, isconnected);
    p.frame = new PlotFrame("Figure "+ifigure, p);
  	p.frame.setVisible(true);
  	p.done = false;
    val runme = new Runnable {
      override def run() = {
      	while (!p.done) {
      		Thread.sleep((1000*interval).toLong);
      		val mat = MatFunctions.cpu(fn());
      		p.clear(0);
      		_replot(Array(mat), p, dataset, isconnected);
      	}
      }
    }
    ifigure += 1;
    p.fut = Image.getService.submit(runme);
    p;
  }
  
  def _liveplot3(fn:()=>ListBuffer[Mat], interval:Float)(xlog:Boolean=false, ylog:Boolean=false, isconnected:Boolean=true, bars:Boolean=false, marks:Int = 0) = {
    var p:MyPlot = new MyPlot;
    p.setXLog(xlog);
    p.setYLog(ylog);
    p.setBars(bars);
    p.setConnected(isconnected);
    p.setMarksStyle(marksmat(marks));
    val dataset = 0;
    val listbuff = fn();
    _replot(listbuff, p, dataset, isconnected);
    p.frame = new PlotFrame("Figure "+ifigure, p);
  	p.frame.setVisible(true);
  	p.done = false;
    val runme = new Runnable {
      override def run() = {
      	while (!p.done) {
      		Thread.sleep((1000*interval).toLong);
      		val listbuff = fn();
      		p.clear(0);
      		_replot(listbuff, p, dataset, isconnected);
      	}
      }
    }
    ifigure += 1;
    p.fut = Image.getService.submit(runme);
    p;
  }

  def _replot(mats:Array[Mat], p:Plot, dataset:Int, isconnected:Boolean) = {
  	if (mats.length == 1) {
  		val m = mats(0);
  		if (m.nrows == 1 || m.ncols == 1) { 
  		  var i = 0;
  			m match { 
  			case mf:FMat => while (i < m.length) {p.addPoint(dataset, i, mf.data(i), isconnected); i += 1}
  			case md:DMat => while (i < m.length) {p.addPoint(dataset, i, md.data(i), isconnected); i += 1}
  			case mi:IMat => while (i < m.length) {p.addPoint(dataset, i, mi.data(i), isconnected); i += 1}
  			}
  		} else {
  			for (i <- 0 until m.ncols) {
  				var j = 0;
  				m match { 
  				case mf:FMat => while (j < m.nrows) {p.addPoint(i, j, mf(j,i), isconnected); j += 1}
  				case md:DMat => while (j < m.nrows) {p.addPoint(i, j, md(j,i), isconnected); j += 1}
  				case mi:IMat => while (j < m.nrows) {p.addPoint(i, j, mi(j,i), isconnected); j += 1}
  				}     
  			}
  		}
  	} else {
  		var i = 0;
  		while (i*2 < mats.length) {
  		  var j = 0;
  			(mats(2*i), mats(2*i+1)) match { 
  			case (a:FMat, b:FMat) => while (j < a.length) {p.addPoint(i, a.data(j), b.data(j), isconnected); j += 1}
  			case (a:FMat, b:DMat) => while (j < a.length) {p.addPoint(i, a.data(j), b.data(j), isconnected); j += 1}
  			case (a:DMat, b:FMat) => while (j < a.length) {p.addPoint(i, a.data(j), b.data(j), isconnected); j += 1}
  			case (a:DMat, b:DMat) => while (j < a.length) {p.addPoint(i, a.data(j), b.data(j), isconnected); j += 1}
  			case (a:FMat, b:IMat) => while (j < a.length) {p.addPoint(i, a.data(j), b.data(j), isconnected); j += 1}
  			case (a:DMat, b:IMat) => while (j < a.length) {p.addPoint(i, a.data(j), b.data(j), isconnected); j += 1}
  			case (a:IMat, b:FMat) => while (j < a.length) {p.addPoint(i, a.data(j), b.data(j), isconnected); j += 1}
  			case (a:IMat, b:DMat) => while (j < a.length) {p.addPoint(i, a.data(j), b.data(j), isconnected); j += 1}
  			case (a:IMat, b:IMat) => while (j < a.length) {p.addPoint(i, a.data(j), b.data(j), isconnected); j += 1}
  			}  
  			i += 1;
  		}
  	} 
  }
  
  def _replot(mats:ListBuffer[Mat], p:Plot, dataset:Int, isconnected:Boolean) = {
  	if (mats.length == 1) {
  		val m = mats(0);
  		var i = 0;
  		m match { 
  		case mf:FMat => {
  		  for (mat <- mats) {
  		  	val fdata = mat.asInstanceOf[FMat].data;
  		  	var j = 0;
  		  	while (j < mat.length) {			  
  		  		p.addPoint(j, i, fdata(j), isconnected); 
  		  		j += 1}
  		  	i += 1;
  		  }
  		}
  		case md:DMat => {
  		  for (mat <- mats) {
  		  	val ddata = mat.asInstanceOf[DMat].data;
  		  	var j = 0;
  		  	while (j < mat.length) {			  
  		  		p.addPoint(j, i, ddata(j), isconnected); 
  		  		j += 1}
  		  	i += 1;
  		  }
  		}
  		case mi:IMat => {
  		  for (mat <- mats) {
  		  	val idata = mat.asInstanceOf[IMat].data;
  		  	var j = 0;
  		  	while (j < mat.length) {			  
  		  		p.addPoint(j, i, idata(j), isconnected); 
  		  		j += 1}
  		  	i += 1;
  		  }
  		}
  		}
  	}
  }
  
  def showGraphics(plot:PlotBox):BufferedImage = {
  	var pframe = new PlotFrame("Figure "+ifigure, plot);
  	pframe.setVisible(true);
    if (Mat.inline) {
      val bi = new BufferedImage(pframe.getWidth(), pframe.getHeight(), BufferedImage.TYPE_INT_ARGB);
      val graphics = bi.createGraphics();
      pframe.print(graphics);
      pframe.dispose;
      graphics.dispose;
      bi;
    } else {
    	Image.dummyImage.img;
    }
  }
  
  def plot(mats:Mat*) = _plot(mats.toArray)();
  
  def scatter(mats:Mat*) = _plot(mats.toArray)(marks=1, isconnected=false);
  
  def loglog(mats:Mat*) = _plot(mats.toArray)(xlog=true, ylog=true)
  
  def semilogx(mats:Mat*) = _plot(mats.toArray)(xlog=true)
  
  def semilogy(mats:Mat*) = _plot(mats.toArray)(ylog=true)

  def barplot(mats:Mat*) = _plot(mats.toArray)(isconnected=false, bars=true)
  
  def barloglog(mats:Mat*) = _plot(mats.toArray)(xlog=true, ylog=true, isconnected=false, bars=true)
  
  def barsemilogx(mats:Mat*) = _plot(mats.toArray)(xlog=true, isconnected=false, bars=true)
  
  def barsemilogy(mats:Mat*) = _plot(mats.toArray)(ylog=true, isconnected=false, bars=true)
  
  def p_plot(mats:Mat*) = _plot(mats.toArray)(isconnected=false)
  
  def ploglog(mats:Mat*) = _plot(mats.toArray)(xlog=true, ylog=true, isconnected=false)
  
  def psemilogx(mats:Mat*) = _plot(mats.toArray)(xlog=true, isconnected=false)
  
  def psemilogy(mats:Mat*) = _plot(mats.toArray)(ylog=true, isconnected=false)
  
  
  def plot(fn:()=>Mat) = _liveplot2(fn, 1f)();
  
  def scatter(fn:()=>Mat) = _liveplot2(fn, 1f)(marks=1, isconnected=false);
  
  def loglog(fn:()=>Mat) = _liveplot2(fn, 1f)(xlog=true, ylog=true)
  
  def semilogx(fn:()=>Mat) = _liveplot2(fn, 1f)(xlog=true)
  
  def semilogy(fn:()=>Mat) = _liveplot2(fn, 1f)(ylog=true)

  def barplot(fn:()=>Mat) = _liveplot2(fn, 1f)(isconnected=false, bars=true)
  
  def barloglog(fn:()=>Mat) = _liveplot2(fn, 1f)(xlog=true, ylog=true, isconnected=false, bars=true)
  
  def barsemilogx(fn:()=>Mat) = _liveplot2(fn, 1f)(xlog=true, isconnected=false, bars=true)
  
  def barsemilogy(fn:()=>Mat) = _liveplot2(fn, 1f)(ylog=true, isconnected=false, bars=true)
  
  def p_plot(fn:()=>Mat) = _liveplot2(fn, 1f)(isconnected=false)
  
  def ploglog(fn:()=>Mat) = _liveplot2(fn, 1f)(xlog=true, ylog=true, isconnected=false)
  
  def psemilogx(fn:()=>Mat) = _liveplot2(fn, 1f)(xlog=true, isconnected=false)
  
  def psemilogy(fn:()=>Mat) = _liveplot2(fn, 1f)(ylog=true, isconnected=false)
  
  
   
  def plot(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)();
  
  def scatter(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(marks=1, isconnected=false);
  
  def loglog(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(xlog=true, ylog=true)
  
  def semilogx(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(xlog=true)
  
  def semilogy(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(ylog=true)

  def barplot(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(isconnected=false, bars=true)
  
  def barloglog(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(xlog=true, ylog=true, isconnected=false, bars=true)
  
  def barsemilogx(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(xlog=true, isconnected=false, bars=true)
  
  def barsemilogy(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(ylog=true, isconnected=false, bars=true)
  
  def p_plot(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(isconnected=false)
  
  def ploglog(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(xlog=true, ylog=true, isconnected=false)
  
  def psemilogx(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(xlog=true, isconnected=false)
  
  def psemilogy(fn:()=>Mat, interval:Float) = _liveplot2(fn, interval)(ylog=true, isconnected=false)
  
  
  def hplot(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)();
  
  def hscatter(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(marks=1, isconnected=false);
  
  def hloglog(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(xlog=true, ylog=true)
  
  def hsemilogx(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(xlog=true)
  
  def hsemilogy(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(ylog=true)

  def hbarplot(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(isconnected=false, bars=true)
  
  def hbarloglog(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(xlog=true, ylog=true, isconnected=false, bars=true)
  
  def hbarsemilogx(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(xlog=true, isconnected=false, bars=true)
  
  def hbarsemilogy(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(ylog=true, isconnected=false, bars=true)
  
  def hp_plot(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(isconnected=false)
  
  def hploglog(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(xlog=true, ylog=true, isconnected=false)
  
  def hpsemilogx(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(xlog=true, isconnected=false)
  
  def hpsemilogy(fn:()=>ListBuffer[Mat], interval:Float) = _liveplot3(fn, interval)(ylog=true, isconnected=false)
  
   
  def liveplot(fn:()=>Array[Mat]) = _liveplot(fn, 1f)();
  
  def livescatter(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(marks=1, isconnected=false);
  
  def liveloglog(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(xlog=true, ylog=true)
  
  def livesemilogx(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(xlog=true)
  
  def livesemilogy(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(ylog=true)

  def livebarplot(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(isconnected=false, bars=true)
  
  def livebarloglog(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(xlog=true, ylog=true, isconnected=false, bars=true)
  
  def livebarsemilogx(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(xlog=true, isconnected=false, bars=true)
  
  def livebarsemilogy(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(ylog=true, isconnected=false, bars=true)
  
  def livep_plot(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(isconnected=false)
  
  def liveploglog(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(xlog=true, ylog=true, isconnected=false)
  
  def livepsemilogx(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(xlog=true, isconnected=false)
  
  def livepsemilogy(fn:()=>Array[Mat]) = _liveplot(fn, 1f)(ylog=true, isconnected=false)
  
  
   
  def liveplot(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)();
  
  def livescatter(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(marks=1, isconnected=false);
  
  def liveloglog(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(xlog=true, ylog=true)
  
  def livesemilogx(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(xlog=true)
  
  def livesemilogy(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(ylog=true)

  def livebarplot(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(isconnected=false, bars=true)
  
  def livebarloglog(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(xlog=true, ylog=true, isconnected=false, bars=true)
  
  def livebarsemilogx(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(xlog=true, isconnected=false, bars=true)
  
  def livebarsemilogy(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(ylog=true, isconnected=false, bars=true)
  
  def livep_plot(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(isconnected=false)
  
  def liveploglog(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(xlog=true, ylog=true, isconnected=false)
  
  def livepsemilogx(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(xlog=true, isconnected=false)
  
  def livepsemilogy(fn:()=>Array[Mat], interval:Float) = _liveplot(fn, interval)(ylog=true, isconnected=false)
   
  
  def hist(m:Mat, nbars:Int):BufferedImage = { 
    var p:Histogram = new Histogram
    _hist(m, nbars, p);
    ifigure += 1;
    showGraphics(p);
  }
  
  def hist(m:Mat):BufferedImage = hist(m, 10);
  
  
  def hist(fn:()=>Mat, nbars:Int, interval:Float):MyHistogram = { 
  	var p:MyHistogram = new MyHistogram;
    _hist(MatFunctions.cpu(fn()), nbars, p);
    p.frame = new PlotFrame("Figure "+ifigure, p);
  	p.frame.setVisible(true);
    p.done = false;
    val runme = new Runnable {
    	override def run() = {
    		while (!p.done) {
    			Thread.sleep((1000*interval).toLong);
    			val mat = MatFunctions.cpu(fn());
    			p.clear(false);
    			_hist(mat, nbars, p);   			
    			p.repaint();
    		}
    	}
    }
    ifigure += 1;
    p.fut = Image.getService.submit(runme);
    p
  }
  
  def hist(fn:()=>Mat, nbars:Int):MyHistogram = hist(fn, nbars, 1f);
  
  def hist(fn:()=>Mat):MyHistogram = hist(fn, 10, 1f);
  
  def _hist(m:Mat, nbars:Int, p:Histogram) = {
    import SciFunctions._
    val dataset = 0
    if (m.nrows == 1 || m.ncols == 1) { 
    	m match { 
    	case mf:FMat => {
    	  var vmax = maxi(mf,0).v
    	  var vmin = mini(mf,0).v
    	  p.setBinWidth((vmax-vmin)/nbars)
    	  for (i <- 0 until m.length) p.addPoint(dataset, mf(i))
    	}
    	case md:DMat => {
    		var vmax = maxi(md,0).v
    	  var vmin = mini(md,0).v
    	  p.setBinWidth((vmax-vmin)/nbars)
    	  for (i <- 0 until m.length) p.addPoint(dataset, md(i))
    	}
    	case mi:IMat => {
    		var vmax = maxi(mi,0).v.asInstanceOf[Double]
    	  var vmin = mini(mi,0).v
    	  p.setBinWidth((vmax-vmin)/nbars)
    	  for (i <- 0 until m.length) p.addPoint(dataset, mi(i))
    	}
      }
    }   
  }
}
