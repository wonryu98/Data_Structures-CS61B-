import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a <code>shortestPath</code> method and <code>routeDirections</code> for
 * finding routes between two points on the map.
 */
public class Router {
    /**
     * Return a <code>List</code> of vertex IDs corresponding to the shortest path from a given
     * starting coordinate and destination coordinate.
     *
     * @param g       <code>GraphDB</code> data source.
     * @param stlon   The longitude of the starting coordinate.
     * @param stlat   The latitude of the starting coordinate.
     * @param destlon The longitude of the destination coordinate.
     * @param destlat The latitude of the destination coordinate.
     * @return The <code>List</code> of vertex IDs corresponding to the shortest path.
     */
    public static List<Long> shortestPath(GraphDB g,
                                          double stlon, double stlat,
                                          double destlon, double destlat) {
        // ODO

        //find the closest nodes for st and dest (kdtree)
        //do A* search from st node to dest node
        //getting our nodes
        long stID = g.closest(stlon, stlat);
        long destID = g.closest(destlon, destlat);
        //GraphDB.Node stNode = g.getNodeMap().get(stID);
        //GraphDB.Node destNode = g.getNodeMap().get(destID);

        HashMap<Long, Double> distanceFromStart = new HashMap<>();
        //heuristic doesn't change after this
        HashMap<Long, Double> heuristicDisttoDest = new HashMap<>();
        for (long id : g.getNodeMap().keySet()) {
            heuristicDisttoDest.put(id, g.distance(id, destID));
        }
        LinkedList<Long> toRtn = new LinkedList<>();
        //fringe with access to comparator
        PriorityQueue<Long> fringe = new PriorityQueue<>(g.getNodeMap().size(),
            (o1, o2) -> Double.compare((distanceFromStart.get(o1) + heuristicDisttoDest.get(o1)),
                    (distanceFromStart.get(o2) + heuristicDisttoDest.get(o2))));
        //for our completed
        HashSet<Long> visited = new HashSet();
        //initialize
        for (long id : g.getNodeMap().keySet()) {
            distanceFromStart.put(id, Double.MAX_VALUE);

        }
        fringe.add(stID);
        distanceFromStart.put(stID, 0.000);
        HashMap<Long, Long> predecessorVertex = new HashMap<>();

        while (!fringe.isEmpty()) {

            long currVertex = fringe.poll();

            if (visited.contains(currVertex)) {
                continue;
            }
            if (currVertex == destID) {
                break;
            } else {
                visited.add(currVertex);
            }
            List<Long> vsNeighbors = (List) g.adjacent(currVertex);

            for (long neighbor : vsNeighbors) {
                if (visited.contains(neighbor)) {
                    continue;
                }
                if (!fringe.contains(neighbor)) {
                    //fringe and distance are always interconnected
                    distanceFromStart.put(neighbor,
                            distanceFromStart.get(currVertex) + g.distance(currVertex, neighbor));
                    predecessorVertex.put(neighbor, currVertex);
                    //add after update bc the comparisons you make are based on updated distances
                    fringe.add(neighbor);
                } else if (distanceFromStart.get(neighbor)
                        >
                        distanceFromStart.get(currVertex) + g.distance(currVertex, neighbor)) {
                    distanceFromStart.put(neighbor,
                            distanceFromStart.get(currVertex) + g.distance(currVertex, neighbor));
                    predecessorVertex.put(neighbor, currVertex);
                    fringe.add(neighbor);
                }
            }
        }
        long index = destID;
        while (index != stID) {
            toRtn.addFirst(index);
            if (predecessorVertex == null) {
                toRtn.addFirst(stID);
                return toRtn;
            }
            index = predecessorVertex.get(index);
        }
        toRtn.addFirst(stID);
        return toRtn;
    }

    /**
     * Given a <code>route</code> of vertex IDs, return a <code>List</code> of
     * <code>NavigationDirection</code> objects representing the travel directions in order.
     *
     * @param g     <code>GraphDB</code> data source.
     * @param route The shortest-path route of vertex IDs.
     * @return A new <code>List</code> of <code>NavigationDirection</code> objects.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {
        // TODO
        return Collections.emptyList();
    }

    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /**
         * Integer constants representing directions.
         */
        public static final int START = 0, STRAIGHT = 1, SLIGHT_LEFT = 2, SLIGHT_RIGHT = 3,
                RIGHT = 4, LEFT = 5, SHARP_LEFT = 6, SHARP_RIGHT = 7;

        /**
         * Number of directions supported.
         */
        public static final int NUM_DIRECTIONS = 8;

        /**
         * A mapping of integer values to directions.
         */
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /**
         * The direction represented.
         */
        int direction;
        /**
         * The name of this way.
         */
        String way;
        /**
         * The distance along this way.
         */
        double distance = 0.0;

