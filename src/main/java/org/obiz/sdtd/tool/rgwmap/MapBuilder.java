/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package org.obiz.sdtd.tool.rgwmap;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MapBuilder {

    private String path = ".";
    private int downScale = 4;
    private double gamma = 5;
    private boolean applyGammaCorrection = true;
    private int mapSize;
    private int scaledSize;
    private long totalPixels;
    private BufferedImage iHeigths;
    private BufferedImage iBiomes;

    public static void main(String[] args) {
        //TODO command line options
        new MapBuilder().build();
    }

    private void build() {
        try {
            readWorldHeights();
            autoAjustImage();
            applyHeightsToBiomes();
            drawRoads();
            drawPrefabs();
            System.out.println("All work done!\nResulting map image: '4_mapWithObjects.png'.");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    private void drawPrefabs() throws IOException, XMLStreamException {
        String prefabs = "\\prefabs.xml";
        XMLInputFactory xmlif = XMLInputFactory.newInstance();
        XMLStreamReader xmlr = xmlif.createXMLStreamReader(prefabs,new FileInputStream(path + prefabs));

        Graphics g = iBiomes.getGraphics();

        int eventType;

        //fixed object sized (autoscaled)
        int i10 = 10/(downScale*3/4);
        int i15 = (i10*3)/2;
        int i20 = 2 * i10;
        int i30 = 3 * i10;
        int i40 = 4 * i10;
        int i80 = 8 * i10;
        int i160 = 16 * i10;

        while(xmlr.hasNext()) {
            eventType = xmlr.next();
            if(eventType == XMLEvent.START_ELEMENT) {
                if(xmlr.getAttributeCount()==4) {
                    String attributeValue = xmlr.getAttributeValue(2);
                    String[] split = attributeValue.split(",");
                    int x = (mapSize/2 + Integer.parseInt(split[0]))/downScale;
                    int y = (mapSize/2 - Integer.parseInt(split[2]))/downScale;

                    int rgb = Color.RED.getRGB();
                    iBiomes.setRGB(x, y, rgb);

                    g.setColor(Color.DARK_GRAY);
                    g.fillOval(x- i20, y- i20, i40, i40);
                    if(xmlr.getAttributeValue(1).startsWith("water")) {
                        g.setColor(new Color(153, 217, 234));
                    } else {
//                            g.setColor(Color.RED);
                        g.setColor(new Color(185, 122, 87));
                    }
                    g.fillOval(x- i15, y- i15, i30, i30);
                    if(xmlr.getAttributeValue(1).startsWith("water")) {
                        g.setColor(Color.BLUE);
                    } else if(xmlr.getAttributeValue(1).startsWith("cave")) {
                        g.setColor(Color.BLACK);
                    }else {
//                            g.setColor(Color.YELLOW);
                        g.setColor(new Color(239, 228, 176));
                    }
                    g.fillOval(x- i10, y- i10, i20, i20);
                }
            }
        }

        File mapWithObjects = new File(path + "\\4_mapWithObjects.png");
        ImageIO.write(iBiomes, "PNG", mapWithObjects);
    }

    private void drawRoads() throws IOException {
        BufferedImage roads = ImageIO.read(new File(path + "\\splat3.png"));
        System.out.println("Roads loaded");

        for(int xi = roads.getMinX(); xi < roads.getWidth() ; xi ++) {
            for(int yi = roads.getMinY(); yi < roads.getHeight() ; yi ++) {
                int p = roads.getRGB(xi, yi);
                if(p!=0) {
                    iBiomes.setRGB(xi/downScale, yi/downScale, Color.DARK_GRAY.getRGB());
                }
            }
        }

        File map_with_roads = new File(path + "\\5_map_with_roads.png");
        ImageIO.write(iBiomes, "PNG", map_with_roads);
    }

    private void applyHeightsToBiomes() throws IOException {
        long start, end;
        BufferedImage inputImage = ImageIO.read(new File(path + "\\biomes.png"));

        iBiomes = new BufferedImage(scaledSize,scaledSize,inputImage.getType());

        // scale the input biomes image to the output image size
        Graphics2D g2d = iBiomes.createGraphics();
        g2d.drawImage(inputImage, 0, 0, scaledSize, scaledSize, null);
        g2d.dispose();

        //free mem
        inputImage.flush();

        start = System.nanoTime();
        //write heights image to file
        File bump = new File(path + "\\1_bump.png");
        ImageIO.write(iHeigths, "PNG", bump);
        //write scaled biomes to file
        File biomes = new File(path + "\\2_biomes.png");
        ImageIO.write(iBiomes, "PNG", biomes);
        end = System.nanoTime();
        System.out.println("File saving time:  = " + (end-start)/1000000000 + "s");
//        Desktop.getDesktop().open(output);


        // normal vectors array
        float[][] normalVectors = new float[scaledSize * scaledSize][3];
        // precalculate normal vectors
        BumpMappingUtils.FindNormalVectors(iHeigths, normalVectors);
        //free mem
        iHeigths.flush();
        //apply bump-mapping using normal vectors
        BumpMappingUtils.paint(iBiomes, scaledSize, scaledSize, normalVectors);

        //Write bump-mapped biomes
        File biomesShadow = new File(path + "\\3_biomesShadow.png");
        ImageIO.write(iBiomes, "PNG", biomesShadow);
    }

    private void autoAjustImage() {
        WritableRaster raster = iHeigths.getRaster();
        // initialisation of image histogram array
        long hist[] = new long[256];
        for (int i = 0; i < hist.length; i++) {
            hist[i] = 0;
        }
        //time measurement vars
        long start, end;
        //init other stats
        int min = raster.getSample(raster.getMinX(), raster.getMinY(), 0);
        int max = min;
        long rms = 0;
        double mean = 0;
        int tcount = 0;

        start = System.nanoTime();
        for (int x = raster.getMinX(); x < raster.getMinX()+raster.getWidth(); x++) {
            for (int y = raster.getMinY(); y < raster.getMinY()+raster.getHeight(); y++) {

                //get integer height value from a current pixel
                int color = raster.getSample(x, y, 0);

                //find min and max heights
                if(color < min) {
                    min = color;
                } else if (color > max) {
                    max = color;
                }

                //build histogram
                hist[color/256]++;

                //calulate MEAN
                mean += color*1./totalPixels;
                long lColor = (long)color;
                lColor*=lColor;
                lColor/=totalPixels;
                rms+= lColor;

                //just check pixels count
                tcount++;
            }
        }
        assert tcount==totalPixels;
        end = System.nanoTime();
        long t1 = end - start;
        System.out.println("Time to solve stats: " + t1/1000000 + "ms");
//        System.out.println("tcount = " + tcount);

        rms = Math.round(Math.sqrt(rms));
        System.out.println("mean = " + Math.round(mean));
        System.out.println("rms = " + rms);
        System.out.println("min = " + min);
        System.out.println("max = " + max);

        double D = 0;
        for (int i = 0; i < hist.length; i++) {
            long a = i * 256 - rms;
            double tmp = Math.pow(a, 2);
            tmp/=tcount;
            tmp*=hist[i];
            D += tmp;
        }
        D = Math.round(Math.sqrt(D));
        System.out.println("D2 = " + D);

        int startHist = (int) Math.round(rms - gamma*D);
        System.out.println("startHist = " + startHist);
        double k = 256*256/(256*256 - startHist);
        System.out.println("k = " + k);
//        System.exit(0);


        if(applyGammaCorrection)
            for (int x = raster.getMinX(); x < raster.getMinX()+raster.getWidth(); x++) {
                for (int y = raster.getMinY(); y < raster.getMinY()+raster.getHeight(); y++) {
                    int grayColor = raster.getSample(x, y, 0);
                    int imageColor;
                    if(grayColor < startHist) {
                        imageColor = 0;
                    } else {
                        imageColor = (int) Math.round((grayColor - startHist) * k);
                    }
                    raster.setSample(x, y, 0, imageColor);
                }
            }
    }

    public void readWorldHeights() throws IOException {
        File heightsFile = new File(path + "\\dtm.raw");
        mapSize = (int) Math.round(Math.sqrt(heightsFile.length()/2.));
        System.out.println("Detected mapSize: " + mapSize);
        scaledSize = mapSize / downScale;
        System.out.println("Resulting image side size will be: " + scaledSize + "px");
        //TODO rename to totalScaledPixels
        totalPixels = scaledSize;
        totalPixels *= totalPixels;
        //Result processed heights image
        iHeigths = new BufferedImage(scaledSize, scaledSize, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster raster = iHeigths.getRaster();

        FileInputStream hmis = new FileInputStream(heightsFile);

        try {
            byte buf[] = new byte[mapSize * 4];

            int readedBytes;
            int curPixelNum = 0;
            System.out.print("File load:\n|----------------|\n|");
            while ((readedBytes = hmis.read(buf)) > -1) {
                //TODO here potential problem if readedBytes%2 != 0
                //convert every 2 bytes to new gray pixel
                for (int i = 0; i < readedBytes / 2; i++) {
                    int grayColor = buf[i * 2] + 128 + 256 * (buf[i * 2 + 1] + 128);

                    //TODO use avg of pixel color with same coordinate in scaled image.
                    //calculate pixel position
                    int x = (curPixelNum % mapSize) / downScale;
                    int y = (curPixelNum / mapSize) / downScale;
                    //write pixel to resulting image
                    raster.setSample(x, y, 0, grayColor);

                    curPixelNum++;
                    //Draw progress bar
                    if (curPixelNum % (mapSize * 512) == 0) {
                        System.out.print("-");
                    }
                }
            }
            System.out.println("|\nDone.");
        } finally {
            hmis.close();
        }
    }
}