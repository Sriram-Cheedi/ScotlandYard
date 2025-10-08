package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.GameSetup;

import java.util.*;
public class Dijkstra {

    private static final int MAX = 2147483647;

    public static Integer findDistance(Board board, int startNode, int targetNode) {
        GameSetup setup = board.getSetup();
        int totalNodes = setup.graph.nodes().size();
        List<Integer> dist = new ArrayList<>(Arrays.asList(new Integer[totalNodes + 1]));
        for (int i = 0; i < dist.size(); i++) {
            dist.set(i, MAX);
        }
        dist.set(startNode, 0);


        while (true) {
            int currNode = -1;
            int smallestDist = MAX;
            for (int node : setup.graph.nodes()) {
                if (dist.get(node) < smallestDist) {
                    smallestDist = dist.get(node);
                    currNode = node;
                }
            }
            if (currNode == -1 || currNode == targetNode) {
                break;
            }

            for (int neighbourNode : setup.graph.adjacentNodes(currNode)) {
                int alt = dist.get(currNode) + 1;
                if (alt < dist.get(neighbourNode)) {
                    dist.set(neighbourNode, alt);
                }
            }
            dist.set(currNode, MAX);
        }

        if (dist.get(targetNode) == MAX) {
            return null;
        } else {
            return dist.get(targetNode);
        }

    }
}

