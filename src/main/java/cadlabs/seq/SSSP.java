package cadlabs.seq;


import cadlabs.graph.GraphBuilder;
import cadlabs.rdd.AbstractFlightAnalyser;
import cadlabs.rdd.Flight;
import cadlabs.rdd.Path;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.linalg.distributed.MatrixEntry;
import scala.Tuple2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A sequential implementation of Dijkstra Single Source Shortest Path
 */
public class SSSP extends AbstractFlightAnalyser<Path> {

    /**
     * Representation of absence of edge between two nodes
     */
    private static final double NOEDGE = Double.MAX_VALUE;

    /**
     * Representation of absence of predecessor for a given node in the current path
     */
    private static final int NOPREDECESSOR = -1;

    /**
     * The graph
     */
    private final List<MatrixEntry> graph;

    /**
     * The name of the source node (airport)
     */
    private final String sourceName;

    /**
     * The name of the destination node (airport)
     */
    private final String destinationName;


    public SSSP(String source, String destination, JavaRDD<Flight> flights, GraphBuilder graphBuilder) {
        super(flights);
        this.sourceName = source;
        this.destinationName = destination;
        this.graph = graphBuilder.getMaterializedGraph();
    }

    @Override
    public Path run() {
        // identifiers of the source and destination nodes
        int source = Flight.getAirportIdFromName(sourceName);
        int destination = Flight.getAirportIdFromName(destinationName);
        int nAirports = (int) Flight.getNumberAirports();

        // The set of nodes to visit
        List<Integer> toVisit = IntStream.range(0, nAirports).boxed().collect(Collectors.toList());
        toVisit.remove(source);

        // the l vector and a vector to store a node's predecessor in the current path
        double[] l = new double[nAirports];
        int[] predecessor = new int[nAirports];

        for (int i = 0; i < l.length; i++) {
            l[i] = NOEDGE;
            predecessor[i] = NOPREDECESSOR;
        }

        l[source] = 0;
        for (Integer v : toVisit) {
            MatrixEntry e = getEdge(source, v);
            if (e != null) {
                l[(int) e.j()] = e.value();
                predecessor[(int) e.j()] = source;
            }
        }

        // Dijkstra's algorithm
        while (toVisit.size() > 0) {

            int u = toVisit.get(0);
            for (Integer v : toVisit)
                if (l[v] < l[u]) u = v;

            toVisit.remove((Integer) u);

            //                System.out.println("Going through " + u);

            for (Integer v : toVisit) {
                double newPath = l[u] + getWeight(u, v);
                if (l[v] > newPath) {
                    l[v] = newPath;
                    predecessor[v] = u;
                }
            }
        }

        return new Path(source, destination, predecessor, l);
    }


    /**
     * Obtain an edge from between origin and dest, if it exists.
     *
     * @return The edge (of type MatrixEntry), if it exists, null otherwise
     */
    private MatrixEntry getEdge(int origin, int dest) {
        for (MatrixEntry e : this.graph)
            if (e.i() == origin && e.j() == dest)
                return e;
        return null;
    }

    /**
     * Obtain the weight of an edge from between origin and dest, if it exists.
     *
     * @return The weight of the edge, if the edge exists, NOEDGE otherwise
     */
    private double getWeight(int origin, int dest) {
        for (MatrixEntry e : this.graph) {
            if (e.i() == origin && e.j() == dest)
                return e.value();
        }
        return NOEDGE;
    }
}