        /**
         * Returns a new <code>NavigationDirection</code> from a string representation.
         *
         * @param dirAsString <code>String</code> instructions for a navigation direction.
         * @return A new <code>NavigationDirection</code> based on the string, or <code>null</code>
         * if unable to parse.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // Not a valid nd
                return null;
            }
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                        && way.equals(((NavigationDirection) o).way)
                        && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }
}

//    public class kdTree {
//
//        public double euclidean(double x1, double x2, double y1, double y2) {
//            return Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
//        }
//
//        kdTreeNode root;
//
//        public kdTree(long id) {
//            root = new kdTreeNode(true, id, true);
//        }
//
//        public kdTree() {
//        }
//
//        public class kdTreeNode {
//            long id;
//            kdTreeNode left;
//            kdTreeNode right;
//            boolean vertical;
//            boolean isBlack;
//
//            public kdTreeNode(boolean isBlack, long id, kdTreeNode left,
//                              kdTreeNode right, boolean vertical) {
//                this.isBlack = isBlack;
//                this.id = id;
//                this.left = left;
//                this.right = right;
//                this.vertical = vertical;
//            }
//
//            public kdTreeNode(boolean isBlack, long id, boolean vertical) {
//                this(isBlack, id, null, null, vertical);
//            }
//
//
//        }
//
//        void flipColors(kdTreeNode node) {
//            node.isBlack = !node.isBlack;
//            node.left.isBlack = !node.left.isBlack;
//            node.right.isBlack = !node.right.isBlack;
//        }
//
//        /* Rotates the given node NODE to the right. Returns the new root node of
//           this subtree. */
//        kdTreeNode rotateRight(kdTreeNode node) {
//            // : YOUR CODE HERE
//
//            if (node == null) {
//                return null;
//            } else if (node.left == null) {
//                return node;
//            } else {
//                boolean backupcolor = node.isBlack;
//                kdTreeNode backup = node.left.right;
//                node.left.right = node;
//                node = node.left;
//                node.isBlack = backupcolor;
//                node.right.isBlack = false;
//                node.right.left = backup;
//
//
//                return node;
//            }
//
//
//        }
//
//        /* Rotates the given node NODE to the left. Returns the new root node of
//           this subtree. */
//        kdTreeNode rotateLeft(kdTreeNode node) {
//            // : YOUR CODE HERE
//
//            if (node == null) {
//                return null;
//            } else if (node.right == null) {
//                return node;
//            } else {
//                boolean backupcolor = node.isBlack;
//                kdTreeNode backup = node.right.left;
//                node.right.left = node;
//                node = node.right;
//                node.isBlack = backupcolor;
//                node.left.isBlack = false;
//                node.left.right = backup;
//
//                return node;
//            }
//
//        }
//
//        /* Insert ITEM into the red black tree, rotating
//           it accordingly afterwards. */
//        void insert(long id) {
//            // : YOUR CODE HERE
//
//            if (root == null) {
//                root = new kdTreeNode(true, id, true);
//            } else {
//                root = insertHelper(id, root);
//            }
//            root.isBlack = true;
//
//
//        }
//
//        kdTreeNode insertHelper(long id, kdTreeNode parent) {
//            // : YOUR CODE HERE
//
//            if (parent == null) {
//                return null;
//            } else {
//                double idCompare;
//                double parentCompare;
//
//                if (parent.vertical) {
//                    idCompare = nodeMap.get(id).lon;
//                    parentCompare = nodeMap.get(parent.id).lon;
//
//                } else {
//                    idCompare = nodeMap.get(id).lat;
//                    parentCompare = nodeMap.get(parent.id).lat;
//
//                }
//
//                if (Double.compare(idCompare, parentCompare) > 0) {
//                    if (parent.right == null) {
//                        parent.right = new kdTreeNode(true, id, !parent.vertical);
//
//                    } else if (parent.right != null) {
//                        parent.right = insertHelper(id, parent.right);
//                    }
//
//                } else if (Double.compare(idCompare, parentCompare) < 0) {
//                    if (parent.left == null) {
//                        parent.left = new kdTreeNode(true, id, !parent.vertical);
//
//                    } else if (parent.left != null) {
//                        parent.left = insertHelper(id, parent.left);
//                    }
//
//                }
//            }
//
//            //restructure
//            //isBlack, isRed
//            //parent is root
//
//            if (!isRed(parent) && (parent.left == null && parent.right != null)) {
//                parent = rotateLeft(parent);
//            } else if (!isRed(parent) && parent.right == null && parent.left != null) {
//                parent = parent;
//            }
//
//            if (isRed(parent.right) && !isRed(parent.left)) {
//                parent = rotateLeft(parent);
//            }
//            if (isRed(parent.left) && isRed(parent.left.left)) {
//                parent = rotateRight(parent);
//            }
//
//            if (isRed(parent.left) && isRed(parent.right)) {
//                flipColors(parent);
//            }
//
//            return parent;
//        }
//
//        /* Returns whether the given node NODE is red. Null nodes (children of leaf
//           nodes are automatically considered black. */
//        private boolean isRed(kdTreeNode node) {
//            return node != null && !node.isBlack;
//        }
//
//
//        public void build() {
//            for (long id : nodeMap.keySet()) {
//                insert(id);
//            }
//        }
//
//
//    }
