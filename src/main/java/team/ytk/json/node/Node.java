package team.ytk.json.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Node {

    protected JsonNode jacksonNode;

    protected String path;

    public static Node gen(JsonNode node, String path) {
        return node.isArray() ? ArrayNode.from(node, path) : new Node(node, path);
    }

    public boolean isArray() {
        return this.jacksonNode.isArray();
    }

    public boolean isMissingNode() {
        return this.jacksonNode.isMissingNode();
    }

    public boolean isNull() {
        return this.jacksonNode.isNull();
    }

    public boolean isObject() {
        return this.jacksonNode.isObject();
    }

    public Stream<Node> stream() {
        throw new RuntimeException("非数组禁止使用此方法");
    }

    public Node get(String fieldName) {
        return Node.gen(
            this.jacksonNode.get(fieldName),
            this.path.equals(".") ? this.path + fieldName : this.path + "." + fieldName
        );
    }

    public Node get(int index) {
        throw new RuntimeException("非数组禁止使用此方法");
    }

    public Node set(String fieldName, JsonNode node) {
        ((ObjectNode) this.jacksonNode).set(fieldName, node);
        return this;
    }

    public boolean has(String fieldName) {
        return this.jacksonNode.has(fieldName);
    }

    public boolean has(int index) {
        return this.jacksonNode.has(index);
    }

    public void remove(String fieldName) {
        if (this.jacksonNode.isMissingNode()) return;
        ((ObjectNode) this.jacksonNode).remove(fieldName);
    }

    public static Node createNullNode(String path) {
        return Node.gen(NullNode.getInstance(), path);
    }

    public static Node createMissingNode(String path) {
        return Node.gen(MissingNode.getInstance(), path);
    }
}
