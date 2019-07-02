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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MapBuilder {

    private String path = ".";
    private int downScale = 4;
    private float gamma = 5;
    private boolean applyGammaCorrection = true;
    private int mapSize;
    private int scaledSize;
    private long totalPixels;
    private BufferedImage iHeigths;
    private BufferedImage iBiomes;
    private int waterLine;
    private boolean doBlureBiomes = true;
    private int bloorK = 256; //part of image size used as blure radius

    //fixed object sized (autoscaled)
    int i2 = 8 / downScale;
    int i10 = 10 / (downScale);
    int i5 = i10 / 2;
    int i15 = (i10 * 3) / 2;
    int i20 = 2 * i10;
    int i25 = (i10 * 5) / 2;
    int i30 = 3 * i10;
    int i35 = (7 * i10) / 2;
    int i40 = 4 * i10;
    int i45 = (9 * i10) / 2;
    int i50 = 5 * i10;
    int i60 = 6 * i10;
    int i70 = 7 * i10;
    int i80 = 8 * i10;
    int i160 = 16 * i10;
    int i250 = 25 * i10;

    int fileNum = 1;
    private BufferedImage iWaterZones;

    public static void main(String[] args) {
        //TODO command line options
        new MapBuilder().build();
    }

    private void build() {
        try {
            readWorldHeights();
            readWatersPoint();
            autoAjustImage();
            applyHeightsToBiomes();
            drawRoads();
            drawPrefabs();
            System.out.println("All work done!\nResulting map image: '6_mapWithObjects.png'.");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    private void readWatersPoint() throws IOException, XMLStreamException {
        String prefabs = "\\water_info.xml";
        XMLInputFactory xmlif = XMLInputFactory.newInstance();
        XMLStreamReader xmlr = xmlif.createXMLStreamReader(prefabs, new FileInputStream(path + prefabs));

        iWaterZones = new BufferedImage(scaledSize, scaledSize, BufferedImage.TYPE_BYTE_BINARY);

        Graphics graphics = iWaterZones.getGraphics();

        int eventType;
        while (xmlr.hasNext()) {
            eventType = xmlr.next();
            if (eventType == XMLEvent.START_ELEMENT) {
                if(xmlr.getAttributeCount()==5) {
                    String attributeValue = xmlr.getAttributeValue(0);
                    String[] split = attributeValue.split(",");
                    int x = (mapSize/2 + Integer.parseInt(split[0].trim()))/downScale;
                    int y = (mapSize/2 - Integer.parseInt(split[2].trim()))/downScale;

                    graphics.setColor(Color.WHITE);
                    graphics.fillOval(x - i250 /2, y - i250 /2, i250, i250);

                }
            }
        }

        File waterZones = new File(path + "\\" + fileNum++ + "_waterZones.png");
        ImageIO.write(iWaterZones, "PNG", waterZones);

    }

    private void drawPrefabs() throws IOException, XMLStreamException {
        String prefabs = "\\prefabs.xml";
        XMLInputFactory xmlif = XMLInputFactory.newInstance();
        XMLStreamReader xmlr = xmlif.createXMLStreamReader(prefabs,new FileInputStream(path + prefabs));

        Graphics g = iBiomes.getGraphics();

        int eventType;

        //fixed buildings colors
        HashMap<String, Color> buildColors = new HashMap();
        buildColors.put("cabin", new Color(77, 72, 59));
        buildColors.put("apartment", new Color(80, 81, 75));
        buildColors.put("house", new Color(90, 92, 91));
        buildColors.put("field", new Color(60, 83, 58));
        buildColors.put("army", new Color(82, 83, 50));
        buildColors.put("gas", new Color(89, 57, 56));
        buildColors.put("garage", new Color(51, 49, 51));
        buildColors.put("site", new Color(61, 71, 55));
        buildColors.put("trader", new Color(180, 151, 0));
        buildColors.put("other", new Color(69, 72, 72));


        while (xmlr.hasNext()) {
            eventType = xmlr.next();
            if (eventType == XMLEvent.START_ELEMENT) {
                if (xmlr.getAttributeCount() == 4) {
                    String attributeValue = xmlr.getAttributeValue(2);
                    String[] split = attributeValue.split(",");
                    int x = (mapSize / 2 + Integer.parseInt(split[0])) / downScale;
                    int y = (mapSize / 2 - Integer.parseInt(split[2])) / downScale;

                    int rot = Integer.parseInt(xmlr.getAttributeValue(3));

                    //int rgb = Color.RED.getRGB();
                    // iBiomes.setRGB(x, y, rgb);
                    int xShift = x + i15;
                    int yShift = y - i50;


                    if (xmlr.getAttributeValue(1).startsWith("cave")) {
                        g.setColor(new Color(180, 151, 0));
                        g.fillOval(xShift, yShift, i60, i50);
                        g.setColor(Color.DARK_GRAY);
                        g.fillOval(xShift + i2, yShift + i2, i40, i40);
                    } else if (xmlr.getAttributeValue(1).startsWith("water")) {
                        g.setColor(Color.DARK_GRAY);
                        g.fillOval(x, yShift + i10, i40, i40);
                        g.setColor(new Color(22, 116, 168));
                        g.fillOval(x + i5, yShift + i15, i30, i30);
                    } else if (xmlr.getAttributeValue(1).contains("house")) {
                        g.setColor(buildColors.get("house"));
                        if (rot == 0 || rot == 2)
                            g.fill3DRect(x, yShift + i10, i40, i35, true);
                        else
                            g.fill3DRect(x, yShift + i10, i35, i40, true);
                    } else if (xmlr.getAttributeValue(1).contains("army")) {
                        g.setColor(buildColors.get("army"));
                        g.fill3DRect(xShift, yShift + i10, i30, i30, true);
                    } else if (xmlr.getAttributeValue(1).contains("cabin")){
                        g.setColor(buildColors.get("cabin"));
                        g.fill3DRect(xShift, yShift + i10, i30, i30, true);
                    } else if (xmlr.getAttributeValue(1).contains("garage")){
                        g.setColor(buildColors.get("garage"));
                        g.fill3DRect(x + i5, y - i30, i20, i20, true);
                    } else if (xmlr.getAttributeValue(1).contains("parking")) {
                        g.setColor(buildColors.get("garage"));
                        g.fill3DRect(x + i5, yShift + i10, i40, i40, true);
                    } else if (xmlr.getAttributeValue(1).contains("apartment")){
                        g.setColor(buildColors.get("apartment"));
                        if (rot == 0 || rot == 2)
                            g.fill3DRect(x + i5, yShift + i10, i40, i30, true);
                        else
                            g.fill3DRect(x + i5, yShift + i10, i30, i40, true);
                    } else if (xmlr.getAttributeValue(1).contains("gas")){
                        g.setColor(buildColors.get("gas"));
                        if (rot == 0 || rot == 2)
                            g.fill3DRect(xShift, yShift + i10, i30, i25, true);
                        else
                            g.fill3DRect(xShift, yShift + i10, i25, i30, true);
                    } else if (xmlr.getAttributeValue(1).contains("field")){
                        g.setColor(buildColors.get("field"));
                        g.fillRect(xShift, yShift + i10, i20, i20);
                    } else if (xmlr.getAttributeValue(1).contains("site")){
                        g.setColor(buildColors.get("site"));
                        g.fillOval(xShift, yShift + i10, i40, i40);
                    } else if (xmlr.getAttributeValue(1).contains("trader")){
                        g.setColor(Color.DARK_GRAY);
                        g.fillOval(xShift, yShift, i50, i50);
                        g.setColor(buildColors.get("trader"));
                        g.fillOval(xShift + i5, yShift + i5, i40, i40);
                    }else {
                        g.setColor(buildColors.get("other"));
                        if (rot == 0 || rot == 2)
                            g.fill3DRect(x, yShift + i10, i30, i25, true);
                        else
                            g.fill3DRect(x, yShift + i10, i25, i30, true);
                    }
                }
            }
        }

        File mapWithObjects = new File(path + "\\"+ fileNum+++"_mapWithObjects.png");
        ImageIO.write(iBiomes, "PNG", mapWithObjects);
    }

    private void drawRoads() throws IOException {
        BufferedImage roads = ImageIO.read(new File(path + "\\splat3.png"));
        System.out.println("Roads loaded");
        Color roadColor;

        for(int xi = roads.getMinX(); xi < roads.getWidth() ; xi ++) {
            for(int yi = roads.getMinY(); yi < roads.getHeight() ; yi ++) {
                int p = roads.getRGB(xi, yi);
                if(p!=0) {
                    if (p==65280)
                        roadColor = new Color(141, 129, 106);
                    else
                        roadColor = new Color(52, 59, 65);

                    iBiomes.setRGB(xi/downScale, yi/downScale, roadColor.getRGB());
                }
            }
        }

        File map_with_roads = new File(path + "\\"+ fileNum+++"_map_with_roads.png");
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

        //fix Original RGB
        Map<Integer, Color> mapColor = new HashMap<>();
        mapColor.put(-16760832, new Color(55, 95, 68));
        mapColor.put(-1, new Color(203, 197, 194));
        mapColor.put(-7049, new Color(124, 116, 94));
        mapColor.put(-22528, new Color(175, 154, 107));
        mapColor.put(-4587265, new Color(68, 70, 67));

        MapBiomeColor:
        for (int x = 0; x < scaledSize; x++) {
            for (int y = 0; y < scaledSize; y++) {
                int rgb = iBiomes.getRGB(x, y);
                if (mapColor.containsKey(rgb))
                    iBiomes.setRGB(x, y, mapColor.get(rgb).getRGB());
                else {
                    System.err.println("Unknown biome color: " + rgb);
                    break MapBiomeColor;
                }
            }
        }

        if(doBlureBiomes) {
            BufferedImage iBiomesBlured = new BufferedImage(scaledSize, scaledSize, inputImage.getType());
            new BoxBlurFilter(scaledSize / bloorK, scaledSize / bloorK, 1).filter(iBiomes, iBiomesBlured);
            iBiomes.flush();
            iBiomes = iBiomesBlured;
        }

        //Draw lakes
        WritableRaster iHeigthsRaster = iHeigths.getRaster();
        for (int x = 0; x < scaledSize; x++) {
            for (int y = 0; y < scaledSize; y++) {
                if(iHeigthsRaster.getSample(x, y, 0)<waterLine
                            && iWaterZones.getRaster().getSample(x, y, 0) > 0) {
                    iBiomes.setRGB(x, y, new Color(49, 87, 145).getRGB());
                }
            }
        }

        start = System.nanoTime();
        //write heights image to file
        File bump = new File(path + "\\"+ fileNum+++"_bump.png");
        ImageIO.write(iHeigths, "PNG", bump);
        //write scaled biomes to file
        File biomes = new File(path + "\\"+ fileNum+++"_biomes.png");
        ImageIO.write(iBiomes, "PNG", biomes);
        end = System.nanoTime();
        System.out.println("File saving time:  = " + (end-start)/1000000000 + "s");

        // normal vectors array
        float[][] normalVectors = new float[scaledSize * scaledSize][3];
        // precalculate normal vectors
        BumpMappingUtils.FindNormalVectors(iHeigths, normalVectors);
        //free mem
        iHeigths.flush();
        //apply bump-mapping using normal vectors
        BumpMappingUtils.paint(iBiomes, scaledSize, scaledSize, normalVectors);

        //Write bump-mapped biomes
        File biomesShadow = new File(path + "\\"+ fileNum+++"_biomesShadow.png");
        ImageIO.write(iBiomes, "PNG", biomesShadow);
    }

    private void autoAjustImage() throws IOException {
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
        int intrms = Math.toIntExact(rms);

        System.out.println("mean = " + Math.round(mean));
        System.out.println("rms = " + rms);
        System.out.println("min = " + min);
        System.out.println("max = " + max);

        StringBuilder sb = new StringBuilder();
        float D = 0;
        for (int i = 0; i < hist.length; i++) {
            sb.append(i*256+"\t").append(hist[i]).append('\n');
            long a = i * 256 - rms;
            double tmp = Math.pow(a, 2);
            tmp/=tcount;
            tmp*=hist[i];
            D += tmp;
        }

        Files.write(Paths.get(path+"\\heigthsHistogram.txt"), Collections.singleton(sb));

        D = Math.round(Math.sqrt(D));
        System.out.println("D2 = " + D);

        int startHist = Math.round(intrms - gamma*D);
        System.out.println("startHist = " + startHist);
        float k = 256*256/(max - min);
        System.out.println("k = " + k);

        waterLine = intrms - Math.round(1.7f*D);
        System.out.println("waterLine = " + waterLine);
        if(applyGammaCorrection) {
            waterLine = Math.round((waterLine - min) * k);
            System.out.println("after gamma waterLine = " + waterLine);
            for (int x = raster.getMinX(); x < raster.getMinX()+raster.getWidth(); x++) {
                for (int y = raster.getMinY(); y < raster.getMinY()+raster.getHeight(); y++) {
                    int grayColor = raster.getSample(x, y, 0);
                    int imageColor = Math.round((grayColor - min) * k);
                    raster.setSample(x, y, 0, imageColor);
                }
            }
        }
    }

    public void readWorldHeights() throws IOException {
        String dtmFileName = path + "\\dtm.raw";
        File heightsFile = new File(dtmFileName);
        if(!heightsFile.exists() || !heightsFile.isFile() || !heightsFile.canRead()) {
            System.err.println("File not found: " + dtmFileName);
            System.exit(1);
        }
        long fileLength = heightsFile.length();
        System.out.println("fileLength = " + fileLength);
        mapSize = (int) Math.round(Math.sqrt(fileLength /2.));
        System.out.println("Detected mapSize: " + mapSize);
        scaledSize = mapSize / downScale;
        System.out.println("Resulting image side size will be: " + scaledSize + "px");
        //TODO rename to totalScaledPixels
        totalPixels = scaledSize;
        totalPixels *= totalPixels;
        //Result processed heights image
        iHeigths = new BufferedImage(scaledSize, scaledSize, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster raster = iHeigths.getRaster();

        try (FileInputStream hmis = new FileInputStream(heightsFile)) {
            byte buf[] = new byte[mapSize * 4];

            int readedBytes;
            int curPixelNum = 0;
            System.out.print("File load:\n|----------------|\n|");
            while ((readedBytes = hmis.read(buf)) > -1) {
                //TODO here potential problem if readedBytes%2 != 0
                //convert every 2 bytes to new gray pixel
                for (int i = 0; i < readedBytes / 2; i++) {
                    //TODO use avg of pixel color with same coordinate in scaled image.
                    //calculate pixel position
                    int x = (curPixelNum % mapSize) / downScale;
                    int y = (mapSize - 1 - curPixelNum / mapSize) / downScale;
                    //write pixel to resulting image
                    int grayColor = (buf[i*2+1]<<8)|(((int)buf[i*2])&0xff);
                    raster.setSample(x, y, 0, grayColor);
                    curPixelNum++;
                    //Draw progress bar
                    if (curPixelNum % (mapSize * 512) == 0) {
                        System.out.print("-");
                    }
                }
            }
            System.out.println("|\nDone.");
        }
    }
}
