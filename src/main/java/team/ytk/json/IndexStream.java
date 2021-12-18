package team.ytk.json;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;

public class IndexStream {

    @Data
    @AllArgsConstructor
    public static class IndexNode<T> {

        private long index;
        private T node;
        private List<T> nodes;
    }

    public static <T> Stream<IndexNode<T>> add(Stream<T> rawStream) {
        List<T> nodes = rawStream.collect(Collectors.toList());
        return LongStream
            .range(0, nodes.size())
            .mapToObj(index -> new IndexNode<T>(index, nodes.get((int) index), nodes));
    }
}
