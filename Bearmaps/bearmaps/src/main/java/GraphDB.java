import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Kevin Lowe, Antares Chen, Kevin Lin
 */
public class GraphDB {
    /**
     * Radius of the Earth in miles.
     */
    private static final int R = 3963;
    /**
     * Latitude centered on Berkeley.
     */
    private static final double ROOT_LAT = (MapServer.ROOT_ULLAT + MapServer.ROOT_LRLAT) / 2;
    /**
     * Longitude centered on Berkeley.
     */
    private static final double ROOT_LON = (MapServer.ROOT_ULLON + MapServer.ROOT_LRLON) / 2;
    /**
     * Scale factor at the natural origin, Berkeley. Prefer to use 1 instead of 0.9996 as in UTM.
     *
     * @source https://gis.stackexchange.com/a/7298
     */
    private static final double K0 = 1.0;
    KDtree myKDtree;
    /**
     * This constructor creates and starts an XML parser, cleans the nodes, and prepares the
     * data structures for processing. Modify this constructor to initialize your data structures.
     *
     * @param dbPath Path to the XML file to be parsed.
     */

    //made
    private HashMap<Long, Node> nodeMap;
    private HashMap<Long, Way> wayMap;
    private HashMap<String, Node> locationMap;
    private HashSet<Long> deleteThese;

