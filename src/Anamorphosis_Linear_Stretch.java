import ij.*;
import ij.process.*;
import ij.plugin.*;
import java.lang.*;
import java.awt.*;
import java.awt.image.*;
import java.text.DecimalFormat;
import ij.measure.*;
import ij.plugin.filter.*;
import ij.gui.*;

/* ImageJ plugin for the AnamorphMeIJ project:
 * Anamorphosis Cylindrical Polar
 */

/*
   AnamorphMeIJ is anamorphosis image transformation code implemented as plugins for ImageJ
   It is based on an original application "Anamorph Me!" created in C++ in year 2001

*/


// -- Author: Phillip Kent
// -- phillip.kent@gmail.com
// -- http://www.anamorphosis.com


public class Anamorphosis_Linear_Stretch implements PlugIn
{
 int widthInitial, heightInitial, widthTransform, heightTransform;
 ImageProcessor ipTransform, ipInitial;
 ImagePlus iTransform, iInitial;
 
 double stretchFactorX = 1.0;
 double stretchFactorY = 1.0;

 // Persistent options with default values:

 boolean isColor = false;
 String title;
 int [] rgbArray = new int[3];
 int [] xLyL = new int[3];
 int [] xLyH = new int[3];
 int [] xHyL = new int[3];
 int [] xHyH = new int[3];

 public void run(String arg)
 {
  iInitial = WindowManager.getCurrentImage();
  if(iInitial ==null)
	{IJ.noImage();return ;}
  ipInitial = iInitial.getProcessor();

  if (showDialog(ipInitial))
  {

   widthInitial = ipInitial.getWidth();
   heightInitial = ipInitial.getHeight();
   if (ipInitial instanceof  ColorProcessor) isColor = true;

   
   title = "Linear stretch of "+iInitial.getTitle();
   // call transform method........  
   linearStretch();

   // Copy settings from the original to the transformed image:
   ipTransform.setMinAndMax(ipInitial.getMin (), ipInitial.getMax ());
   ipTransform.setCalibrationTable (ipInitial.getCalibrationTable ());
   iTransform = new ImagePlus(title, ipTransform);
   iTransform.setCalibration (iInitial.getCalibration());
   iTransform.show();
  }

 }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
 public void linearStretch()
 {
	 widthTransform = (int) ((float) widthInitial * stretchFactorX); 
	 heightTransform = (int) ((float) heightInitial * stretchFactorY);
	 
	 // -- Create the new image
	  if (isColor) ipTransform = new ColorProcessor(widthTransform, heightTransform);
	  else ipTransform = new ShortProcessor(widthTransform, heightTransform);

	  // Fill the Polar Grid
	  IJ.showStatus("Calculating...");
	  for (int yy = 0; yy < heightTransform; yy++)
	  {
	   for (int xx = 0; xx < widthTransform; xx++)
	   {
	    double x = (double)xx / stretchFactorX;
	    double y = (double)yy / stretchFactorY;

	    if (isColor)
	    {
	     interpolateColorPixel(x, y);
	     ipTransform.putPixel(xx,yy,rgbArray);
	    }
	    else
	    {
	     double newValue = ipInitial.getInterpolatedPixel(x,y);
	     ipTransform.putPixelValue(xx,yy,newValue);
	    }

	    // -- End out the loops
	   }
	   IJ.showProgress(yy, heightTransform);
	  }
	  IJ.showProgress(1.0);
 }

 boolean showDialog(ImageProcessor ip)
 {
  GenericDialog gd = new GenericDialog("Anamorphosis Linear Stretch");
  gd.addNumericField("Stretch factor horizontally: ", stretchFactorX, 2);
  gd.addNumericField("Stretch factor vertically: ", stretchFactorY, 2);
  gd.showDialog();
  if (gd.wasCanceled()) return false;
  stretchFactorX =  gd.getNextNumber();
  stretchFactorY =  gd.getNextNumber();
  return true;
 }

  void interpolateColorPixel(double x, double y)
 {

  int xL, yL;

  xL = (int)Math.floor(x);
  yL = (int)Math.floor(y);
  xLyL = ipInitial.getPixel(xL, yL, xLyL);
  xLyH = ipInitial.getPixel(xL, yL+1, xLyH);
  xHyL = ipInitial.getPixel(xL+1, yL, xHyL);
  xHyH = ipInitial.getPixel(xL+1, yL+1, xHyH);
  for (int rr = 0; rr<3; rr++)
  {
    double newValue = (xL+1-x)*(yL+1-y)*xLyL[rr];
    newValue += (x-xL)*(yL+1-y)*xHyL[rr];
    newValue += (xL+1-x)*(y-yL)*xLyH[rr];
    newValue += (x-xL)*(y-yL)*xHyH[rr];
    rgbArray[rr] = (int)newValue;
  }
 }

}
