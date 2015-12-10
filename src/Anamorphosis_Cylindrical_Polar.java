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
   <http://www.anamorphosis.com>
*/

/*
   Copyright (C) 2015 Phillip Kent
   
   Author contact: phillip.kent@xmlsoup.com, <http://www.phillipkent.net>

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.
	
	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	Please be aware that the GNU GPL forbids the use or modification of
	this program in proprietary software. For enquiries about this contact the
	author via the address given above.
	
	A copy of the GNU General Public License version 3 can be found in the
	file LICENSE-gpl-v3.md in this repository. If it is missing from your
	version of this repository, see <http://www.gnu.org/licenses/>.
*/


public class Anamorphosis_Cylindrical_Polar implements PlugIn
{
 int widthInitial, heightInitial, widthTransform, heightTransform;
 double centerX, centerY;
 ImageProcessor ipTransform, ipInitial;
 ImagePlus iTransform, iInitial;

 // Persistent options with default values:
 /////TO BE DELETED/MODIFED
 static boolean toPolar = true;
 static boolean polar180 = false;
 static boolean defaultLines = true;
 static boolean defaultCenter = true;
 static boolean clockWise = false;
 
 /////ADDED
 int mirrorRadius = 100;
 int angularSize = 220;

 boolean isColor = false;
 String title;
 //DELETE THESE...
 int angleLines = 180;
 /////
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

   title = "Cylindrical mirror (polar) anamorphosis of "+iInitial.getTitle();
   // call transform method........  
   polarTransform();
   ////
   //angleLines = 360;
   //if(!defaultLines) getLines();
   //polar360();
   ////

   // Copy settings from the original to the transformed image:
   ipTransform.setMinAndMax(ipInitial.getMin (), ipInitial.getMax ());
   ipTransform.setCalibrationTable (ipInitial.getCalibrationTable ());
   iTransform = new ImagePlus(title, ipTransform);
   iTransform.setCalibration (iInitial.getCalibration());
   iTransform.show();
  }

 }

///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
 public void polarTransform()
 {
	 
 }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////// -- polar360()
 public void polar360()
 {
  // Establish the default center of Cartesian space
  getPolarCenter ();

  // Set up the Polar Grid:
  // Use y values for angles
  // -- Need 360 degrees (0 to 359...)
  heightTransform = angleLines;

  // Line width will be:
  //  --  equal to radius -- Need to find the greatest radius
  //  --  (4 possibilities: from center to each corner)
  //  --  Top-Left Corner (0,0):
  double radius = Math.sqrt((centerX-0)*(centerX-0) + (centerY-0)*(centerY-0));
  //  --  Top-Right Corner (widthInitial, 0):
  double radiusTemp = Math.sqrt((centerX-widthInitial)*(centerX-widthInitial) + (centerY-0)*(centerY-0));
  if (radiusTemp>radius) radius = radiusTemp;
  //  --  Bottom-Left Corner (0, heightInitial):
  radiusTemp = Math.sqrt((centerX-0)*(centerX-0) + (centerY-heightInitial)*(centerY-heightInitial));
  if (radiusTemp>radius) radius = radiusTemp;
  //  -- Bottom-Right Corner (widthInitial , heightInitial):
  radiusTemp = Math.sqrt((centerX-widthInitial)*(centerX-widthInitial) + (centerY-heightInitial)*(centerY-heightInitial));
  if (radiusTemp>radius) radius = radiusTemp;
  int radiusInt = (int)radius;
  widthTransform = radiusInt;

  // -- Create the new image
  if (isColor) ipTransform = new ColorProcessor(widthTransform, heightTransform);
  else ipTransform = new ShortProcessor(widthTransform, heightTransform);

  // Fill the Polar Grid
  IJ.showStatus("Calculating...");
  for (int yy = 0; yy < heightTransform; yy++)
  {
   for (int xx = 0; xx < widthTransform; xx++)
   {

    // -- For each polar pixel, need to convert it to Cartesian coordinates
    double r = xx;
    double angle = (yy/(double)angleLines)*Math.PI*2;

    // -- Need convert (x,y) into pixel coordinates
    double x = getCartesianX (r, angle) + centerX;
    double y = getCartesianY (r, angle) + centerY;

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
  GenericDialog gd = new GenericDialog("Anamorphosis Cylindrical Polar");
  gd.addNumericField("Mirror radius: ", mirrorRadius, 0, 4, "pixels");
  gd.addNumericField("Angular size: ", angularSize, 0, 3, "degrees");
  gd.showDialog();
  if (gd.wasCanceled()) return false;
  mirrorRadius =  (int) gd.getNextNumber();
  angularSize = (int) gd.getNextNumber();
  return true;
 }

 public void getCartesianCenter ()
 {
  centerX = widthTransform/2;
  centerY = heightTransform/2;
  if (! defaultCenter) {
   getCenter();
  }
 }

 public void getPolarCenter ()
 {
  // If a roi was defined, use it as default center :
  Rectangle b = new Rectangle(0, 0, widthInitial, heightInitial);
  Roi roi = iInitial.getRoi();
  if (roi!=null) {
   b = roi.getBounds();
  }
  centerX = b.x + b.width/2;
  centerY = b.y + b.height/2;
  if (! defaultCenter) {
   getCenter();
  }
 }

 void getCenter()
 {
  GenericDialog gd = new GenericDialog("Center of Cartesian Grid");
  gd.addNumericField("Center x Coordinate:", centerX, 2);
  gd.addNumericField("Center y Coordinate:", centerY, 2);
  gd.showDialog();
  centerX =  gd.getNextNumber();
  centerY =  gd.getNextNumber();
 }

 void getLines()
 {
  GenericDialog gd = new GenericDialog("Polar Transform Options");
  gd.addNumericField("Number of Lines in Angle Dimension:", angleLines, 0);
  gd.showDialog();
  angleLines =  (int)gd.getNextNumber();
 }

 double getCartesianX (double r, double angle)
 {
  return r*Math.cos(angle);
 }

 double getCartesianY (double r, double angle)
 {
  double y = r*Math.sin(angle);
  return clockWise ? -y : y;
 }

 double getRadius(double x, double y)
 {
  return Math.sqrt(x*x+ y*y);
 }

 double getAngle(double x, double y)
 {
  // Returns an angle in the range [0, 360[
  double angle = Math.toDegrees (Math.atan2 (y, x));
  if (angle < 0) {
   angle += 360;
  }
  return clockWise ? 360 - angle : angle;
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
