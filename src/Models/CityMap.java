package Models;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class CityMap {
    private Graph<String, DefaultWeightedEdge> cityGraph;

    public CityMap() {
        cityGraph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
    }

    public void addLocation(String location) {
        cityGraph.addVertex(location);
    }

    public void addRoad(String loc1, String loc2, double distance) {
        DefaultWeightedEdge edge = cityGraph.addEdge(loc1, loc2);
        cityGraph.setEdgeWeight(edge, distance);
    }

    public Graph<String, DefaultWeightedEdge> getGraph() {
        return cityGraph;
    }
}
