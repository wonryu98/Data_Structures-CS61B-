//import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    /**
     * The max image depth level.
     */
    public static final int MAX_DEPTH = 7;

    /**
     * Takes a user query and finds the grid of images that best matches the query. These images
     * will be combined into one big image (rastered) by the front end. The grid of images must obey
     * the following properties, where image in the grid is referred to as a "tile".
     * <ul>
     * <li>The tiles collected must cover the most longitudinal distance per pixel (LonDPP)
     * possible, while still covering less than or equal to the amount of longitudinal distance
     * per pixel in the query box for the user viewport size.</li>
     * <li>Contains all tiles that intersect the query bounding box that fulfill the above
     * condition.</li>
     * <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     * </ul>
     *
     * params The RasterRequestParams containing coordinates of the query box and the browser
     *                 viewport width and height.
     * @return A valid RasterResultParams containing the computed results.
     */

    private boolean failCasesCheck(double ullat, double ullon, double lrlat, double lrlon) {
        return ((ullon < MapServer.ROOT_ULLON && ullat > MapServer.ROOT_ULLAT
                && MapServer.ROOT_LRLON < lrlon && MapServer.ROOT_LRLAT > lrlat)
                ||

                (ullat < lrlat || ullon > lrlon));

    }

    public RasterResultParams getMapRaster(RasterRequestParams params) {
        double ullat = params.ullat;
        double ullon = params.ullon;
        double lrlat = params.lrlat;
        double lrlon = params.lrlon;
        double w = params.w;
        if (failCasesCheck(ullat, ullon, lrlat, lrlon)) {
            return RasterResultParams.queryFailed();
        }
        if (ullon < MapServer.ROOT_ULLON) {
            ullon = MapServer.ROOT_ULLON;
        }
        if (lrlon > MapServer.ROOT_LRLON) {
            lrlon = MapServer.ROOT_LRLON;
        }
        if (ullat > MapServer.ROOT_ULLAT) {
            ullat = MapServer.ROOT_ULLAT;
        }
        if (lrlat < MapServer.ROOT_LRLAT) {
            lrlat = MapServer.ROOT_LRLAT;
        }
        RasterResultParams.Builder builder = new RasterResultParams.Builder();
        //for depth
        double lonDDP = lonDPP(lrlon, ullon, w);
        double ftOverPixelRatio = MapServer.ROOT_LONDPP;
        int depth = 0;
        for (int i = 0; i <= 7; i += 1) {
            if (lonDDP >= ftOverPixelRatio) {
                depth = i;
                break;
            } else {
                depth = 7;
            }
            ftOverPixelRatio /= 2;
        }
        double eachTileLengthHori = MapServer.ROOT_LON_DELTA / (Math.pow(2, depth));
        double eachTileLengthVert = MapServer.ROOT_LAT_DELTA / (Math.pow(2, depth));

        int firstXIndex = (int) Math.floor(Math.abs((ullon - MapServer.ROOT_ULLON))
                / (eachTileLengthHori));
        int firstYIndex = (int) Math.floor(Math.abs((MapServer.ROOT_ULLAT - ullat))
                / (eachTileLengthVert));

        int lastXIndex = (int) Math.ceil(Math.abs((lrlon - MapServer.ROOT_ULLON))
                / (eachTileLengthHori));
        int lastYIndex = (int) Math.ceil(Math.abs((MapServer.ROOT_ULLAT - lrlat))
                / (eachTileLengthVert));

        int numberOfTilesHoriInRaster = lastXIndex - firstXIndex;
        int numberOfTilesVertInRaster = lastYIndex - firstYIndex;

        if (MapServer.ROOT_ULLON == ullon) {
            firstXIndex = 0;
        }
        if (MapServer.ROOT_ULLAT == ullat) {
            firstYIndex = 0;
        }
        String[][] myRenderGrid = new String[numberOfTilesVertInRaster][numberOfTilesHoriInRaster];
        for (int y = 0; y < numberOfTilesVertInRaster; y += 1) {
            for (int x = 0; x < numberOfTilesHoriInRaster; x += 1) {
                myRenderGrid[y][x] = fileNameGenerator(depth, firstXIndex + x, firstYIndex + y);
            }
        }


        double myULlon = MapServer.ROOT_ULLON + eachTileLengthHori * firstXIndex;
        double myULlat = MapServer.ROOT_ULLAT - eachTileLengthVert * firstYIndex;

        double myLRlon = MapServer.ROOT_ULLON + eachTileLengthHori
                *
                (firstXIndex + numberOfTilesHoriInRaster);
        double myLRlat = MapServer.ROOT_ULLAT - eachTileLengthVert
                *
                (firstYIndex + numberOfTilesVertInRaster);


        return myBuilder(myULlat, myULlon, myLRlon, myLRlat, myRenderGrid, depth, builder);
    }

    private RasterResultParams myBuilder(double myULlat, double myULlon,
                                         double myLRlon, double myLRlat,
                                         String[][] myRenderGrid, int depth,
                                         RasterResultParams.Builder builder) {

        builder.setRasterUlLon(myULlon);
        builder.setRasterUlLat(myULlat);
        builder.setRasterLrLon(myLRlon);
        builder.setRasterLrLat(myLRlat);
        builder.setRenderGrid(myRenderGrid);
        builder.setDepth(depth);
        builder.setQuerySuccess(true);
        return builder.create();

    }

    private String fileNameGenerator(int depth, int x, int y) {
        return "d" + Integer.toString(depth) + "_x"
                +
                Integer.toString(x) + "_y" + Integer.toString(y) + ".png";
    }

    /**
     * Calculates the lonDPP of an image or query box
     *
     * @param lrlon Lower right longitudinal value of the image or query box
     * @param ullon Upper left longitudinal value of the image or query box
     * @param width Width of the query box or image
     * @return lonDPP
     */
    private double lonDPP(double lrlon, double ullon, double width) {
        return (lrlon - ullon) / width;
    }
}
