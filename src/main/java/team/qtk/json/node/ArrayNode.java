package team.qtk.json.node;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.stream.Stream;
import lombok.Getter;
import team.qtk.json.JSON;

public class ArrayNode extends Node {

    @Getter
    private ArrayList<Node> nodes = new ArrayList<>();

    public static ArrayNode create(String path, JSON jsonHelper) {
        ArrayNode arr = new ArrayNode();
        arr.path = path;
        arr.jacksonNode = jsonHelper.jackson.createArrayNode();
        return arr;
    }

    public static ArrayNode from(JsonNode jacksonNode, String path) {
        ArrayNode arr = new ArrayNode();
        arr.path = path;
        arr.jacksonNode = jacksonNode;

        jacksonNode
            .elements()
            .forEachRemaining(
                node -> arr.nodes.add(Node.gen(node, path + "[" + arr.nodes.size() + "]"))
            );
        return arr;
    }

    public Node get(int index) {
        return this.nodes.get(index);
    }

    public ArrayNode add(JsonNode jacksonNode) {
        Node node = Node
            .builder()
            .jacksonNode(jacksonNode)
            .path(this.path + "[" + nodes.size() + "]")
            .build();
        this.nodes.add(node);
        ((com.fasterxml.jackson.databind.node.ArrayNode) this.jacksonNode).add(jacksonNode);
        return this;
    }

    public ArrayNode add(Node node) {
        this.nodes.add(node);
        ((com.fasterxml.jackson.databind.node.ArrayNode) this.jacksonNode).add(node.jacksonNode);
        return this;
    }

    public ArrayNode addAll(ArrayNode nodes) {
        this.nodes.addAll(nodes.getNodes());
        ((com.fasterxml.jackson.databind.node.ArrayNode) this.jacksonNode).addAll(
                (com.fasterxml.jackson.databind.node.ArrayNode) nodes.jacksonNode
            );
        return this;
    }

    public void remove(int index) {
        this.nodes.remove(index);
        ((com.fasterxml.jackson.databind.node.ArrayNode) this.jacksonNode).remove(index);
    }

    public Stream<Node> stream() {
        return this.nodes.stream();
    }
}