    public GraphDB(String dbPath) {
        nodeMap = new HashMap<>();
        wayMap = new HashMap<>();
        locationMap = new HashMap<>();
        deleteThese = new HashSet<>();

        File inputFile = new File(dbPath);
        try (FileInputStream inputStream = new FileInputStream(inputFile)) {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            saxParser.parse(inputStream, new GraphBuildingHandler(this));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
        myKDtree = new KDtree();


    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     *
     * @param s Input string.
     * @return Cleaned string.
     */
    private static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     * Return the Euclidean x-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     *
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean x-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToX(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double b = Math.sin(dlon) * Math.cos(phi);
        return (K0 / 2) * Math.log((1 + b) / (1 - b));
    }

    /**
     * Return the Euclidean y-value for some point, p, in Berkeley. Found by computing the
     * Transverse Mercator projection centered at Berkeley.
     *
     * @param lon The longitude for p.
     * @param lat The latitude for p.
     * @return The flattened, Euclidean y-value for p.
     * @source https://en.wikipedia.org/wiki/Transverse_Mercator_projection
     */
    static double projectToY(double lon, double lat) {
        double dlon = Math.toRadians(lon - ROOT_LON);
        double phi = Math.toRadians(lat);
        double con = Math.atan(Math.tan(phi) / Math.cos(dlon));
        return K0 * (con - Math.toRadians(ROOT_LAT));
    }

    public HashMap<Long, Node> getNodeMap() {
        return nodeMap;
    }

    public HashMap<String, Node> getLocationMap() {
        return locationMap;
    }

    public void addNode(Node vertex) {

        nodeMap.put(vertex.id, vertex);

    }

    public void addNodeEdge(Long id, List<Long> list) {
        wayMap.put(id, new Way(id, list));

        for (int i = 0; i < list.size() - 1; i += 1) {
            long var1 = list.get(i);
            long var2 = list.get(i + 1);
            nodeMap.get(var1).neighbors.add(var2);
            nodeMap.get(var2).neighbors.add(var1);
        }
    }

    /**
     * Remove nodes with no connections from the graph.
     * While this does not guarantee that any two nodes in the remaining graph are connected,
     * we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        for (long id : nodeMap.keySet()) {
            Iterable<Long> nei = adjacent(id);
            if (nei == null || ((ArrayList<Long>) nei).size() == 0) {
                deleteThese.add(id);
            }
        }

        for (long i : deleteThese) {
            nodeMap.remove(i);
        }
    }

    /**
     * Returns the longitude of vertex <code>v</code>.
     *
     * @param v The ID of a vertex in the graph.
     * @return The longitude of that vertex, or 0.0 if the vertex is not in the graph.
     */
    double lon(long v) {
        // TOD
        double x = nodeMap.get(v).lon;
        return x;
    }

    /**
     * Returns the latitude of vertex <code>v</code>.
     *
     * @param v The ID of a vertex in the graph.
     * @return The latitude of that vertex, or 0.0 if the vertex is not in the graph.
     */
    double lat(long v) {
        // TOD
        double x = nodeMap.get(v).lat;
        return x;
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     *
     * @return An iterable of all vertex IDs in the graph.
     */
    Iterable<Long> vertices() {

        Iterable x = this.nodeMap.keySet();
        return x;
    }

    /**
     * Returns an iterable over the IDs of all vertices adjacent to <code>v</code>.
     *
     * @param v The ID for any vertex in the graph.
     * @return An iterable over the IDs of all vertices adjacent to <code>v</code>, or an empty
     * iterable if the vertex is not in the graph.
     */
    Iterable<Long> adjacent(long v) {
        // TOD
        Iterable x;
        x = this.nodeMap.get(v).neighbors;
        return x;
    }

    /**
     * Returns the great-circle distance between two vertices, v and w, in miles.
     * Assumes the lon/lat methods are implemented properly.
     *
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The great-circle distance between vertices and w.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    public double distance(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double dphi = Math.toRadians(lat(w) - lat(v));
        double dlambda = Math.toRadians(lon(w) - lon(v));

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Returns the ID of the vertex closest to the given longitude and latitude.
     *
     * @param lon The given longitude.
     * @param lat The given latitude.
     * @return The ID for the vertex closest to the <code>lon</code> and <code>lat</code>.
     */
    public long closest(double lon, double lat) {

        return myKDtree.closestHelper(lon, lat, myKDtree.root, myKDtree.root).id;

    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     *
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public List<String> getLocationsByPrefix(String prefix) {
        return Collections.emptyList();
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     *
     * @param locationName A full name of a location searched for.
     * @return A <code>List</code> of <code>LocationParams</code> whose cleaned name matches the
     * cleaned <code>locationName</code>
     */
    public List<LocationParams> getLocations(String locationName) {
        return Collections.emptyList();
    }

    /**
     * Returns the initial bearing between vertices <code>v</code> and <code>w</code> in degrees.
     * The initial bearing is the angle that, if followed in a straight line along a great-circle
     * arc from the starting point, would take you to the end point.
     * Assumes the lon/lat methods are implemented properly.
     *
     * @param v The ID for the first vertex.
     * @param w The ID for the second vertex.
     * @return The bearing between <code>v</code> and <code>w</code> in degrees.
     * @source https://www.movable-type.co.uk/scripts/latlong.html
     */
    double bearing(long v, long w) {
        double phi1 = Math.toRadians(lat(v));
        double phi2 = Math.toRadians(lat(w));
        double lambda1 = Math.toRadians(lon(v));
        double lambda2 = Math.toRadians(lon(w));

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    public static class Node {
        private static Comparator<Node> xComparator = new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                double x1 = projectToX(o1.lon, o1.lat);
                double x2 = projectToX(o2.lon, o2.lat);
                return Double.compare(x1, x2);
            }
        };
        private static Comparator<Node> yComparator = new Comparator<Node>() {
            @Override
            public int compare(Node o1, Node o2) {
                double y1 = projectToY(o1.lon, o1.lat);
                double y2 = projectToY(o2.lon, o2.lat);
                return Double.compare(y1, y2);
            }
        };
        long id;
        double lat;
        double lon;
        String name = "No Name";
        List<Long> neighbors;

        Node(long id, double lat, double lon) {
            this.id = id;
            this.lat = lat;
            this.lon = lon;
            this.neighbors = new ArrayList<>();
        }

        public void hisNameIs(String n) {
            this.name = n;
        }
    }


    public static class Way {
        long id;
        List<Long> nodes;

        Way(long id, List<Long> nodes) {
            this.id = id;
            this.nodes = nodes;
        }
    }


    public class KDtree {

        KdTreeNode root;

        public KDtree() {
            List<Node> verticesList = new ArrayList<>(nodeMap.values());
            root = build(verticesList, true);
        }

        public KdTreeNode build(List<Node> m, boolean vertical) {
            if (vertical) {
                Collections.sort(m, Node.xComparator);
            } else {
                Collections.sort(m, Node.yComparator);
            }

            if (m.isEmpty()) {
                return null;
            }
            if (m.size() == 1) {
                return new KdTreeNode(m.get(0).id, null, null, vertical);
            }

            int median = m.size() / 2;
            Node medianNode = m.get(median);

            KdTreeNode leftChild = build(m.subList(0, median), !vertical);
            KdTreeNode rightChild = build(m.subList(median + 1, m.size()), !vertical);

            return new KdTreeNode(medianNode.id, leftChild, rightChild, vertical);
        }

        double euclidean(double x1, double x2, double y1, double y2) {
            return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
        }

        //public class nearestNeighbor {
        //    public kdTreeNode winner = root;

        public KdTreeNode closestHelper(double lon, double lat,
                                        KdTreeNode node, KdTreeNode closestNode) {

            double projX = projectToX(lon, lat);
            double projY = projectToY(lon, lat);

            if (node == null) {
                return closestNode;
            }

            double pointsXorY;
            double rootsXorY;
            if (node.vertical) {
                pointsXorY = projX;
                rootsXorY = node.x;
            } else {
                pointsXorY = projY;
                rootsXorY = node.y;
            }

            double currentDistance = euclidean(closestNode.x, projX,
                    closestNode.y, projY);
            double distToRoot = euclidean(node.x, projX,
                    node.y, projY);
//currentDistance > euclidean(root.left.x, x, root.left.y, y
            //currentDistance > euclidean(root.left.x, x, root.left.y, y)
            boolean left = false;
//(root.left == null && root.right == null) &&
            if (currentDistance > distToRoot) {
                closestNode = node;

                //return closestNode;
            }

            if ((node.left == null && node.right == null)) {
                return closestNode;
            } else if (node.left == null || pointsXorY >= rootsXorY) {

                closestNode = closestHelper(lon, lat, node.right, closestNode);
                left = false;

            } else if (node.right == null || pointsXorY < rootsXorY) {

                closestNode = closestHelper(lon, lat, node.left, closestNode);
                left = true;
            }
            //we found the bottom

//(this.root.id == root.id) ||
            //currentDistance
            if (Math.abs(rootsXorY - pointsXorY)
                    >=
                    euclidean(closestNode.x, projX,
                            closestNode.y, projY)) {
                return closestNode;
            } else {
                if (left) {
                    // closestNode = root.right;
                    closestNode = closestHelper(lon, lat, node.right, closestNode);
                    //return closestNode;
                } else {
                    // closestNode = root.left;
                    closestNode = closestHelper(lon, lat, node.left, closestNode);
                    //return closestNode;
                }
                //closestNode = closestHelper(lon, lat, closestNode, closestNode);

            }


            return closestNode;

            //if (Math.abs(root.x - x) < euclidean(winner.x, x, winner.y, y)) {
            //    return closestHelper(lon, lat, root.left, winner);
            //} else if (Math.abs(root.y - y) < euclidean(winner.x, x, winner.y, y)) {
            //    return closestHelper(lon, lat, root.right, winner);
            //}

        }

        public class KdTreeNode {

            long id;
            KdTreeNode left;
            KdTreeNode right;
            double x;
            double y;
            boolean vertical;

            public KdTreeNode(long id, KdTreeNode left,
                              KdTreeNode right, boolean vertical) {
                this.id = id;
                this.left = left;
                this.right = right;
                this.y = projectToY(nodeMap.get(id).lon, nodeMap.get(id).lat);
                this.x = projectToX(nodeMap.get(id).lon, nodeMap.get(id).lat);
                this.vertical = vertical;
            }
        }

    }
}


