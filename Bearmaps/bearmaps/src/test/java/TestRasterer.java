import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TestRasterer {
    private static final double DOUBLE_THRESHOLD = 0.000000001;
    private static final String PARAMS_FILE = "raster_params.txt";
    private static final String RESULTS_FILE = "raster_results.txt";
    private static final int NUM_TESTS = 8;
    private static DecimalFormat df2 = new DecimalFormat(".#########");
    private static Rasterer rasterer;


    @Before
    public void setUp() throws Exception {
        rasterer = new Rasterer();
    }

    @Test
    public void myTest() {
        double lrlon = -122.24053369025242;
        double ullon = -122.24163047377972;
        double w = 892.0;
        double h = 875.0;
        double ullat = 37.87655856892288;
        double lrlat = 37.87548268822065;

        RasterRequestParams.Builder myBuilder = new RasterRequestParams.Builder();
        myBuilder.setH(h); myBuilder.setLrlat(lrlat); myBuilder.setLrlon(lrlon); myBuilder.setUllat(ullat);
        myBuilder.setUllon(ullon); myBuilder.setW(w);
        System.out.println(rasterer.getMapRaster(myBuilder.create()));
    }
    @Test
    public void myTest2() {
        double lrlon=-122.20908713544797;
        double ullon=-122.3027284165759;
        double w=305.0;
        double h=300.0;
        double ullat=37.88708748276975;
        double lrlat=37.848731523430196;
        RasterRequestParams.Builder myBuilder = new RasterRequestParams.Builder();
        myBuilder.setH(h); myBuilder.setLrlat(lrlat); myBuilder.setLrlon(lrlon); myBuilder.setUllat(ullat);
        myBuilder.setUllon(ullon); myBuilder.setW(w);
        RasterRequestParams input = myBuilder.create();
        System.out.println(rasterer.getMapRaster(input));

    }
    @Test
    public void myTest3() {
        double lrlon=-122.2104604264636;
        double ullon=-122.30410170759153;
        double  w=1091.0;
        double h=566.0;
        double ullat=37.870213571328854;
        double lrlat=37.8318576119893;
        RasterRequestParams.Builder myBuilder = new RasterRequestParams.Builder();
        myBuilder.setH(h); myBuilder.setLrlat(lrlat); myBuilder.setLrlon(lrlon); myBuilder.setUllat(ullat);
        myBuilder.setUllon(ullon); myBuilder.setW(w);
        RasterRequestParams input = myBuilder.create();
        System.out.println(rasterer.getMapRaster(input));

    }


    @Test
    public void testGetMapRaster() throws Exception {
        List<RasterRequestParams> testParams = paramsFromFile();
        List<RasterResultParams> expectedResults = resultsFromFile();

        for (int i = 0; i < NUM_TESTS; i++) {
            System.out.println(String.format("Running test: %d", i));
            RasterRequestParams params = testParams.get(i);
            RasterResultParams actual = rasterer.getMapRaster(params);
            RasterResultParams expected = expectedResults.get(i);
            assertResultParamsEquals(
                    "Your results did not match the expected results for input " + params + ".\n",
                    expected,
                    actual
            );
        }
    }

    private void assertResultParamsEquals(String err, RasterResultParams expected, RasterResultParams actual) {
        assertEquals(err, expected.rasterUlLon, actual.rasterUlLon, DOUBLE_THRESHOLD);
        assertEquals(err, expected.rasterUlLat, actual.rasterUlLat, DOUBLE_THRESHOLD);
        assertEquals(err, expected.rasterLrLon, actual.rasterLrLon, DOUBLE_THRESHOLD);
        assertEquals(err, expected.rasterLrLat, actual.rasterLrLat, DOUBLE_THRESHOLD);
        assertEquals(err, expected.depth, actual.depth);
        assertEquals(err, expected.querySuccess, actual.querySuccess);
        assertArrayEquals(err, expected.renderGrid, actual.renderGrid);
    }

    private List<RasterRequestParams> paramsFromFile() throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(PARAMS_FILE), Charset.defaultCharset());
        List<RasterRequestParams> testParams = new ArrayList<>();
        int lineIdx = 2; // ignore comment lines
        for (int i = 0; i < NUM_TESTS; i++) {
            RasterRequestParams params = new RasterRequestParams.Builder()
                    .setUllon(Double.parseDouble(lines.get(lineIdx)))
                    .setUllat(Double.parseDouble(lines.get(lineIdx + 1)))
                    .setLrlon(Double.parseDouble(lines.get(lineIdx + 2)))
                    .setLrlat(Double.parseDouble(lines.get(lineIdx + 3)))
                    .setW(Double.parseDouble(lines.get(lineIdx + 4)))
                    .setH(Double.parseDouble(lines.get(lineIdx + 5)))
                    .create();
            testParams.add(params);
            lineIdx += 6;
        }
        return testParams;
    }

    private List<RasterResultParams> resultsFromFile() throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(RESULTS_FILE), Charset.defaultCharset());
        List<RasterResultParams> expected = new ArrayList<>();
        int lineIdx = 4; // ignore comment lines
        for (int i = 0; i < NUM_TESTS; i++) {
            RasterResultParams.Builder results = new RasterResultParams.Builder()
                    .setRasterUlLon(Double.parseDouble(lines.get(lineIdx)))
                    .setRasterUlLat(Double.parseDouble(lines.get(lineIdx + 1)))
                    .setRasterLrLon(Double.parseDouble(lines.get(lineIdx + 2)))
                    .setRasterLrLat(Double.parseDouble(lines.get(lineIdx + 3)))
                    .setDepth(Integer.parseInt(lines.get(lineIdx + 4)))
                    .setQuerySuccess(Boolean.parseBoolean(lines.get(lineIdx + 5)));
            lineIdx += 6;
            String[] dimensions = lines.get(lineIdx).split(" ");
            int rows = Integer.parseInt(dimensions[0]);
            int cols = Integer.parseInt(dimensions[1]);
            lineIdx += 1;
            String[][] grid = new String[rows][cols];
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    grid[r][c] = lines.get(lineIdx);
                    lineIdx++;
                }
            }
            results.setRenderGrid(grid);
            expected.add(results.create());
        }
        return expected;
    }
}
